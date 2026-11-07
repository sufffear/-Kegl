package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChantalGeminiAccessTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    // Test key provider exercises cipher/storage; hardware Android Keystore still needs a device check.
    private val key = SecretKeySpec(ByteArray(32) { (it + 7).toByte() }, "AES")
    @Before fun clear() { context.getSharedPreferences("chantal_gemini_access", 0).edit().clear().commit() }
    @Test fun punctuatedCredentialRoundTripsWithoutPlaintextOrModelDependency() {
        val secret = "test.only.ABC_def-1234567890/+=:XYZ"
        val access = ChantalGeminiAccess(context) { key }
        access.setModel("gemini-3.8")
        access.save(secret)
        val recreated = ChantalGeminiAccess(context) { key }
        assertEquals(secret, recreated.read())
        assertTrue(recreated.hasKey())
        val prefs = context.getSharedPreferences("chantal_gemini_access", 0)
        assertFalse(prefs.all.values.any { it.toString().contains(secret) })
        assertFalse(ChantalProgressRepository(context).export().contains(secret))
        val firstCiphertext = prefs.getString("encrypted", null)
        access.save(secret)
        assertNotEquals(firstCiphertext, prefs.getString("encrypted", null))
        access.remove()
        assertFalse(recreated.hasKey())
        assertEquals("gemini-3.8", recreated.model())
    }
    @Test fun rejectedInputAndFailedEncryptionKeepPriorCredential() {
        val access = ChantalGeminiAccess(context) { key }
        val secret = "test.only.valid.123456789012345"
        access.save(secret)
        for (invalid in listOf("short", "$secret\nextra", "\"$secret\"")) {
            assertTrue(runCatching { access.save(invalid) }.isFailure)
            assertEquals(secret, access.read())
        }
        val failed = ChantalGeminiAccess(context) { error("hardware unavailable") }
        assertTrue(runCatching { failed.save(secret) }.isFailure)
        assertEquals(secret, access.read())
    }
    @Test fun connectionMessagesDistinguishQuotaAndModelWithoutEchoingServerBody() {
        assertTrue(geminiFailureMessage(GeminiRequestException(404)).contains("Модель"))
        assertTrue(geminiFailureMessage(GeminiRequestException(429)).contains("Лимит"))
        assertTrue(geminiFailureMessage(GeminiRequestException(403)).contains("HTTP 403"))
    }
}
