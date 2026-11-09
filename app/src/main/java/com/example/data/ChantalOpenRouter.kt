package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** All requests are manual, one POST, no retry or provider fallback. */
fun interface OpenRouterHttp { suspend fun post(body: String, secret: String): String }
class OpenRouterRequestException(val status: Int) : Exception("OpenRouter request failed")
class NarrativeValidationException : Exception("Story response failed validation")
class OpenRouterStoryTransport(private val secret: String, private val model: String = DEFAULT_MODEL,
    private val http: OpenRouterHttp = OpenRouterHttpClient()) : ChantalStoryTransport {
    override suspend fun generate(prompt: String): String {
        require(model==DEFAULT_MODEL)
        ChantalGeminiAccess.validateSecret(secret)
        val response=http.post(body(prompt).toString(),secret)
        return try { decodeResponse(response) } catch(_: Exception) { throw NarrativeValidationException() }
    }
    companion object {
        const val DEFAULT_MODEL = "x-ai/grok-4.20"
        fun body(prompt: String): JSONObject {
            require(prompt.length<=48000)
            return JSONObject().put("model",DEFAULT_MODEL)
                .put("messages",JSONArray().put(JSONObject().put("role","user").put("content",prompt)))
                .put("stream",false).put("max_tokens",4096).put("temperature",0.85)
                .put("reasoning",JSONObject().put("enabled",false))
                .put("response_format",JSONObject().put("type","json_object"))
                .put("provider",JSONObject().put("allow_fallbacks",false))
        }
        fun decodeResponse(raw: String): String {
            require(raw.length<=131072)
            val root=JSONObject(raw)
            require(!root.has("error"))
            val choices=root.getJSONArray("choices")
            require(choices.length()==1)
            val choice=choices.getJSONObject(0)
            require(choice.getString("finish_reason")=="stop")
            val message=choice.getJSONObject("message")
            require(message.optJSONArray("tool_calls")==null)
            val text=message.get("content")
            require(text is String && text.isNotBlank() && text.length<=32000)
            return text
        }
    }
}
internal class OpenRouterHttpClient(private val connectionFactory: () -> HttpsURLConnection = {
    URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpsURLConnection
}) : OpenRouterHttp {
    override suspend fun post(body: String, secret: String): String = kotlinx.coroutines.coroutineScope {
        val connection=connectionFactory()
        // Cancellation of a blocking socket read needs disconnect, not only Thread.interrupt.
        val closer=launch(context=kotlinx.coroutines.Dispatchers.IO,start=kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try { kotlinx.coroutines.awaitCancellation() } finally { connection.disconnect() }
        }
        try { withContext(Dispatchers.IO) {
            connection.requestMethod="POST";connection.instanceFollowRedirects=false
            connection.connectTimeout=10000;connection.readTimeout=60000
            connection.doOutput=true
            connection.setRequestProperty("Content-Type","application/json; charset=utf-8")
            connection.setRequestProperty("Authorization","Bearer $secret")
            val bytes=body.toByteArray(Charsets.UTF_8)
            // Streaming mode prevents HttpURLConnection from replaying a buffered paid POST.
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
            val status=connection.responseCode
            if(status!=200) throw OpenRouterRequestException(status)
            connection.inputStream.use { input ->
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096)
                while(true) {
                    val count=input.read(buffer);if(count<0)break
                    require(out.size()+count<=131072)
                    out.write(buffer,0,count)
                }
                out.toString("UTF-8")
            }
        } } catch(error: Exception) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            throw error
        } finally { closer.cancel();connection.disconnect() }
    }
}

/** Provider selection is device configuration, not game canon or backup content. */
class ChantalAiAccess(private val context: Context) {
    private val prefs=context.getSharedPreferences("chantal_ai_selection",Context.MODE_PRIVATE)
    fun provider()=prefs.getString("provider","openrouter")?.takeIf { it in setOf("openrouter","gemini") } ?: "openrouter"
    fun select(provider: String) {
        require(provider in setOf("openrouter","gemini"))
        check(prefs.edit().putString("provider",provider).commit())
    }
    fun credential()=ChantalGeminiAccess(context,provider())
    fun label(selected: String = provider())=if(selected=="openrouter") "Grok · OpenRouter" else "Gemini"
    fun claimRequest(now: Long=System.currentTimeMillis()): Boolean = synchronized(lock) {
        val last=prefs.getLong("lastRequest",0)
        if(last>0 && now-last<60000) false else prefs.edit().putLong("lastRequest",now).commit()
    }
    suspend fun transport(selected: String = provider()): ChantalStoryTransport = withContext(Dispatchers.IO) {
        require(selected in setOf("openrouter","gemini"));val access=ChantalGeminiAccess(context,selected)
        val secret=access.read()
        if(selected=="openrouter") OpenRouterStoryTransport(secret) else GeminiStoryTransport(secret,access.model())
    }
    companion object { private val lock=Any() }
}
fun storyFailureMessage(error: Throwable): String = when(error) {
    is OpenRouterRequestException -> when(error.status) {
        401,403 -> "OpenRouter отклонил ключ или доступ к модели (HTTP ${error.status}). Проверь настройки ключа."
        402 -> "Недостаточно средств или лимита ключа OpenRouter (HTTP 402)."
        404 -> "Grok сейчас недоступен по выбранному ID (HTTP 404). Автоматической замены модели нет."
        429 -> "OpenRouter ограничил запросы (HTTP 429). Попробуй позже."
        else -> "OpenRouter временно недоступен (HTTP ${error.status}). Повтори вручную позже."
    }
    is NarrativeValidationException -> "Ответ не прошёл проверку. Сохранённая история не изменилась; повторный запрос не отправлялся."
    is java.io.IOException -> "Не удалось связаться с моделью. Проверь интернет. Сохранённая история осталась доступной."
    is GeminiRequestException, is GeminiLocalQuotaException -> geminiFailureMessage(error)
    else -> "Не удалось завершить запрос. Проверь ключ и настройки. Код PROVIDER_CHECK. Сохранённая история осталась доступной."
}
