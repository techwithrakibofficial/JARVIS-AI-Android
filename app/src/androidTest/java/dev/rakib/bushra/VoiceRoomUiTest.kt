package dev.rakib.bushra

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the actual Android permission/session UI; no live AI account is used. */
@RunWith(AndroidJUnit4::class)
class VoiceRoomUiTest {
    @get:Rule val ui=createEmptyComposeRule()
    @Test fun sessionCanPauseAndBackgroundStopsItsMicrophone(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val store=LocalStore(context)
        // Deliberately unreachable local endpoint: the test cannot call a paid/live API.
        val p=Provider(id="ui-only",name="OpenRouter",endpoint="https://127.0.0.1:12345/v1/chat/completions",model="ui-only")
        store.saveProviders(listOf(p));store.put("activeChat",p.id)
        store.putFlag("consent",true);store.putFlag("speechConsentSeen",true);store.putFlag("networkSpeech",true)
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.RECORD_AUDIO")).use{it.readBytes()}
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        var model:AssistantModel?=null
        try {
            scenario.onActivity{model=ViewModelProvider(it)[AssistantModel::class.java]}
            ui.onNodeWithText("ভয়েসে কথা বলো").performScrollTo().performClick()
            ui.onNodeWithText("LIVE CONVERSATION").assertIsDisplayed()
            ui.onNodeWithContentDescription("Mic pause বা resume").performClick()
            ui.onNodeWithText("একটু বিরতি।").assertIsDisplayed()
            assertEquals(VoicePhase.PAUSED,model!!.voice.phase)
            assertFalse(model!!.voice.listening)
            val dir=File(context.filesDir,"ui-captures").apply{mkdirs()}
            val bitmap=instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
            File(dir,"voice-room-paused.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            scenario.moveToState(Lifecycle.State.CREATED)
            assertFalse(model!!.voice.sessionActive)
            assertFalse(model!!.voice.listening)
            scenario.moveToState(Lifecycle.State.RESUMED)
            assertFalse(model!!.voice.sessionActive)
            if(ui.onAllNodesWithContentDescription("Session শেষ করো").fetchSemanticsNodes().isNotEmpty())ui.onNodeWithContentDescription("Session শেষ করো").performClick()
            ui.onAllNodesWithText("LIVE CONVERSATION").assertCountEquals(0)
        }finally{
            scenario.close()
            context.getSharedPreferences("bushra_private_v1",0).edit().clear().commit()
        }
    }
}
