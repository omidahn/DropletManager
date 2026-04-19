package com.omiddd.dropletmanager.data.repository

import com.omiddd.dropletmanager.data.api.DigitalOceanService
import com.omiddd.dropletmanager.data.api.RetrofitInstance
import com.omiddd.dropletmanager.data.model.*
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.io.IOException
import java.time.Instant

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

open class DropletRepository(
    private val serviceFactory: (String) -> DigitalOceanService = RetrofitInstance::getClient
) {

    private suspend fun <T, R> safeApiCall(
        retries: Int = 0,
        initialDelayMs: Long = 500,
        apiCall: suspend () -> Response<T>,
        onSuccess: (Response<T>) -> Result<R> = { response ->
            @Suppress("UNCHECKED_CAST")
            response.body()?.let { Result.Success(it as R) } ?: Result.Error("Response body is null")
        }
    ): Result<R> {
        var attempt = 0
        var delayMs = initialDelayMs
        while (true) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    return onSuccess(response)
                }
                if (attempt < retries && shouldRetry(response.code())) {
                    attempt++
                    delay(delayMs)
                    delayMs = (delayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                    continue
                }
                return Result.Error(getErrorMessage(response))
            } catch (ex: Exception) {
                if (attempt < retries && isRetryableException(ex)) {
                    attempt++
                    delay(delayMs)
                    delayMs = (delayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                    continue
                }
                return Result.Error(ex.message ?: "An unknown error occurred")
            }
        }
    }

    private fun parseMetricsResult(response: Result<MetricsResponse>): Result<List<MetricPoint>> {
        return when (response) {
            is Result.Success -> {
                val points = response.data.data.result.flatMap { metricResult ->
                    metricResult.values.mapNotNull { valuePair ->
                        if (valuePair.size == 2) {
                            val ts = parseAnyToLong(valuePair[0])
                            val v = parseAnyToDouble(valuePair[1])
                            if (ts != null && v != null) MetricPoint(ts, v) else null
                        } else null
                    }
                }
                Result.Success(points)
            }
            is Result.Error -> response
            is Result.Loading -> Result.Loading
        }
    }

    fun getDroplets(token: String): Flow<Result<List<Droplet>>> = flow {
        emit(Result.Loading)
        val api = serviceFactory(token)
        when (val response = safeApiCall(retries = 2, apiCall = { api.listDroplets() }, onSuccess = {
            it.body()?.droplets?.let { droplets -> Result.Success(droplets) } ?: Result.Error("Empty response")
        })) {
            is Result.Success -> emit(Result.Success(response.data))
            is Result.Error -> emit(response)
            is Result.Loading -> emit(Result.Loading)
        }
    }

    open suspend fun listRegions(token: String): Result<List<Region>> {
        val api = serviceFactory(token)
        return when (val response = safeApiCall(retries = 2, apiCall = { api.listRegions() }, onSuccess = {
            it.body()?.regions?.let { regions -> Result.Success(regions) } ?: Result.Error("Empty response")
        })) {
            is Result.Success -> Result.Success(response.data)
            is Result.Error -> response
            is Result.Loading -> Result.Loading
        }
    }

    open suspend fun listSizes(token: String): Result<List<Size>> {
        val api = serviceFactory(token)
        return when (val response = safeApiCall(retries = 2, apiCall = { api.listSizes() }, onSuccess = {
            it.body()?.sizes?.let { sizes -> Result.Success(sizes) } ?: Result.Error("Empty response")
        })) {
            is Result.Success -> Result.Success(response.data)
            is Result.Error -> response
            is Result.Loading -> Result.Loading
        }
    }

    open suspend fun listImages(token: String): Result<List<Image>> {
        val api = serviceFactory(token)
        return when (val response = safeApiCall(retries = 2, apiCall = { api.listImages() }, onSuccess = {
            it.body()?.images?.let { images -> Result.Success(images) } ?: Result.Error("Empty response")
        })) {
            is Result.Success -> Result.Success(response.data)
            is Result.Error -> response
            is Result.Loading -> Result.Loading
        }
    }

    open suspend fun createDroplet(token: String, request: DropletCreationRequest): Result<Droplet> {
        val api = serviceFactory(token)
        return when (val response = safeApiCall(apiCall = { api.createDroplet(request) }, onSuccess = {
            it.body()?.droplet?.let { droplet -> Result.Success(droplet) } ?: Result.Error("Droplet created but response is empty")
        })) {
            is Result.Success -> Result.Success(response.data)
            is Result.Error -> response
            is Result.Loading -> Result.Loading
        }
    }

    open suspend fun listSshKeys(token: String): Result<List<SshKey>> {
        val api = serviceFactory(token)
        return when (val response = safeApiCall(retries = 2, apiCall = { api.listSshKeys() }, onSuccess = {
            it.body()?.ssh_keys?.let { keys -> Result.Success(keys) } ?: Result.Error("Empty response")
        })) {
            is Result.Success -> Result.Success(response.data)
            is Result.Error -> response
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun deleteDroplet(token: String, dropletId: Int): Result<Unit> {
        val api = serviceFactory(token)
        return safeApiCall(apiCall = { api.deleteDroplet(dropletId) }, onSuccess = { _ -> Result.Success(Unit) })
    }

    suspend fun performAction(token: String, dropletId: Int, action: DropletAction, name: String? = null): Result<Unit> {
        val api = serviceFactory(token)
        val request = ActionRequest(action.key, name)
        return when (val response = safeApiCall(apiCall = { api.performAction(dropletId, request) }, onSuccess = { _ ->
            Result.Success(Unit)
        })) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> response
            is Result.Loading -> Result.Loading
        }
    }

    fun getCpuMetrics(token: String, dropletId: Int): Flow<Result<List<MetricPoint>>> = flow {
        emit(Result.Loading)
        val api = serviceFactory(token)
        val (start, end) = twentyFourHourWindowSeconds()
        val result = parseMetricsResult(safeApiCall(retries = 2, apiCall = { api.getCpuMetrics(dropletId, start, end) }))
        emit(result)
    }

    fun getMemoryMetrics(token: String, dropletId: Int): Flow<Result<List<MetricPoint>>> = flow {
        emit(Result.Loading)
        val api = serviceFactory(token)
        val (start, end) = twentyFourHourWindowSeconds()

        val usedResult = parseMetricsResult(safeApiCall(retries = 2, apiCall = { api.getMemoryUtilizationDroplet(dropletId, start, end) }))
        if (usedResult is Result.Success && usedResult.data.isNotEmpty()) {
            emit(usedResult)
            return@flow
        }

        val totalResult = parseMetricsResult(safeApiCall(retries = 2, apiCall = { api.getMemoryTotalDroplet(dropletId, start, end) }))
        val freeResult = parseMetricsResult(safeApiCall(retries = 2, apiCall = { api.getMemoryFreeDroplet(dropletId, start, end) }))

        if (totalResult is Result.Success && freeResult is Result.Success) {
            val totalMap = totalResult.data.associateBy { it.timestamp }
            val freeMap = freeResult.data.associateBy { it.timestamp }
            val merged = totalMap.keys.intersect(freeMap.keys).sorted().mapNotNull { ts ->
                val total = totalMap[ts]?.value
                val free = freeMap[ts]?.value
                if (total != null && free != null && total > 0) {
                    val used = total - free
                    val percentage = (used / total) * 100
                    MetricPoint(ts, percentage)
                } else null
            }
            if (merged.isNotEmpty()) {
                emit(Result.Success(merged))
                return@flow
            }
        }

        emit(Result.Error("No memory metrics available"))
    }

    fun getBandwidth(token: String, dropletId: Int, interfaceName: String, direction: String): Flow<Result<List<MetricPoint>>> = flow {
        emit(Result.Loading)
        val api = serviceFactory(token)
        val (start, end) = twentyFourHourWindowSeconds()
        val result = parseMetricsResult(safeApiCall(retries = 2, apiCall = { api.getBandwidth(dropletId, interfaceName, direction, start, end) }))
        emit(result)
    }

    fun getFilesystemUsed(token: String, dropletId: Int): Flow<Result<List<MetricPoint>>> = flow {
        emit(Result.Loading)
        val api = serviceFactory(token)
        val (start, end) = twentyFourHourWindowSeconds()

        val sizeResult = parseMetricsResult(safeApiCall(retries = 2, apiCall = { api.getFilesystemSize(dropletId, start, end) }))
        val freeResult = parseMetricsResult(safeApiCall(retries = 2, apiCall = { api.getFilesystemFree(dropletId, start, end) }))

        if (sizeResult is Result.Success && freeResult is Result.Success) {
            val sizeMap = sizeResult.data.associateBy { it.timestamp }
            val freeMap = freeResult.data.associateBy { it.timestamp }

            val merged = sizeMap.keys.intersect(freeMap.keys).sorted().mapNotNull { ts ->
                val total = sizeMap[ts]?.value
                val free = freeMap[ts]?.value
                if (total != null && free != null && total > 0) {
                    val used = total - free
                    val percentage = (used / total) * 100
                    MetricPoint(ts, percentage)
                } else null
            }
            if (merged.isNotEmpty()) {
                emit(Result.Success(merged))
                return@flow
            }
        }
        emit(Result.Error("Could not calculate filesystem usage"))
    }

    fun getLoad1(token: String, dropletId: Int): Flow<Result<List<MetricPoint>>> = flow {
        emit(Result.Loading)
        val api = serviceFactory(token)
        val (start, end) = twentyFourHourWindowSeconds()
        val result = parseMetricsResult(safeApiCall(retries = 2, apiCall = { api.getLoad1(dropletId, start, end) }))
        emit(result)
    }

    suspend fun listProjects(token: String): Result<List<Project>> {
        val api = serviceFactory(token)
        return when (val response = safeApiCall(retries = 2, apiCall = { api.listProjects() }, onSuccess = {
            it.body()?.projects?.let { projects -> Result.Success(projects) } ?: Result.Error("Empty response")
        })) {
            is Result.Success -> Result.Success(response.data)
            is Result.Error -> response
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun setDefaultProject(token: String, projectId: String): Result<Unit> {
        val api = serviceFactory(token)
        return when (val result = safeApiCall(apiCall = { api.setDefaultProject(projectId, mapOf("is_default" to true)) }, onSuccess = { _ -> Result.Success(Unit) })) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    private fun parseAnyToDouble(any: Any?): Double? {
        return when (any) {
            is Number -> any.toDouble()
            is String -> any.toDoubleOrNull()
            else -> null
        }
    }

    private fun parseAnyToLong(any: Any?): Long? {
        return when (any) {
            is Number -> any.toLong()
            is String -> any.toDoubleOrNull()?.toLong()
            else -> null
        }
    }

    private fun getErrorMessage(response: Response<*>): String {
        val errorBody = response.errorBody()?.string()
        return try {
            val body = errorBody ?: "Unknown error"
            val json = JsonParser.parseString(body).asJsonObject
            json.get("message")?.asString?.ifBlank { body } ?: body
        } catch (_: Exception) {
            errorBody?.takeIf { it.isNotBlank() } ?: "Unknown error: ${response.code()}"
        }
    }

    private fun twentyFourHourWindowSeconds(): Pair<String, String> {
        val now = Instant.now()
        val end = now.epochSecond.toString()
        val start = now.minusSeconds(24 * 60 * 60L).epochSecond.toString()
        return start to end
    }

    private fun shouldRetry(code: Int): Boolean = code == 429 || code >= 500

    private fun isRetryableException(e: Exception): Boolean = e is IOException

    companion object {
        private const val MAX_RETRY_DELAY_MS = 4_000L
    }
}
