package dev.rakib.bushra

/** Compare ASR text against current playback; never dispatch recognized assistant echo as a command. */
fun isHumanInterruption(heard:String,spoken:String):Boolean {
    val h=normalizedName(heard);val s=normalizedName(spoken)
    if(h.length<2 || s.isBlank())return false
    if(s.contains(h))return false
    val hw=h.split(' ').filter{it.isNotBlank()};val sw=s.split(' ').toSet()
    val novel=hw.filter {it !in sw}
    if(hw.size>=2 && novel.size>=2)return true
    return hw.size==1 && h in setOf("থামো","থাম","চুপ","স্টপ","stop","wait","শোনো","শোন")
}
