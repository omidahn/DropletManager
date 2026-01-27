package com.omiddd.dropletmanager.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class CostEstimation(
    val dropletId: Int,
    val name: String,
    @SerializedName("price_monthly")
    val monthlyPrice: Double,
    @SerializedName("price_hourly")
    val hourlyPrice: Double,
    val backupsEnabled: Boolean = false,
    @SerializedName("backup_cost")
    val backupCost: Double = 0.0,
    val monitoringEnabled: Boolean = false,
    @SerializedName("monitoring_cost")
    val monitoringCost: Double = 0.0
) : Parcelable {
    val totalMonthlyCost: Double
        get() = monthlyPrice + 
                (if (backupsEnabled) backupCost else 0.0) + 
                (if (monitoringEnabled) monitoringCost else 0.0)

    val totalHourlyCost: Double
        get() = hourlyPrice + 
                (if (backupsEnabled) backupCost / 730.0 else 0.0) + // 730 hours in a month
                (if (monitoringEnabled) monitoringCost / 730.0 else 0.0)
}

@Parcelize
data class ProjectCostSummary(
    val totalMonthlyCost: Double,
    val totalHourlyCost: Double,
    val dropletCosts: List<CostEstimation>,
    val backupCosts: Double,
    val monitoringCosts: Double
) : Parcelable 