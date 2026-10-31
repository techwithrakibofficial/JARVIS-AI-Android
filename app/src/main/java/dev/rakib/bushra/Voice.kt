package dev.rakib.bushra

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class Voice(private val context:Context,private val store:LocalStore,private val api:ApiClient,private val scope:CoroutineScope,
            private val status:(String)->Unit,private val transcript:(String)->Unit) {
    private val main=Handler(Looper.getMainLooper())
    private val gate=VoiceTurnGate()
    private var recognizer:SpeechRecognizer?=null
    private val activityWatch=VoiceActivityWatch(context,scope)
    private var quietWakes=0
    private var spokenReference=""
    var onHandsFreeInterrupt:(()->Unit)?=null
    var interruptionHint by mutableStateOf("");private set
    private var player:MediaPlayer?=null
    private var speechJob:Job?=null
    private var timer:Job?=null
    private var retry:Job?=null
    private var preferNetwork=false
    private var busyRetries=0
    private var lastActivity=0L
    private var ttsReady=false
    private var ttsFailed=false
    private var pending:Pair<Int,String>?=null
    private var tts:TextToSpeech?=null
    var phase by mutableStateOf(VoicePhase.OFF);private set
    var sessionActive by mutableStateOf(false);private set
    var startedAt by mutableLongStateOf(0L);private set
    var caption by mutableStateOf("");private set
    var level by mutableFloatStateOf(0f);private set
    var selectedPhoneVoice by mutableStateOf("");private set
    val listening get()=phase==VoicePhase.LISTENING || phase==VoicePhase.STARTING
    init {
        tts=TextToSpeech(context) { result -> main.post {
            ttsReady=result==TextToSpeech.SUCCESS;ttsFailed=!ttsReady
            pending?.let { (token,text)->pending=null;if(token==gate.token){if(ttsReady)androidSpeech(text,token) else outputFailed("ফোনের TTS service প্রস্তুত হয়নি। TTS voice data install করুন।",token)} }
        } }
        tts?.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
            override fun onStart(id:String?) {main.post { if(id=="bushra-${gate.token}"){setPhase(VoicePhase.SPEAKING,"বলছি…");startBargeMonitor(spokenReference,gate.token)} }}
            override fun onDone(id:String?) {main.post { id?.removePrefix("bushra-")?.toIntOrNull()?.let(::finishOutput) }}
            @Deprecated("Platform callback") override fun onError(id:String?) {main.post {id?.removePrefix("bushra-")?.toIntOrNull()?.let {outputFailed("Voice playback হয়নি। TTS language data পরীক্ষা করুন।",it)}}}
        })
    }
    private fun setPhase(value:VoicePhase,message:String){phase=value;level=0f;status(message)}
    private fun clearInput(){val old=recognizer;recognizer=null;runCatching{old?.cancel()};runCatching{old?.destroy()}}
    private fun clearOutput(stopMonitor:Boolean=true){if(stopMonitor)stopBargeMonitor();pending=null;speechJob?.cancel();speechJob=null;api.cancel();runCatching{player?.stop()};player?.release();player=null;tts?.stop()}
    fun stop(){gate.stop();sessionActive=false;timer?.cancel();retry?.cancel();clearInput();clearOutput();caption="";setPhase(VoicePhase.OFF,"প্রস্তুত")}
    fun startSession(){
        stop();sessionActive=true;quietWakes=0;startedAt=System.currentTimeMillis();lastActivity=startedAt;preferNetwork=false;busyRetries=0;gate.begin()
        timer=scope.launch {
            val duration=(store.text("voiceMinutes","10").toLongOrNull() ?: 10).coerceIn(3,30)*60_000
            while(sessionActive){delay(1000);val now=System.currentTimeMillis()
                if(now-startedAt>=duration){stop();status("সময় শেষ, voice session বন্ধ হয়েছে।");break}
                if(now-lastActivity>=90_000 && phase in listOf(VoicePhase.STARTING,VoicePhase.LISTENING,VoicePhase.WAITING,VoicePhase.PAUSED,VoicePhase.ERROR)){stop();status("নীরব থাকায় voice session বন্ধ হয়েছে।");break}
            }
        }
        startInput()
    }
    /** One-shot dictation is deliberately separate from a conversation session. */
    fun listen(){stop();gate.begin();preferNetwork=false;busyRetries=0;startInput()}
    fun pauseForReply(){retry?.cancel();gate.thinking();clearInput();clearOutput();setPhase(VoicePhase.THINKING,"ভাবছি…")}
    fun pauseSession(){if(!sessionActive)return;gate.pause();retry?.cancel();clearInput();clearOutput();setPhase(VoicePhase.PAUSED,"Mic বন্ধ। আবার চাপলে শুনব।")}
    fun interrupt(){
        if(!sessionActive){startSession();return}
        retry?.cancel();gate.resume();clearInput();clearOutput();busyRetries=0;lastActivity=System.currentTimeMillis();startInput()
    }
    fun fail(message:String){gate.error();clearInput();clearOutput();setPhase(VoicePhase.ERROR,message)}
    private fun startInput(){
        if(!gate.active)return
        stopBargeMonitor()
        val token=gate.resume();clearInput();setPhase(VoicePhase.STARTING,"Mic চালু হচ্ছে…")
        val hasLocal=Build.VERSION.SDK_INT>=31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        val useLocal=hasLocal && !preferNetwork
        if(!useLocal && !store.flag("networkSpeech")){fail("ভয়েস শুরু করতে speech service-এর network অনুমতি দিন।");return}
        if(!useLocal && !SpeechRecognizer.isRecognitionAvailable(context)){fail("ফোনে speech service নেই। Google speech service install/enable করে আবার চেষ্টা করুন।");return}
        try {
            recognizer=if(useLocal && Build.VERSION.SDK_INT>=31)SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(object:RecognitionListener{
                override fun onReadyForSpeech(params:Bundle?){if(gate.listening(token)){setPhase(VoicePhase.LISTENING,"শুনছি…");caption=""}}
                override fun onBeginningOfSpeech(){if(token==gate.token)lastActivity=System.currentTimeMillis()}
                override fun onRmsChanged(rmsdB:Float){if(token==gate.token && phase==VoicePhase.LISTENING)level=((rmsdB+2)/12).coerceIn(0f,1f)}
                override fun onBufferReceived(buffer:ByteArray?){}
                override fun onEndOfSpeech(){if(token==gate.token){level=0f;status("কথা বোঝার চেষ্টা করছি…")}}
                override fun onError(error:Int){
                    if(token!=gate.token)return
                    clearInput()
                    if(useLocal && error in listOf(12,13,SpeechRecognizer.ERROR_CLIENT,SpeechRecognizer.ERROR_SERVER) && store.flag("networkSpeech")){
                        preferNetwork=true;scheduleInput(450);return
                    }
                    if(error in listOf(SpeechRecognizer.ERROR_NO_MATCH,SpeechRecognizer.ERROR_SPEECH_TIMEOUT)){
                        if(sessionActive && quietWakes<2)waitForVoice()
                        else {gate.pause();setPhase(VoicePhase.PAUSED,"কথা ধরা যায়নি। Mic চাপলে আবার শুনব; আর নিজে restart হবে না।")}
                        return
                    }
                    fail(when(error){
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS->"Microphone permission দিন।"
                        SpeechRecognizer.ERROR_NETWORK,SpeechRecognizer.ERROR_NETWORK_TIMEOUT->"Speech network পাওয়া যাচ্ছে না। ইন্টারনেট দেখে আবার চাপুন।"
                        12,13->"বাংলা speech language data নেই। Network speech অনুমতি দিন বা speech service-এ বাংলা download করুন।"
                        SpeechRecognizer.ERROR_AUDIO->"Mic অন্য অ্যাপ ব্যবহার করছে বা audio service ব্যস্ত। আবার চেষ্টা করুন।"
                        else->"Speech service error $error। Mic চাপলে আবার চেষ্টা হবে।"
                    })
                }
                override fun onResults(results:Bundle?){
                    val text=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if(!gate.accept(token,text))return
                    clearInput();caption=text;lastActivity=System.currentTimeMillis();busyRetries=0;quietWakes=0
                    if(sessionActive)setPhase(VoicePhase.THINKING,"ভাবছি…") else {gate.stop();setPhase(VoicePhase.OFF,"কথা পাওয়া গেছে")}
                    transcript(text)
                }
                override fun onPartialResults(results:Bundle?){if(token==gate.token)caption=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()}
                override fun onEvent(eventType:Int,params:Bundle?){}
            })
            val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,store.text("language","bn-BD"))
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1)
            recognizer?.startListening(intent)
        }catch(_:Exception){fail("Mic চালু হয়নি। Permission এবং ফোনের speech service পরীক্ষা করুন।")}
    }
    private fun scheduleInput(delayMs:Long){
        val token=gate.resume();retry?.cancel();retry=scope.launch{delay(delayMs);if(gate.active && gate.token==token)startInput()}
    }
    fun speak(text:String,provider:Provider?,force:Boolean=false){
        if(!force && !store.flag("speak",true)){finishWithoutOutput();return}
        retry?.cancel();clearInput();clearOutput();val token=gate.speaking();lastActivity=System.currentTimeMillis()
        spokenReference=text
        setPhase(VoicePhase.THINKING,"কণ্ঠ তৈরি হচ্ছে…")
        if(provider==null){androidSpeech(text,token);return}
        speechJob=scope.launch{
            try {
                val bytes=api.speech(provider,store.secret(provider.id),text)
                if(token!=gate.token)return@launch
                val file=File(context.cacheDir,"bushra-voice.mp3");file.writeBytes(bytes)
                val mp=MediaPlayer();player=mp
                mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                mp.setOnPreparedListener {if(token==gate.token){it.start();setPhase(VoicePhase.SPEAKING,"বলছি…");startBargeMonitor(text,token)}else it.release()}
                mp.setOnCompletionListener {it.release();if(player===it)player=null;finishOutput(token)}
                mp.setOnErrorListener {m,_,_->m.release();if(player===m)player=null;if(token==gate.token){if(!force)androidSpeech(text,token)else outputFailed("নির্বাচিত voice চালানো যায়নি।",token)};true}
                mp.setDataSource(file.absolutePath);mp.prepareAsync()
            }catch(_:kotlinx.coroutines.CancellationException){}catch(e:Exception){
                if(token==gate.token){if(force)outputFailed("Voice test ব্যর্থ: "+(e.message ?: "ElevenLabs key/access পরীক্ষা করুন।"),token) else androidSpeech(text,token)}
            }
        }
    }
    private fun androidSpeech(text:String,token:Int){
        if(token!=gate.token)return
        if(!ttsReady){
            if(ttsFailed){outputFailed("TTS service নেই। Android TTS settings থেকে voice install করুন।",token);return}
            pending=token to text
            scope.launch {delay(5000);if(pending?.first==token){pending=null;outputFailed("TTS প্রস্তুত হয়নি। Voice data install করে আবার চেষ্টা করুন।",token)}}
            return
        }
        val engine=tts ?: return
        val requested=Locale.forLanguageTag(store.text("language","bn-BD"))
        val candidates=engine.voices.orEmpty().filter { it.locale.language==requested.language }
        val best=candidates.maxByOrNull {femaleVoiceScore(it.name,it.locale.language==requested.language,!it.isNetworkConnectionRequired)}
        val language=best?.locale ?: requested
        if(engine.setLanguage(language) in listOf(TextToSpeech.LANG_MISSING_DATA,TextToSpeech.LANG_NOT_SUPPORTED)){
            outputFailed("এই ভাষার voice data নেই। Settings থেকে বাংলা TTS voice install করুন।",token);return
        }
        if(best!=null){engine.voice=best;selectedPhoneVoice=best.name}else selectedPhoneVoice="System voice"
        engine.setSpeechRate(.92f)
        setPhase(VoicePhase.SPEAKING,"ফোনের কণ্ঠে বলছি…")
        if(engine.speak(text.take(3500),TextToSpeech.QUEUE_FLUSH,null,"bushra-$token")==TextToSpeech.ERROR)outputFailed("TTS উত্তর বলতে পারেনি। Voice data পরীক্ষা করুন।",token)
    }
    private fun finishOutput(token:Int){
        if(token!=gate.token)return
        stopBargeMonitor()
        lastActivity=System.currentTimeMillis()
        if(sessionActive && gate.active){setPhase(VoicePhase.STARTING,"তোমার কথা শুনব…");scheduleInput(650)}else setPhase(VoicePhase.OFF,"প্রস্তুত")
    }
    private fun finishWithoutOutput(){if(sessionActive && gate.active)scheduleInput(400)else setPhase(VoicePhase.OFF,"প্রস্তুত")}
    private fun outputFailed(message:String,token:Int){if(token==gate.token){gate.error();setPhase(VoicePhase.ERROR,message)}}
    private fun stopBargeMonitor(){activityWatch.stop()}
    /** Stable playback never starts a second SpeechRecognizer. Optional interruption uses local AEC/VAD. */
    private fun startBargeMonitor(spoken:String,outputToken:Int){
        if(!sessionActive || gate.token!=outputToken || !store.flag("handsFreeInterrupt",false))return
        val started=activityWatch.start(true,onUnavailable={interruptionHint="Voice detector পাওয়া যায়নি; orbit দিয়ে থামাও।"}){
            if(sessionActive && gate.token==outputToken){
                clearOutput();gate.resume();onHandsFreeInterrupt?.invoke()
                status("থেমেছি। এবার পুরো নির্দেশটি বলো।");startInput()
            }
        }
        interruptionHint=if(started)"লোকাল voice detector চালু; থামার পরে পুরো নির্দেশ বলো। Headset-এ ভালো কাজ করে।" else "এই ফোনে echo cancellation/headset পাওয়া যায়নি। স্থির voice mode চলছে; orbit দিয়ে থামাও।"
    }
    private fun waitForVoice(){
        gate.pause();setPhase(VoicePhase.WAITING,"চুপচাপ অপেক্ষা করছি। আবার কথা বললে শুনব।")
        val token=gate.token
        if(!activityWatch.start(false,onUnavailable={if(sessionActive && gate.token==token)setPhase(VoicePhase.PAUSED,"Speech service প্রস্তুত নয়। Mic চাপলে চেষ্টা হবে।")}){if(sessionActive && gate.token==token){quietWakes++;startInput()}}){setPhase(VoicePhase.PAUSED,"Mic চাপলে আবার শুনব।")}
    }
    fun close(){stop();tts?.shutdown()}
}
