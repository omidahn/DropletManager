package com.omiddd.dropletmanager.data.repository

import com.omiddd.dropletmanager.data.api.BackupResponse
import com.omiddd.dropletmanager.data.api.CreateDropletResponse
import com.omiddd.dropletmanager.data.api.DigitalOceanService
import com.omiddd.dropletmanager.data.api.DropletResponse
import com.omiddd.dropletmanager.data.api.ImagesResponse
import com.omiddd.dropletmanager.data.api.ProjectResponse
import com.omiddd.dropletmanager.data.api.RegionsResponse
import com.omiddd.dropletmanager.data.api.SingleDropletResponse
import com.omiddd.dropletmanager.data.api.SizesResponse
import com.omiddd.dropletmanager.data.api.SnapshotResponse
import com.omiddd.dropletmanager.data.model.ActionRequest
import com.omiddd.dropletmanager.data.model.DropletCreationRequest
import com.omiddd.dropletmanager.data.model.MetricsResponse
import com.omiddd.dropletmanager.data.model.ProjectsResponse
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class DropletRepositoryTest {

    @Test
    fun `setDefaultProject preserves backend error message`() {
        val repository = DropletRepository { ErrorService("""{"message":"project is locked"}""") }

        val result = runSuspend { repository.setDefaultProject("token", "project-1") }

        assertEquals(Result.Error("project is locked"), result)
    }

    @Test
    fun `listRegions preserves non-json error body`() {
        val repository = DropletRepository { ErrorService("gateway temporarily unavailable") }

        val result = runSuspend { repository.listRegions("token") }

        assertEquals(Result.Error("gateway temporarily unavailable"), result)
    }

    @Test
    fun `listRegions retries retryable server failures`() = runTest {
        val service = RetryThenSuccessService(failuresBeforeSuccess = 2)
        val repository = DropletRepository { service }

        val result = repository.listRegions("token")

        assertEquals(3, service.calls)
        assertEquals(Result.Success(service.regions), result)
    }

    @Test
    fun `listRegions retries io exceptions`() = runTest {
        val service = IOExceptionThenSuccessService(failuresBeforeSuccess = 2)
        val repository = DropletRepository { service }

        val result = repository.listRegions("token")

        assertEquals(3, service.calls)
        assertEquals(Result.Success(service.regions), result)
    }

    private fun <T> runSuspend(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }

    private class ErrorService(
        private val errorBody: String
    ) : StubDigitalOceanService() {
        override suspend fun listRegions(): Response<RegionsResponse> = errorResponse()

        override suspend fun setDefaultProject(
            projectId: String,
            request: Map<String, Boolean>
        ): Response<ProjectResponse> = errorResponse()

        private fun <T> errorResponse(): Response<T> {
            return Response.error(
                400,
                errorBody.toResponseBody("application/json".toMediaType())
            )
        }
    }

    private class RetryThenSuccessService(
        private val failuresBeforeSuccess: Int
    ) : StubDigitalOceanService() {
        var calls = 0
        val regions = listOf(com.omiddd.dropletmanager.data.model.Region("fra1", "Frankfurt", emptyList(), true, emptyList()))

        override suspend fun listRegions(): Response<RegionsResponse> {
            calls++
            return if (calls <= failuresBeforeSuccess) {
                Response.error(
                    500,
                    """{"message":"temporary outage"}""".toResponseBody("application/json".toMediaType())
                )
            } else {
                Response.success(RegionsResponse(regions))
            }
        }
    }

    private class IOExceptionThenSuccessService(
        private val failuresBeforeSuccess: Int
    ) : StubDigitalOceanService() {
        var calls = 0
        val regions = listOf(com.omiddd.dropletmanager.data.model.Region("fra1", "Frankfurt", emptyList(), true, emptyList()))

        override suspend fun listRegions(): Response<RegionsResponse> {
            calls++
            if (calls <= failuresBeforeSuccess) {
                throw IOException("temporary network issue")
            }
            return Response.success(RegionsResponse(regions))
        }
    }

    private open class StubDigitalOceanService : DigitalOceanService {
        override suspend fun listDroplets(): Response<DropletResponse> = notImplemented()
        override suspend fun createDroplet(request: DropletCreationRequest): Response<CreateDropletResponse> = notImplemented()
        override suspend fun deleteDroplet(dropletId: Int): Response<Unit> = notImplemented()
        override suspend fun listRegions(): Response<RegionsResponse> = notImplemented()
        override suspend fun listSizes(): Response<SizesResponse> = notImplemented()
        override suspend fun listImages(type: String): Response<ImagesResponse> = notImplemented()
        override suspend fun getDroplet(dropletId: Int): Response<SingleDropletResponse> = notImplemented()
        override suspend fun performAction(dropletId: Int, action: ActionRequest): Response<Unit> = notImplemented()
        override suspend fun getSnapshots(dropletId: Int): Response<SnapshotResponse> = notImplemented()
        override suspend fun getBackups(dropletId: Int): Response<BackupResponse> = notImplemented()
        override suspend fun getCpuMetrics(dropletId: Int, startTime: String, endTime: String): Response<MetricsResponse> = notImplemented()
        override suspend fun getMemoryUtilizationDroplet(dropletId: Int, startTime: String, endTime: String): Response<MetricsResponse> = notImplemented()
        override suspend fun getMemoryTotalDroplet(dropletId: Int, startTime: String, endTime: String): Response<MetricsResponse> = notImplemented()
        override suspend fun getMemoryFreeDroplet(dropletId: Int, startTime: String, endTime: String): Response<MetricsResponse> = notImplemented()
        override suspend fun getBandwidth(dropletId: Int, interfaceName: String, direction: String, startTime: String, endTime: String): Response<MetricsResponse> = notImplemented()
        override suspend fun getFilesystemFree(dropletId: Int, startTime: String, endTime: String): Response<MetricsResponse> = notImplemented()
        override suspend fun getFilesystemSize(dropletId: Int, startTime: String, endTime: String): Response<MetricsResponse> = notImplemented()
        override suspend fun getLoad1(dropletId: Int, startTime: String, endTime: String): Response<MetricsResponse> = notImplemented()
        override suspend fun listProjects(): Response<ProjectsResponse> = notImplemented()
        override suspend fun setDefaultProject(projectId: String, request: Map<String, Boolean>): Response<ProjectResponse> = notImplemented()
        override suspend fun listSshKeys(): Response<com.omiddd.dropletmanager.data.model.SshKeysResponse> = notImplemented()
        override suspend fun createSshKey(request: com.omiddd.dropletmanager.data.model.CreateSshKeyRequest): Response<com.omiddd.dropletmanager.data.model.SshKeyResponse> = notImplemented()

        private fun <T> notImplemented(): Response<T> {
            throw NotImplementedError("Not needed in this test")
        }
    }
}
