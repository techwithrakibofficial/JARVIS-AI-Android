package dev.rakib.bushra

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.json.JSONObject

data class InstalledApp(val label:String,val pkg:String,val activity:String="")
class PhoneActions(private val context:Context,private val store:LocalStore) {
    fun apps():List<InstalledApp> = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
        .map { InstalledApp(it.loadLabel(context.packageManager).toString(),it.activityInfo.packageName,it.activityInfo.name) }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    fun deviceContacts():List<ContactAlias> {
        require(granted(Manifest.permission.READ_CONTACTS)) { "Settings → একবারে setup থেকে Contacts permission দাও। ফোনের saved নাম সরাসরি কাজ করবে; alias লাগবে না।" }
        val result=mutableListOf<ContactAlias>()
        context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")?.use { c ->
            while(c.moveToNext()) { val name=c.getString(0).orEmpty();val phone=normalizedPhone(c.getString(1).orEmpty());if(validPhone(phone))result.add(ContactAlias(name=name,phone=phone,aliases=emptyList())) }
        }
        return result.distinctBy { normalizedName(it.name)+":"+normalizedPhone(it.phone) }
    }
    fun granted(permission:String)=ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED
    private fun recipient(query:String):ContactAlias {
        if(validPhone(query))return ContactAlias(name=query,phone=normalizedPhone(query),aliases=emptyList())
        val saved=store.contacts()
        // Match all numbers before selecting; never silently prefer an alias over a different phone contact.
        val contacts=saved+if(granted(Manifest.permission.READ_CONTACTS))deviceContacts() else emptyList()
        val matches=resolveContacts(query,contacts)
        if(matches.isEmpty() && !granted(Manifest.permission.READ_CONTACTS))error("Contacts permission দাও। ফোনে save থাকা নামেই কল/মেসেজ হবে; এখানে আবার save করতে হবে না।")
        require(matches.isNotEmpty()) { "ফোনের contacts-এ '$query' পাওয়া যায়নি। নামটি ঠিক করে বলো বা নম্বর দাও।" }
        require(matches.size==1) { "একাধিক নম্বর মিলেছে: "+matches.joinToString { it.name+" (…"+it.phone.takeLast(4)+")" }+"। কাকে বলছ নম্বর দিয়ে নির্দিষ্ট করো।" }
        return matches.single()
    }
    fun describe(tool:String,args:JSONObject):String=when(tool) {
        "call" -> recipient(args.optString("contact")).let { "কল: ${it.name}\n${it.phone}" }
        "sms" -> recipient(args.optString("contact")).let { "Carrier SMS: ${it.name}\n${it.phone}\n${args.optString("text")}\nDirect SMS চালু থাকলে তোমার default SIM থেকে পাঠানোর চেষ্টা হবে। SIM charge প্রযোজ্য।" }
        "whatsapp" -> recipient(args.optString("contact")).let { "WhatsApp: ${it.name}\n${it.phone}\n${args.optString("text")}\nWhatsApp-এই কাজ হবে; SMS-এ বদলাবে না।" }
        "read_screen" -> "বর্তমান অ্যাপের দৃশ্যমান লেখা তোমার নির্বাচিত AI provider-এ যাবে। Password field বাদ থাকবে।"
        "click" -> "বর্তমান স্ক্রিনের '${args.optString("text")}' বাটনে click হবে।"
        "type" -> "বর্তমান text field-এ বসবে:\n${args.optString("text")}"
        "open_app" -> "অ্যাপ খুলবে: ${args.optString("name")}"
        "open_url" -> "লিংক খুলবে: ${args.optString("url")}"
        else -> "$tool\n$args"
    }
    private fun launch(intent:Intent){
        val i=intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        (PhoneAccessibility.instance ?: context).startActivity(i)
    }
    private fun launchApp(name:String):InstalledApp {
        val all=apps();val known=appPackages(name)
        val found=if(known.isNotEmpty())known.mapNotNull{p->all.firstOrNull{it.pkg==p}} else {
            val exact=all.filter { normalizedName(it.label)==normalizedName(name) || it.pkg==name }
            if(exact.isNotEmpty())exact else all.filter { normalizedName(it.label).contains(normalizedName(name)) }
        }
        require(name.isNotBlank() && found.isNotEmpty()){ "'$name' ফোনে পাওয়া যায়নি। Install/enable আছে কি দেখো।" }
        require(known.isNotEmpty() || found.size==1){"একাধিক app মিলেছে: "+found.joinToString{it.label}+"। ঠিক নাম বলো।"}
        val app=found.first()
        // Explicit launcher component works for localized labels and package launch aliases.
        launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(app.pkg,app.activity))
        return app
    }
    suspend fun execute(tool:String,args:JSONObject):String {
        return when(tool) {
            "open_app" -> {
                val app=launchApp(args.optString("name"));delay(750)
                val current=PhoneAccessibility.instance?.currentPackage()
                if(current==app.pkg)"${app.label} foreground-এ দেখা গেছে।" else "${app.label} খোলার অনুরোধ Android-এ পাঠানো হয়েছে; foreground এখনও যাচাই হয়নি।"
            }
            "open_url" -> { val u=Uri.parse(args.optString("url"));require(u.scheme in listOf("http","https") && !u.host.isNullOrBlank()) { "শুধু web URL খোলা যায়।" };launch(Intent(Intent.ACTION_VIEW,u));"লিংক খোলার অনুরোধ পাঠানো হয়েছে; পেজ যাচাই হয়নি।" }
            "call" -> {
                val c=recipient(args.optString("contact"));val direct=store.flag("directMessaging") && granted(Manifest.permission.CALL_PHONE)
                launch(Intent(if(direct)Intent.ACTION_CALL else Intent.ACTION_DIAL,Uri.fromParts("tel",c.phone,null)))
                if(direct)"${c.name}-কে কল করার অনুরোধ Android-এ পাঠানো হয়েছে; সংযোগ হওয়া নিশ্চিত নয়।" else "${c.name}-এর dialer খোলা হয়েছে; Call চাপতে হবে। Direct call চাইলে একবারে setup করো।"
            }
            "sms" -> {
                val c=recipient(args.optString("contact"));val body=args.optString("text");require(body.isNotBlank() && body.length<=4000){"SMS-এর লেখা ১–৪০০০ অক্ষরে দাও।"}
                if(store.flag("directMessaging")){
                    require(granted(Manifest.permission.SEND_SMS)){"একবারে setup থেকে SMS permission দাও।"}
                    val sub=SubscriptionManager.getDefaultSmsSubscriptionId()
                    require(SubscriptionManager.isValidSubscriptionId(sub)){"ফোনের SIM settings-এ default SMS SIM বেছে নাও; কোন SIM থেকে পাঠাব তা আন্দাজ করব না।"}
                    val manager=if(Build.VERSION.SDK_INT>=31)context.getSystemService(SmsManager::class.java).createForSubscriptionId(sub) else SmsManager.getSmsManagerForSubscriptionId(sub)
                    val parts=manager.divideMessage(body)
                    val sent=ArrayList<PendingIntent>();parts.forEachIndexed { index,_ ->
                        val id=(System.nanoTime()+index).toInt()
                        sent.add(PendingIntent.getBroadcast(context,id,Intent(context,SmsStatusReceiver::class.java).putExtra("label",c.name).putExtra("part",index+1).putExtra("total",parts.size),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    }
                    manager.sendMultipartTextMessage(c.phone,null,parts,sent,null)
                    "${c.name}-এর SMS (${parts.size} অংশ) default SIM-এ জমা দেওয়া হয়েছে। Sent callback আসার আগে পাঠানো বা পৌঁছানো নিশ্চিত নয়। Tasks-এ ফল দেখো।"
                }else{
                    launch(Intent(Intent.ACTION_SENDTO,Uri.fromParts("smsto",c.phone,null)).putExtra("sms_body",body));"SMS draft খোলা হয়েছে; পাঠানো হয়নি। Direct SMS চালু করলে প্রতি কাজের app approval লাগবে না।"
                }
            }
            "whatsapp" -> {
                val c=recipient(args.optString("contact"));val body=args.optString("text");require(body.isNotBlank() && body.length<=4000){"WhatsApp মেসেজের লেখা দাও।"}
                val target=appPackages("WhatsApp").firstOrNull { pkg->apps().any{it.pkg==pkg} } ?: error("WhatsApp install/enable পাওয়া যায়নি। SMS পাঠানো হয়নি।")
                val url=Uri.Builder().scheme("https").authority("wa.me").appendPath(whatsappPhone(c.phone)).appendQueryParameter("text",body).build()
                launch(Intent(Intent.ACTION_VIEW,url).setPackage(target));delay(1400)
                if(store.flag("directMessaging")){
                    val service=PhoneAccessibility.instance ?: error("WhatsApp chat খোলার অনুরোধ হয়েছে। Automatic Send-এর জন্য Accessibility চালু করো। SMS পাঠানো হয়নি।")
                    var result:String?=null
                    repeat(8){if(result==null){result=service.sendWhatsAppIfReady(target,body,c.name,c.phone);if(result==null)delay(450)}}
                    result ?: "WhatsApp chat/লেখা/Send একসঙ্গে যাচাই হয়নি, তাই Send চাপা হয়নি। WhatsApp-এ দেখো; SMS পাঠানো হয়নি।"
                }else "${c.name}-এর WhatsApp chat-এ মেসেজ খোলার অনুরোধ হয়েছে; পাঠানো হয়নি। Send করো বা Direct messaging চালু করো।"
            }
            "read_screen" -> service().snapshot()
            "click" -> service().click(args.optString("text"))
            "type" -> service().type(args.optString("text"))
            "scroll" -> service().scroll(args.optString("direction"))
            "home","back","recents" -> service().global(tool)
            else -> error("এই action সমর্থিত নয়: $tool")
        }
    }
    private fun service()=PhoneAccessibility.instance ?: error("Settings থেকে BUSHRA Accessibility service চালু করো।")
}
