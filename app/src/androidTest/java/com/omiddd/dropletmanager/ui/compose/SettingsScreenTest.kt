package com.omiddd.dropletmanager.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.omiddd.dropletmanager.R
import com.omiddd.dropletmanager.ui.activity.SshSettingsActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        Intents.init()
        intending(anyIntent()).respondWith(
            android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_OK, null)
        )
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun primaryButtonsInvokeCallbacks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var savedToken: String? = null
        var loggedOut = false
        val darkModeChanges = mutableListOf<Boolean>()

        composeRule.setContent {
            SettingsScreen(
                currentTokenMasked = "abcd****wxyz",
                onChangeToken = { savedToken = it },
                onLogout = { loggedOut = true },
                darkMode = false,
                onDarkModeChange = { darkModeChanges += it }
            )
        }

        composeRule.onNodeWithTag("settings_token").performTextInput("  next-token  ")
        composeRule.onNodeWithText("Save Token").performClick()
        composeRule.onNodeWithText("Logout").performClick()
        composeRule.onNodeWithTag("settings_dark_mode").performClick()

        assertEquals("next-token", savedToken)
        assertTrue(loggedOut)
        assertEquals(listOf(true), darkModeChanges)
        composeRule.onNodeWithText("Current token: abcd****wxyz").assertIsDisplayed()
        composeRule.onNodeWithText("SSH Console Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
    }

    @Test
    fun externalButtonsLaunchExpectedIntents() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            SettingsScreen(
                currentTokenMasked = "masked",
                onChangeToken = {},
                onLogout = {},
                darkMode = false,
                onDarkModeChange = {}
            )
        }

        composeRule.onNodeWithText("SSH Console Settings").performClick()
        intended(hasComponent(SshSettingsActivity::class.java.name))

        composeRule.onNodeWithText("Privacy Policy").performClick()
        intended(hasAction(android.content.Intent.ACTION_VIEW))
        intended(hasData(context.getString(R.string.privacy_policy_url)))
    }
}
