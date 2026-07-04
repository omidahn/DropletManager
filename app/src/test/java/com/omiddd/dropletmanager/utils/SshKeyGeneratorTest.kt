package com.omiddd.dropletmanager.utils

import org.junit.Assert.assertTrue
import org.junit.Test

class SshKeyGeneratorTest {

    @Test
    fun `generate returns pem public key and fingerprint`() {
        val key = SshKeyGenerator.generate("droplet-manager@test")

        assertTrue(key.privateKeyPem.startsWith("-----BEGIN PRIVATE KEY-----"))
        assertTrue(key.privateKeyPem.contains("-----END PRIVATE KEY-----"))
        assertTrue(key.publicKeyOpenSsh.startsWith("ssh-rsa "))
        assertTrue(key.publicKeyOpenSsh.endsWith(" droplet-manager@test"))
        assertTrue(key.fingerprintSha256.startsWith("SHA256:"))
    }
}
