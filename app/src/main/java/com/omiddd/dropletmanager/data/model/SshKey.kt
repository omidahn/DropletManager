package com.omiddd.dropletmanager.data.model

data class SshKey(
    val id: Int,
    val name: String,
    val fingerprint: String
)

data class SshKeysResponse(
    val ssh_keys: List<SshKey>,
    val meta: Meta
)

