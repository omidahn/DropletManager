package com.omiddd.dropletmanager.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.omiddd.dropletmanager.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loginButtonPassesTrimmedToken() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loginLabel = context.getString(R.string.login)
        var received: String? = null

        composeRule.setContent {
            LoginScreen(onLogin = { received = it })
        }

        composeRule.onNodeWithTag("login_token").performTextInput("  my-token  ")
        composeRule.onNodeWithText(loginLabel).performClick()

        assertEquals("my-token", received)
    }

    @Test
    fun linkButtonsOpenExpectedUrls() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val privacyLabel = context.getString(R.string.privacy_policy)
        val signUpLabel = context.getString(R.string.sign_up_for_digitalocean)
        val createTokenLabel = context.getString(R.string.create_api_token)
        val fakeUriHandler = RecordingUriHandler()

        composeRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides fakeUriHandler) {
                LoginScreen(onLogin = {})
            }
        }

        composeRule.onNodeWithText(privacyLabel).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(signUpLabel).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(createTokenLabel).assertIsDisplayed().performClick()

        assertEquals(
            listOf(
                context.getString(R.string.privacy_policy_url),
                context.getString(R.string.digitalocean_signup_url),
                context.getString(R.string.digitalocean_api_token_url)
            ),
            fakeUriHandler.openedUris
        )
    }

    private class RecordingUriHandler : UriHandler {
        val openedUris = mutableListOf<String>()

        override fun openUri(uri: String) {
            openedUris += uri
        }
    }
}
