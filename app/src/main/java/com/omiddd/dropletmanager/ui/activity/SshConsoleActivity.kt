package com.omiddd.dropletmanager.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.omiddd.dropletmanager.ui.compose.SshConsoleScreen
import com.omiddd.dropletmanager.ui.theme.DropletManagerTheme
import com.omiddd.dropletmanager.ui.viewmodel.SshConsoleViewModel
import com.omiddd.dropletmanager.utils.SshKeyManager

class SshConsoleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val host = intent.getStringExtra(EXTRA_HOST)
        val user = intent.getStringExtra(EXTRA_USER)
        val viewModel: SshConsoleViewModel by viewModels()

        if (savedInstanceState == null && !host.isNullOrBlank() && !user.isNullOrBlank()) {
            val keyMgr = SshKeyManager(this)
            val key = keyMgr.getPrivateKeyBytes()
            if (key != null) {
                val pass = keyMgr.getPassphrase()
                viewModel.connect(host, 22, user, password = null, privateKey = key, passphrase = pass)
            }
        }

        setContent {
            DropletManagerTheme {
                SshConsoleScreen(viewModel = viewModel, defaultHost = host, defaultUser = user, onClose = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_HOST = "host"
        const val EXTRA_USER = "user"
    }
}
