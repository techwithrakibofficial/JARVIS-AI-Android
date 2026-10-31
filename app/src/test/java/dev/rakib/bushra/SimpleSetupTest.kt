package dev.rakib.bushra

import org.junit.Assert.*
import org.junit.Test

class SimpleSetupTest {
    @Test fun onlyRequestedServiceFamiliesAreShown(){assertEquals(setOf("openrouter","openai","deepseek","gemini","claude","groq","grok","openclaw"),simpleServices.map{it.id}.toSet())}
    @Test fun openAiChatGptIsOneConnection(){assertEquals(1,simpleServices.count{it.title.contains("ChatGPT")});assertTrue(simpleServices.first{it.id=="openai"}.title.contains("OpenAI"))}
    @Test fun setupDoesNotShowSpeechOnlyProviders(){assertTrue(simpleServices.all{it.provider().isChat})}
    @Test fun providerDefaultsUseNativeAuthentication(){val g=simpleServices.first{it.id=="gemini"}.provider();assertEquals(ApiProtocol.GEMINI,g.protocol);assertEquals("x-goog-api-key",g.authHeader);assertEquals("",g.authPrefix)}
    @Test fun oldProviderNamesMigrateToSimpleUi(){assertEquals("gemini",serviceFor(providerPresets.first{it.name=="Google Gemini"}.provider())?.id);assertEquals("openai",serviceFor(providerPresets.first{it.name=="OpenAI"}.provider())?.id)}
    @Test fun freeRoutingNeverIncludesPaidFallback(){val ids=listOf("paid/model","vendor/model:free","openrouter/free","vendor/audio:free");assertEquals(listOf("openrouter/free","vendor/model:free"),chatCandidates(ids,"openrouter"))}
    @Test fun noAvailableFreeModelFailsClosed(){assertTrue(chatCandidates(listOf("paid/model"),"openrouter").isEmpty())}
    @Test fun geminiDiscoveryExcludesAudioImageLiveModels(){val ids=listOf("gemini-3.5-flash-lite","gemini-3.8-flash-tts","gemini-3.8-live","gemini-3.1-flash-image");assertEquals(listOf("gemini-3.5-flash-lite"),chatCandidates(ids,"gemini"))}
    @Test fun chatSetupCannotChooseEmbeddingOrWhisper(){assertEquals(listOf("llama-versatile"),chatCandidates(listOf("whisper-large-v3","text-embedding-3","llama-versatile"),"groq"))}
    @Test fun cheaperChatFamilyPreferredOverFlagship(){val ids=listOf("gpt-6","gpt-6-mini","gpt-audio");assertEquals("gpt-6-mini",chatCandidates(ids,"openai").first())}
    @Test fun femaleNameDoesNotAccidentallyCountAsMale(){assertTrue(femaleVoiceScore("female",true,true)>femaleVoiceScore("male",true,true))}
    @Test fun matchingLanguageWinsOverForeignFemaleVoice(){assertTrue(femaleVoiceScore("system",true,true)>femaleVoiceScore("female",false,true))}
    @Test fun openClawUsesDefaultAgentNotAnInventedModel(){assertEquals("openclaw/default",simpleServices.first{it.id=="openclaw"}.provider().model)}
}
