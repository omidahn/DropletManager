package com.omiddd.dropletmanager.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SshConsoleScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun passwordConnectPassesTrimmedValues() {
        var received: ConnectionCapture? = null

        composeRule.setContent {
            SshConnectForm(
                connecting = false,
                inlineError = null,
                defaultHost = " 203.0.113.10 ",
                defaultUser = " root ",
                onConnect = { host, port, user, password, key, passphrase ->
                    received = ConnectionCapture(host, port, user, password, key, passphrase)
                }
            )
        }

        composeRule.onNodeWithTag("ssh_port").performTextClearance()
        composeRule.onNodeWithTag("ssh_port").performTextInput("2222")
        composeRule.onNodeWithTag("ssh_password").performTextInput("secret")
        composeRule.onNodeWithTag("ssh_connect").performScrollTo().performClick()

        val capture = requireNotNull(received)
        assertEquals("203.0.113.10", capture.host)
        assertEquals(2222, capture.port)
        assertEquals("root", capture.user)
        assertEquals("secret", capture.password)
        assertNull(capture.key)
        assertNull(capture.passphrase)
    }

    @Test
    fun keyConnectPassesKeyMaterial() {
        var received: ConnectionCapture? = null

        composeRule.setContent {
            SshConnectForm(
                connecting = false,
                inlineError = null,
                defaultHost = "198.51.100.7",
                defaultUser = "root",
                onConnect = { host, port, user, password, key, passphrase ->
                    received = ConnectionCapture(host, port, user, password, key, passphrase)
                }
            )
        }

        composeRule.onNodeWithTag("ssh_auth_key").performScrollTo().performClick()
        composeRule.onNodeWithTag("ssh_key").performTextInput("PRIVATE-KEY")
        composeRule.onNodeWithTag("ssh_passphrase").performTextInput("phrase")
        composeRule.onNodeWithTag("ssh_connect").performScrollTo().performClick()

        val capture = requireNotNull(received)
        assertEquals("198.51.100.7", capture.host)
        assertEquals(22, capture.port)
        assertEquals("root", capture.user)
        assertNull(capture.password)
        assertArrayEquals("PRIVATE-KEY".toByteArray(), capture.key)
        assertEquals("phrase", capture.passphrase)
    }

    private data class ConnectionCapture(
        val host: String,
        val port: Int,
        val user: String,
        val password: String?,
        val key: ByteArray?,
        val passphrase: String?
    )
}
