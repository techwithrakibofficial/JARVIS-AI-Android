package dev.rakib.bushra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable internal fun AutomationSetup(vm:AssistantModel){
    val ctx=LocalContext.current;val revision=vm.revision
    val requested=arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS,Manifest.permission.SEND_SMS,Manifest.permission.CALL_PHONE)
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){result->
        vm.refreshDeviceState()
        vm.notice=if(result.values.all{it})"ফোনের permissions দেওয়া হয়েছে। Screen control-এর জন্য Accessibility চালু করো।" else "কিছু permission দেওয়া হয়নি। নিচের status দেখো; দেওয়া permissions-এ কাজ চলবে।"
    }
    Glass(Modifier.fillMaxWidth()){
        Text("COMMAND ACCESS",fontWeight=FontWeight.Bold);Label("একবারে setup • যেকোনো সময় বন্ধ করতে পারো")
        Spacer(Modifier.height(10.dp))
        Button(onClick={
            vm.flag("trustedActions",true);vm.flag("directMessaging",true)
            val missing=requested.filter{ContextCompat.checkSelfPermission(ctx,it)!=PackageManager.PERMISSION_GRANTED}
            if(missing.isNotEmpty())launcher.launch(missing.toTypedArray())else vm.notice="সব runtime permission আছে। Accessibility status দেখো।"
        },modifier=Modifier.fillMaxWidth()){Text("সব permission নিয়ে automation চালু করো")}
        Label("এটি চালু করলে তোমার নির্দেশে কল, SMS ও screen actions-এর জন্য প্রতি ধাপে app confirmation চাইবে না। SMS-এ SIM charge হতে পারে। Android আলাদা permission dialog দেখাতে পারে; Accessibility নিজে enable করতে হবে।")
        requested.forEach {p->val label=when(p){Manifest.permission.RECORD_AUDIO->"Microphone";Manifest.permission.READ_CONTACTS->"ফোনের Contacts";Manifest.permission.SEND_SMS->"SMS পাঠানো";else->"কল করা"};Label("${if(ContextCompat.checkSelfPermission(ctx,p)==PackageManager.PERMISSION_GRANTED)"✓" else "○"} $label")}
        SettingSwitch("আমার নির্দেশে screen actions",vm.store.flag("trustedActions")){vm.flag("trustedActions",it)}
        SettingSwitch("Direct SMS / call / WhatsApp Send",vm.store.flag("directMessaging")){vm.flag("directMessaging",it)}
        Label("App খোলা ও Back/Home-এ app confirmation লাগে না। একই নামের একাধিক contact থাকলে নম্বর নির্দিষ্ট করতে হবে।")
        val smsReady=validSmsSubscription(SubscriptionManager.getDefaultSmsSubscriptionId())
        Label(if(smsReady)"✓ Default SMS SIM নির্ধারিত" else "○ ফোনের SIM settings-এ default SMS SIM বেছে নাও")
        Label(if(PhoneAccessibility.instance!=null)"✓ Screen control connected" else "○ Screen control বন্ধ")
        OutlinedButton(onClick={ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))},modifier=Modifier.fillMaxWidth()){Text("Accessibility / screen control setup")}
        TextButton(onClick={ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:${ctx.packageName}")))}){Text("Android permission settings")}
        Label("Unknown install, payment, password, account/security change তোমাকেই করতে হবে। Stop চাপলে app-এর চলমান কাজ থামে; ইতিমধ্যে পাঠানো SMS ফেরত নেওয়া যায় না।")
    }
}
