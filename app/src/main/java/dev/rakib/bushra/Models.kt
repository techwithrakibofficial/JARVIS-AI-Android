package dev.rakib.bushra

import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.UUID

enum class ApiProtocol { OPENAI_CHAT, GEMINI, ANTHROPIC, ELEVENLABS_TTS, OPENAI_TTS }
data class Provider(
    val id: String = UUID.randomUUID().toString(), val name: String = "", val endpoint: String = "",
    val model: String = "", val protocol: ApiProtocol = ApiProtocol.OPENAI_CHAT,
    val authHeader: String = "Authorization", val authPrefix: String = "Bearer ",
    val voice: String = "75CHIF904aWz6uq77J3P"
) {
    val isChat get() = protocol in listOf(ApiProtocol.OPENAI_CHAT, ApiProtocol.GEMINI, ApiProtocol.ANTHROPIC)
    fun json() = JSONObject().put("id", id).put("name", name).put("endpoint", endpoint).put("model", model)
        .put("protocol", protocol.name).put("authHeader", authHeader).put("authPrefix", authPrefix).put("voice", voice)
    companion object {
        fun parse(j: JSONObject) = Provider(j.getString("id"), j.getString("name"), j.getString("endpoint"), j.optString("model"),
            ApiProtocol.valueOf(j.getString("protocol")), j.optString("authHeader", "Authorization"), j.optString("authPrefix", "Bearer "), j.optString("voice", "75CHIF904aWz6uq77J3P"))
    }
}
data class Preset(val name: String, val endpoint: String, val model: String, val protocol: ApiProtocol, val purpose: String) {
    fun provider() = Provider(name=name, endpoint=endpoint, model=model, protocol=protocol,
        authHeader=when(protocol) { ApiProtocol.GEMINI -> "x-goog-api-key"; ApiProtocol.ANTHROPIC -> "x-api-key"; ApiProtocol.ELEVENLABS_TTS -> "xi-api-key"; else -> "Authorization" },
        authPrefix=if(protocol in listOf(ApiProtocol.OPENAI_CHAT, ApiProtocol.OPENAI_TTS)) "Bearer " else "")
}
val providerPresets = listOf(
    Preset("OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "", ApiProtocol.OPENAI_CHAT, "Chat, planning ও tool actions। Model ID বেছে দিন; free availability পরিবর্তন হয়।"),
    Preset("OpenAI", "https://api.openai.com/v1/chat/completions", "", ApiProtocol.OPENAI_CHAT, "Chat ও tools। API billing আলাদা; নিজের model ID দিন।"),
    Preset("Google Gemini", "https://generativelanguage.googleapis.com/v1beta", "", ApiProtocol.GEMINI, "Gemini generateContent। নিজের উপলব্ধ model ID দিন।"),
    Preset("Anthropic Claude", "https://api.anthropic.com/v1/messages", "", ApiProtocol.ANTHROPIC, "Chat ও code planning। নিজের উপলব্ধ model ID দিন।"),
    Preset("Groq", "https://api.groq.com/openai/v1/chat/completions", "", ApiProtocol.OPENAI_CHAT, "Compatible chat ও tool-capable models।"),
    Preset("DeepSeek", "https://api.deepseek.com/chat/completions", "deepseek-chat", ApiProtocol.OPENAI_CHAT, "Chat ও planning; provider quota প্রযোজ্য।"),
    Preset("xAI / Grok", "https://api.x.ai/v1/chat/completions", "", ApiProtocol.OPENAI_CHAT, "Chat। নিজের উপলব্ধ model ID দিন।"),
    Preset("ElevenLabs", "https://api.elevenlabs.io/v1/text-to-speech", "eleven_multilingual_v2", ApiProtocol.ELEVENLABS_TTS, "শুধু voice output। Voice ও language availability Test করে নিন; chat API নয়।"),
    Preset("OpenAI Speech", "https://api.openai.com/v1/audio/speech", "tts-1", ApiProtocol.OPENAI_TTS, "শুধু voice output। Voice field-এ alloy বা নিজের supported voice দিন।"),
    Preset("Custom API", "", "", ApiProtocol.OPENAI_CHAT, "নিজের নাম, HTTPS endpoint, protocol ও auth header দিন। যেকোনো নাম চলে; API-কে নির্বাচিত protocol সমর্থন করতে হবে।")
)
data class ContactAlias(val id: String = UUID.randomUUID().toString(), val name: String, val phone: String, val aliases: List<String>) {
    fun json() = JSONObject().put("id",id).put("name",name).put("phone",phone).put("aliases",JSONArray(aliases))
    companion object { fun parse(j:JSONObject) = ContactAlias(j.getString("id"),j.getString("name"),j.getString("phone"), j.getJSONArray("aliases").strings()) }
}
data class ChatLine(val who: String, val text: String, val time: Long = System.currentTimeMillis()) {
    fun json() = JSONObject().put("who",who).put("text",text).put("time",time)
}
data class TaskEntry(val text:String, val status:String, val time:Long=System.currentTimeMillis()) {
    fun json() = JSONObject().put("text",text).put("status",status).put("time",time)
}
fun JSONArray.strings() = (0 until length()).map { getString(it) }
fun normalizedName(value: String): String = Normalizer.normalize(value.lowercase().trim(), Normalizer.Form.NFKC)
    .replace(Regex("[\\p{P}\\p{Z}\\s]+"), " ").trim()
fun normalizedPhone(value: String): String = buildString {
    value.forEach { ch -> when { ch in '0'..'9' -> append(ch); ch in '০'..'৯' -> append('0'+(ch-'০')); ch=='+' && isEmpty() -> append(ch) } }
}
fun validPhone(value: String) = Regex("\\+?[0-9]{7,15}").matches(normalizedPhone(value))
fun resolveContacts(query: String, contacts: List<ContactAlias>): List<ContactAlias> {
    val q = normalizedName(query)
    if (q.isBlank()) return emptyList()
    val phone=normalizedPhone(query)
    val exact=contacts.filter { c -> (validPhone(query) && normalizedPhone(c.phone)==phone) || (c.aliases+c.name).any { normalizedName(it)==q } }
    if (exact.isNotEmpty()) return exact.distinctBy { normalizedPhone(it.phone) }
    return contacts.filter { c -> (c.aliases+c.name).any { normalizedName(it).contains(q) } }.distinctBy { normalizedPhone(it.phone) }
}
fun aliasConflict(candidate:ContactAlias, contacts:List<ContactAlias>):Boolean {
    val names=(candidate.aliases+candidate.name).map(::normalizedName).filter { it.isNotBlank() }.toSet()
    return contacts.any { c -> c.id!=candidate.id && normalizedPhone(c.phone)!=normalizedPhone(candidate.phone) && (c.aliases+c.name).any { normalizedName(it) in names } }
}
fun safeProjectPath(path:String):Boolean = path.isNotBlank() && path.length<=180 && path.none { it.code<32 } && !path.startsWith('/') && !path.contains('\\') && !path.contains(':') && path.split('/').none { it==".." || it=="." || it.isBlank() || it==".git" } && !path.endsWith(".keystore") && !path.endsWith(".jks")
