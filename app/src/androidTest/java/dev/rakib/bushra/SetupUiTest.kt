package dev.rakib.bushra

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupUiTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    @Before fun acceptDisclosure(){
        ui.waitForIdle()
        if(ui.onAllNodesWithText("বুঝেছি, শুরু করি").fetchSemanticsNodes().isNotEmpty())ui.onNodeWithText("বুঝেছি, শুরু করি").performClick()
        ui.waitForIdle()
    }
    private fun capture(name:String){
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val dir=File(context.filesDir,"ui-captures").apply{mkdirs()}
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
        File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun homeAndSimpleConnectionAreRenderedWithoutTechnicalFields(){
        ui.onAllNodesWithText("Home").onFirst().performClick()
        ui.onNodeWithText("তোমার নিজস্ব").assertIsDisplayed()
        capture("home-glass-dashboard")
        ui.onAllNodesWithContentDescription("Settings").onFirst().performClick()
        ui.onNodeWithText("Simple. Connected.").assertIsDisplayed()
        capture("simple-settings")
        ui.onNodeWithText("AI connect করো").performClick()
        ui.onNodeWithText("দুই ধাপেই প্রস্তুত।").assertIsDisplayed()
        ui.onNodeWithText("API key").assertIsDisplayed()
        ui.onAllNodesWithText("Model ID").assertCountEquals(0)
        ui.onAllNodesWithText("Advanced authentication").assertCountEquals(0)
        ui.onAllNodesWithText("পূর্ণ HTTPS endpoint").assertCountEquals(0)
        capture("provider-and-key-only")
    }
    @Test fun autoChatSetupDoesNotSendBeforeOwnerStarts(){
        ui.runOnIdle{androidx.lifecycle.ViewModelProvider(ui.activity)[AssistantModel::class.java].autoChatOpen=true}
        ui.onNodeWithText("SMS / WhatsApp auto-chat").assertIsDisplayed()
        ui.onNodeWithText("ফোনে saved নাম অথবা নম্বর").assertIsDisplayed()
        ui.onNodeWithText("এই contact-এর auto-chat চালু করো").assertIsNotEnabled()
        capture("auto-chat-setup")
        ui.onNodeWithText("সব automatic reply এখনই বন্ধ করো").performScrollTo().performClick()
    }

}
