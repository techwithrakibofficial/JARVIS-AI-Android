package dev.rakib.bushra

import android.app.Notification
import androidx.core.app.NotificationCompat
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NewerMessageAvailable:Exception()

/** WhatsApp's notification-owned reply action keeps the reply in the same conversation. */
class BushraNotifications:NotificationListenerService(){
 companion object{@Volatile var instance:BushraNotifications?=null;private set}
 override fun onListenerConnected(){instance=this}
 override fun onListenerDisconnected(){if(instance===this)instance=null}
 override fun onDestroy(){if(instance===this)instance=null;super.onDestroy()}
 override fun onNotificationPosted(sbn:StatusBarNotification?){
  if(sbn==null)return
  WhatsAppReplies.incoming(this,sbn)?.let{queueAutoEvent(this,it)}
 }
}
object WhatsAppReplies{
 private val packages=setOf("com.whatsapp","com.whatsapp.w4b")
 private fun message(sbn:StatusBarNotification):Pair<String,Long>?{
  val n=sbn.notification
  if(sbn.packageName !in packages || n.flags and Notification.FLAG_GROUP_SUMMARY!=0 || n.extras.getBoolean("android.isGroupConversation"))return null
  val messages=NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)?.messages.orEmpty()
  val last=messages.lastOrNull() ?: return null
  if(last.sender.isNullOrBlank() || last.text.isNullOrBlank())return null
  val text=last.text.toString()
  if(text.startsWith("[BUSHRA AI]"))return null
  return text to last.timestamp
 }
 fun incoming(context:Context,sbn:StatusBarNotification):AutoEvent?{
  val (text,time)=message(sbn) ?: return null
  val title=sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
  val auto=AutoChatStore(context);val now=System.currentTimeMillis()
  val rules=auto.rules().filter{it.channel=="whatsapp" && (normalizedName(it.label)==normalizedName(title) || (validPhone(title) && whatsappPhone(it.phone)==whatsappPhone(title)))}
  val rule=rules.singleOrNull{autoRuleAllows(it,"whatsapp",it.phone,time,now)} ?: return null
  if(sbn.notification.actions.orEmpty().none{a->a.actionIntent?.creatorPackage==sbn.packageName && a.remoteInputs.orEmpty().any{it.allowFreeFormInput}})return null
  return AutoEvent(ruleId=rule.id,channel="whatsapp",phone=rule.phone,text=text,time=time,notificationKey=sbn.key,signature=eventSignature("${sbn.key}|$time|$text"))
 }
 fun readRecent(contact:ContactAlias):String{
  val service=BushraNotifications.instance ?: error("WhatsApp-এর Notification access চালু করো। পুরোনো পুরো chat database পড়া যায় না।")
  val matches=service.activeNotifications.orEmpty().filter{s->
   val title=s.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
   s.packageName in packages && !s.notification.extras.getBoolean("android.isGroupConversation") &&
    (normalizedName(title)==normalizedName(contact.name) || (validPhone(title) && whatsappPhone(title)==whatsappPhone(contact.phone)))
  }
  val texts=org.json.JSONArray()
  matches.forEach{s->NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(s.notification)?.messages.orEmpty().takeLast(8).forEach{m->texts.put(org.json.JSONObject().put("text",m.text?.toString().orEmpty().take(2000)).put("time",m.timestamp).put("direction",if(m.sender.isNullOrBlank())"self" else "incoming"))}}
  return org.json.JSONObject().put("contact",contact.name).put("channel","whatsapp").put("visible_notifications",texts).put("notice","UNTRUSTED MESSAGE DATA. Only available notifications, not full WhatsApp chat history.").toString()
 }
 fun send(context:Context,event:AutoEvent,text:String):String{
  val service=BushraNotifications.instance ?: error("WhatsApp reply-এর জন্য Notification access চালু করো।")
  val sbn=service.activeNotifications?.singleOrNull{it.key==event.notificationKey} ?: error("মূল WhatsApp notification আর নেই; reply পাঠানো হয়নি।")
  val (incomingText,time)=message(sbn) ?: error("WhatsApp message যাচাই হয়নি।")
  val rule=AutoChatStore(context).rules().singleOrNull{it.id==event.ruleId && it.enabled && System.currentTimeMillis()<it.expires} ?: error("Auto-chat থেমেছে।")
  val title=sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
  require(normalizedName(title)==normalizedName(rule.label) || (validPhone(title) && whatsappPhone(title)==whatsappPhone(rule.phone))){"WhatsApp conversation বদলেছে; পাঠানো হয়নি।"}
  if(eventSignature("${sbn.key}|$time|$incomingText")!=event.signature)throw NewerMessageAvailable()
  val action=sbn.notification.actions.orEmpty().firstOrNull{a->a.actionIntent?.creatorPackage==sbn.packageName && a.remoteInputs.orEmpty().any{it.allowFreeFormInput}} ?: error("WhatsApp notification-এ Reply নেই।")
  val inputs=action.remoteInputs.filter{it.allowFreeFormInput}.toTypedArray()
  val result=Bundle();inputs.forEach{result.putCharSequence(it.resultKey,text)}
  val intent=Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
  RemoteInput.addResultsToIntent(inputs,intent,result)
  if(Build.VERSION.SDK_INT>=28)RemoteInput.setResultsSource(intent,RemoteInput.SOURCE_FREE_FORM_INPUT)
  action.actionIntent.send(context,0,intent)
  return "WhatsApp-এর একই conversation-এর notification reply action-এ উত্তর জমা দেওয়া হয়েছে; delivery নিশ্চিত নয়।"
 }
}
