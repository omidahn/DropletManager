package com.omiddd.dropletmanager.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.omiddd.dropletmanager.data.repository.DropletRepository
import com.omiddd.dropletmanager.ui.compose.UsageMetricsScreen
import com.omiddd.dropletmanager.ui.theme.DropletManagerTheme
import com.omiddd.dropletmanager.ui.viewmodel.UsageMetricsViewModel
import com.omiddd.dropletmanager.ui.viewmodel.UsageMetricsViewModelFactory

class UsageMetricsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dropletId = intent.getIntExtra(EXTRA_ID, -1)
        val dropletName = intent.getStringExtra(EXTRA_NAME) ?: ""
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: ""

        if (dropletId == -1 || token.isEmpty()) {
            finish()
            return
        }

        val factory = UsageMetricsViewModelFactory(token, dropletId, DropletRepository())
        val viewModel: UsageMetricsViewModel by viewModels { factory }

        setContent {
            DropletManagerTheme {
                UsageMetricsScreen(viewModel = viewModel, dropletName = dropletName, onBack = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_ID = "EXTRA_ID"
        const val EXTRA_NAME = "EXTRA_NAME"
        const val EXTRA_VCPUS = "EXTRA_VCPUS"
        const val EXTRA_MEMORY_MB = "EXTRA_MEMORY_MB"
        const val EXTRA_TOKEN = "EXTRA_TOKEN"
    }
}
