package dev.rakib.bushra

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AutoChatStore(context:Context){
 private val prefs=context.getSharedPreferences("bushra_auto_v1",Context.MODE_PRIVATE)
 companion object{private val lock=Any()}
 private fun array(key:String)=runCatching{JSONArray(prefs.getString(key,"[]"))}.getOrDefault(JSONArray())
 private fun put(key:String,items:List<JSONObject>){check(prefs.edit().putString(key,JSONArray(items).toString()).commit())}
 fun rules():List<AutoRule> = synchronized(lock){val a=array("rules");(0 until a.length()).mapNotNull{runCatching{AutoRule.parse(a.getJSONObject(it))}.getOrNull()}}
 fun events():List<AutoEvent> = synchronized(lock){val a=array("events");(0 until a.length()).mapNotNull{runCatching{AutoEvent.parse(a.getJSONObject(it))}.getOrNull()}}
 fun saveRule(rule:AutoRule)=synchronized(lock){put("rules",(rules().filterNot{it.id==rule.id || (it.channel==rule.channel && whatsappPhone(it.phone)==whatsappPhone(rule.phone))}+rule).takeLast(30).map{it.json()})}
 fun stop(id:String?=null)=synchronized(lock){put("rules",rules().map{if(id==null||it.id==id)it.copy(enabled=false).json() else it.json()})}
 fun addEvent(event:AutoEvent):Boolean=synchronized(lock){
  val rule=rules().firstOrNull{it.id==event.ruleId} ?: return@synchronized false
  if(!autoRuleAllows(rule,event.channel,event.phone,event.time,System.currentTimeMillis()) || events().any{it.signature==event.signature})return@synchronized false
  put("events",(events()+event.copy(text=event.text.take(2000))).takeLast(100).map{it.json()});true
 }
 fun state(id:String,state:String)=synchronized(lock){put("events",events().map{if(it.id==id)it.copy(state=state).json() else it.json()})}
 /** Reserve before external sending. A killed worker is never allowed to resend this event. */
 fun reserve(event:AutoEvent,now:Long):AutoRule?=synchronized(lock){
  val latest=events().firstOrNull{it.id==event.id} ?: return@synchronized null
  val rule=rules().firstOrNull{it.id==event.ruleId} ?: return@synchronized null
  if(latest.state!="pending" || !autoRuleAllows(rule,event.channel,event.phone,event.time,now) || now-rule.lastSent<15_000)return@synchronized null
  saveRule(rule.copy(count=rule.count+1,lastSent=now));state(event.id,"sending");rule
 }
 fun log(label:String,message:String)=synchronized(lock){
  val a=array("log");val items=(0 until a.length()).map{a.getJSONObject(it)}
  put("log",(items+JSONObject().put("label",label).put("text",message.take(1200)).put("time",System.currentTimeMillis())).takeLast(60))
 }
 fun logs():List<String> = synchronized(lock){val a=array("log");(0 until a.length()).map{val j=a.getJSONObject(it);j.optString("label")+" · "+j.optString("text")}.asReversed()}
 fun thread(ruleId:String):List<ChatLine> = synchronized(lock){
  val a=array("thread_$ruleId");(0 until a.length()).map{val j=a.getJSONObject(it);ChatLine(j.getString("who"),j.getString("text"))}
 }
 fun append(ruleId:String,who:String,text:String)=synchronized(lock){put("thread_$ruleId",(thread(ruleId)+ChatLine(who,text.take(2000))).takeLast(16).map{it.json()})}
}
