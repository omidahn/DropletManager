package com.omiddd.dropletmanager.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.omiddd.dropletmanager.R
import com.omiddd.dropletmanager.ui.compose.LoginScreen
import com.omiddd.dropletmanager.ui.theme.DropletManagerTheme
import com.omiddd.dropletmanager.utils.LogUtils
import com.omiddd.dropletmanager.utils.TokenManager

@androidx.compose.material3.ExperimentalMaterial3Api
class LoginActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager

    companion object {
        const val API_TOKEN_KEY = "api_token"
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtils.d(TAG, "Initializing LoginActivity")
        tokenManager = TokenManager(this)

        // Check if we already have a token
        if (tokenManager.hasToken()) {
            LogUtils.d(TAG, "Found existing token, navigating to MainActivity")
            navigateToMainActivity(tokenManager.getToken()!!)
            return
        }

        LogUtils.d(TAG, "No existing token found, showing login UI")
        setContent {
            val dark = com.omiddd.dropletmanager.utils.ThemePreferences.isDark(this)
            DropletManagerTheme(useDarkTheme = dark) {
                LoginScreen(onLogin = { token ->
                    LogUtils.d(TAG, "Login button clicked, validating token (length: ${'$'}{token.length})")
                    if (isValidApiToken(token)) {
                        LogUtils.d(TAG, "Token validation successful, saving token")
                        tokenManager.saveToken(token)
                        LogUtils.d(TAG, "Token saved, navigating to MainActivity")
                        navigateToMainActivity(token)
                    } else {
                        LogUtils.w(TAG, "Invalid token format")
                        Toast.makeText(this, R.string.invalid_api_token, Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    private fun isValidApiToken(token: String): Boolean {
        // Accept any non-empty token
        val isValid = token.isNotEmpty()
        LogUtils.d(TAG, "Token validation: $isValid (length: ${token.length})")
        return isValid
    }

    private fun navigateToMainActivity(token: String) {
        LogUtils.d(TAG, "Navigating to MainActivity with token (length: ${token.length})")
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(API_TOKEN_KEY, token)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
