package dev.rakib.bushra

enum class VoicePhase { OFF, STARTING, LISTENING, WAITING, THINKING, SPEAKING, PAUSED, ERROR }

/** Rejects stale recognition/playback callbacks and prevents the assistant hearing itself. */
class VoiceTurnGate {
    var active=false;private set
    var phase=VoicePhase.OFF;private set
    var token=0;private set
    private var misses=0
    fun begin():Int { token++;active=true;misses=0;phase=VoicePhase.STARTING;return token }
    fun listening(t:Int):Boolean {if(!active||t!=token||phase!=VoicePhase.STARTING)return false;phase=VoicePhase.LISTENING;return true}
    fun accept(t:Int,text:String):Boolean {
        if(!active||t!=token||phase !in listOf(VoicePhase.LISTENING,VoicePhase.STARTING)||text.isBlank())return false
        token++;misses=0;phase=VoicePhase.THINKING;return true
    }
    fun thinking(){if(active){token++;phase=VoicePhase.THINKING}}
    fun speaking():Int {token++;if(active)phase=VoicePhase.SPEAKING;return token}
    fun playbackDone(t:Int):Boolean=active && token==t && phase==VoicePhase.SPEAKING
    fun resume():Int {token++;if(active)phase=VoicePhase.STARTING;return token}
    fun pause(){token++;if(active)phase=VoicePhase.PAUSED}
    fun error(){token++;if(active)phase=VoicePhase.ERROR}
    fun stop(){token++;active=false;phase=VoicePhase.OFF}
    fun missed(t:Int):Boolean {
        if(!active||t!=token)return false
        misses++;token++
        phase=if(misses<=1)VoicePhase.STARTING else VoicePhase.PAUSED
        return phase==VoicePhase.STARTING
    }
}
