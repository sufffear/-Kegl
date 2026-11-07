package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

/** Device-local credential, deliberately outside the game snapshot and backup archive. */
class ChantalGeminiAccess(context: Context, private val keyProvider: (() -> SecretKey)? = null) {
    private val prefs = context.getSharedPreferences("chantal_gemini_access", Context.MODE_PRIVATE)
    private val alias = "velaris.gemini.personal.v1"
    private fun key(): SecretKey {
        keyProvider?.let { return it() }
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun hasKey() = prefs.contains("encrypted")
    fun save(secret: String) {
        validateSecret(secret)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("encrypted", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).commit())
    }
    fun read(): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)))
        }
        return String(cipher.doFinal(Base64.decode(prefs.getString("encrypted", ""), Base64.NO_WRAP)), Charsets.UTF_8)
    }
    fun remove() { check(prefs.edit().remove("encrypted").remove("iv").commit()) }
    fun model() = prefs.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL
    fun setModel(model: String) {
        require(model.matches(Regex("gemini-[a-zA-Z0-9.-]{1,80}")))
        check(prefs.edit().putString("model", model).commit())
    }
    /** A local throttle only, never a claim about the Google project's billing tier. */
    fun claimRequest(now: Long = System.currentTimeMillis()): Boolean {
        val last = prefs.getLong("lastRequest", 0)
        if (last > 0 && now - last < 60_000) return false
        return prefs.edit().putLong("lastRequest", now).commit()
    }
    companion object {
        const val DEFAULT_MODEL = "gemini-3.1-flash-lite"
        fun validateSecret(secret: String) {
            require(secret.length in 20..2048 && secret.all { it.code in 33..126 && it !in "\"'\\" }) {
                "Invalid credential format"
            }
        }
    }
}

class GeminiStoryTransport(private val secret: String, private val model: String) : ChantalStoryTransport {
    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        require(model.matches(Regex("gemini-[a-zA-Z0-9.-]{1,80}")))
        val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 45_000
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("x-goog-api-key", secret)
            connection.doOutput = true
            val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                .put("generationConfig", JSONObject().put("responseMimeType", "application/json").put("maxOutputTokens", 4096))
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            // Never surface error bodies, request headers or credentials in UI/logs.
            val status = connection.responseCode
            if (status != 200) throw GeminiRequestException(status)
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 131_072)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            require(bytes.size <= 131_072)
            decodeResponse(String(bytes, Charsets.UTF_8))
        } finally { connection.disconnect() }
    }
    companion object {
        fun decodeResponse(raw: String): String {
            val candidate = JSONObject(raw).getJSONArray("candidates").getJSONObject(0)
            require(candidate.getString("finishReason") == "STOP")
            val parts = candidate.getJSONObject("content").getJSONArray("parts")
            return (0 until parts.length()).map { parts.getJSONObject(it) }
                .filter { !it.optBoolean("thought", false) }.joinToString("") { it.optString("text", "") }
                .also { require(it.isNotBlank() && it.length <= 32_000) }
        }
    }
}

class GeminiLocalQuotaException : Exception("Local request limit")
class GeminiRequestException(val status: Int) : Exception("Gemini unavailable")
fun geminiFailureMessage(error: Throwable): String = when (error) {
    is GeminiLocalQuotaException -> "Между запросами нужна минута. Подожди немного и попробуй снова."
    is GeminiRequestException -> when (error.status) {
        400, 401, 403 -> "Google отклонил запрос. Проверь ключ, доступ к Gemini API и ограничения проекта (HTTP ${error.status})."
        404 -> "Модель не найдена (HTTP 404). В настройках укажи полный ID модели из AI Studio или верни предложенную модель."
        429 -> "Лимит запросов исчерпан (HTTP 429). Подожди и проверь квоту проекта в AI Studio."
        else -> "Сервис Gemini временно недоступен (HTTP ${error.status}). Попробуй позже."
    }
    is java.io.IOException -> "Не удалось связаться с Gemini. Проверь интернет и попробуй позже."
    else -> "Не удалось проверить подключение. Сохрани ключ заново; если повторится — сообщи код CONNECTION_CHECK."
}
