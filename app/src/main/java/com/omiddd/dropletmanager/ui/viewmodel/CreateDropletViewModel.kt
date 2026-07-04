package com.omiddd.dropletmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omiddd.dropletmanager.data.model.Droplet
import com.omiddd.dropletmanager.data.model.DropletCreationRequest
import com.omiddd.dropletmanager.data.model.Image
import com.omiddd.dropletmanager.data.model.Region
import com.omiddd.dropletmanager.data.model.Size
import com.omiddd.dropletmanager.data.repository.DropletRepository
import com.omiddd.dropletmanager.data.repository.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class CreateUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val regions: List<Region> = emptyList(),
    val sizes: List<Size> = emptyList(),
    val images: List<Image> = emptyList(),
    val sshKeys: List<com.omiddd.dropletmanager.data.model.SshKey> = emptyList(),
    val created: Droplet? = null
)

class CreateDropletViewModel(
    private var token: String,
    private val repository: DropletRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CreateUiState())
    val state: StateFlow<CreateUiState> = _state
    private var loadOptionsJob: Job? = null
    private var createJob: Job? = null

    fun loadOptions(force: Boolean = false) {
        if (!force && (_state.value.loading || _state.value.regions.isNotEmpty())) return

        loadOptionsJob?.cancel()
        val requestToken = token
        loadOptionsJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            coroutineScope {
                val regionsDeferred = async { repository.listRegions(requestToken) }
                val sizesDeferred = async { repository.listSizes(requestToken) }
                val imagesDeferred = async { repository.listImages(requestToken) }
                val sshKeysDeferred = async { repository.listSshKeys(requestToken) }

                val regionsResult = regionsDeferred.await()
                val sizesResult = sizesDeferred.await()
                val imagesResult = imagesDeferred.await()
                val sshKeysResult = sshKeysDeferred.await()

                val errors = listOf(regionsResult, sizesResult, imagesResult)
                    .filterIsInstance<Result.Error>()
                    .map { it.message }

                if (requestToken != token) return@coroutineScope

                if (errors.isNotEmpty()) {
                    _state.value = _state.value.copy(loading = false, error = errors.joinToString("\n"))
                } else {
                    val sshKeys = when (sshKeysResult) {
                        is Result.Success -> sshKeysResult.data
                        is Result.Error -> emptyList()
                        is Result.Loading -> emptyList()
                    }
                    _state.value = _state.value.copy(
                        loading = false,
                        regions = (regionsResult as Result.Success).data,
                        sizes = (sizesResult as Result.Success).data,
                        images = (imagesResult as Result.Success).data,
                        sshKeys = sshKeys
                    )
                }
            }
        }
    }

    fun create(request: DropletCreationRequest, onResult: (Boolean, String?) -> Unit) {
        createJob?.cancel()
        val requestToken = token
        createJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val res = repository.createDroplet(requestToken, request)) {
                is Result.Success -> {
                    if (requestToken != token) return@launch
                    _state.value = _state.value.copy(created = res.data)
                    onResult(true, null)
                }
                is Result.Error -> {
                    if (requestToken != token) return@launch
                    onResult(false, res.message)
                }
                is Result.Loading -> {}
            }
            if (requestToken == token) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun updateToken(newToken: String) {
        if (token == newToken) return
        loadOptionsJob?.cancel()
        createJob?.cancel()
        token = newToken
        _state.value = CreateUiState()
        loadOptions(force = true)
    }
}

class CreateDropletViewModelFactory(
    private val token: String,
    private val repository: DropletRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateDropletViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateDropletViewModel(token, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
