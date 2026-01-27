package com.omiddd.dropletmanager.data.model

sealed class DropletAction(val key: String) {
    object PowerOn : DropletAction("power_on")
    object PowerOff : DropletAction("power_off")
    object Reboot : DropletAction("reboot")
    object PowerCycle : DropletAction("power_cycle")
    object Shutdown : DropletAction("shutdown")
    object Snapshot : DropletAction("snapshot")
    object Backup : DropletAction("backup")
    object EnableBackups : DropletAction("enable_backups")
    object DisableBackups : DropletAction("disable_backups")
}
