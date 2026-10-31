package dev.rakib.bushra

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsStatusReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val store=LocalStore(context)
        val label=intent.getStringExtra("label").orEmpty()
        val part=intent.getIntExtra("part",1);val total=intent.getIntExtra("total",1)
        val status=if(resultCode==Activity.RESULT_OK)"SIM থেকে পাঠানো হয়েছে; delivery নিশ্চিত নয়" else "SMS ব্যর্থ (code $resultCode); নিজে থেকে retry হয়নি"
        synchronized(SmsStatusReceiver::class.java){store.saveTasks((store.tasks()+TaskEntry("SMS: $label · $part/$total",status)).takeLast(100))}
    }
}
