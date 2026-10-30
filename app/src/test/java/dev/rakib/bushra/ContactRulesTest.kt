package dev.rakib.bushra

import org.junit.Assert.*
import org.junit.Test

class ContactRulesTest {
    private val mum=ContactAlias(id="1",name="Mrs A",phone="+8801712345678",aliases=listOf("মা","আম্মু","Amma"))
    private val boss=ContactAlias(id="2",name="Office",phone="01812345678",aliases=listOf("বস","Boss"))
    @Test fun banglaAliasResolvesOriginalNumber(){assertEquals(mum,resolveContacts("আম্মু",listOf(mum,boss)).single())}
    @Test fun englishAliasIgnoresCaseAndSpacing(){assertEquals(mum,resolveContacts("  AMMA  ",listOf(mum,boss)).single())}
    @Test fun exactAliasWinsOverSubstring(){val other=boss.copy(aliases=listOf("Amma office"));assertEquals(mum,resolveContacts("Amma",listOf(other,mum)).single())}
    @Test fun ambiguousNamesRemainAmbiguous(){val other=boss.copy(name="Mrs B");assertEquals(2,resolveContacts("Mrs",listOf(mum,other)).size)}
    @Test fun phonePunctuationNormalizes(){assertEquals("+8801712345678",normalizedPhone("+880 1712-345678"))}
    @Test fun banglaDigitsNormalize(){assertEquals("01712345678",normalizedPhone("০১৭১২৩৪৫৬৭৮"))}
    @Test fun banglaPhoneFindsStoredContact(){val c=mum.copy(phone="01712345678");assertEquals(c,resolveContacts("০১৭১২৩৪৫৬৭৮",listOf(c)).single())}
    @Test fun duplicateAliasAcrossNumbersRejected(){assertTrue(aliasConflict(boss.copy(aliases=listOf("Amma")),listOf(mum)))}
    @Test fun editOwnAliasAllowed(){assertFalse(aliasConflict(mum.copy(aliases=listOf("মা","Mom")),listOf(mum,boss)))}
    @Test fun shortAndInvalidPhonesRejected(){assertFalse(validPhone("12"));assertFalse(validPhone(""));assertFalse(validPhone("1234567890123456"))}
    @Test fun blankQueryNeverChoosesContact(){assertTrue(resolveContacts("",listOf(mum,boss)).isEmpty())}
    @Test fun lettersInPhoneMustNotBecomeUnintendedNumber(){assertFalse(validPhone("boss01712345678"));assertFalse(validPhone("++8801712345678"))}
    @Test fun unknownNameNeverGuesses(){assertTrue(resolveContacts("Unknown",listOf(mum,boss)).isEmpty())}
    @Test fun sharedNumberDeduplicates(){assertEquals(1,resolveContacts("মা",listOf(mum,mum.copy(id="3"))).size)}
    @Test fun safeExportPaths(){assertTrue(safeProjectPath("app/src/main/AndroidManifest.xml"));assertTrue(safeProjectPath("index.html"))}
    @Test fun exportTraversalRejected(){listOf("../secret","a/../../b","/etc/passwd","C:/file","a\\..\\b",".git/config","a//b","key.jks",".","a/./b","a\u0000b").forEach{assertFalse(it,safeProjectPath(it))}}
    @Test fun malformedModelReplyDoesNotBecomeAction(){assertNull(parseEnvelope("I will call someone now"));assertNull(parseEnvelope("{broken"))}
    @Test fun fencedJsonAccepted(){assertEquals("hello",parseEnvelope("```json\n{\"reply\":\"hello\",\"actions\":[]}\n```")?.getString("reply"))}
    @Test fun providerRoundTripPreservesCustomNameAndAuth(){val p=Provider(name="My private API",endpoint="https://example.org/chat/completions",model="my-model",authHeader="x-custom-key",authPrefix="");assertEquals(p,Provider.parse(p.json()))}
    @Test fun voiceNotClassifiedAsChat(){assertFalse(Provider(protocol=ApiProtocol.ELEVENLABS_TTS).isChat);assertFalse(Provider(protocol=ApiProtocol.OPENAI_TTS).isChat)}
    @Test fun nativeChatProtocolsRecognized(){assertTrue(Provider(protocol=ApiProtocol.GEMINI).isChat);assertTrue(Provider(protocol=ApiProtocol.ANTHROPIC).isChat)}
}
