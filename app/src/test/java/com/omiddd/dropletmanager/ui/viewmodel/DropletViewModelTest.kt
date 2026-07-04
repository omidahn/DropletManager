package com.omiddd.dropletmanager.ui.viewmodel

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
import com.omiddd.dropletmanager.data.model.Droplet
import com.omiddd.dropletmanager.data.model.Image
import com.omiddd.dropletmanager.data.model.MetricsResponse
import com.omiddd.dropletmanager.data.model.NetworkV4
import com.omiddd.dropletmanager.data.model.Networks
import com.omiddd.dropletmanager.data.model.ProjectsResponse
import com.omiddd.dropletmanager.data.model.Region
import com.omiddd.dropletmanager.data.model.SshKeyResponse
import com.omiddd.dropletmanager.data.model.SshKeysResponse
import com.omiddd.dropletmanager.data.model.Size
import com.omiddd.dropletmanager.data.repository.DropletRepository
import com.omiddd.dropletmanager.data.repository.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DropletViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `old droplet responses do not overwrite state after token change`() = runTest(dispatcher) {
        val repository = DropletRepository { token -> FakeDigitalOceanService(token) }

        val viewModel = DropletViewModel("old-token", repository)
        viewModel.loadDroplets()
        viewModel.updateToken("new-token")
        viewModel.loadDroplets()

        advanceUntilIdle()

        assertEquals(listOf("new-droplet"), viewModel.uiState.value.droplets.map { it.name })
    }

    private fun fakeDroplet(name: String): Droplet {
        return Droplet(
            id = 1,
            name = name,
            memory = 1024,
            vcpus = 1,
            disk = 25,
            status = "active",
            region = Region("fra1", "Frankfurt", emptyList(), true, emptyList()),
            image = Image(101, "22.04 x64", "Ubuntu", "ubuntu-22-04-x64", "distribution"),
            size = Size("s-1vcpu-1gb", 1024, 1, 25, 1.0, 6.0, 0.009, true),
            createdAt = "2024-01-01T00:00:00Z",
            networks = Networks(
                v4 = listOf(NetworkV4("203.0.113.10", "255.255.255.0", "203.0.113.1", "public")),
                v6 = emptyList()
            ),
            features = emptyList(),
            tags = emptyList(),
            volumeIds = emptyList(),
            sizeSlug = "s-1vcpu-1gb",
            backupIds = emptyList(),
            snapshotIds = emptyList(),
            monitoringPolicy = null,
            backups = false,
            monitoring = true,
            ipv6 = false
        )
    }

    private class FakeDigitalOceanService(
        private val token: String
    ) : DigitalOceanService {
        override suspend fun listDroplets() = retrofit2.Response.success(
            DropletResponse(
                droplets = listOf(fakeDropletStatic(if (token == "old-token") "old-droplet" else "new-droplet", token == "old-token")),
                meta = com.omiddd.dropletmanager.data.model.Meta(total = 1, page = 1, perPage = 20)
            )
        )

        override suspend fun createDroplet(request: com.omiddd.dropletmanager.data.model.DropletCreationRequest) = notImplemented<CreateDropletResponse>()
        override suspend fun deleteDroplet(dropletId: Int) = notImplemented<Unit>()
        override suspend fun listRegions() = notImplemented<RegionsResponse>()
        override suspend fun listSizes() = notImplemented<SizesResponse>()
        override suspend fun listImages(type: String) = notImplemented<ImagesResponse>()
        override suspend fun getDroplet(dropletId: Int) = notImplemented<SingleDropletResponse>()
        override suspend fun performAction(dropletId: Int, action: ActionRequest) = notImplemented<Unit>()
        override suspend fun getSnapshots(dropletId: Int) = notImplemented<SnapshotResponse>()
        override suspend fun getBackups(dropletId: Int) = notImplemented<BackupResponse>()
        override suspend fun getCpuMetrics(dropletId: Int, startTime: String, endTime: String) = notImplemented<MetricsResponse>()
        override suspend fun getMemoryUtilizationDroplet(dropletId: Int, startTime: String, endTime: String) = notImplemented<MetricsResponse>()
        override suspend fun getMemoryTotalDroplet(dropletId: Int, startTime: String, endTime: String) = notImplemented<MetricsResponse>()
        override suspend fun getMemoryFreeDroplet(dropletId: Int, startTime: String, endTime: String) = notImplemented<MetricsResponse>()
        override suspend fun getBandwidth(dropletId: Int, interfaceName: String, direction: String, startTime: String, endTime: String) = notImplemented<MetricsResponse>()
        override suspend fun getFilesystemFree(dropletId: Int, startTime: String, endTime: String) = notImplemented<MetricsResponse>()
        override suspend fun getFilesystemSize(dropletId: Int, startTime: String, endTime: String) = notImplemented<MetricsResponse>()
        override suspend fun getLoad1(dropletId: Int, startTime: String, endTime: String) = notImplemented<MetricsResponse>()
        override suspend fun listProjects() = notImplemented<ProjectsResponse>()
        override suspend fun setDefaultProject(projectId: String, request: Map<String, Boolean>) = notImplemented<ProjectResponse>()
        override suspend fun listSshKeys() = notImplemented<SshKeysResponse>()
        override suspend fun createSshKey(request: com.omiddd.dropletmanager.data.model.CreateSshKeyRequest) = notImplemented<SshKeyResponse>()

        private suspend fun <T> notImplemented(): retrofit2.Response<T> {
            throw NotImplementedError("Not needed in this test")
        }

        companion object {
            private suspend fun fakeDropletStatic(name: String, slow: Boolean): Droplet {
                if (slow) {
                    delay(1_000)
                }
                return Droplet(
                    id = 1,
                    name = name,
                    memory = 1024,
                    vcpus = 1,
                    disk = 25,
                    status = "active",
                    region = Region("fra1", "Frankfurt", emptyList(), true, emptyList()),
                    image = Image(101, "22.04 x64", "Ubuntu", "ubuntu-22-04-x64", "distribution"),
                    size = Size("s-1vcpu-1gb", 1024, 1, 25, 1.0, 6.0, 0.009, true),
                    createdAt = "2024-01-01T00:00:00Z",
                    networks = Networks(
                        v4 = listOf(NetworkV4("203.0.113.10", "255.255.255.0", "203.0.113.1", "public")),
                        v6 = emptyList()
                    ),
                    features = emptyList(),
                    tags = emptyList(),
                    volumeIds = emptyList(),
                    sizeSlug = "s-1vcpu-1gb",
                    backupIds = emptyList(),
                    snapshotIds = emptyList(),
                    monitoringPolicy = null,
                    backups = false,
                    monitoring = true,
                    ipv6 = false
                )
            }
        }
    }
}
