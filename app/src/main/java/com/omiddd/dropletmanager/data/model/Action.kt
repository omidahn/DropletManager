package com.omiddd.dropletmanager.data.model

import com.google.gson.annotations.SerializedName

data class Action(
    val id: Int,
    val status: String,
    val type: String,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("completed_at") val completedAt: String?,
    @SerializedName("resource_id") val resourceId: Int,
    @SerializedName("resource_type") val resourceType: String,
    val region: Region
)

data class ActionRequest(
    val type: String,
    val name: String? = null
) 