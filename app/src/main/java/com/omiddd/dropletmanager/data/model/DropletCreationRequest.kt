package com.omiddd.dropletmanager.data.model

import com.google.gson.annotations.SerializedName

data class DropletCreationRequest(
    val name: String,
    val region: String,
    val size: String,
    @SerializedName("image")
    val imageId: String,
    @SerializedName("ssh_keys")
    val sshKeys: List<Int>? = null,
    val backups: Boolean = false,
    @SerializedName("ipv6")
    val ipv6: Boolean = false,
    @SerializedName("private_networking")
    val privateNetworking: Boolean = false,
    val monitoring: Boolean = true,
    val tags: List<String>? = null,
    @SerializedName("user_data")
    val userData: String? = null,
    @SerializedName("vpc_uuid")
    val vpcUuid: String? = null
) 
