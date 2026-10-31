package dev.rakib.bushra

import org.junit.Assert.*
import org.junit.Test

class CommandRulesTest {
    @Test fun bengaliWhatsAppLaunchUsesKnownPackage(){assertEquals("com.whatsapp",appPackages("হোয়াটসঅ্যাপ").first());assertEquals("com.whatsapp",appPackages("হোয়াটসঅ্যাপ").first())}
    @Test fun simpleOpenByVoiceNeedsNoModel(){assertEquals("open_app",routeCommand("তুমি হোয়াটসঅ্যাপ ওপেন করো")?.tool);assertEquals("WhatsApp",routeCommand("open WhatsApp")?.args?.getString("name"))}
    @Test fun whatsappSmsWordNeverChoosesCarrier(){val c=routeCommand("হোয়াটসঅ্যাপ খুলে রাকিব ব্রো কে হাই লিখে এসএমএস সেন্ড করো")!!;assertEquals("whatsapp",c.tool);assertEquals("রাকিব ব্রো",c.args.getString("contact"));assertEquals("হাই",c.args.getString("text"))}
    @Test fun realSmsStillUsesSms(){assertEquals("sms",routeCommand("রাকিব ব্রো কে হাই এসএমএস পাঠাও")?.tool)}
    @Test fun missingMessageDoesNotGuessText(){assertNull(routeCommand("হোয়াটসঅ্যাপ রাকিব ব্রোকে মেসেজ"))}
    @Test fun ambiguousOpenIsLeftToResolution(){assertEquals("Facebook",routeCommand("Facebook ওপেন করো")?.args?.getString("name"))}
    @Test fun modelCannotDowngradeWhatsApp(){try{validateChannel("WhatsApp এ পাঠাও","sms");fail()}catch(_:IllegalArgumentException){}}
    @Test fun whatsappChannelAcceptsWhatsApp(){validateChannel("হোয়াটসঅ্যাপ এ এসএমএস করো","whatsapp")}
    @Test fun savedEnglishContactMatchesBanglaName(){val c=ContactAlias(name="Rakib Bro",phone="01712345678",aliases=emptyList());assertEquals(c,resolveContacts("রাকিব ব্রো",listOf(c)).single())}
    @Test fun soundCollisionsNeverChooseOne(){val a=ContactAlias(name="Rakib Bro",phone="01712345678",aliases=emptyList());val b=a.copy(name="Rokib Bro",phone="01812345678");assertEquals(2,resolveContacts("রাকিব ব্রো",listOf(a,b)).size)}
    @Test fun duplicateSavedNamesAcrossNumbersStayAmbiguous(){val a=ContactAlias(name="Rakib",phone="01712345678",aliases=emptyList());assertEquals(2,resolveContacts("Rakib",listOf(a,a.copy(phone="01812345678"))).size)}
    @Test fun bdWhatsappNumbersGetCountryCode(){assertEquals("8801712345678",whatsappPhone("01712345678"));assertEquals("8801712345678",whatsappPhone("+8801712345678"));assertEquals("14155552671",whatsappPhone("+1 415 555 2671"))}
    @Test fun elevenVoiceIdWorks(){assertEquals("75CHIF904aWz6uq77J3P",parseElevenVoice("75CHIF904aWz6uq77J3P"))}
    @Test fun elevenVoiceLinksWork(){assertEquals("75CHIF904aWz6uq77J3P",parseElevenVoice("https://elevenlabs.io/voices/75CHIF904aWz6uq77J3P"));assertEquals("75CHIF904aWz6uq77J3P",parseElevenVoice("https://elevenlabs.io/app/voice-library?voiceId=75CHIF904aWz6uq77J3P"))}
    @Test fun thirdPartyVoiceLinkRejected(){try{parseElevenVoice("https://evil.example/voices/75CHIF904aWz6uq77J3P");fail()}catch(_:IllegalArgumentException){}}
    @Test fun echoMustNotInterrupt(){assertFalse(isHumanInterruption("আমি তোমার কথা শুনছি","হ্যালো আমি তোমার কথা শুনছি এখন বলো"));assertFalse(isHumanInterruption("করো","অ্যাপটি খুলে কাজ করো"))}
    @Test fun newUserSpeechInterrupts(){assertTrue(isHumanInterruption("না আগে ইউটিউব খোলো","আমি তোমার জন্য একটি পরিকল্পনা করছি"));assertTrue(isHumanInterruption("থামো","তোমার প্রশ্নের উত্তর দিচ্ছি"))}
    @Test fun unrelatedSingleNoiseDoesNotInterrupt(){assertFalse(isHumanInterruption("আহ","তোমার প্রশ্নের উত্তর দিচ্ছি"));assertFalse(isHumanInterruption("", "আমি বলছি"))}
}
