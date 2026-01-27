package com.omiddd.dropletmanager.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Droplet(
    val id: Int,
    val name: String,
    val memory: Int,
    val vcpus: Int,
    val disk: Int,
    val status: String,
    val region: Region,
    val image: Image,
    val size: Size,
    @SerializedName("created_at") val createdAt: String,
    val networks: Networks,
    val features: List<String>,
    val tags: List<String>,
    @SerializedName("volume_ids") val volumeIds: List<String>,
    @SerializedName("size_slug") val sizeSlug: String,
    @SerializedName("backup_ids") val backupIds: List<String>,
    @SerializedName("snapshot_ids") val snapshotIds: List<String>,
    @SerializedName("monitoring_policy") val monitoringPolicy: MonitoringPolicy?,
    val backups: Boolean,
    val monitoring: Boolean,
    val ipv6: Boolean
) : Parcelable {
    fun isDropletRunning(): Boolean {
        return status.equals("active", ignoreCase = true) || status.equals("on", ignoreCase = true)
    }
}

@Keep
@Parcelize
data class Region(
    val slug: String,
    val name: String,
    val sizes: List<String>,
    val available: Boolean,
    val features: List<String>
) : Parcelable

@Keep
@Parcelize
data class Image(
    val id: Int,
    val name: String,
    val distribution: String,
    val slug: String?,
    val type: String
) : Parcelable

@Keep
@Parcelize
data class Size(
    val slug: String,
    val memory: Int,
    val vcpus: Int,
    @SerializedName("disk")
    val diskSize: Int,
    val transfer: Double,
    @SerializedName("price_monthly") val priceMonthly: Double,
    @SerializedName("price_hourly") val priceHourly: Double? = null,
    val available: Boolean
) : Parcelable

@Keep
@Parcelize
data class Networks(
    val v4: List<NetworkV4>,
    val v6: List<NetworkV6>
) : Parcelable

@Keep
@Parcelize
data class NetworkV4(
    val ip_address: String,
    val netmask: String,
    val gateway: String,
    val type: String
) : Parcelable

@Keep
@Parcelize
data class NetworkV6(
    val ip_address: String,
    val netmask: Int,
    val gateway: String,
    val type: String
) : Parcelable

@Keep
@Parcelize
data class MonitoringPolicy(
    @SerializedName("cpu_threshold") val cpuThreshold: Int,
    @SerializedName("disk_threshold") val diskThreshold: Int,
    @SerializedName("memory_threshold") val memoryThreshold: Int,
    @SerializedName("alert_interval") val alertInterval: Int
) : Parcelable