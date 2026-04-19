package com.omiddd.dropletmanager.data.api

import com.omiddd.dropletmanager.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface DigitalOceanService {
    @GET("v2/droplets")
    suspend fun listDroplets(): Response<DropletResponse>

    @POST("v2/droplets")
    suspend fun createDroplet(@Body request: DropletCreationRequest): Response<CreateDropletResponse>

    @DELETE("v2/droplets/{id}")
    suspend fun deleteDroplet(@Path("id") dropletId: Int): Response<Unit>

    @GET("v2/regions")
    suspend fun listRegions(): Response<RegionsResponse>

    @GET("v2/sizes")
    suspend fun listSizes(): Response<SizesResponse>

    @GET("v2/images")
    suspend fun listImages(@Query("type") type: String = "distribution"): Response<ImagesResponse>

    @GET("v2/droplets/{id}")
    suspend fun getDroplet(@Path("id") dropletId: Int): Response<SingleDropletResponse>

    @POST("v2/droplets/{id}/actions")
    suspend fun performAction(
        @Path("id") dropletId: Int,
        @Body action: ActionRequest
    ): Response<Unit>

    @GET("v2/droplets/{id}/snapshots")
    suspend fun getSnapshots(@Path("id") dropletId: Int): Response<SnapshotResponse>

    @GET("v2/droplets/{id}/backups")
    suspend fun getBackups(@Path("id") dropletId: Int): Response<BackupResponse>

    @GET("v2/monitoring/metrics/droplet/cpu")
    suspend fun getCpuMetrics(
        @Query("host_id") dropletId: Int,
        @Query("start") startTime: String,
        @Query("end") endTime: String
    ): Response<MetricsResponse>

    @GET("v2/monitoring/metrics/droplet/memory_utilization")
    suspend fun getMemoryUtilizationDroplet(
        @Query("host_id") dropletId: Int,
        @Query("start") startTime: String,
        @Query("end") endTime: String
    ): Response<MetricsResponse>

    @GET("v2/monitoring/metrics/droplet/memory_total")
    suspend fun getMemoryTotalDroplet(
        @Query("host_id") dropletId: Int,
        @Query("start") startTime: String,
        @Query("end") endTime: String
    ): Response<MetricsResponse>

    @GET("v2/monitoring/metrics/droplet/memory_free")
    suspend fun getMemoryFreeDroplet(
        @Query("host_id") dropletId: Int,
        @Query("start") startTime: String,
        @Query("end") endTime: String
    ): Response<MetricsResponse>

    @GET("v2/monitoring/metrics/droplet/bandwidth")
    suspend fun getBandwidth(
        @Query("host_id") dropletId: Int,
        @Query("interface") interfaceName: String, // public|private
        @Query("direction") direction: String,     // inbound|outbound
        @Query("start") startTime: String,         // epoch seconds
        @Query("end") endTime: String
    ): Response<MetricsResponse>

    @GET("v2/monitoring/metrics/droplet/filesystem_free")
    suspend fun getFilesystemFree(
        @Query("host_id") dropletId: Int,
        @Query("start") startTime: String,
        @Query("end") endTime: String
    ): Response<MetricsResponse>

    @GET("v2/monitoring/metrics/droplet/filesystem_size")
    suspend fun getFilesystemSize(
        @Query("host_id") dropletId: Int,
        @Query("start") startTime: String,
        @Query("end") endTime: String
    ): Response<MetricsResponse>

    @GET("v2/monitoring/metrics/droplet/load_1")
    suspend fun getLoad1(
        @Query("host_id") dropletId: Int,
        @Query("start") startTime: String,
        @Query("end") endTime: String
    ): Response<MetricsResponse>

    @GET("v2/projects")
    suspend fun listProjects(): Response<ProjectsResponse>

    @PATCH("v2/projects/{id}")
    suspend fun setDefaultProject(
        @Path("id") projectId: String,
        @Body request: Map<String, Boolean>
    ): Response<ProjectResponse>

    @GET("v2/account/keys")
    suspend fun listSshKeys(): Response<SshKeysResponse>
}

data class DropletResponse(
    val droplets: List<Droplet>,
    val meta: Meta
)

data class SingleDropletResponse(
    val droplet: Droplet
)

data class SnapshotResponse(
    val snapshots: List<Snapshot>
)

data class BackupResponse(
    val backups: List<Backup>
)

data class RegionsResponse(
    val regions: List<Region>
)

data class SizesResponse(
    val sizes: List<Size>
)

data class ImagesResponse(
    val images: List<Image>
)

data class ProjectResponse(
    val project: Project
)

data class CreateDropletResponse(
    val droplet: Droplet?
)
