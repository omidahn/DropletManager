package com.omiddd.dropletmanager.utils

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey
import java.util.Base64

data class GeneratedSshKey(
    val privateKeyPem: String,
    val publicKeyOpenSsh: String,
    val fingerprintSha256: String
)

object SshKeyGenerator {
    fun generate(comment: String): GeneratedSshKey {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(4096)
        }.generateKeyPair()

        val publicKey = keyPair.public as RSAPublicKey
        val publicBlob = buildRsaPublicBlob(publicKey)
        val publicBase64 = Base64.getEncoder().encodeToString(publicBlob)
        val normalizedComment = comment.trim().ifBlank { "droplet-manager@android" }

        return GeneratedSshKey(
            privateKeyPem = encodePem("PRIVATE KEY", keyPair.private.encoded),
            publicKeyOpenSsh = "ssh-rsa $publicBase64 $normalizedComment",
            fingerprintSha256 = "SHA256:${sha256Base64(publicBlob)}"
        )
    }

    private fun buildRsaPublicBlob(publicKey: RSAPublicKey): ByteArray {
        val out = ByteArrayOutputStream()
        writeSshString(out, "ssh-rsa".toByteArray(Charsets.UTF_8))
        writeMpInt(out, publicKey.publicExponent)
        writeMpInt(out, publicKey.modulus)
        return out.toByteArray()
    }

    private fun writeSshString(out: ByteArrayOutputStream, value: ByteArray) {
        out.write(intToBytes(value.size))
        out.write(value)
    }

    private fun writeMpInt(out: ByteArrayOutputStream, value: BigInteger) {
        val encoded = value.toByteArray()
        out.write(intToBytes(encoded.size))
        out.write(encoded)
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            ((value ushr 24) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    private fun sha256Base64(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return Base64.getEncoder().withoutPadding().encodeToString(digest)
    }

    private fun encodePem(type: String, encoded: ByteArray): String {
        val body = Base64.getMimeEncoder(64, "\n".toByteArray())
            .encodeToString(encoded)
        return "-----BEGIN $type-----\n$body\n-----END $type-----"
    }
}
