package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.*
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ChantalOpenRouterTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun envelope(content: String, finish: String="stop")=JSONObject().put("choices",JSONArray().put(JSONObject()
        .put("finish_reason",finish).put("message",JSONObject().put("content",content)))).toString()
    @Before fun clear() {
        for(name in listOf("chantal_ai_selection","chantal_openrouter_access","chantal_gemini_access")) context.getSharedPreferences(name,0).edit().clear().commit()
    }
    @Test fun onePostHasBoundedOutputNoToolsAndNoProviderFallback() = runBlocking {
        var calls=0
        val dummy="dummy.openrouter.key.for.unit.test"
        val transport=OpenRouterStoryTransport(dummy,http=OpenRouterHttp { body,secret ->
            calls++;assertEquals(dummy,secret)
            val json=JSONObject(body)
            assertEquals("x-ai/grok-4.20",json.getString("model"))
            assertEquals(4096,json.getInt("max_tokens"))
            assertFalse(json.getJSONObject("reasoning").getBoolean("enabled"))
            assertFalse(json.getJSONObject("provider").getBoolean("allow_fallbacks"))
            assertFalse(json.has("tools"));assertFalse(body.contains(dummy))
            envelope("{\"ok\":true}")
        })
        assertTrue(JSONObject(transport.generate("Short connection check")).getBoolean("ok"))
        assertEquals(1,calls)
    }
    @Test fun paidFailureAndCancellationNeverRetry() = runBlocking {
        var count=0
        val failing=OpenRouterStoryTransport("dummy.only.long.enough.key",http=OpenRouterHttp { _,_ -> count++;throw OpenRouterRequestException(402) })
        val error=runCatching { failing.generate("check") }.exceptionOrNull()!!
        assertTrue(error is OpenRouterRequestException)
        assertTrue(storyFailureMessage(error).contains("402"));assertEquals(1,count)
        val cancelled=OpenRouterStoryTransport("dummy.only.long.enough.key",http=OpenRouterHttp { _,_ -> count++;throw CancellationException() })
        assertTrue(runCatching { cancelled.generate("check") }.exceptionOrNull() is CancellationException)
        assertEquals(2,count)
    }
    @Test fun incompleteOrToolResponsesAreRejected() {
        assertTrue(runCatching { OpenRouterStoryTransport.decodeResponse(envelope("{}","length")) }.isFailure)
        assertTrue(runCatching { OpenRouterStoryTransport.decodeResponse("{\"error\":{\"message\":\"private\"}}") }.isFailure)
        val tool=JSONObject(envelope("{}"))
        tool.getJSONArray("choices").getJSONObject(0).getJSONObject("message").put("tool_calls",JSONArray())
        assertTrue(runCatching { OpenRouterStoryTransport.decodeResponse(tool.toString()) }.isFailure)
        for(status in listOf(401,403,429,500)) assertTrue(storyFailureMessage(OpenRouterRequestException(status)).contains(status.toString()))
    }
    @Test fun providersUseSeparateCiphertextAndDefaultDoesNotFallbackToExistingGeminiKey() {
        val testKey=SecretKeySpec(ByteArray(32) { (it+11).toByte() },"AES")
        val g=ChantalGeminiAccess(context) { testKey }
        g.save("gemini.dummy.secret.123456789")
        val selection=ChantalAiAccess(context)
        assertEquals("openrouter",selection.provider())
        assertFalse(selection.credential().hasKey())
        val router=ChantalGeminiAccess(context,"openrouter") { testKey }
        router.save("router.dummy.secret.123456789")
        assertEquals("router.dummy.secret.123456789",router.read())
        assertEquals("gemini.dummy.secret.123456789",g.read())
        assertEquals("x-ai/grok-4.20",router.model())
        assertFalse(context.getSharedPreferences("chantal_openrouter_access",0).all.values.any { it.toString().contains("router.dummy.secret") })
        assertTrue(selection.claimRequest(100000));assertFalse(selection.claimRequest(100001))
        selection.select("gemini");assertFalse(selection.claimRequest(100002))
        assertTrue(selection.claimRequest(160001))
    }
    @Test fun cancellationClosesTheActualBlockedSocketAdapter() = runBlocking {
        val entered=java.util.concurrent.CountDownLatch(1)
        val disconnected=java.util.concurrent.CountDownLatch(1)
        val connection=object : javax.net.ssl.HttpsURLConnection(java.net.URL("https://openrouter.ai/api/v1/chat/completions")) {
            override fun connect() {}
            override fun disconnect() { disconnected.countDown() }
            override fun usingProxy()=false
            override fun getCipherSuite()="FAKE"
            override fun getLocalCertificates(): Array<java.security.cert.Certificate>?=null
            override fun getServerCertificates(): Array<java.security.cert.Certificate> = emptyArray()
            override fun getOutputStream(): java.io.OutputStream=java.io.ByteArrayOutputStream()
            override fun getResponseCode()=200
            override fun getInputStream(): java.io.InputStream=object : java.io.InputStream() {
                override fun read(): Int {
                    entered.countDown()
                    check(disconnected.await(5,java.util.concurrent.TimeUnit.SECONDS))
                    throw java.io.IOException("closed test socket")
                }
            }
        }
        val client=OpenRouterHttpClient { connection }
        val job=launch { client.post("{}","dummy.secret") }
        assertTrue(withContext(Dispatchers.IO) { entered.await(5,java.util.concurrent.TimeUnit.SECONDS) })
        withTimeout(2000) { job.cancelAndJoin() }
        assertEquals(0,disconnected.count)
    }

}
