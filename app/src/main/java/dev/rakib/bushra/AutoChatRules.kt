package dev.rakib.bushra

import org.json.JSONObject
import java.util.UUID

data class AutoRule(val id:String=UUID.randomUUID().toString(),val channel:String,val label:String,val phone:String,val instruction:String,
 val created:Long=System.currentTimeMillis(),val expires:Long,val limit:Int=10,val count:Int=0,val enabled:Boolean=true,val lastSent:Long=0){
 fun json()=JSONObject().put("id",id).put("channel",channel).put("label",label).put("phone",phone).put("instruction",instruction).put("created",created).put("expires",expires).put("limit",limit).put("count",count).put("enabled",enabled).put("lastSent",lastSent)
 companion object{fun parse(j:JSONObject)=AutoRule(j.getString("id"),j.getString("channel"),j.getString("label"),j.getString("phone"),j.getString("instruction"),j.getLong("created"),j.getLong("expires"),j.getInt("limit"),j.optInt("count"),j.optBoolean("enabled",true),j.optLong("lastSent"))}
}
data class AutoEvent(val id:String=UUID.randomUUID().toString(),val ruleId:String,val channel:String,val phone:String,val text:String,val time:Long,val notificationKey:String="",val signature:String="",val state:String="pending"){
 fun json()=JSONObject().put("id",id).put("ruleId",ruleId).put("channel",channel).put("phone",phone).put("text",text).put("time",time).put("notificationKey",notificationKey).put("signature",signature).put("state",state)
 companion object{fun parse(j:JSONObject)=AutoEvent(j.getString("id"),j.getString("ruleId"),j.getString("channel"),j.getString("phone"),j.getString("text"),j.getLong("time"),j.optString("notificationKey"),j.optString("signature"),j.optString("state","pending"))}
}
fun autoRuleAllows(rule:AutoRule,channel:String,phone:String,time:Long,now:Long):Boolean = rule.enabled && rule.channel==channel &&
 whatsappPhone(rule.phone)==whatsappPhone(phone) && now<rule.expires && time>=rule.created && now-time in 0..300_000 && rule.count<rule.limit
fun validAutoReply(text:String)=text.isNotBlank() && text.length<=500 && !text.trim().equals("null",true)
fun autoChatRequested(text:String)=Regex("অটো.?রিপ্লাই|auto.?reply|auto.?chat|তার.*রিপ্লাই|সে.*রিপ্লাই",RegexOption.IGNORE_CASE).containsMatchIn(text)
