package com.omiddd.dropletmanager.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.omiddd.dropletmanager.BuildConfig
import com.omiddd.dropletmanager.R
import com.omiddd.dropletmanager.data.model.Droplet
import com.omiddd.dropletmanager.data.repository.DropletRepository
import com.omiddd.dropletmanager.ui.compose.*
import com.omiddd.dropletmanager.ui.theme.DropletManagerTheme
import com.omiddd.dropletmanager.ui.viewmodel.*
import com.omiddd.dropletmanager.utils.SshKeyManager
import com.omiddd.dropletmanager.utils.ThemePreferences
import com.omiddd.dropletmanager.utils.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val repository = DropletRepository()
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)
        val token = intent.getStringExtra(LoginActivity.API_TOKEN_KEY) ?: tokenManager.getToken()

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.no_api_token), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val listFactory = DropletViewModelFactory(token, repository)
        val listVm: DropletViewModel by viewModels { listFactory }

        val createFactory = CreateDropletViewModelFactory(token, repository)
        val createVm: CreateDropletViewModel by viewModels { createFactory }

        setContent {
            var darkMode by rememberSaveable { mutableStateOf(ThemePreferences.isDark(this)) }
            DropletManagerTheme(useDarkTheme = darkMode) {
                MainScreen(
                    listVm = listVm,
                    createVm = createVm,
                    initialToken = token,
                    darkMode = darkMode,
                    onDarkModeChange = { newMode ->
                        ThemePreferences.setDark(this, newMode)
                        darkMode = newMode
                    },
                    onChangeToken = { newToken ->
                        val trimmed = newToken.trim()
                        tokenManager.saveToken(trimmed)
                        listVm.clearPermissionWarning()
                        listVm.updateToken(trimmed)
                        createVm.updateToken(trimmed)
                        listVm.loadDroplets()
                        getString(R.string.token_updated)
                    },
                    onLogout = {
                        tokenManager.clearToken()
                        Toast.makeText(this, getString(R.string.logged_out), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainActivity.MainScreen(
    listVm: DropletViewModel,
    createVm: CreateDropletViewModel,
    initialToken: String,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onChangeToken: (String) -> String,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var selectedDroplet by remember { mutableStateOf<Droplet?>(null) }
    var token by rememberSaveable { mutableStateOf(initialToken) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val listState by listVm.uiState.collectAsState()
    val permissionWarning by listVm.permissionWarning.collectAsState()

    LaunchedEffect(Unit) { listVm.loadDroplets() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { listVm.loadDroplets() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                    }
                    // Debug-only Crashlytics test button
                    if (BuildConfig.DEBUG) {
                        IconButton(onClick = {
                            // throw a runtime exception to test Crashlytics setup
                            throw RuntimeException("Test Crash from Debug button")
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Test Crash")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text(stringResource(R.string.tab_droplets)) })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.AddCircle, null) }, label = { Text(stringResource(R.string.tab_create)) })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.tab_settings)) })
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> DropletListScreen(
                droplets = listState.droplets,
                isLoading = listState.isLoading,
                error = listState.error,
                costSummary = listVm.costSummary.collectAsState().value,
                onRefresh = { listVm.loadDroplets() },
                onDropletClicked = { selectedDroplet = it },
                onActionClicked = { droplet, action ->
                    listVm.performAction(droplet.id, action) { success, message ->
                        val resolved = message ?: if (success) getString(R.string.action_succeeded) else getString(R.string.action_failed)
                        scope.launch { snackbarHostState.showSnackbar(resolved) }
                    }
                },
                onOpenConsole = { launchSshConsole(it) },
                query = listState.query,
                onQueryChange = { listVm.setQuery(it) },
                statusOptions = listState.statusOptions,
                selectedStatus = listState.statusFilter,
                onStatusChange = { listVm.setStatusFilter(it) },
                regionOptions = listState.regionOptions,
                selectedRegion = listState.regionFilter,
                onRegionChange = { listVm.setRegionFilter(it) },
                onSwitchProject = { listVm.loadProjects() },
                modifier = Modifier.padding(padding)
            )
            1 -> CreateDropletScreen(
                viewModel = createVm,
                onCreated = {
                    scope.launch { snackbarHostState.showSnackbar(getString(R.string.droplet_created)) }
                    selectedTab = 0
                    listVm.loadDroplets()
                },
                modifier = Modifier.padding(padding)
            )
            2 -> SettingsScreen(
                currentTokenMasked = token.let { current ->
                    if (current.length < 8) current
                    else current.take(4) + "****" + current.takeLast(4)
                },
                onChangeToken = { updated ->
                    val message = onChangeToken(updated)
                    token = updated.trim()
                    selectedTab = 0
                    selectedDroplet = null
                    scope.launch { snackbarHostState.showSnackbar(message.ifBlank { getString(R.string.token_updated) }) }
                },
                onLogout = onLogout,
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                permissionHint = if (permissionWarning) stringResource(R.string.permission_hint_readonly) else null,
                modifier = Modifier.padding(padding)
            )
        }
    }

    selectedDroplet?.let { droplet ->
        DropletDetailsScreen(
            droplet = droplet,
            onActionClicked = { action ->
                listVm.performAction(droplet.id, action) { success, message ->
                    val msg = message ?: if (success) getString(R.string.action_succeeded) else getString(R.string.action_failed)
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            },
            onDelete = {
                listVm.deleteDroplet(droplet.id) { success, message ->
                    val msg = if (success) getString(R.string.droplet_destroyed) else message ?: getString(R.string.droplet_destroy_failed)
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
                selectedDroplet = null
            },
            onOpenConsole = { launchSshConsole(droplet) },
            onOpenUsage = { launchUsageMetrics(droplet, token) },
            accruedCost = listVm.getAccruedCostForDroplet(droplet),
            onClose = { selectedDroplet = null }
        )
    }
}

private fun MainActivity.launchSshConsole(droplet: Droplet) {
    val ip = droplet.networks.v4.firstOrNull { it.type == "public" }?.ip_address
    if (ip != null) {
        val user = SshKeyManager(this).getUsername() ?: getString(R.string.ssh_default_user)
        val intent = Intent(this, SshConsoleActivity::class.java).apply {
            putExtra(SshConsoleActivity.EXTRA_HOST, ip)
            putExtra(SshConsoleActivity.EXTRA_USER, user)
        }
        startActivity(intent)
    } else {
        Toast.makeText(this, getString(R.string.no_public_ip_for_ssh), Toast.LENGTH_SHORT).show()
    }
}

private fun MainActivity.launchUsageMetrics(droplet: Droplet, token: String) {
    val intent = Intent(this, UsageMetricsActivity::class.java).apply {
        putExtra(UsageMetricsActivity.EXTRA_ID, droplet.id)
        putExtra(UsageMetricsActivity.EXTRA_NAME, droplet.name)
        putExtra(UsageMetricsActivity.EXTRA_VCPUS, droplet.vcpus)
        putExtra(UsageMetricsActivity.EXTRA_MEMORY_MB, droplet.memory)
        putExtra(UsageMetricsActivity.EXTRA_TOKEN, token)
    }
    startActivity(intent)
}
