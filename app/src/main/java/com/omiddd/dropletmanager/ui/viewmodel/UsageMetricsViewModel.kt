package com.omiddd.dropletmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omiddd.dropletmanager.data.model.MetricPoint
import com.omiddd.dropletmanager.data.repository.DropletRepository
import com.omiddd.dropletmanager.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// State for a single metric
data class MetricUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val points: List<MetricPoint> = emptyList()
)

class UsageMetricsViewModel(
    private val token: String,
    private val dropletId: Int,
    private val repository: DropletRepository
) : ViewModel() {

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

    init {
        loadAllMetrics()
    }

    fun loadAllMetrics() {
        loadCpuMetrics()
        loadMemoryMetrics()
        loadBandwidth()
        loadFilesystem()
        loadLoad1()
    }

    private fun loadCpuMetrics() {
        viewModelScope.launch {
            repository.getCpuMetrics(token, dropletId).collect { result ->
                _cpu.value = when (result) {
                    is Result.Loading -> _cpu.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(isLoading = false, error = null, points = result.data)
                    is Result.Error -> MetricUiState(isLoading = false, error = result.message, points = emptyList())
                }
            }
        }
    }

    private fun loadMemoryMetrics() {
        viewModelScope.launch {
            repository.getMemoryMetrics(token, dropletId).collect { result ->
                _memory.value = when (result) {
                    is Result.Loading -> _memory.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(isLoading = false, error = null, points = result.data)
                    is Result.Error -> MetricUiState(isLoading = false, error = result.message, points = emptyList())
                }
            }
        }
    }

    private fun loadBandwidth() {
        viewModelScope.launch {
            repository.getBandwidth(token, dropletId, interfaceName = "public", direction = "outbound").collect { res ->
                _bwOut.value = when (res) {
                    is Result.Loading -> _bwOut.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(false, null, res.data)
                    is Result.Error -> MetricUiState(false, res.message, emptyList())
                }
            }
        }
        viewModelScope.launch {
            repository.getBandwidth(token, dropletId, interfaceName = "public", direction = "inbound").collect { res ->
                _bwIn.value = when (res) {
                    is Result.Loading -> _bwIn.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(false, null, res.data)
                    is Result.Error -> MetricUiState(false, res.message, emptyList())
                }
            }
        }
    }

    private fun loadFilesystem() {
        viewModelScope.launch {
            repository.getFilesystemUsed(token, dropletId).collect { res ->
                _fsUsed.value = when (res) {
                    is Result.Loading -> _fsUsed.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(false, null, res.data)
                    is Result.Error -> MetricUiState(false, res.message, emptyList())
                }
            }
        }
    }

    private fun loadLoad1() {
        viewModelScope.launch {
            repository.getLoad1(token, dropletId).collect { res ->
                _load1.value = when (res) {
                    is Result.Loading -> _load1.value.copy(isLoading = true, error = null)
                    is Result.Success -> MetricUiState(false, null, res.data)
                    is Result.Error -> MetricUiState(false, res.message, emptyList())
                }
            }
        }
    }
}

class UsageMetricsViewModelFactory(
    private val token: String,
    private val dropletId: Int,
    private val repository: DropletRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UsageMetricsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UsageMetricsViewModel(token, dropletId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
