package com.omiddd.dropletmanager.data.model

import com.google.gson.annotations.SerializedName

data class SshKey(
    val id: Int,
    val name: String,
    val fingerprint: String,
    @SerializedName("public_key")
    val publicKey: String? = null
)

data class SshKeysResponse(
    val ssh_keys: List<SshKey>,
    val meta: Meta
)

data class CreateSshKeyRequest(
    val name: String,
    @SerializedName("public_key")
    val publicKey: String
)

data class SshKeyResponse(
    @SerializedName("ssh_key")
    val sshKey: SshKey
)
