package dev.rakib.bushra

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlin.math.sqrt

/** Local energy gate only: no network recognition, files, or recurring recognizer start sounds. */
class VoiceActivityWatch(private val context:Context,private val scope:CoroutineScope){
 private var job:Job?=null
 @Volatile private var record:AudioRecord?=null
 private var generation=0
 fun stop(){generation++;job?.cancel();job=null;runCatching{record?.stop()}}
 @android.annotation.SuppressLint("MissingPermission")
 @Suppress("DEPRECATION")
 fun start(playback:Boolean,onUnavailable:()->Unit={},onVoice:()->Unit):Boolean{
  stop()
  if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)return false
  val headphones=context.getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_OUTPUTS).any{it.type in setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET,AudioDeviceInfo.TYPE_WIRED_HEADPHONES,AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,AudioDeviceInfo.TYPE_BLUETOOTH_SCO,AudioDeviceInfo.TYPE_USB_HEADSET)}
  if(playback && !headphones && !AcousticEchoCanceler.isAvailable())return false
  val token=generation
  fun unavailable(){Handler(Looper.getMainLooper()).post{if(token==generation)onUnavailable()}}
  job=scope.launch(Dispatchers.IO){
   var audio:AudioRecord?=null;var echo:AcousticEchoCanceler?=null;var noise:NoiseSuppressor?=null
   try{
    delay(450)
    val min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
    if(min<=0){unavailable();return@launch}
    audio=AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,maxOf(min,6400));record=audio
    if(audio.state!=AudioRecord.STATE_INITIALIZED){unavailable();return@launch}
    if(AcousticEchoCanceler.isAvailable())echo=AcousticEchoCanceler.create(audio.audioSessionId)?.apply{enabled=true}
    if(NoiseSuppressor.isAvailable())noise=NoiseSuppressor.create(audio.audioSessionId)?.apply{enabled=true}
    audio.startRecording()
    val samples=ShortArray(800);var floor=200.0;var frames=0;var sustained=0
    while(isActive && token==generation){
     val n=audio.read(samples,0,samples.size);if(n<=0)break
     val rms=sqrt(samples.take(n).sumOf{it.toDouble()*it}/n)
     frames++
     if(frames<if(playback)16 else 8){floor=floor*.8+rms*.2;continue}
     val threshold=maxOf(if(playback)650.0 else 220.0,floor*(if(playback)3.5 else 2.7))
     if(rms>threshold)sustained++ else {sustained=0;floor=floor*.98+rms*.02}
     if(sustained>=4){Handler(Looper.getMainLooper()).post{if(token==generation)onVoice()};break}
    }
   }catch(_:CancellationException){}catch(_:Exception){unavailable()}finally{
    runCatching{audio?.stop()};echo?.release();noise?.release();audio?.release();if(record===audio)record=null
   }
  }
  return true
 }
}
