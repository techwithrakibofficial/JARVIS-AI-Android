package dev.rakib.bushra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class IncomingSmsReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  if(intent.action!=Telephony.Sms.Intents.SMS_RECEIVED_ACTION)return
  val parts=Telephony.Sms.Intents.getMessagesFromIntent(intent)
  if(parts.isEmpty())return
  val phone=parts.first().originatingAddress.orEmpty();val text=parts.joinToString(""){it.messageBody.orEmpty()}
  if(!validPhone(phone) || text.startsWith("[BUSHRA AI]"))return
  val time=System.currentTimeMillis();val auto=AutoChatStore(context)
  val rule=auto.rules().singleOrNull{autoRuleAllows(it,"sms",phone,time,time)} ?: return
  val signature=eventSignature("sms|$phone|${parts.first().timestampMillis}|$text")
  queueAutoEvent(context,AutoEvent(ruleId=rule.id,channel="sms",phone=phone,text=text,time=time,signature=signature))
 }
}
