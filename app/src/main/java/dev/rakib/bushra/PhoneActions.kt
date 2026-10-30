package dev.rakib.bushra

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.json.JSONObject

data class InstalledApp(val label:String,val pkg:String)
class PhoneActions(private val context:Context,private val store:LocalStore) {
    fun apps():List<InstalledApp> = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
        .map { InstalledApp(it.loadLabel(context.packageManager).toString(),it.activityInfo.packageName) }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    fun deviceContacts():List<ContactAlias> {
        require(ContextCompat.checkSelfPermission(context,android.Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED) { "Contacts permission দিন।" }
        val result=mutableListOf<ContactAlias>()
        context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")?.use { c ->
            while(c.moveToNext() && result.size<1500) { val name=c.getString(0).orEmpty();val phone=normalizedPhone(c.getString(1).orEmpty());if(validPhone(phone))result.add(ContactAlias(name=name,phone=phone,aliases=emptyList())) }
        }
        return result.distinctBy { normalizedPhone(it.phone) }
    }
    private fun recipient(query:String):ContactAlias {
        if(validPhone(query))return ContactAlias(name=query,phone=normalizedPhone(query),aliases=emptyList())
        val local=resolveContacts(query,store.contacts())
        val matches=if(local.isNotEmpty())local else runCatching { resolveContacts(query,deviceContacts()) }.getOrDefault(emptyList())
        require(matches.isNotEmpty()) { "Contact পাওয়া যায়নি। Contacts tab-এ নম্বর ও ডাকনাম যোগ করুন।" }
        require(matches.size==1) { "একাধিক contact মিলেছে: "+matches.joinToString { it.name+" (…"+it.phone.takeLast(4)+")" }+"। Contacts tab-এ অনন্য alias দিন।" }
        return matches.single()
    }
    fun describe(tool:String,args:JSONObject):String=when(tool) {
        "call" -> recipient(args.optString("contact")).let { "Dialer খুলবে: ${it.name}\n${it.phone}\nকল শুরু করতে dialer-এর Call চাপবেন।" }
        "sms" -> recipient(args.optString("contact")).let { "SMS draft খুলবে: ${it.name}\n${it.phone}\n\n${args.optString("text")}\n\nপাঠানোর শেষ সিদ্ধান্ত SMS app-এ নেবেন।" }
        "read_screen" -> "বর্তমান অ্যাপের দৃশ্যমান লেখা আপনার নির্বাচিত AI provider-এ যাবে। Password field বাদ দেওয়া হবে; অন্য ব্যক্তিগত লেখা থাকতে পারে।"
        "click" -> "স্ক্রিনের '${args.optString("text")}' বাটনে click হবে। Send/Post/Confirm হলে বাহ্যিক কাজ হতে পারে। বর্তমান অ্যাপ দেখে অনুমতি দিন।"
        "type" -> "বর্তমান text field-এ এই লেখা বসবে:\n${args.optString("text")}"
        "open_app" -> "অ্যাপ খুলবে: ${args.optString("name")}"
        "open_url" -> "লিংক খুলবে: ${args.optString("url")}"
        else -> "$tool\n$args"
    }
    fun execute(tool:String,args:JSONObject):String {
        fun launch(i:Intent) { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        return when(tool) {
            "open_app" -> {
                val q=args.optString("name");val all=apps();val exact=all.filter { normalizedName(it.label)==normalizedName(q) || it.pkg==q }
                val found=if(exact.isNotEmpty())exact else all.filter { normalizedName(it.label).contains(normalizedName(q)) }
                require(q.isNotBlank() && found.size==1) { "অ্যাপটি নির্দিষ্ট করে বলুন। মিলেছে: "+found.take(8).joinToString { it.label } }
                launch(context.packageManager.getLaunchIntentForPackage(found.single().pkg) ?: error("অ্যাপ খোলা যায়নি।"));"Opened ${found.single().label}."
            }
            "open_url" -> { val u=Uri.parse(args.optString("url"));require(u.scheme in listOf("http","https") && !u.host.isNullOrBlank()) { "শুধু web URL খোলা যায়।" };launch(Intent(Intent.ACTION_VIEW,u));"URL opened; page result not verified." }
            "call" -> { val c=recipient(args.optString("contact"));launch(Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",c.phone,null)));"Dialer opened for ${c.name}; user must tap Call. Call not started by app." }
            "sms" -> { val c=recipient(args.optString("contact"));launch(Intent(Intent.ACTION_SENDTO,Uri.fromParts("smsto",c.phone,null)).putExtra("sms_body",args.optString("text")));"SMS draft opened for ${c.name}; not sent. User sends in SMS app." }
            "read_screen" -> service().snapshot()
            "click" -> service().click(args.optString("text"))
            "type" -> service().type(args.optString("text"))
            "scroll" -> service().scroll(args.optString("direction"))
            "home","back","recents" -> service().global(tool)
            else -> error("এই action সমর্থিত নয়: $tool")
        }
    }
    private fun service()=PhoneAccessibility.instance ?: error("Settings থেকে BUSHRA Accessibility service চালু করুন।")
}
