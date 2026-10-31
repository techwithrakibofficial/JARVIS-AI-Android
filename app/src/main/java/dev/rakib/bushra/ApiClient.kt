package dev.rakib.bushra

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ProviderFailure(val statusCode:Int,message:String):IOException(message)

class ApiClient(private val client:OkHttpClient=OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(90,TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()) {
    @Volatile private var running:Call?=null
    fun cancel() { running?.cancel() }
    private fun request(p:Provider,key:String,url:String,body:JSONObject?=null):Request {
        val u=url.toHttpUrlOrNull() ?: error("সঠিক HTTPS endpoint দিন।")
        require(u.isHttps && u.username.isEmpty() && u.password.isEmpty()) { "শুধু HTTPS endpoint ব্যবহার করুন।" }
        require(p.authHeader.matches(Regex("[A-Za-z0-9-]+"))) { "Auth header name ঠিক করুন।" }
        val builder=Request.Builder().url(u).header("Accept","application/json")
        if(key.isNotBlank()) builder.header(p.authHeader,p.authPrefix+key)
        if(p.protocol==ApiProtocol.ANTHROPIC)builder.header("anthropic-version","2023-06-01")
        if(body!=null)builder.post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        return builder.build()
    }
    private fun execute(request:Request):ByteArray {
        val call=client.newCall(request);running=call
        try {
            return call.execute().use { response ->
                if(!response.isSuccessful) throw ProviderFailure(response.code,when(response.code) {
                    401,403 -> "API অনুমতি নেই (${response.code})। Key, endpoint ও model access পরীক্ষা করুন।"
                    402 -> "Provider credit/quota শেষ। Free model বা অন্য provider বেছে নিন।"
                    429 -> "Provider rate limit। একটু পরে চেষ্টা করুন বা নিজে অন্য provider বেছে নিন।"
                    else -> "Provider HTTP ${response.code}। Endpoint/model পরীক্ষা করুন।"
                })
                val body=response.body ?: error("Provider খালি উত্তর দিয়েছে।")
                require(body.contentLength()<=8*1024*1024) { "Provider response খুব বড়।" }
                body.byteStream().use { input ->
                    val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                    while(true) { val count=input.read(buffer);if(count<0)break;require(output.size()+count<=8*1024*1024) { "Provider response খুব বড়।" };output.write(buffer,0,count) }
                    output.toByteArray()
                }
            }
        } finally { if(running===call)running=null }
    }
    suspend fun chat(p:Provider,key:String,system:String,turns:List<ChatLine>,limit:Int=1600):String=withContext(Dispatchers.IO) {
        require(p.isChat) { "Chat provider নির্বাচন করুন।" };require(p.model.isNotBlank()) { "Settings থেকে আবার Connect করুন।" }
        val recent=turns.takeLast(20)
        val body:JSONObject;val url:String
        when(p.protocol) {
            ApiProtocol.GEMINI -> {
                require(p.model.removePrefix("models/").matches(Regex("[a-zA-Z0-9._-]+"))) { "সঠিক Gemini model ID দিন।" }
                url=p.endpoint.trimEnd('/')+"/models/"+p.model.removePrefix("models/")+":generateContent"
                val contents=JSONArray()
                recent.forEach { line -> contents.put(JSONObject().put("role",if(line.who=="assistant")"model" else "user").put("parts",JSONArray().put(JSONObject().put("text",line.text)))) }
                body=JSONObject().put("systemInstruction",JSONObject().put("parts",JSONArray().put(JSONObject().put("text",system))))
                    .put("contents",contents).put("generationConfig",JSONObject().put("maxOutputTokens",limit))
            }
            ApiProtocol.ANTHROPIC -> {
                url=p.endpoint
                val messages=JSONArray();recent.forEach { messages.put(JSONObject().put("role",if(it.who=="assistant")"assistant" else "user").put("content",it.text)) }
                body=JSONObject().put("model",p.model).put("system",system).put("max_tokens",limit).put("messages",messages)
            }
            else -> {
                url=p.endpoint
                val messages=JSONArray().put(JSONObject().put("role","system").put("content",system))
                recent.forEach { messages.put(JSONObject().put("role",if(it.who=="assistant")"assistant" else "user").put("content",it.text)) }
                val budgetKey=if(p.endpoint.contains("api.openai.com") && (p.model.startsWith("gpt-5")||p.model.startsWith("gpt-6")||p.model.startsWith("o1")||p.model.startsWith("o3")))"max_completion_tokens" else "max_tokens"
                body=JSONObject().put("model",p.model).put("messages",messages).put(budgetKey,limit).put("stream",false)
            }
        }
        val data=JSONObject(String(execute(request(p,key,url,body)),Charsets.UTF_8))
        when(p.protocol) {
            ApiProtocol.GEMINI -> data.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.let { a -> (0 until a.length()).joinToString("") { a.getJSONObject(it).optString("text") } }
            ApiProtocol.ANTHROPIC -> data.optJSONArray("content")?.let { a -> (0 until a.length()).joinToString("") { a.getJSONObject(it).optString("text") } }
            else -> data.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.opt("content")?.let { meaningfulReply(it) }
        }?.takeIf { meaningfulReply(it).isNotBlank() } ?: error("Model থেকে text পাওয়া যায়নি। Model capability পরীক্ষা করুন।")
    }
    suspend fun models(p:Provider,key:String):List<String> = withContext(Dispatchers.IO) {
        require(p.protocol==ApiProtocol.OPENAI_CHAT) { "এই protocol-এ model ID নিজে লিখুন।" }
        require(p.endpoint.endsWith("/chat/completions")) { "Model list পেতে endpoint /chat/completions দিয়ে শেষ হতে হবে।" }
        val j=JSONObject(String(execute(request(p,key,p.endpoint.removeSuffix("/chat/completions")+"/models")),Charsets.UTF_8))
        val a=j.optJSONArray("data") ?: error("এই API model list দেয়নি। Model ID নিজে লিখুন।")
        (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optString("id") }.filter { it.isNotBlank() }.sorted()
    }
    suspend fun automaticModels(p:Provider,key:String,service:String):List<String> = withContext(Dispatchers.IO) {
        if(service=="openclaw")return@withContext listOf("openclaw/default")
        val ids=when(p.protocol) {
            ApiProtocol.GEMINI -> {
                val j=JSONObject(String(execute(request(p,key,p.endpoint.trimEnd('/')+"/models?pageSize=1000")),Charsets.UTF_8))
                val a=j.optJSONArray("models") ?: error("Gemini model list পাওয়া যায়নি।")
                (0 until a.length()).mapNotNull { i -> val m=a.getJSONObject(i)
                    if(m.optJSONArray("supportedGenerationMethods")?.strings()?.contains("generateContent")==true)m.optString("name").removePrefix("models/") else null }
            }
            ApiProtocol.ANTHROPIC -> {
                val url=p.endpoint.removeSuffix("/messages")+"/models?limit=100"
                val a=JSONObject(String(execute(request(p,key,url)),Charsets.UTF_8)).optJSONArray("data") ?: error("Claude model list পাওয়া যায়নি।")
                (0 until a.length()).map { a.getJSONObject(it).getString("id") }
            }
            else -> models(p,key)
        }
        chatCandidates(ids,service).also { require(it.isNotEmpty()) { if(service=="openrouter")"এখন free chat model পাওয়া যায়নি। পরে আবার Connect করুন বা অন্য provider বেছে নিন।" else "এই key-এ ব্যবহারযোগ্য chat model পাওয়া যায়নি। Provider access পরীক্ষা করুন।" } }
    }
    suspend fun connectAutomatically(p:Provider,key:String,service:String):Provider {
        require(key.isNotBlank()) { "API key দিন।" }
        val ids=automaticModels(p,key,service)
        var last:Exception?=null
        for(id in ids.take(3)) {
            val candidate=p.copy(model=id)
            try {
                chat(candidate,key,"Return only the word OK.",listOf(ChatLine("user","Connection test. Say OK.")),320)
                return candidate
            } catch(e:ProviderFailure) { if(e.statusCode !in listOf(400,404))throw e;last=e }
        }
        throw (last ?: IOException("সংযোগ পরীক্ষা হয়নি।"))
    }
    suspend fun speech(p:Provider,key:String,text:String):ByteArray=withContext(Dispatchers.IO) {
        require(!p.isChat) { "Voice provider নির্বাচন করুন।" }
        val body:JSONObject;val url:String
        if(p.protocol==ApiProtocol.ELEVENLABS_TTS) {
            require(p.voice.matches(Regex("[a-zA-Z0-9_-]+"))) { "Voice ID সঠিক নয়।" }
            url=p.endpoint.trimEnd('/')+"/"+p.voice
            body=JSONObject().put("text",text.take(2500)).put("model_id",p.model)
        } else {
            url=p.endpoint;body=JSONObject().put("model",p.model).put("input",text.take(2500)).put("voice",p.voice).put("response_format","mp3")
        }
        execute(request(p,key,url,body).newBuilder().header("Accept","audio/mpeg").build())
    }
}

fun parseEnvelope(raw:String):JSONObject? {
    val clean=raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    return runCatching { JSONObject(clean) }.getOrNull()
}
