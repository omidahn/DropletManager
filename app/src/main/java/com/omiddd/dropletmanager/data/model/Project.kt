package com.omiddd.dropletmanager.data.model

import com.google.gson.annotations.SerializedName

data class Project(
    val id: String,
    val name: String,
    @SerializedName("owner_uuid")
    val ownerUuid: String,
    val description: String?,
    val purpose: String?,
    @SerializedName("environment")
    val environmentName: String?,
    @SerializedName("is_default")
    val isDefault: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class ProjectsResponse(
    val projects: List<Project>,
    val meta: Meta
)

data class Meta(
    val total: Int,
    val page: Int,
    @SerializedName("per_page") val perPage: Int
)
