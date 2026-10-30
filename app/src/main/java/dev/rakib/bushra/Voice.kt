package dev.rakib.bushra

import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class Voice(private val context:Context,private val store:LocalStore,private val api:ApiClient,private val scope:CoroutineScope,
            private val status:(String)->Unit,private val transcript:(String)->Unit) {
    private var recognizer:SpeechRecognizer?=null
    private var player:MediaPlayer?=null
    private var speechJob:Job?=null
    private var generation=0
    private var ttsReady=false
    private val tts=TextToSpeech(context) { result -> ttsReady=result==TextToSpeech.SUCCESS }
    init {
        tts.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
            override fun onStart(id:String?) {}
            override fun onDone(id:String?) { Handler(Looper.getMainLooper()).post { status("প্রস্তুত") } }
            @Deprecated("Platform callback") override fun onError(id:String?) { Handler(Looper.getMainLooper()).post { status("TTS playback হয়নি। Voice data পরীক্ষা করুন।") } }
        })
    }
    var listening=false;private set
    fun stop() {
        generation++;listening=false;recognizer?.cancel();recognizer?.destroy();recognizer=null
        speechJob?.cancel();api.cancel();player?.release();player=null;tts.stop();status("প্রস্তুত")
    }
    fun listen() {
        stop();val token=generation
        val local=Build.VERSION.SDK_INT>=31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        if(!local && !store.flag("networkSpeech")) { status("Offline recognizer নেই। Settings-এ network speech অনুমতি দিন।");return }
        if(!SpeechRecognizer.isRecognitionAvailable(context)) { status("ফোনে speech recognition service পাওয়া যায়নি।");return }
        recognizer=if(local && Build.VERSION.SDK_INT>=31)SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object:RecognitionListener {
            override fun onReadyForSpeech(params:Bundle?) { if(token==generation){listening=true;status("শুনছি…")} }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB:Float) {}
            override fun onBufferReceived(buffer:ByteArray?) {}
            override fun onEndOfSpeech() { if(token==generation){listening=false;status("কথা বোঝার চেষ্টা করছি…")} }
            override fun onError(error:Int) { if(token==generation){listening=false;status(when(error){SpeechRecognizer.ERROR_NO_MATCH,SpeechRecognizer.ERROR_SPEECH_TIMEOUT->"কথা ধরা যায়নি। আবার mic চাপুন।";SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS->"Microphone permission দিন।";else->"Speech error $error। Language/network speech সেটিং পরীক্ষা করুন।"})} }
            override fun onResults(results:Bundle?) { if(token==generation){listening=false;val text=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull();if(!text.isNullOrBlank()){status("প্রস্তুত");transcript(text)}else status("কথা পাওয়া যায়নি।")} }
            override fun onPartialResults(partialResults:Bundle?) {}
            override fun onEvent(eventType:Int,params:Bundle?) {}
        })
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE,store.text("language","bn-BD")).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
        runCatching { recognizer?.startListening(intent) }.onFailure { listening=false;status("Speech চালু হয়নি। Permission পরীক্ষা করুন।") }
    }
    fun speak(text:String,provider:Provider?,force:Boolean=false) {
        if(!force && !store.flag("speak",true))return
        stop();val token=generation
        if(provider==null) { androidSpeech(text);return }
        speechJob=scope.launch {
            status("Voice তৈরি হচ্ছে…")
            try {
                val bytes=api.speech(provider,store.secret(provider.id),text)
                if(token!=generation)return@launch
                val file=File(context.cacheDir,"voice.mp3");file.writeBytes(bytes)
                player=MediaPlayer().apply {
                    setDataSource(file.absolutePath);setOnCompletionListener { status("প্রস্তুত");it.release();if(player===it)player=null }
                    setOnErrorListener { mp,_,_->mp.release();if(player===mp)player=null;status("Voice playback হয়নি।");true }
                    prepare();start()
                };status("বলছি…")
            } catch (_:kotlinx.coroutines.CancellationException) {} catch(_:Exception) {
                if(token==generation) { if(store.flag("ttsFallback",true)){status("Cloud voice হয়নি; ফোনের voice ব্যবহার করছি।");androidSpeech(text)}else status("Cloud voice হয়নি। API/voice access পরীক্ষা করুন।") }
            }
        }
    }
    fun voiceNames():List<String> = if(ttsReady)tts.voices?.map { it.name }?.sorted().orEmpty() else emptyList()
    private fun androidSpeech(text:String) {
        if(!ttsReady){status("ফোনের TTS এখনো প্রস্তুত নয়। আবার Test voice চাপুন।");return}
        val locale=Locale.forLanguageTag(store.text("language","bn-BD"))
        val result=tts.setLanguage(locale)
        if(result==TextToSpeech.LANG_MISSING_DATA || result==TextToSpeech.LANG_NOT_SUPPORTED){status("এই ভাষার TTS data নেই। Android TTS settings-এ voice install করুন।");return}
        tts.voices?.firstOrNull { it.name==store.text("androidVoice") }?.let { tts.voice=it }
        tts.setSpeechRate(store.text("speechRate","0.9").toFloatOrNull()?.coerceIn(0.6f,1.3f) ?: 0.9f)
        tts.speak(text.take(3500),TextToSpeech.QUEUE_FLUSH,null,"bushra-${System.nanoTime()}");status("ফোনের voice-এ বলছি…")
    }
    fun close() { stop();tts.shutdown() }
}
