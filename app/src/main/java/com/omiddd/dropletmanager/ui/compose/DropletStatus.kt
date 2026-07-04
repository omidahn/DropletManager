package com.omiddd.dropletmanager.ui.compose

internal fun isDropletRunning(status: String?): Boolean {
    if (status == null) return false
    return when (status.trim().lowercase()) {
        "active", "on", "running", "online", "up" -> true
        else -> false
    }
}
