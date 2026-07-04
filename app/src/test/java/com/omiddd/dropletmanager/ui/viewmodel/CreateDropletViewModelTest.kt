package com.omiddd.dropletmanager.ui.viewmodel

import com.omiddd.dropletmanager.data.model.Image
import com.omiddd.dropletmanager.data.model.Region
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateDropletViewModelTest {

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
    fun `loadOptions does not fail when ssh key lookup fails`() = runTest(dispatcher) {
        val repository = object : DropletRepository() {
            override suspend fun listRegions(token: String) = Result.Success(
                listOf(Region("fra1", "Frankfurt", emptyList(), true, emptyList()))
            )

            override suspend fun listSizes(token: String) = Result.Success(
                listOf(Size("s-1vcpu-1gb", 1024, 1, 25, 1.0, 6.0, 0.009, true))
            )

            override suspend fun listImages(token: String) = Result.Success(
                listOf(Image(101, "22.04 x64", "Ubuntu", "ubuntu-22-04-x64", "snapshot"))
            )

            override suspend fun listSshKeys(token: String) = Result.Error("ssh endpoint unavailable")
        }

        val viewModel = CreateDropletViewModel("token", repository)
        viewModel.loadOptions()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.error)
        assertEquals(1, state.regions.size)
        assertEquals(1, state.sizes.size)
        assertEquals(1, state.images.size)
        assertEquals(emptyList<com.omiddd.dropletmanager.data.model.SshKey>(), state.sshKeys)
    }

    @Test
    fun `loadOptions ignores stale results after token update`() = runTest(dispatcher) {
        val repository = object : DropletRepository() {
            override suspend fun listRegions(token: String): Result<List<Region>> {
                if (token == "old-token") delay(1_000)
                return Result.Success(listOf(Region(token, "Region-$token", emptyList(), true, emptyList())))
            }

            override suspend fun listSizes(token: String): Result<List<Size>> =
                Result.Success(listOf(Size("size-$token", 1024, 1, 25, 1.0, 6.0, 0.009, true)))

            override suspend fun listImages(token: String): Result<List<Image>> =
                Result.Success(listOf(Image(101, "Image-$token", "Ubuntu", "ubuntu-$token", "snapshot")))

            override suspend fun listSshKeys(token: String) = Result.Success(emptyList<com.omiddd.dropletmanager.data.model.SshKey>())
        }

        val viewModel = CreateDropletViewModel("old-token", repository)
        viewModel.loadOptions()
        viewModel.updateToken("new-token")

        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.error)
        assertEquals(listOf("new-token"), state.regions.map { it.slug })
        assertEquals(listOf("size-new-token"), state.sizes.map { it.slug })
        assertEquals(listOf("ubuntu-new-token"), state.images.mapNotNull { it.slug })
    }
}
