package dev.rakib.bushra

import java.net.URI
import org.json.JSONObject

data class DeviceCommand(val tool:String,val args:JSONObject)
fun validSmsSubscription(id:Int)=id>=0 && id!=Int.MAX_VALUE
fun whatsappMention(text:String)=Regex("whats\\s*app|হো[য়য়]া?টস?অ?্যা?প|হোয়াটসঅ্যাপ|হোয়াটসঅ্যাপ|ওয়াটসঅ্যাপ|ওয়াটসঅ্যাপ",RegexOption.IGNORE_CASE).containsMatchIn(text)
fun appPackages(name:String):List<String> {
    val n=normalizedName(name).replace(" ","")
    return when {
        whatsappMention(name)||n=="wa" -> listOf("com.whatsapp","com.whatsapp.w4b")
        n in listOf("messenger","মেসেঞ্জার","ম্যাসেঞ্জার") -> listOf("com.facebook.orca","com.facebook.mlite")
        n in listOf("facebook","ফেসবুক") -> listOf("com.facebook.katana","com.facebook.lite")
        n in listOf("youtube","ইউটিউব") -> listOf("com.google.android.youtube")
        n in listOf("chrome","ক্রোম") -> listOf("com.android.chrome")
        n in listOf("settings","সেটিংস","সেটিং") -> listOf("com.android.settings")
        n in listOf("telegram","টেলিগ্রাম") -> listOf("org.telegram.messenger")
        n in listOf("messages","message","মেসেজ","মেসেজেস","sms") -> listOf("com.google.android.apps.messaging","com.android.mms")
        else -> emptyList()
    }
}
/** Only remove command scaffolding at the beginning, never words inside a message. */
fun cleanCommandTarget(value:String):String {
    var t=value.trim()
    val prefix=Regex("^(?:(?:আচ্ছা|তুমি|প্লিজ|সোনা|দয়া করে|দয়া করে|ওপেন করো|ওপেন কর|ওপেন করে|ওপেন|খুলে|খোলো|এবং|অ্যান্ড|and|করো|কর|তে|এ)\\s+)+",RegexOption.IGNORE_CASE)
    repeat(3){t=t.replace(prefix,"").trim()}
    return t
}
/** Local routing has priority over generative text. WhatsApp is never downgraded to carrier SMS. */
fun routeCommand(input:String):DeviceCommand? {
    val text=input.trim().trimEnd('।','.','!','?')
    val wa=whatsappMention(text)
    val sms=Regex("এসএমএস|এস এম এস|\\bsms\\b",RegexOption.IGNORE_CASE).containsMatchIn(text)
    // An auto-chat instruction must go to the rule editor, never become a message body.
    if(Regex("অটো.?রিপ্লাই|auto.?reply|auto.?chat|তার.*রিপ্লাই|সে.*রিপ্লাই",RegexOption.IGNORE_CASE).containsMatchIn(text))return null
    if(wa || sms) {
        val withoutChannel=if(wa)text.replace(Regex("whats\\s*app|হোয়াটসঅ্যাপ|হোয়াটসঅ্যাপ|ওয়াটসঅ্যাপ|ওয়াটসঅ্যাপ",RegexOption.IGNORE_CASE),"") else text
        val clean=cleanCommandTarget(withoutChannel)
        val match=Regex("^(.+?)(?:\\s+(?:কে|কাছে|নাম্বারে|নম্বরে|আইডিতে)|কে)\\s+(.+?)\\s+(?:লিখে\\s+)?(?:(?:এসএমএস|এস এম এস|sms|মেসেজ|ম্যাসেজ|message)\\s+)?(?:(?:সেন্ড|send|পাঠিয়ে|পাঠিয়ে)\\s+)?(?:করো|কর|দাও|দিন|পাঠাও)$",RegexOption.IGNORE_CASE).find(clean)
        if(match!=null) {
            val target=cleanCommandTarget(match.groupValues[1]);val body=match.groupValues[2].trim().removeSuffix(" লিখে").trim(' ','"','“','”','\'')
            if(target.isNotBlank() && body.isNotBlank())return DeviceCommand(if(wa)"whatsapp" else "sms",JSONObject().put("contact",target).put("text",body))
        }
        val english=Regex("^(?:open\\s+and\\s+)?(?:send\\s+)?(?:sms|message)?\\s*(.+?)\\s+to\\s+(.+)$",RegexOption.IGNORE_CASE).find(clean)
        if(english!=null)return DeviceCommand(if(wa)"whatsapp" else "sms",JSONObject().put("contact",cleanCommandTarget(english.groupValues[2])).put("text",english.groupValues[1].trim()))
    }
    val call=Regex("^(?:(?:আচ্ছা|তুমি|প্লিজ)\\s+)*(.+?)(?:\\s+কে|কে)?\\s+(?:কল|ফোন)\\s+(?:করো|কর|দাও)$",RegexOption.IGNORE_CASE).find(text)
    if(call!=null)return DeviceCommand("call",JSONObject().put("contact",cleanCommandTarget(call.groupValues[1])))
    val callEnglish=Regex("^(?:please )?call (.+)$",RegexOption.IGNORE_CASE).find(text)
    if(callEnglish!=null)return DeviceCommand("call",JSONObject().put("contact",callEnglish.groupValues[1].trim()))
    val open=Regex("^(?:(?:আচ্ছা|তুমি|প্লিজ|দয়া করে|দয়া করে)\\s+)*(.*?)\\s+(?:ওপেন\\s*(?:করো|কর|করে দাও)|খোলো|খুলে দাও|চালু করো)$",RegexOption.IGNORE_CASE).find(text)
    if(open!=null && open.groupValues[1].isNotBlank())return DeviceCommand("open_app",JSONObject().put("name",open.groupValues[1].trim()))
    val en=Regex("^(?:please )?open (.+)$",RegexOption.IGNORE_CASE).find(text)
    if(en!=null)return DeviceCommand("open_app",JSONObject().put("name",en.groupValues[1].trim()))
    return null
}
fun meaningfulReply(value:Any?):String = (value as? String)?.trim()?.takeIf{it.isNotBlank() && it.lowercase() !in setOf("null","undefined")} ?: ""
fun validateChannel(userRequest:String,tool:String){
    require(!(whatsappMention(userRequest) && tool=="sms")){"WhatsApp-এর নির্দেশে SMS পাঠানো বন্ধ করা হয়েছে। WhatsApp contact ও মেসেজটি বলো।"}
}
fun parseElevenVoice(value:String):String {
    val text=value.trim()
    val id=if(text.startsWith("https://")){
        val uri=URI(text)
        require(uri.host?.lowercase() in listOf("elevenlabs.io","www.elevenlabs.io")){"ElevenLabs voice link ব্যবহার করো।"}
        val query=uri.rawQuery.orEmpty().split('&').map{it.split('=',limit=2)}.firstOrNull{it.size==2 && it[0] in listOf("voiceId","voice_id") }?.get(1)
        query ?: uri.path.orEmpty().split('/').lastOrNull{it.isNotBlank()}.orEmpty()
    }else text
    require(id.matches(Regex("[A-Za-z0-9_-]{10,100}"))){"সঠিক ElevenLabs voice ID বা voice link দাও।"}
    return id
}
/** Conservative cross-script key: full consonant sequence must match, never closest-name guessing. */
fun contactSoundKey(value:String):String {
    val map=mapOf('ক' to "k",'খ' to "kh",'গ' to "g",'ঘ' to "gh",'ঙ' to "ng",'চ' to "ch",'ছ' to "ch",'জ' to "j",'ঝ' to "jh",'ঞ' to "n",'ট' to "t",'ঠ' to "th",'ড' to "d",'ঢ' to "dh",'ণ' to "n",'ত' to "t",'থ' to "th",'দ' to "d",'ধ' to "dh",'ন' to "n",'প' to "p",'ফ' to "f",'ব' to "b",'ভ' to "bh",'ম' to "m",'য' to "j",'র' to "r",'ল' to "l",'শ' to "sh",'ষ' to "sh",'স' to "s",'হ' to "h",'ড়' to "r",'ঢ়' to "r",'য়' to "y")
    return normalizedName(value).map{map[it] ?: if(it in 'a'..'z'||it==' ')it.toString() else ""}.joinToString("").replace(Regex("[aeiou]"),"").replace(Regex("\\s+")," ").trim()
}
fun whatsappPhone(value:String):String {
    val n=normalizedPhone(value)
    return when {n.startsWith("+" )->n.drop(1);n.startsWith("00880")->n.drop(2);n.matches(Regex("01[3-9][0-9]{8}"))->"88$n";n.startsWith("00")->n.drop(2);else->n}
}

fun contactByPhone(phone:String,contacts:List<ContactAlias>)=contacts.firstOrNull{whatsappPhone(it.phone)==whatsappPhone(phone)}
