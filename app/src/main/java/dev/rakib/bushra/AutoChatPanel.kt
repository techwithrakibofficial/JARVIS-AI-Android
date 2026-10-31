package dev.rakib.bushra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable internal fun AutoChatPanel(vm:AssistantModel){
 val ctx=LocalContext.current;val scope=rememberCoroutineScope();val auto=remember{AutoChatStore(ctx)}
 var rules by remember{mutableStateOf(auto.rules())};var logs by remember{mutableStateOf(auto.logs())}
 var contact by remember{mutableStateOf("")};var purpose by remember{mutableStateOf(vm.autoChatBrief)};var first by remember{mutableStateOf("")}
 var channel by remember{mutableStateOf("whatsapp")};var minutes by remember{mutableStateOf(30)};var limit by remember{mutableStateOf(5)}
 var error by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
 val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){vm.refreshDeviceState()}
 LaunchedEffect(Unit){while(true){rules=auto.rules();logs=auto.logs();delay(2500)}}
 FullSheet("SMS / WhatsApp auto-chat",{vm.autoChatOpen=false}){
  Glass(Modifier.fillMaxWidth()){
   Text("নির্দিষ্ট contact-এর reply",fontWeight=FontWeight.Bold)
   Label("তুমি চালু করলে শুধু ওই ব্যক্তি ও ওই channel-এ নতুন incoming message-এর উত্তর AI তৈরি করে পাঠাবে। Contact-এর লেখা ও সাম্প্রতিক ওই chat নির্বাচিত AI provider-এ যাবে। উত্তর [BUSHRA AI] নামে যাবে।")
  }
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("sms" to "SMS","whatsapp" to "WhatsApp").forEach{(id,label)->FilterChip(channel==id,{channel=id},label={Text(label)})}}
  Field(contact,{contact=it},"ফোনে saved নাম অথবা নম্বর")
  Field(purpose,{purpose=it},"কী বিষয়ে / কীভাবে কথা বলবে",single=false)
  Field(first,{first=it},"শুরুতে এই মেসেজ পাঠাও (ঐচ্ছিক)",single=false)
  Label("সময় ও সর্বোচ্চ automatic reply")
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(10,30,60).forEach{v->FilterChip(minutes==v,{minutes=v},label={Text("$v মিনিট")})}}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(3,5,10).forEach{v->FilterChip(limit==v,{limit=v},label={Text("$v reply")})}}
  OutlinedButton(onClick={permissions.launch(arrayOf(Manifest.permission.READ_CONTACTS,Manifest.permission.SEND_SMS,Manifest.permission.RECEIVE_SMS,Manifest.permission.READ_SMS))},modifier=Modifier.fillMaxWidth()){Text("Contacts ও SMS permission দাও")}
  OutlinedButton(onClick={ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))},modifier=Modifier.fillMaxWidth()){Text("WhatsApp-এর Notification access চালু করো")}
  Label(if(BushraNotifications.instance!=null)"✓ Notification access connected" else "○ WhatsApp auto-reply-এর জন্য Notification access লাগবে; notification-এ Reply action থাকতে হবে।")
  Label("SMS-এর জন্য RECEIVE/SEND permission ও default SMS SIM প্রয়োজন। Carrier charge প্রযোজ্য। WhatsApp group-এ auto-reply হবে না। App/Android battery restriction-এ background reply দেরি হতে পারে।")
  if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
  Button(enabled=!busy && contact.isNotBlank() && purpose.isNotBlank(),onClick={scope.launch{
   busy=true;error=""
   try{
    require(vm.chatProvider()!=null){"আগে AI provider connect করো।"}
    require(vm.store.flag("directMessaging")){"COMMAND ACCESS-এ Direct messaging চালু করো।"}
    if(channel=="sms")require(ContextCompat.checkSelfPermission(ctx,Manifest.permission.RECEIVE_SMS)==PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(ctx,Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED){"SMS receive/send permission দাও।"}
    else require(BushraNotifications.instance!=null){"BUSHRA-এর Notification access চালু করো।"}
    val person=withContext(Dispatchers.IO){
        val selected=vm.phone.recipient(contact)
        if(ContextCompat.checkSelfPermission(ctx,Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED)contactByPhone(selected.phone,vm.phone.deviceContacts()) ?: selected else selected
    }
    val now=System.currentTimeMillis();val rule=AutoRule(channel=channel,label=person.name,phone=person.phone,instruction=purpose.take(1000),created=now,expires=now+minutes*60_000L,limit=limit)
    auto.saveRule(rule);auto.log(person.name,"$channel auto-chat চালু: $minutes মিনিট / $limit reply")
    if(first.isNotBlank()){
     try{val result=vm.phone.execute(channel,JSONObject().put("contact",person.phone).put("text",first));auto.append(rule.id,"assistant",first);auto.log(person.name,result)}catch(e:Exception){auto.stop(rule.id);throw e}
    }
    rules=auto.rules();logs=auto.logs();error="চালু হয়েছে। নতুন incoming message এলে reply করবে; নিচে Stop আছে।"
   }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){error=e.message ?: "চালু হয়নি।"}finally{busy=false}
  }},modifier=Modifier.fillMaxWidth()){Text(if(busy)"প্রস্তুত হচ্ছে…" else "এই contact-এর auto-chat চালু করো")}
  TextButton(onClick={auto.stop();WorkManager.getInstance(ctx).cancelAllWorkByTag("bushra-auto-chat");rules=auto.rules();logs=auto.logs()}){Text("সব automatic reply এখনই বন্ধ করো")}
  rules.asReversed().forEach{r->Glass(Modifier.fillMaxWidth()){
   Text("${r.label} · ${r.channel}",fontWeight=FontWeight.Bold);Label("${r.count}/${r.limit} reply · "+if(r.enabled && r.count<r.limit && r.expires>System.currentTimeMillis())"চালু" else "বন্ধ/সময় শেষ")
   Label(r.instruction)
   TextButton(onClick={auto.stop(r.id);rules=auto.rules()}){Text("Stop this contact")}
  }}
  Text("সাম্প্রতিক ফল",fontWeight=FontWeight.Bold)
  logs.take(12).forEach{line->Glass(Modifier.fillMaxWidth()){Text(line)}}
 }
}
