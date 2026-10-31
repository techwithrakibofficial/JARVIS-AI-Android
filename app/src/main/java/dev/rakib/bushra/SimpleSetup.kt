package dev.rakib.bushra

data class SimpleService(val id:String,val title:String,val subtitle:String,val presetName:String) {
    fun provider():Provider {
        if(id=="openclaw")return Provider(name="OpenClaw",endpoint="",model="openclaw/default")
        return providerPresets.first { it.name==presetName }.provider().copy(name=title,model="")
    }
}
val simpleServices=listOf(
    SimpleService("openrouter","OpenRouter","Free-first automatic routing","OpenRouter"),
    SimpleService("openai","OpenAI / ChatGPT","OpenAI API key","OpenAI"),
    SimpleService("deepseek","DeepSeek","DeepSeek API key","DeepSeek"),
    SimpleService("gemini","Gemini","Google AI Studio API key","Google Gemini"),
    SimpleService("claude","Claude","Anthropic API key","Anthropic Claude"),
    SimpleService("groq","Groq","Groq API key","Groq"),
    SimpleService("grok","Grok","xAI API key","xAI / Grok"),
    SimpleService("openclaw","OpenClaw","নিজের HTTPS gateway + token","")
)
fun serviceFor(p:Provider):SimpleService?=simpleServices.firstOrNull { s ->
    p.name==s.title || p.name==s.presetName || (s.id=="openclaw" && p.name.equals("OpenClaw",true))
}
fun chatCandidates(ids:List<String>,service:String):List<String> {
    val usable=ids.distinct().filter { id ->
        val s=id.lowercase()
        listOf("embedding","whisper","tts","audio","transcrib","realtime","live","moderation","dall-e","imagen","image","vision-only","codex","research","video","omni-1.","aqa").none { s.contains(it) }
    }
    val filtered=when(service) {
        "openrouter" -> usable.filter { it=="openrouter/free" || it.endsWith(":free") }
        "gemini" -> usable.filter { it.startsWith("gemini-") && (it.contains("flash") || it.contains("pro") || it.contains("argon")) }
        "openai" -> usable.filter { it.startsWith("gpt-") && !it.contains("search") && !it.contains("-pro") }
        "claude" -> usable.filter { it.contains("claude-") }
        else -> usable
    }
    fun score(id:String):Int {
        val s=id.lowercase()
        if(service=="openrouter" && s=="openrouter/free")return 10000
        var score=0
        if(s.contains("lite"))score+=100
        if(s.contains("mini"))score+=90
        if(s.contains("haiku"))score+=90
        if(s.contains("flash"))score+=60
        if(s.contains("fast"))score+=50
        if(s.contains("chat"))score+=40
        if(s.contains("versatile"))score+=40
        if(s.contains("instruct"))score+=30
        if(s.contains("preview")||s.contains("experimental"))score-=25
        if(s.contains("reason")||s.contains("thinking")||s.contains("r1"))score-=40
        return score
    }
    return filtered.sortedWith(compareByDescending<String>{score(it)}.thenByDescending { it })
}
fun femaleVoiceScore(name:String,localeMatch:Boolean,offline:Boolean):Int {
    val n=name.lowercase()
    val female=listOf("female","woman","bnf","fem","f1","girl").any { it in n }
    val male=!female && Regex("(^|[-_])male($|[-_])").containsMatchIn(n)
    return (if(localeMatch)100 else 0)+(if(female)50 else 0)+(if(offline)10 else 0)-(if(male)40 else 0)
}
