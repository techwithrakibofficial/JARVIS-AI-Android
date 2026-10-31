package dev.rakib.bushra

import org.junit.Assert.*
import org.junit.Test

class AutoChatRulesTest {
 private val now=100_000L
 private fun rule()=AutoRule(id="r",channel="sms",label="Rakib",phone="01712345678",instruction="শুধু সময় ঠিক করো",created=90_000,expires=200_000,limit=3)
 @Test fun onlySelectedRecipientCanTrigger(){assertTrue(autoRuleAllows(rule(),"sms","+8801712345678",now,now));assertFalse(autoRuleAllows(rule(),"sms","01812345678",now,now))}
 @Test fun channelsNeverCross(){assertFalse(autoRuleAllows(rule(),"whatsapp",rule().phone,now,now))}
 @Test fun stoppedOrExpiredNeverSend(){assertFalse(autoRuleAllows(rule().copy(enabled=false),"sms",rule().phone,now,now));assertFalse(autoRuleAllows(rule(),"sms",rule().phone,now,200_001))}
 @Test fun limitStopsLoop(){assertFalse(autoRuleAllows(rule().copy(count=3),"sms",rule().phone,now,now))}
 @Test fun oldNotificationCannotTriggerNewRule(){assertFalse(autoRuleAllows(rule(),"sms",rule().phone,89_999,now))}
 @Test fun staleQueueExpires(){assertFalse(autoRuleAllows(rule().copy(expires=900_000),"sms",rule().phone,now,now+300_001))}
 @Test fun futureTimeRejected(){assertFalse(autoRuleAllows(rule(),"sms",rule().phone,now+1,now))}
 @Test fun invalidReplyNeverSends(){assertFalse(validAutoReply(""));assertFalse(validAutoReply("null"));assertFalse(validAutoReply("x".repeat(501)));assertTrue(validAutoReply("কাল বিকেলে কথা বলি।"))}
 @Test fun rulesRoundTrip(){val r=rule();assertEquals(r,AutoRule.parse(r.json()))}
 @Test fun eventsRoundTrip(){val e=AutoEvent(ruleId="r",channel="sms",phone=rule().phone,text="কখন আসবে?",time=now,signature="abc");assertEquals(e,AutoEvent.parse(e.json()))}
 @Test fun setupInstructionIsNotSentAsChatBody(){assertNull(routeCommand("রাকিব ব্রো কে এসএমএস করো এবং সে যে রিপ্লাই দিবে তার রিপ্লাই করো"));assertTrue(autoChatRequested("তার রিপ্লাই দাও"))}
 @Test fun screenshotCompoundCommandCleansContact(){val c=routeCommand("হোয়াটসঅ্যাপ ওপেন কর এবং রাকিব ব্রো কে হাই লিখে এসএমএস সেন্ড করো")!!;assertEquals("রাকিব ব্রো",c.args.getString("contact"));assertEquals("হাই",c.args.getString("text"));assertEquals("whatsapp",c.tool)}
 @Test fun secondScreenshotGrammarWorks(){val c=routeCommand("whatsapp ওপেন করো এবং রাকিব ব্রো কে হাই লিখে এসএমএস সেন্ড করো")!!;assertEquals("রাকিব ব্রো",c.args.getString("contact"))}
 @Test fun politeWordsDoNotBecomeContact(){assertEquals("রাকিব ব্রো",cleanCommandTarget("আচ্ছা সোনা তুমি রাকিব ব্রো"))}
 @Test fun modelNullIsNotShown(){assertEquals("",meaningfulReply(org.json.JSONObject.NULL));assertEquals("",meaningfulReply("null"));assertEquals("ঠিক আছে",meaningfulReply("ঠিক আছে"))}
 @Test fun numberEntryUsesSavedWhatsAppLabel(){val c=ContactAlias(name="Rakib Bro",phone="01712345678",aliases=emptyList());assertEquals(c,contactByPhone("+8801712345678",listOf(c)))}
 @Test fun duplicateCountryFormatsAreOneContact(){val c=ContactAlias(name="Rakib Bro",phone="01712345678",aliases=emptyList());assertEquals(1,resolveContacts("Rakib Bro",listOf(c,c.copy(phone="+8801712345678"))).size)}
 @Test fun spokenSpacingDoesNotLoseTheContact(){val c=ContactAlias(name="Rakib Bro",phone="01712345678",aliases=emptyList());assertEquals(c,resolveContacts("রাকিবব্রো",listOf(c)).single())}

}
