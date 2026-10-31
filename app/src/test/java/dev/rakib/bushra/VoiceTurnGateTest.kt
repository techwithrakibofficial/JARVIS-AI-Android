package dev.rakib.bushra

import org.junit.Assert.*
import org.junit.Test

class VoiceTurnGateTest {
    @Test fun microphoneOnlyOpensAfterStart(){val g=VoiceTurnGate();assertFalse(g.listening(0));assertFalse(g.active);assertEquals(VoicePhase.OFF,g.phase)}
    @Test fun startReadyTranscriptMovesToThinking(){val g=VoiceTurnGate();val t=g.begin();assertTrue(g.listening(t));assertTrue(g.accept(t,"বাংলা কথা"));assertEquals(VoicePhase.THINKING,g.phase)}
    @Test fun duplicateFinalTranscriptCannotSubmitTwice(){val g=VoiceTurnGate();val t=g.begin();g.listening(t);assertTrue(g.accept(t,"Call Mum"));assertFalse(g.accept(t,"Call Mum"))}
    @Test fun blankTranscriptDoesNotMakeRequest(){val g=VoiceTurnGate();val t=g.begin();g.listening(t);assertFalse(g.accept(t," "));assertEquals(VoicePhase.LISTENING,g.phase)}
    @Test fun speakingCannotBeMistakenForUserInput(){val g=VoiceTurnGate();val t=g.begin();g.listening(t);g.accept(t,"hello");val speech=g.speaking();assertFalse(g.accept(speech,"assistant echo"));assertFalse(g.listening(speech))}
    @Test fun staleRecognitionAfterStopIsIgnored(){val g=VoiceTurnGate();val t=g.begin();g.listening(t);g.stop();assertFalse(g.accept(t,"send message"));assertFalse(g.active)}
    @Test fun stalePlaybackCannotRestartMicrophone(){val g=VoiceTurnGate();g.begin();val t=g.speaking();g.stop();assertFalse(g.playbackDone(t))}
    @Test fun interruptionInvalidatesPlaybackBeforeListening(){val g=VoiceTurnGate();g.begin();val spoken=g.speaking();val next=g.resume();assertFalse(g.playbackDone(spoken));assertTrue(g.listening(next));assertTrue(g.accept(next,"new question"))}
    @Test fun pauseInvalidatesRecognitionAndPlayback(){val g=VoiceTurnGate();val t=g.begin();g.listening(t);g.pause();assertFalse(g.accept(t,"old result"));assertFalse(g.playbackDone(t));assertEquals(VoicePhase.PAUSED,g.phase)}
    @Test fun silenceRetriesAreBounded(){val g=VoiceTurnGate();val t=g.begin();g.listening(t);assertTrue(g.missed(t));val second=g.resume();g.listening(second);assertFalse(g.missed(second));assertEquals(VoicePhase.PAUSED,g.phase)}
    @Test fun newUtteranceResetsSilenceBudget(){val g=VoiceTurnGate();val t=g.begin();g.missed(t);val t2=g.resume();g.listening(t2);g.accept(t2,"hello");val t3=g.resume();g.listening(t3);assertTrue(g.missed(t3))}
    @Test fun errorDoesNotAcceptLateResults(){val g=VoiceTurnGate();val t=g.begin();g.error();assertFalse(g.accept(t,"late"));assertEquals(VoicePhase.ERROR,g.phase)}
    @Test fun newSessionRejectsOldSessionCallbacks(){val g=VoiceTurnGate();val old=g.begin();val next=g.begin();assertFalse(g.listening(old));assertTrue(g.listening(next))}
    @Test fun interruptedThinkingRejectsPreviousSpeechCompletion(){val g=VoiceTurnGate();g.begin();val t=g.speaking();g.thinking();assertFalse(g.playbackDone(t))}
}
