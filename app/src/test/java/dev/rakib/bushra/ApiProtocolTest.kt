package dev.rakib.bushra

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ApiProtocolTest {
    private lateinit var server:MockWebServer
    private lateinit var api:ApiClient
    @Before fun setup(){
        val certificate=HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverTls=HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls=HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        server=MockWebServer().apply {useHttps(serverTls.sslSocketFactory(),false);start()}
        api=ApiClient(OkHttpClient.Builder().sslSocketFactory(clientTls.sslSocketFactory(),clientTls.trustManager).followRedirects(false).build())
    }
    @After fun close(){server.shutdown()}
    @Test fun openAiCompatibleRoundTrip()=runBlocking {
        server.enqueue(MockResponse().setBody("{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}"))
        val p=Provider(name="custom",endpoint=server.url("/chat/completions").toString(),model="private-model")
        assertEquals("hello",api.chat(p,"test-secret","system",listOf(ChatLine("user","Hi"))))
        val request=server.takeRequest();assertEquals("Bearer test-secret",request.getHeader("Authorization"))
        val body=JSONObject(request.body.readUtf8());assertEquals("private-model",body.getString("model"));assertFalse(body.getBoolean("stream"));assertEquals("system",body.getJSONArray("messages").getJSONObject(0).getString("role"))
    }
    @Test fun geminiUsesNativeHeaderAndContents()=runBlocking {
        server.enqueue(MockResponse().setBody("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"বাংলা\"}]}}]}"))
        val p=Provider(endpoint=server.url("/v1beta").toString(),model="gemini-test",protocol=ApiProtocol.GEMINI,authHeader="x-goog-api-key",authPrefix="")
        assertEquals("বাংলা",api.chat(p,"gem-key","system",listOf(ChatLine("user","Hi"))))
        val r=server.takeRequest();assertEquals("/v1beta/models/gemini-test:generateContent",r.path);assertEquals("gem-key",r.getHeader("x-goog-api-key"));assertTrue(JSONObject(r.body.readUtf8()).has("systemInstruction"))
    }
    @Test fun anthropicUsesMessagesAndVersionHeader()=runBlocking {
        server.enqueue(MockResponse().setBody("{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}"))
        val p=Provider(endpoint=server.url("/v1/messages").toString(),model="claude-test",protocol=ApiProtocol.ANTHROPIC,authHeader="x-api-key",authPrefix="")
        assertEquals("ok",api.chat(p,"anth-key","system",listOf(ChatLine("user","Hi"))))
        val r=server.takeRequest();assertEquals("2023-06-01",r.getHeader("anthropic-version"));assertEquals("anth-key",r.getHeader("x-api-key"));assertEquals("system",JSONObject(r.body.readUtf8()).getString("system"))
    }
    @Test fun modelDiscoveryUsesSameAuth()=runBlocking {
        server.enqueue(MockResponse().setBody("{\"data\":[{\"id\":\"b\"},{\"id\":\"a:free\"}]}"))
        val p=Provider(endpoint=server.url("/v1/chat/completions").toString())
        assertEquals(listOf("a:free","b"),api.models(p,"key"));val r=server.takeRequest();assertEquals("/v1/models",r.path);assertEquals("GET",r.method)
    }
    @Test fun quotaErrorDoesNotLeakServerBodyOrKey()=runBlocking {
        server.enqueue(MockResponse().setResponseCode(402).setBody("test-secret some provider detail"))
        val p=Provider(endpoint=server.url("/chat/completions").toString(),model="model")
        val e=runCatching{api.chat(p,"test-secret","",listOf(ChatLine("user","Hi")))}.exceptionOrNull()
        assertNotNull(e);assertFalse(e!!.message.orEmpty().contains("test-secret"));assertTrue(e.message.orEmpty().contains("credit"))
    }
    @Test fun insecureEndpointRejectedBeforeNetwork()=runBlocking {
        val p=Provider(endpoint="http://localhost:12345/chat/completions",model="model")
        assertTrue(runCatching{api.chat(p,"secret","",listOf(ChatLine("user","Hi")))}.isFailure)
    }
    @Test fun elevenLabsUsesVoicePathAndOwnKey()=runBlocking {
        server.enqueue(MockResponse().setBody("audio"))
        val p=Provider(endpoint=server.url("/v1/text-to-speech").toString(),model="tts-model",protocol=ApiProtocol.ELEVENLABS_TTS,authHeader="xi-api-key",authPrefix="",voice="voice-id")
        assertEquals("audio",String(api.speech(p,"voice-key","Hello")));val r=server.takeRequest();assertEquals("/v1/text-to-speech/voice-id",r.path);assertEquals("voice-key",r.getHeader("xi-api-key"));assertEquals("tts-model",JSONObject(r.body.readUtf8()).getString("model_id"))
    }
}
