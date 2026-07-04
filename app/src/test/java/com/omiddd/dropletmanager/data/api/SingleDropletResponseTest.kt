package com.omiddd.dropletmanager.data.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class SingleDropletResponseTest {

    @Test
    fun `single droplet payload parses with dedicated response model`() {
        val json = """
            {
              "droplet": {
                "id": 42,
                "name": "web-1",
                "memory": 1024,
                "vcpus": 1,
                "disk": 25,
                "status": "active",
                "region": {
                  "slug": "fra1",
                  "name": "Frankfurt",
                  "sizes": ["s-1vcpu-1gb"],
                  "available": true,
                  "features": ["metadata"]
                },
                "image": {
                  "id": 101,
                  "name": "22.04 x64",
                  "distribution": "Ubuntu",
                  "slug": "ubuntu-22-04-x64",
                  "type": "snapshot"
                },
                "size": {
                  "slug": "s-1vcpu-1gb",
                  "memory": 1024,
                  "vcpus": 1,
                  "disk": 25,
                  "transfer": 1.0,
                  "price_monthly": 6.0,
                  "price_hourly": 0.009,
                  "available": true
                },
                "created_at": "2024-01-01T00:00:00Z",
                "networks": {
                  "v4": [
                    {
                      "ip_address": "203.0.113.10",
                      "netmask": "255.255.255.0",
                      "gateway": "203.0.113.1",
                      "type": "public"
                    }
                  ],
                  "v6": []
                },
                "features": [],
                "tags": [],
                "volume_ids": [],
                "size_slug": "s-1vcpu-1gb",
                "backup_ids": [],
                "snapshot_ids": [],
                "monitoring_policy": null,
                "backups": false,
                "monitoring": true,
                "ipv6": false
              }
            }
        """.trimIndent()

        val response = Gson().fromJson(json, SingleDropletResponse::class.java)

        assertEquals(42, response.droplet.id)
        assertEquals("web-1", response.droplet.name)
    }
}
