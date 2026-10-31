package dev.rakib.bushra

import android.app.Activity
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val Ice=Color(0xFF4EE8F5)
private val Iris=Color(0xFFAA8BFF)
private val Electric=Color(0xFF3984FF)
@Composable private fun motionEnabled(requested:Boolean=true):Boolean {
    val ctx=LocalContext.current
    return requested && !(ctx.getSystemService(PowerManager::class.java)?.isPowerSaveMode ?: false)
}
@Composable internal fun Atmosphere(animated:Boolean=true) {
    val motion=motionEnabled(animated)
    val cycle=rememberInfiniteTransition(label="atmosphere")
    val drift=if(motion)cycle.animateFloat(0f,1f,infiniteRepeatable(tween(18000,easing=LinearEasing)),label="drift").value else 0f
    val points=remember {val r=Random(47);List(38){Offset(r.nextFloat(),r.nextFloat())}}
    val light=MaterialTheme.colorScheme.background.red>.5f
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.radialGradient(listOf(Electric.copy(alpha=if(light).13f else .20f),Color.Transparent),Offset(size.width*(.1f+.12f*drift),size.height*.12f),size.width*1.05f))
        drawRect(Brush.radialGradient(listOf(Iris.copy(alpha=if(light).10f else .14f),Color.Transparent),Offset(size.width*.95f,size.height*.65f),size.width*.9f))
        points.forEachIndexed { i,p ->
            val a=(.18f+.15f*sin((drift*2*PI+i).toFloat())).coerceIn(.03f,.4f)
            drawCircle(if(light)Electric.copy(alpha=a) else Ice.copy(alpha=a),if(i%4==0)1.6.dp.toPx() else .8.dp.toPx(),Offset(p.x*size.width,p.y*size.height))
        }
        for(i in 0..4){val x=size.width*(i*.23f);drawLine(Electric.copy(alpha=.055f),Offset(x,0f),Offset(x+size.width*.3f,size.height),.7.dp.toPx())}
    }
}
@Composable internal fun NeonWordmark() {
    Text("BUSHRA AI",style=TextStyle(brush=Brush.linearGradient(listOf(Ice,Electric,Iris)),fontSize=20.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp))
}
@Composable private fun StatusChip(text:String,active:Boolean) {
    Row(Modifier.clip(RoundedCornerShape(50)).background((if(active)Ice else Iris).copy(alpha=.09f)).border(1.dp,(if(active)Ice else Iris).copy(alpha=.22f),RoundedCornerShape(50)).padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.size(5.dp).background(if(active)Ice else Iris,CircleShape));Spacer(Modifier.width(6.dp));Text(text,fontSize=11.sp,maxLines=1)
    }
}
@Composable internal fun OrbitAvatar(phase:VoicePhase,level:Float,motion:Boolean,onTap:()->Unit,sizeDp:Int=210) {
    val enabled=motionEnabled(motion)
    val cycle=rememberInfiniteTransition(label="voice orbit")
    val angle=if(enabled)cycle.animateFloat(0f,360f,infiniteRepeatable(tween(if(phase==VoicePhase.THINKING)3500 else 12000,easing=LinearEasing)),label="orbit rotation").value else 45f
    val pulse=if(enabled)cycle.animateFloat(.90f,1.02f,infiniteRepeatable(tween(2200),RepeatMode.Reverse),label="orbit breathing").value else 1f
    val tone=when(phase){VoicePhase.SPEAKING->Iris;VoicePhase.THINKING->Color(0xFFFFC880);VoicePhase.ERROR->Color(0xFFFF9CAB);else->Ice}
    Box(Modifier.size(sizeDp.dp).clickable(onClick=onTap),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center=Offset(size.width/2,size.height/2);val r=size.minDimension*.43f
            drawCircle(Brush.radialGradient(listOf(tone.copy(alpha=.19f),Color.Transparent),center,size.minDimension*.5f),size.minDimension*.5f,center)
            drawCircle(tone.copy(alpha=.12f),r*pulse,center,style=Stroke(1.dp.toPx()))
            drawCircle(tone.copy(alpha=.08f),r*1.13f,center,style=Stroke(.8.dp.toPx()))
            rotate(angle){
                drawCircle(Brush.sweepGradient(listOf(tone.copy(alpha=.08f),Electric,tone,Iris,tone.copy(alpha=.08f)),center),r*.91f,center,style=Stroke(2.8.dp.toPx()))
                drawArc(tone,10f,54f,false,Offset(center.x-r,center.y-r),Size(r*2,r*2),style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
                drawArc(Iris,190f,32f,false,Offset(center.x-r*1.1f,center.y-r*1.1f),Size(r*2.2f,r*2.2f),style=Stroke(1.5.dp.toPx(),cap=StrokeCap.Round))
            }
            if(phase==VoicePhase.LISTENING)drawCircle(tone.copy(alpha=.2f),r*(.75f+.14f*level),center,style=Stroke((2+level*5).dp.toPx()))
        }
        Image(painterResource(R.drawable.brand_logo),"ভয়েস শুরু বা মাঝখানে বাধা দাও",Modifier.size((sizeDp*.65).dp).clip(CircleShape).border(2.dp,tone.copy(alpha=.65f),CircleShape),contentScale=ContentScale.Crop)
    }
}
@Composable private fun WaveStrip(phase:VoicePhase,level:Float,motion:Boolean) {
    val cycle=rememberInfiniteTransition(label="waveform")
    val enabled=motionEnabled(motion)
    val progress=if(enabled)cycle.animateFloat(0f,1f,infiniteRepeatable(tween(1100,easing=LinearEasing)),label="wave").value else .2f
    Canvas(Modifier.width(164.dp).height(35.dp)) {
        for(i in 0..16){
            val strength=when(phase){VoicePhase.LISTENING->level;VoicePhase.SPEAKING->.7f;VoicePhase.THINKING,VoicePhase.STARTING->.18f;else->0f}
            val h=4.dp.toPx()+(size.height*.8f*strength*(.35f+.65f*kotlin.math.abs(sin((progress*2*PI+i*.65).toFloat()))))
            val x=size.width*(i+.5f)/17
            drawLine(if(i%3==0)Iris else Ice.copy(alpha=.75f),Offset(x,(size.height-h)/2),Offset(x,(size.height+h)/2),3.dp.toPx(),StrokeCap.Round)
        }
    }
}
@Composable private fun GradientAction(text:String,icon:ImageVector,onClick:()->Unit,modifier:Modifier=Modifier) {
    Box(modifier.height(52.dp).shadow(12.dp,RoundedCornerShape(18.dp),ambientColor=Electric.copy(alpha=.2f),spotColor=Electric.copy(alpha=.25f)).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Electric,Color(0xFF7465EA)))).clickable(onClick=onClick),contentAlignment=Alignment.Center){
        Row(verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Color.White,modifier=Modifier.size(21.dp));Spacer(Modifier.width(8.dp));Text(text,color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=15.sp)}
    }
}
@Composable private fun PremiumTile(title:String,detail:String,icon:ImageVector,onClick:()->Unit,modifier:Modifier=Modifier) {
    Glass(modifier.clickable(onClick=onClick)) {
        Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(37.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Ice.copy(alpha=.16f),Iris.copy(alpha=.12f)))),contentAlignment=Alignment.Center){Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(21.dp))};Spacer(Modifier.width(8.dp));Icon(Icons.Outlined.NorthEast,null,modifier=Modifier.size(15.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)}
        Spacer(Modifier.height(13.dp));Text(title,fontWeight=FontWeight.Medium,fontSize=15.sp);Spacer(Modifier.height(3.dp));Label(detail)
    }
}
@Composable internal fun PremiumDashboard(vm:AssistantModel,mic:()->Unit,shortcut:(String)->Unit) {
    val motion=vm.store.flag("motion",true) && vm.foreground
    var line by remember {mutableIntStateOf(0)}
    val words=listOf("তোমার কথায়, তোমার পাশে।","ভাবনা থেকে কাজ, একসঙ্গে।","আড্ডা হোক, কাজও হোক।")
    LaunchedEffect(motion){while(motion){delay(4200);line=(line+1)%words.size}}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp).padding(top=12.dp,bottom=22.dp),verticalArrangement=Arrangement.spacedBy(17.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("PERSONAL INTELLIGENCE",fontSize=10.sp,letterSpacing=2.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);StatusChip(if(vm.chatProvider()!=null)"Connected" else "Let's connect",vm.chatProvider()!=null)}
        Column {Text("তোমার নিজস্ব",fontSize=29.sp,fontWeight=FontWeight.Light);Text("AI universe.",style=TextStyle(brush=Brush.linearGradient(listOf(Ice,Electric,Iris)),fontSize=35.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp))}
        Glass(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("VOICE SPACE",fontSize=10.sp,letterSpacing=2.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Icon(Icons.Outlined.GraphicEq,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(19.dp))}
            Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){OrbitAvatar(vm.voice.phase,vm.voice.level,motion,mic,216)}
            AnimatedContent(line,transitionSpec={(fadeIn(tween(500))+slideInVertically{it/3}) togetherWith fadeOut(tween(220))},label="rotating headline",modifier=Modifier.align(Alignment.CenterHorizontally)){i->Text(words[i],fontSize=18.sp,fontWeight=FontWeight.Medium,textAlign=TextAlign.Center)}
            Spacer(Modifier.height(6.dp));LabelCentered("কথা বলো • পরিকল্পনা করো • কাজ এগিয়ে নাও")
            Spacer(Modifier.height(19.dp));GradientAction("ভয়েসে কথা বলো",Icons.Outlined.Mic,mic,Modifier.fillMaxWidth())
            TextButton(onClick={vm.tab=1},modifier=Modifier.align(Alignment.CenterHorizontally)){Text("লিখে বলো",fontSize=12.sp);Spacer(Modifier.width(6.dp));Icon(Icons.Outlined.ArrowOutward,null,modifier=Modifier.size(14.dp))}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("QUICK ACTIONS",fontSize=11.sp,letterSpacing=1.8.sp,fontWeight=FontWeight.Medium);Label("তোমার নিয়ন্ত্রণে")}
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){PremiumTile("কল করো","নাম বা ডাকনাম",Icons.Outlined.Phone,{if(!vm.busy)shortcut("call") else vm.notice="আগের কাজ শেষ করো বা Stop চাপো।"},Modifier.weight(1f));PremiumTile("মেসেজ","খসড়া তৈরি করো",Icons.Outlined.Sms,{if(!vm.busy)shortcut("sms") else vm.notice="আগের কাজ শেষ করো বা Stop চাপো।"},Modifier.weight(1f))}
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){PremiumTile("অ্যাপ খোলো","এক কথায় শুরু",Icons.Outlined.Apps,{if(!vm.busy)shortcut("open_app") else vm.notice="আগের কাজ শেষ করো বা Stop চাপো।"},Modifier.weight(1f));PremiumTile("Build Studio","আইডিয়া থেকে source",Icons.Outlined.Code,{vm.tab=5},Modifier.weight(1f))}
        Glass(Modifier.fillMaxWidth()) {Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Shield,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("Private by design",fontSize=14.sp,fontWeight=FontWeight.Medium);Label("অনুমতি নিয়ে কাজ • এক ট্যাপে Stop")}}}
        vm.tasks.lastOrNull()?.let {t->Glass(Modifier.fillMaxWidth()){Label("LAST ACTIVITY");Spacer(Modifier.height(5.dp));Text(t.text,maxLines=2,overflow=TextOverflow.Ellipsis);Label(t.status)}}
        Text("RAKIB CHOWDHURY LABS  /  0.2.0",fontSize=10.sp,letterSpacing=1.5.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.align(Alignment.CenterHorizontally))
    }
}
@Composable private fun LabelCentered(text:String){Text(text,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())}

@Composable internal fun VoiceRoom(vm:AssistantModel,close:()->Unit,start:()->Unit,allowNetwork:()->Unit) {
    val ctx=LocalContext.current;val active=vm.voice.sessionActive;val phase=vm.voice.phase
    var seconds by remember {mutableLongStateOf(0L)}
    LaunchedEffect(active,vm.voice.startedAt){while(active){seconds=((System.currentTimeMillis()-vm.voice.startedAt)/1000).coerceAtLeast(0);delay(1000)}}
    DisposableEffect(active){val activity=ctx as? Activity;if(active)activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);onDispose{activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}}
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)){
        val view=LocalView.current
        DisposableEffect(active,view){
            val window=(view.parent as? DialogWindowProvider)?.window
            if(active)window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose{window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        }
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){Box(Modifier.fillMaxSize()){
            Atmosphere(vm.store.flag("motion",true) && vm.foreground)
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(17.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){NeonWordmark();Text("LIVE CONVERSATION",fontSize=10.sp,letterSpacing=2.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick=close){Icon(Icons.Outlined.Close,"ভয়েস বন্ধ করো")}}
                Spacer(Modifier.height(12.dp));StatusChip(if(active)"${seconds/60}:${(seconds%60).toString().padStart(2,'0')}  •  Voice session" else "Session off",active)
                Spacer(Modifier.height(4.dp))
                OrbitAvatar(phase,vm.voice.level,vm.store.flag("motion",true),{if(active)vm.interruptVoice() else start()},246)
                WaveStrip(phase,vm.voice.level,vm.store.flag("motion",true))
                Text(when(phase){VoicePhase.STARTING->"তোমার কথা শুনব…";VoicePhase.LISTENING->"বলো, শুনছি।";VoicePhase.THINKING->"একটু ভাবছি…";VoicePhase.SPEAKING->"তোমাকে বলছি…";VoicePhase.PAUSED->"একটু বিরতি।";VoicePhase.ERROR->"আবার চেষ্টা করি।";VoicePhase.OFF->"আবার কথা হবে?"},fontSize=25.sp,fontWeight=FontWeight.Medium,textAlign=TextAlign.Center)
                Text(vm.status,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center,maxLines=4,overflow=TextOverflow.Ellipsis)
                val caption=if(phase in listOf(VoicePhase.LISTENING,VoicePhase.THINKING))vm.voice.caption else vm.history.lastOrNull{it.who=="assistant"}?.text.orEmpty()
                if(caption.isNotBlank())Glass(Modifier.fillMaxWidth()){Text(caption.take(650),fontSize=15.sp,maxLines=7,overflow=TextOverflow.Ellipsis)}
                if(phase==VoicePhase.ERROR && !vm.store.flag("networkSpeech"))OutlinedButton(onClick=allowNetwork){Text("Speech network অনুমতি দাও")}
                if(phase==VoicePhase.ERROR && (vm.status.contains("TTS")||vm.status.contains("voice data")))TextButton(onClick={runCatching {ctx.startActivity(Intent("com.android.settings.TTS_SETTINGS"))}}){Text("ফোনের voice data সেটআপ")}
                Row(horizontalArrangement=Arrangement.spacedBy(20.dp),verticalAlignment=Alignment.CenterVertically){
                    FilledTonalIconButton(onClick={if(!active)start() else if(phase in listOf(VoicePhase.PAUSED,VoicePhase.ERROR))vm.interruptVoice() else vm.pauseVoice()},modifier=Modifier.size(58.dp)){Icon(if(phase==VoicePhase.PAUSED)Icons.Outlined.Mic else Icons.Outlined.MicOff,"Mic pause বা resume")}
                    FilledIconButton(onClick=close,modifier=Modifier.size(68.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color(0xFFDF526F))){Icon(Icons.Outlined.CallEnd,"Session শেষ করো",modifier=Modifier.size(27.dp))}
                    FilledTonalIconButton(onClick={if(active)vm.interruptVoice() else start()},modifier=Modifier.size(58.dp)){Icon(Icons.Outlined.GraphicEq,"উত্তর থামিয়ে কথা বলো")}
                }
                if(!active)GradientAction("Voice mode শুরু করো",Icons.Outlined.Mic,start,Modifier.fillMaxWidth())
                LabelCentered("বলতে থাকলে mic আবার নিজে শুনবে। উত্তর দেওয়ার সময় mic বন্ধ থাকে। মাঝখানে orbit চাপলে নতুন কথা নেওয়া হয়।")
                LabelCentered("${vm.store.text("voiceMinutes","10")} মিনিটে auto end • নীরবতায় auto stop • অ্যাপ background/lock হলে mic বন্ধ")
            }
        }}
    }
}

@Composable internal fun SimpleSettings(vm:AssistantModel){
    val ctx=LocalContext.current;val revision=vm.revision
    var connect by remember {mutableStateOf(false)};var editing by remember {mutableStateOf<Provider?>(null)}
    var key by remember {mutableStateOf(vm.store.secret("preferred-voice"))};var saved by remember {mutableStateOf("")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("Simple. Connected.",fontSize=28.sp,fontWeight=FontWeight.Light);Label("Provider বেছে key দাও। বাকি connection অ্যাপ ঠিক করবে।")
        GradientAction("AI connect করো",Icons.Outlined.Key,{editing=null;connect=true},Modifier.fillMaxWidth())
        vm.simpleProviders().forEach {p->
            val chosen=vm.chatProvider()?.id==p.id
            Glass(Modifier.fillMaxWidth()){
                Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Electric.copy(alpha=.3f),Iris.copy(alpha=.2f)))),contentAlignment=Alignment.Center){Icon(Icons.Outlined.AutoAwesome,null,tint=MaterialTheme.colorScheme.primary)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Medium);Label(if(chosen)"নির্বাচিত • Chat + voice + actions" else "সংরক্ষিত key")};if(chosen)Icon(Icons.Outlined.CheckCircle,null,tint=MaterialTheme.colorScheme.primary)}
                Row{TextButton(onClick={vm.select(p)},enabled=!vm.busy){Text("ব্যবহার করো")};TextButton(onClick={editing=p;connect=true}){Text("Reconnect")};TextButton(onClick={vm.deleteProvider(p)},enabled=!vm.busy){Text("সরাও")}}
            }
        }
        Glass(Modifier.fillMaxWidth()){
            Text("একটি key-তে মূল কাজগুলো",fontWeight=FontWeight.Medium);Spacer(Modifier.height(6.dp));Label("নির্বাচিত AI দিয়ে chat ও actions, আর ফোনের speech/TTS দিয়ে voice চলে। ElevenLabs-এর নির্দিষ্ট কণ্ঠ চাইলে শুধু তার key আলাদা লাগবে। Provider-এর quota/খরচ প্রযোজ্য।")
        }
        Glass(Modifier.fillMaxWidth()){
            Text("তোমার কণ্ঠসঙ্গী",fontSize=18.sp,fontWeight=FontWeight.Medium);Spacer(Modifier.height(8.dp))
            Row(verticalAlignment=Alignment.CenterVertically){RadioButton(vm.store.flag("preferredVoice",true),{vm.flag("preferredVoice",true)});Text("তোমার নির্বাচিত ElevenLabs voice",modifier=Modifier.weight(1f),fontSize=13.sp)}
            Row(verticalAlignment=Alignment.CenterVertically){RadioButton(!vm.store.flag("preferredVoice",true),{vm.flag("preferredVoice",false)});Text("ফোনের নারী কণ্ঠ / system fallback",modifier=Modifier.weight(1f),fontSize=13.sp)}
            if(vm.store.flag("preferredVoice",true)){
                Field(key,{key=it;saved=""},"ElevenLabs key (ঐচ্ছিক)",secret=true)
                TextButton(onClick={vm.savePreferredVoice(key);saved="পছন্দের voice key সংরক্ষণ হয়েছে।"}){Text("Voice key রাখো")}
                if(saved.isNotBlank())Label(saved)
                Label("কণ্ঠ নির্দিষ্ট রাখা হয়েছে। Key না থাকলে বা voice ব্যর্থ হলে ফোনের কণ্ঠ ব্যবহার হবে।")
            }
            TextButton(onClick={vm.voice.speak("হ্যালো, আমি বুশরা। এখন তোমার কথা শুনতে প্রস্তুত।",vm.ttsProvider(),true)},enabled=!vm.busy){Icon(Icons.Outlined.VolumeUp,null);Text(" কণ্ঠ শুনে দেখো")}
            if(vm.voice.phase==VoicePhase.ERROR)Text(vm.status,color=MaterialTheme.colorScheme.error,fontSize=12.sp)
            Label("ফোনে ভাষার উপযুক্ত female voice থাকলে সেটি আগে নেওয়া হবে। না থাকলে Android TTS-এ বাংলা voice data লাগতে পারে।")
            Row{FilterChip(vm.store.text("language","bn-BD")=="bn-BD",{vm.setting("language","bn-BD")},label={Text("বাংলা")});Spacer(Modifier.width(10.dp));FilterChip(vm.store.text("language")=="en-US",{vm.setting("language","en-US")},label={Text("English")})}
            Spacer(Modifier.height(5.dp));Label("এক session-এর সর্বোচ্চ সময়")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("3","10","30").forEach {min->FilterChip(vm.store.text("voiceMinutes","10")==min,{vm.setting("voiceMinutes",min)},label={Text("$min মিনিট")})}}
            Label("Mic start/stop, turn-taking, interruption ও silence recovery স্বয়ংক্রিয়। Gemini Live-এর মতো পূর্ণ-duplex audio নয়।")
        }
        Glass(Modifier.fillMaxWidth()){
            Text("Glassy universe",fontSize=18.sp,fontWeight=FontWeight.Medium)
            SettingSwitch("Motion graphics",vm.store.flag("motion",true)){vm.flag("motion",it)}
            SettingSwitch("Light theme",vm.store.flag("light")){vm.flag("light",it)}
            Label("Power saving mode-এ animation কমে যাবে।")
        }
        Glass(Modifier.fillMaxWidth()){
            Text("ফোনের অনুমতি",fontWeight=FontWeight.Medium)
            SettingSwitch("Speech network fallback",vm.store.flag("networkSpeech")){vm.flag("networkSpeech",it);vm.flag("speechConsentSeen",true)}
            Label("Network recognition-এ অডিও ফোনের speech provider-এ যেতে পারে।")
            TextButton(onClick={ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}){Text("Screen control চালু করো")}
            TextButton(onClick={runCatching{ctx.startActivity(Intent("com.android.settings.TTS_SETTINGS"))}}){Text("ফোনের voice data সেটআপ")}
            Label("Root/Termux লাগে না। Accessibility service শুধু তোমার অনুমোদিত কাজ করবে।")
        }
        Glass(Modifier.fillMaxWidth()){
            Text("Privacy & updates",fontWeight=FontWeight.Medium);Label("Keys encrypted; কোনো tracking SDK নেই। Chat, saved memory, alias labels ও app names নির্বাচিত AI-তে যায়। Screen text শুধু অনুমতির পরে পাঠানো হয়।")
            TextButton(onClick={vm.stop()}){Text("সব কাজ ও voice থামাও")}
            TextButton(onClick={ctx.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/techwithrakibofficial/JARVIS-AI-Android/releases")))}){Text("APK updates")}
        }
        Label("BUSHRA AI 0.2.0 • Rakib Chowdhury Labs")
    }
    if(connect)ConnectionSheet(vm,editing){connect=false;editing=null}
}
@Composable private fun ConnectionSheet(vm:AssistantModel,existing:Provider?,close:()->Unit){
    var service by remember {mutableStateOf(existing?.let(::serviceFor) ?: simpleServices.first())}
    var key by remember {mutableStateOf(existing?.let{vm.store.secret(it.id)}.orEmpty())}
    var gateway by remember {mutableStateOf(if(service.id=="openclaw")existing?.endpoint.orEmpty() else "")}
    var expanded by remember {mutableStateOf(false)};var show by remember {mutableStateOf(false)};var result by remember {mutableStateOf("")}
    FullSheet("AI connect করো",{if(!vm.connecting)close()}){
        Glass(Modifier.fillMaxWidth()){Text("দুই ধাপেই প্রস্তুত।",fontSize=23.sp,fontWeight=FontWeight.Light);Label("Provider বেছে key দাও। Model, endpoint ও protocol লেখা লাগবে না।")}
        Box{
            OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(),enabled=!vm.connecting){Text(service.title,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ExpandMore,null)}
            DropdownMenu(expanded,{expanded=false}){simpleServices.forEach{s->DropdownMenuItem(text={Column{Text(s.title);Label(s.subtitle)}},onClick={service=s;expanded=false;val saved=vm.simpleProviders().firstOrNull{serviceFor(it)?.id==s.id};key=saved?.let{vm.store.secret(it.id)}.orEmpty();gateway=if(s.id=="openclaw")saved?.endpoint.orEmpty() else "";result=""})}}
        }
        OutlinedTextField(value=key,onValueChange={key=it.trim();result=""},label={Text(if(service.id=="openclaw")"Gateway token" else "API key")},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!vm.connecting,shape=RoundedCornerShape(16.dp),visualTransformation=if(show)VisualTransformation.None else PasswordVisualTransformation(),trailingIcon={IconButton(onClick={show=!show}){Icon(if(show)Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,"Key দেখাও বা লুকাও")}})
        if(service.id=="openclaw"){
            Field(gateway,{gateway=it},"নিজের private HTTPS gateway address")
            Label("OpenClaw hosted AI key service নয়। নিজের gateway-এ Chat Completions চালু থাকতে হবে। Gateway agent-এর tools তার নিজের policy অনুযায়ী চলে।")
        }
        if(service.id=="openai")Label("OpenAI API key ব্যবহার করো। ChatGPT login বা password এখানে নয়।")
        if(service.id=="openrouter")Label("OpenRouter-এ শুধু পাওয়া যাওয়া free model/router নেওয়া হবে; paid model-এ নিজে থেকে বদলাবে না।")
        else Label("তোমার account-এর উপলব্ধ chat model স্বয়ংক্রিয়ভাবে নেওয়া ও পরীক্ষা করা হবে। API quota/খরচ provider-এর।")
        if(vm.connecting){LinearProgressIndicator(Modifier.fillMaxWidth());Label("Key যাচাই ও automatic setup হচ্ছে…")}
        if(result.isNotBlank())Glass(Modifier.fillMaxWidth()){Text(result,fontSize=14.sp)}
        Button(onClick={vm.connectSimple(service,key,gateway){ok,message->result=message;if(ok)close()}},enabled=key.isNotBlank()&&!vm.connecting,modifier=Modifier.fillMaxWidth().height(50.dp)){Text("Connect & save")}
        Label("Key ফোনে encrypted থাকবে। আর কোনো model field পূরণ করতে হবে না।")
    }
}
