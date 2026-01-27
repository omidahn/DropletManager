package com.omiddd.dropletmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omiddd.dropletmanager.data.model.Droplet
import com.omiddd.dropletmanager.data.model.DropletAction
import com.omiddd.dropletmanager.data.repository.DropletRepository
import com.omiddd.dropletmanager.data.repository.Result
import com.omiddd.dropletmanager.data.model.Project
import com.omiddd.dropletmanager.data.model.MetricPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class DropletUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val droplets: List<Droplet> = emptyList(),
    val query: String = "",
    val statusFilter: String? = null,
    val regionFilter: String? = null,
    val statusOptions: List<String> = emptyList(),
    val regionOptions: List<String> = emptyList()
)

data class CostSummary(
    val totalMonthly: Double = 0.0,
    val totalMonthlyWithBackups: Double = 0.0,
    val accruedThisMonth: Double = 0.0
)

class DropletViewModel(
    private var token: String,
    private val repository: DropletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DropletUiState(isLoading = true))
    val uiState: StateFlow<DropletUiState> = _uiState

    private val _costSummary = MutableStateFlow(CostSummary())
    val costSummary: StateFlow<CostSummary> = _costSummary

    private var allDroplets: List<Droplet> = emptyList()

    private val _permissionWarning = MutableStateFlow(false)
    val permissionWarning: StateFlow<Boolean> = _permissionWarning

    private fun checkPermission(message: String?) {
        if (message == null) return
        if (message.contains("Permission denied: your API token is read-only", ignoreCase = true) ||
            message.contains("not authorized", ignoreCase = true) ||
            message.contains("forbidden", ignoreCase = true)
        ) {
            _permissionWarning.value = true
        }
    }

    fun clearPermissionWarning() {
        _permissionWarning.value = false
    }

    fun updateToken(newToken: String) {
        if (token == newToken) return
        token = newToken
        allDroplets = emptyList()
        _uiState.value = DropletUiState(
            isLoading = false,
            query = _uiState.value.query,
            statusFilter = _uiState.value.statusFilter,
            regionFilter = _uiState.value.regionFilter
        )
        _permissionWarning.value = false
    }

    fun loadDroplets() {
        viewModelScope.launch {
            repository.getDroplets(token).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    is Result.Success -> {
                        allDroplets = result.data
                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                        applyFilters()
                    }
                    is Result.Error -> {
                        checkPermission(result.message)
                        allDroplets = emptyList()
                        _uiState.value = DropletUiState(
                            isLoading = false,
                            error = result.message,
                            droplets = emptyList(),
                            query = _uiState.value.query,
                            statusFilter = _uiState.value.statusFilter,
                            regionFilter = _uiState.value.regionFilter
                        )
                    }
                }
            }
        }
    }

    fun performAction(
        dropletId: Int,
        action: DropletAction,
        onResult: (success: Boolean, message: String?) -> Unit
    ) {
        viewModelScope.launch {
            val extraName = when (action) {
                is DropletAction.Snapshot -> generateSnapshotName(dropletId)
                else -> null
            }
            when (val res = repository.performAction(token, dropletId, action, extraName)) {
                is Result.Success -> {
                    loadDroplets()
                    onResult(true, null)
                }
                is Result.Error -> { checkPermission(res.message); onResult(false, res.message) }
                is Result.Loading -> Unit
            }
        }
    }

    private fun generateSnapshotName(dropletId: Int): String? {
        val droplet = allDroplets.firstOrNull { it.id == dropletId } ?: return null
        val normalized = droplet.name.lowercase()
            .replace(Regex("[^a-z0-9-]"), "-")
            .replace(Regex("-{2,}"), "-")
            .trim('-')
            .ifBlank { "droplet" }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(ZonedDateTime.now())
        return "$normalized-$timestamp"
    }

    fun deleteDroplet(
        dropletId: Int,
        onResult: (success: Boolean, message: String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.deleteDroplet(token, dropletId)) {
                is Result.Success -> { loadDroplets(); onResult(true, null) }
                is Result.Error -> { checkPermission(res.message); onResult(false, res.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        applyFilters()
    }

    fun setStatusFilter(status: String?) {
        _uiState.value = _uiState.value.copy(statusFilter = status)
        applyFilters()
    }

    fun setRegionFilter(region: String?) {
        _uiState.value = _uiState.value.copy(regionFilter = region)
        applyFilters()
    }

    private fun applyFilters() {
        val q = _uiState.value.query.trim().lowercase()
        val status = _uiState.value.statusFilter?.lowercase()
        val region = _uiState.value.regionFilter?.lowercase()

        val statusOptions = allDroplets.map { it.status }.distinct().sorted()
        val regionOptions = allDroplets.map { it.region.slug }.distinct().sorted()

        val filtered = allDroplets.filter { d ->
            val matchesQuery = q.isEmpty() ||
                d.name.lowercase().contains(q) ||
                d.region.name.lowercase().contains(q) ||
                d.region.slug.lowercase().contains(q)
            val matchesStatus = status == null || d.status.lowercase() == status
            val matchesRegion = region == null || d.region.slug.lowercase() == region
            matchesQuery && matchesStatus && matchesRegion
        }
        _uiState.value = _uiState.value.copy(
            droplets = filtered,
            statusOptions = statusOptions,
            regionOptions = regionOptions
        )
        calculateCosts(filtered)
    }

    private fun calculateCosts(droplets: List<Droplet>) {
        var monthly = 0.0
        var monthlyWithBackups = 0.0
        var accrued = 0.0
        val startOfMonth = ZonedDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneId.systemDefault())
        val monthHours = Duration.between(startOfMonth, startOfMonth.plusMonths(1)).toHours().toDouble()

        droplets.forEach { d ->
            val m = d.size.priceMonthly
            val h = d.size.priceHourly
            monthly += m
            val backupSurcharge = if (d.backups) m * 0.20 else 0.0
            monthlyWithBackups += (m + backupSurcharge)
            accrued += estimateAccruedThisMonth(
                hourly = h,
                monthly = m,
                createdAt = d.createdAt,
                startOfMonth = startOfMonth,
                monthHours = monthHours,
                backupMonthly = backupSurcharge
            )
        }
        _costSummary.value = CostSummary(totalMonthly = monthly, totalMonthlyWithBackups = monthlyWithBackups, accruedThisMonth = accrued)
    }

    fun getAccruedCostForDroplet(droplet: Droplet): Double {
        val startOfMonth = ZonedDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneId.systemDefault())
        val monthHours = Duration.between(startOfMonth, startOfMonth.plusMonths(1)).toHours().toDouble()
        val backupSurcharge = if (droplet.backups) droplet.size.priceMonthly * 0.20 else 0.0

        return estimateAccruedThisMonth(
            hourly = droplet.size.priceHourly,
            monthly = droplet.size.priceMonthly,
            createdAt = droplet.createdAt,
            startOfMonth = startOfMonth,
            monthHours = monthHours,
            backupMonthly = backupSurcharge
        )
    }

    private fun estimateAccruedThisMonth(
        hourly: Double?,
        monthly: Double,
        createdAt: String,
        startOfMonth: ZonedDateTime,
        monthHours: Double,
        backupMonthly: Double
    ): Double {
        val now = ZonedDateTime.now()
        val created = parseIso(createdAt) ?: startOfMonth
        val start = if (created.isAfter(startOfMonth)) created else startOfMonth
        val hours = Duration.between(start, now).toHours().coerceAtLeast(0).toDouble()
        val base = if (hourly != null) hourly * hours else monthly * (Duration.between(startOfMonth, now).toHours().toDouble() / monthHours)
        val addOns = (backupMonthly) * (hours / monthHours)
        return base + addOns
    }

    private fun parseIso(s: String): ZonedDateTime? {
        return try {
            OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault())
        } catch (_: Exception) {
            try { Instant.parse(s).atZone(ZoneId.systemDefault()) } catch (_: Exception) { null }
        }
    }

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects
    private val _projectsLoading = MutableStateFlow(false)
    val projectsLoading: StateFlow<Boolean> = _projectsLoading
    private val _projectsError = MutableStateFlow<String?>(null)
    val projectsError: StateFlow<String?> = _projectsError

    fun loadProjects() {
        viewModelScope.launch {
            _projectsLoading.value = true
            _projectsError.value = null
            when (val res = repository.listProjects(token)) {
                is Result.Success -> _projects.value = res.data
                is Result.Error -> { checkPermission(res.message); _projectsError.value = res.message }
                is Result.Loading -> Unit
            }
            _projectsLoading.value = false
        }
    }

    fun setDefaultProject(projectId: String, onResult: (success: Boolean, message: String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.setDefaultProject(token, projectId)) {
                is Result.Success -> onResult(true, null)
                is Result.Error -> { checkPermission(res.message); onResult(false, res.message) }
                is Result.Loading -> Unit
            }
        }
    }

    data class MetricUiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val points: List<MetricPoint> = emptyList()
    )

    private val _cpu = MutableStateFlow(MetricUiState())
    val cpu: StateFlow<MetricUiState> = _cpu
    private val _memory = MutableStateFlow(MetricUiState())
    val memory: StateFlow<MetricUiState> = _memory
    private val _bwOut = MutableStateFlow(MetricUiState())
    val bandwidthOutbound: StateFlow<MetricUiState> = _bwOut
    private val _bwIn = MutableStateFlow(MetricUiState())
    val bandwidthInbound: StateFlow<MetricUiState> = _bwIn
    private val _fsUsed = MutableStateFlow(MetricUiState())
    val filesystemUsed: StateFlow<MetricUiState> = _fsUsed
    private val _load1 = MutableStateFlow(MetricUiState())
    val load1: StateFlow<MetricUiState> = _load1

    fun loadCpuMetrics(dropletId: Int) {
        viewModelScope.launch {
            repository.getCpuMetrics(token, dropletId).collect { result ->
                _cpu.value = when (result) {
                    is Result.Loading -> _cpu.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(isLoading = false, error = null, points = result.data)
                    is Result.Error -> { checkPermission(result.message); MetricUiState(isLoading = false, error = result.message, points = emptyList()) }
                }
            }
        }
    }

    fun loadMemoryMetrics(dropletId: Int) {
        viewModelScope.launch {
            repository.getMemoryMetrics(token, dropletId).collect { result ->
                _memory.value = when (result) {
                    is Result.Loading -> _memory.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(isLoading = false, error = null, points = result.data)
                    is Result.Error -> { checkPermission(result.message); MetricUiState(isLoading = false, error = result.message, points = emptyList()) }
                }
            }
        }
    }

    fun loadBandwidth(dropletId: Int) {
        viewModelScope.launch {
            repository.getBandwidth(token, dropletId, interfaceName = "public", direction = "outbound").collect { res ->
                _bwOut.value = when (res) {
                    is Result.Loading -> _bwOut.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(false, null, res.data)
                    is Result.Error -> { checkPermission(res.message); MetricUiState(false, res.message, emptyList()) }
                }
            }
        }
        viewModelScope.launch {
            repository.getBandwidth(token, dropletId, interfaceName = "public", direction = "inbound").collect { res ->
                _bwIn.value = when (res) {
                    is Result.Loading -> _bwIn.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(false, null, res.data)
                    is Result.Error -> { checkPermission(res.message); MetricUiState(false, res.message, emptyList()) }
                }
            }
        }
    }

    fun loadFilesystem(dropletId: Int) {
        viewModelScope.launch {
            repository.getFilesystemUsed(token, dropletId).collect { res ->
                _fsUsed.value = when (res) {
                    is Result.Loading -> _fsUsed.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(false, null, res.data)
                    is Result.Error -> { checkPermission(res.message); MetricUiState(false, res.message, emptyList()) }
                }
            }
        }
    }

    fun loadLoad1(dropletId: Int) {
        viewModelScope.launch {
            repository.getLoad1(token, dropletId).collect { res ->
                _load1.value = when (res) {
                    is Result.Loading -> _load1.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(false, null, res.data)
                    is Result.Error -> { checkPermission(res.message); MetricUiState(false, res.message, emptyList()) }
                }
            }
        }
    }
}

class DropletViewModelFactory(
    private val token: String,
    private val repository: DropletRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DropletViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DropletViewModel(token, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
