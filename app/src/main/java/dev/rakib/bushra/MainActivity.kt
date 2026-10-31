package dev.rakib.bushra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity:ComponentActivity() {
    private val model:AssistantModel by viewModels()
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);setContent { BushraApp(model) } }
    override fun onStart() { super.onStart();model.foreground=true;model.refreshDeviceState() }
    override fun onStop() { model.onBackground();super.onStop() }
}

private val Cyan=Color(0xFF43E7EF)
private val Blue=Color(0xFF4389FF)
private val Navy=Color(0xFF060811)
private val Panel=Color(0xFF111326)
private val Muted=Color(0xFFADBFD1)
@Composable internal fun Glass(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    val palette=MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(26.dp)).background(Brush.verticalGradient(listOf(palette.surfaceVariant.copy(alpha=.52f),palette.surface.copy(alpha=.92f),palette.surface.copy(alpha=.75f))))
        .border(1.dp,Brush.linearGradient(listOf(Color.White.copy(alpha=.25f),palette.primary.copy(alpha=.35f),Color(0xFFB16EFF).copy(alpha=.30f))),RoundedCornerShape(26.dp)).padding(18.dp),content=content)
}
@Composable internal fun Label(text:String){Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp)}
@Composable internal fun Field(value:String,change:(String)->Unit,label:String,modifier:Modifier=Modifier,secret:Boolean=false,single:Boolean=true) {
    OutlinedTextField(value=value,onValueChange=change,label={Text(label)},modifier=modifier.fillMaxWidth(),singleLine=single,
        visualTransformation=if(secret)PasswordVisualTransformation() else VisualTransformation.None,shape=RoundedCornerShape(14.dp))
}
@Composable private fun ActionTile(label:String,icon:ImageVector,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
    Glass(modifier.clickable(enabled=enabled,onClick=onClick)) { Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(25.dp));Spacer(Modifier.height(10.dp));Text(label,fontWeight=FontWeight.Medium,fontSize=14.sp) }
}

@Composable fun BushraApp(vm:AssistantModel) {
    val context=LocalContext.current
    val revision=vm.revision
    val light=vm.store.flag("light")
    val scheme=if(light)lightColorScheme(primary=Color(0xFF005CBF),secondary=Color(0xFF007E86),background=Color(0xFFF0F6FD),surface=Color.White,onSurfaceVariant=Color(0xFF4E647D))
        else darkColorScheme(primary=Cyan,secondary=Blue,background=Navy,surface=Panel,onSurface=Color(0xFFF0F6FF),onSurfaceVariant=Muted,outlineVariant=Color(0xFF36516D))
    var consent by remember { mutableStateOf(!vm.store.flag("consent")) }
    var shortcut by remember { mutableStateOf("") }
    var showVoice by remember { mutableStateOf(false) }
    var speechConsent by remember { mutableStateOf(false) }
    fun beginMic() {
        if(vm.dictationOnly)vm.voice.listen() else { showVoice=true;vm.startConversation() }
    }
    fun afterPermission() { if(!vm.store.flag("speechConsentSeen"))speechConsent=true else beginMic() }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted)afterPermission() else {vm.dictationOnly=false;vm.notice="Microphone permission দেওয়া হয়নি। App info → Permissions থেকে Microphone allow করুন।"}
    }
    val mic:()->Unit={
        if(!vm.dictationOnly && vm.chatProvider()==null){vm.tab=4;vm.notice="একটি provider বেছে key দিয়ে Connect করো। Model/endpoint লিখতে হবে না।"}
        else if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)afterPermission()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }
    MaterialTheme(colorScheme=scheme) {
        Surface(Modifier.fillMaxSize(),color=scheme.background) {
          Box(Modifier.fillMaxSize()) {
            Atmosphere(vm.store.flag("motion",true) && vm.foreground)
            Scaffold(containerColor=Color.Transparent,
                topBar={ Column(Modifier.statusBarsPadding().padding(horizontal=20.dp,vertical=10.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.brand_logo),"BUSHRA logo",Modifier.size(42.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                        Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){NeonWordmark();Text("RAKIB CHOWDHURY LABS",fontSize=10.sp,color=scheme.onSurfaceVariant,letterSpacing=1.sp)}
                        if(vm.busy)IconButton(onClick={vm.stop()}){Icon(Icons.Outlined.StopCircle,"সব কাজ থামাও",tint=Color(0xFFFF9F9F))}
                        IconButton(onClick={vm.tab=4}){Icon(Icons.Outlined.Tune,"Settings")}
                    }
                    if(vm.status!="প্রস্তুত")Text(vm.status,fontSize=12.sp,color=scheme.primary,modifier=Modifier.padding(top=6.dp))
                }},
                bottomBar={NavigationBar(modifier=Modifier.padding(horizontal=12.dp,vertical=7.dp).clip(RoundedCornerShape(28.dp)).border(1.dp,scheme.primary.copy(alpha=.20f),RoundedCornerShape(28.dp)),containerColor=scheme.surface.copy(alpha=.88f)) {
                    val names=listOf("Home","Chat","Contacts","Tasks","Settings")
                    val icons=listOf(Icons.Outlined.Dashboard,Icons.Outlined.ChatBubbleOutline,Icons.Outlined.Contacts,Icons.Outlined.Checklist,Icons.Outlined.Settings)
                    names.forEachIndexed { i,n->NavigationBarItem(selected=vm.tab==i,onClick={vm.tab=i},icon={Icon(icons[i],n)},label={Text(n,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(indicatorColor=scheme.primary.copy(alpha=.15f),selectedIconColor=scheme.primary,selectedTextColor=scheme.primary)) }
                }}
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(scheme.primary.copy(alpha=.08f),Color.Transparent)))) {
                    when(vm.tab) {
                        0 -> Dashboard(vm,mic,{shortcut=it})
                        1 -> ChatScreen(vm,mic)
                        2 -> ContactsScreen(vm,mic)
                        3 -> TasksScreen(vm)
                        4 -> SettingsScreen(vm)
                        5 -> BuildScreen(vm)
                    }
                }
            }
        }}
        if(showVoice)VoiceRoom(vm,{showVoice=false;vm.stop()},mic,{speechConsent=true})
        if(speechConsent)AlertDialog(onDismissRequest={speechConsent=false;vm.dictationOnly=false},title={Text("ভয়েসের অনুমতি")},text={Text("ফোনের offline বাংলা service কাজ না করলে network speech ব্যবহার করা হবে। অডিও ফোনের speech provider-এ যেতে পারে। Mic শুধু এই session-এ চালু থাকবে; background/lock হলে বন্ধ হবে।")},confirmButton={Button(onClick={vm.flag("networkSpeech",true);vm.flag("speechConsentSeen",true);speechConsent=false;beginMic()}){Text("অনুমতি দিয়ে চালু করো")}},dismissButton={TextButton(onClick={vm.flag("networkSpeech",false);vm.flag("speechConsentSeen",true);speechConsent=false;beginMic()}){Text("শুধু offline")}})
        if(consent)AlertDialog(onDismissRequest={},title={Text("তোমার ব্যক্তিগত AI workspace")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Chat ও voice text তোমার নির্বাচিত API provider-এ পাঠানো হবে। Cloud voice ব্যবহার করলে উত্তর ElevenLabs/নির্বাচিত voice provider-এও যাবে।")
            Text("History ও contacts ফোনে সংরক্ষিত থাকে। Chat-এর সঙ্গে সাম্প্রতিক কথা, saved memory, alias labels ও installed app names নির্বাচিত AI-তে যায়। Screen text পাঠানোর আগে আলাদা অনুমতি নেওয়া হয়। Microphone শুধু তোমার চাপায় চালু হয়। কোনো tracking SDK নেই।")
            Text("Call/SMS shortcuts dialer ও draft খোলে। পাঠানোর শেষ কাজ সংশ্লিষ্ট অ্যাপে করবে। Carrier charge ও provider quota প্রযোজ্য।")
        }},confirmButton={Button(onClick={vm.flag("consent",true);consent=false}){Text("বুঝেছি, শুরু করি")}})
        vm.approval?.let { request -> AlertDialog(onDismissRequest={vm.answerApproval(false)},title={Text(request.title)},text={Text(request.detail)},confirmButton={Button(onClick={vm.answerApproval(true)}){Text("অনুমতি দিন")}},dismissButton={TextButton(onClick={vm.answerApproval(false)}){Text("বাতিল")}}) }
        if(vm.notice.isNotBlank())AlertDialog(onDismissRequest={vm.notice=""},title={Text("BUSHRA")},text={Text(vm.notice)},confirmButton={TextButton(onClick={vm.notice=""}){Text("ঠিক আছে")}})
        if(shortcut.isNotEmpty())ShortcutDialog(shortcut,vm,{shortcut=""})
    }
}

@Composable private fun Dashboard(vm:AssistantModel,mic:()->Unit,shortcut:(String)->Unit) = PremiumDashboard(vm,mic,shortcut)

@Composable private fun ChatScreen(vm:AssistantModel,mic:()->Unit) {
    var input by remember { mutableStateOf("") }
    val scroll=rememberScrollState()
    LaunchedEffect(vm.history.size){scroll.animateScrollTo(scroll.maxValue)}
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal=16.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text("Conversation",fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f));TextButton(onClick={vm.clearHistory()},enabled=!vm.busy){Text("Clear history")}}
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(vm.history.isEmpty())Glass(Modifier.fillMaxWidth()){Text("আমি BUSHRA। কী নিয়ে কথা বলবে?");Label("API ছাড়া Home-এর call, SMS ও app shortcuts চলবে।")}
            vm.history.forEach { line ->
                Column(Modifier.fillMaxWidth(),horizontalAlignment=if(line.who=="user")Alignment.End else Alignment.Start) {
                    Column(Modifier.widthIn(max=340.dp).clip(RoundedCornerShape(18.dp)).background(if(line.who=="user")MaterialTheme.colorScheme.primary.copy(alpha=.15f) else MaterialTheme.colorScheme.surface).padding(14.dp)) {
                        Label(if(line.who=="user")"তুমি" else "BUSHRA");androidx.compose.foundation.text.selection.SelectionContainer {Text(line.text,fontSize=15.sp)}
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        if(vm.busy)LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical=6.dp))
        Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(input,{input=it},placeholder={Text("কিছু বলো…")},modifier=Modifier.weight(1f),maxLines=4,shape=RoundedCornerShape(22.dp),enabled=!vm.busy)
            IconButton(onClick=mic){Icon(Icons.Outlined.Mic,"ভয়েসে বলো")}
            IconButton(onClick={val t=input;input="";vm.send(t)},enabled=!vm.busy && input.isNotBlank()){Icon(Icons.AutoMirrored.Outlined.Send,"পাঠাও",tint=MaterialTheme.colorScheme.primary)}
        }
    }
}

@Composable private fun ShortcutDialog(tool:String,vm:AssistantModel,close:()->Unit) {
    var target by remember { mutableStateOf("") };var message by remember { mutableStateOf("") }
    val apps=remember { if(tool=="open_app")vm.phone.apps() else emptyList() }
    AlertDialog(onDismissRequest=close,title={Text(when(tool){"call"->"কাকে কল করবে?";"sms"->"মেসেজের খসড়া";else->"অ্যাপ খোলো"})},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Field(target,{target=it},if(tool=="open_app")"অ্যাপের নাম" else "ডাকনাম / নম্বর")
        if(tool=="sms")Field(message,{message=it},"মেসেজ",single=false)
        if(tool=="open_app")Column(Modifier.heightIn(max=210.dp).verticalScroll(rememberScrollState())) {apps.filter { it.label.contains(target,true) }.take(20).forEach { app ->TextButton(onClick={target=app.label}){Text(app.label)} }}
        else Label("Contacts tab-এ সহজ ডাকনাম যোগ করতে পারো।")
    }},confirmButton={Button(onClick={vm.localAction(tool,JSONObject().put(if(tool=="open_app")"name" else "contact",target).put("text",message));close()},enabled=target.isNotBlank() && (tool!="sms" || message.isNotBlank())){Text("পরের ধাপ")}},dismissButton={TextButton(onClick=close){Text("বাতিল")}})
}

@Composable private fun ContactsScreen(vm:AssistantModel,mic:()->Unit) {
    val ctx=LocalContext.current;val scope=rememberCoroutineScope()
    var query by remember { mutableStateOf("") };var editing by remember { mutableStateOf<ContactAlias?>(null) };var picker by remember { mutableStateOf(false) };var contacts by remember { mutableStateOf<List<ContactAlias>>(emptyList()) }
    fun load() { scope.launch { runCatching { withContext(Dispatchers.IO){vm.phone.deviceContacts()} }.onSuccess { contacts=it;picker=true }.onFailure { vm.notice=it.message.orEmpty() } } }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)load() else vm.notice="Contacts permission দেওয়া হয়নি। Manual নম্বর যোগ করতে পারো।"}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("ফোনের contacts ও ডাকনাম",fontSize=23.sp,fontWeight=FontWeight.Medium)
        Label("Saved contact name সরাসরি কাজ করবে; এখানে আবার save করতে হবে না। শুধু আলাদা ডাকনাম চাইলে যোগ করো। যেমন: অফিসের বস, মা, Rahim bhai। একই ব্যক্তির একাধিক বাংলা/English alias রাখা যায়।")
        Button(onClick={editing=ContactAlias(name="",phone="",aliases=emptyList())},modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Add,null);Text(" নম্বর ও ডাকনাম যোগ করো")}
        OutlinedButton(onClick={if(ContextCompat.checkSelfPermission(ctx,Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED)load() else permission.launch(Manifest.permission.READ_CONTACTS)},modifier=Modifier.fillMaxWidth()){Text("ফোনের Contacts থেকে নির্বাচন")}
        Field(query,{query=it},"নাম / ডাকনাম খোঁজো")
        vm.aliases.filter { query.isBlank() || (it.aliases+it.name+it.phone).any { n->n.contains(query,true) } }.forEach { c ->
            Glass(Modifier.fillMaxWidth()) {Row {Column(Modifier.weight(1f)){Text(c.name,fontWeight=FontWeight.Medium);Label(c.phone);Text(c.aliases.joinToString(" • "),color=MaterialTheme.colorScheme.primary,fontSize=13.sp)};IconButton(onClick={editing=c}){Icon(Icons.Outlined.Edit,"সম্পাদনা")}}
                Row {TextButton(onClick={vm.localAction("call",JSONObject().put("contact",c.phone))},enabled=!vm.busy){Text("Dialer")};TextButton(onClick={vm.deleteAlias(c)}){Text("Remove alias")}}
            }
        }
        if(vm.aliases.isEmpty())Glass(Modifier.fillMaxWidth()){Text("ফোনে save থাকা নাম বললেই হবে।");Label("Settings-এ Contacts permission দাও। এখানে alias রাখা ঐচ্ছিক।")}
    }
    if(picker)FullSheet("ফোনের Contact বেছে নাও",{picker=false}) {
        var filter by remember { mutableStateOf("") };Field(filter,{filter=it},"নাম বা নম্বর")
        contacts.filter { it.name.contains(filter,true)||it.phone.contains(filter) }.take(100).forEach { c ->TextButton(onClick={editing=c;picker=false},modifier=Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth()){Text(c.name);Label(c.phone)}} }
    }
    editing?.let { c -> AliasEditor(c,vm,mic){editing=null} }
}

@Composable private fun AliasEditor(c:ContactAlias,vm:AssistantModel,mic:()->Unit,close:()->Unit) {
    var name by remember(c.id){mutableStateOf(c.name)};var phone by remember(c.id){mutableStateOf(c.phone)};var aliases by remember(c.id){mutableStateOf(c.aliases.joinToString(", "))};var error by remember {mutableStateOf("")}
    LaunchedEffect(vm.dictated){if(vm.dictated.isNotBlank()){aliases=listOf(aliases,vm.dictated).filter { it.isNotBlank() }.joinToString(", ");vm.dictated=""}}
    FullSheet("নাম ও ডাকনাম",{vm.dictationOnly=false;vm.voice.stop();close()}) {
        Field(name,{name=it},"পরিচিতি / আসল নাম");Field(phone,{phone=it},"ফোন নম্বর")
        Field(aliases,{aliases=it},"ডাকনাম, কমা দিয়ে আলাদা করো",single=false)
        OutlinedButton(onClick={vm.dictationOnly=true;mic()},modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Mic,null);Text(" ডাকনাম বলে যোগ করো")}
        Label("উদাহরণ: মা, আম্মু, Amma। কাছাকাছি নাম হলে নিজস্ব আলাদা ডাকনাম দিন।")
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        Button(onClick={runCatching { vm.saveAlias(c.copy(name=name.trim(),phone=phone,aliases=aliases.split(',', '،','，').map(String::trim))) }.onSuccess {close()}.onFailure {error=it.message.orEmpty()}},modifier=Modifier.fillMaxWidth()){Text("সংরক্ষণ")}
    }
}

@Composable private fun TasksScreen(vm:AssistantModel) {
    var note by remember {mutableStateOf(vm.store.text("memory"))}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Memory & activity",fontSize=23.sp,fontWeight=FontWeight.Medium)
        Glass(Modifier.fillMaxWidth()) {Text("যা মনে রাখতে বলবে",fontWeight=FontWeight.Medium);Spacer(Modifier.height(10.dp));Field(note,{note=it},"পছন্দ, পরিকল্পনা বা প্রয়োজনীয় তথ্য",single=false);TextButton(onClick={vm.setting("memory",note.take(1600));vm.notice="Memory সংরক্ষণ হয়েছে। এটি পরের AI chat-এ পাঠানো হবে।"}){Text("Save memory")};Label("Password বা API key এখানে লিখবে না। Selected AI এই তথ্য পাবে।")}
        Row(verticalAlignment=Alignment.CenterVertically){Text("কাজের activity",modifier=Modifier.weight(1f));TextButton(onClick={vm.clearTasks()}){Text("Clear")}}
        if(vm.tasks.isEmpty())Label("কাজ চালালে এখানে ফলাফল দেখা যাবে।")
        vm.tasks.asReversed().forEach { t ->Glass(Modifier.fillMaxWidth()){Text(t.text,fontWeight=FontWeight.Medium);Label(t.status);Label(SimpleDateFormat("dd MMM, HH:mm",Locale.getDefault()).format(Date(t.time)))}}
    }
}

@Composable internal fun FullSheet(title:String,close:()->Unit,content:@Composable ColumnScope.()->Unit) {
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),color=MaterialTheme.colorScheme.background) {
            Column {Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(title,fontSize=20.sp,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f));IconButton(onClick=close){Icon(Icons.Outlined.Close,"বন্ধ")}}
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)
            }
        }
    }
}

@Composable private fun SettingsScreen(vm:AssistantModel) = SimpleSettings(vm)

@Composable internal fun SettingSwitch(label:String,checked:Boolean,change:(Boolean)->Unit) {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,modifier=Modifier.weight(1f),fontSize=14.sp);Switch(checked,change)}}

@Composable private fun BuildScreen(vm:AssistantModel) {
    var brief by remember {mutableStateOf("")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={vm.tab=0}){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"ফিরে যাও")};Text("Build Studio",fontSize=24.sp,fontWeight=FontWeight.Medium)}
        Glass(Modifier.fillMaxWidth()){Icon(Icons.Outlined.Code,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(10.dp));Text("ভাবনা থেকে source project",fontSize=20.sp);Label("এই preview-তে AI দিয়ে ছোট website/app-এর source ZIP তৈরি ও share করা যায়। এটি APK compiler নয়। Automatic GitHub upload, cloud build বা website deployment এখনো যুক্ত হয়নি।")}
        Field(brief,{brief=it},"কী বানাতে চাও? Design ও কাজ লিখে বলো",single=false)
        Button(onClick={vm.generateProject(brief)},enabled=!vm.busy && brief.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Source ZIP তৈরি করো")}
        if(vm.busy){LinearProgressIndicator(Modifier.fillMaxWidth());TextButton(onClick={vm.stop()}){Text("Cancel")}}
        vm.exportedFile?.let { f ->Glass(Modifier.fillMaxWidth()){Text("Source প্রস্তুত",fontWeight=FontWeight.Medium);Label("Compile/test করা হয়নি। চালানোর আগে code review করো।");Button(onClick={vm.share(f)}){Text("ZIP share / save")}} }
        Label("API token limit অনুযায়ী ছোট project দিয়ে শুরু করো। কোনো generated code এই ফোনে চালানো হয় না।")
    }
}
