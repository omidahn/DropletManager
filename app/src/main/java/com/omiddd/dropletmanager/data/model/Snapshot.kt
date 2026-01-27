package com.omiddd.dropletmanager.data.model

data class Snapshot(
    val id: Int,
    val name: String,
    val created_at: String,
    val size_gigabytes: Double,
    val status: String,
    val description: String?,
    val regions: List<String>
) 