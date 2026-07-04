package com.omiddd.dropletmanager.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

class SshKeyManager(context: Context) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences = createEncryptedPrefsWithRecovery(
        fileName = "secure_prefs_ssh"
    )

    companion object {
        private const val KEY_PRIVATE_PEM = "ssh_private_pem"
        private const val KEY_PUBLIC_OPENSSH = "ssh_public_openssh"
        private const val KEY_PASSPHRASE = "ssh_passphrase"
        private const val KEY_USERNAME = "ssh_username"
        private const val KEY_REMOTE_KEY_ID = "ssh_remote_key_id"
        private const val KEY_REMOTE_FINGERPRINT = "ssh_remote_fingerprint"
        private const val KEY_REMOTE_NAME = "ssh_remote_name"
        private const val KEY_KEY_SOURCE = "ssh_key_source"
        private const val SOURCE_IMPORTED = "imported"
        private const val SOURCE_APP_MANAGED = "app_managed"
        private const val TAG = "SshKeyManager"
        private const val MASTER_KEY_ALIAS = "androidx_security_master_key"
        private const val KEYSET_PREFS = "androidx.security.crypto.master_key_keyset_prefs"
    }

    private fun createEncryptedPrefsWithRecovery(fileName: String): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (t: Throwable) {
            Log.w(TAG, "EncryptedSharedPreferences init failed: ${t.javaClass.simpleName}. Attempting recovery...")
            CrashReporter.recordNonFatal(t, mapOf("where" to "SshKeyManager.init", "file" to fileName))
            runCatching {
                appContext.getSharedPreferences(KEYSET_PREFS, Context.MODE_PRIVATE)
                    .edit().clear().commit()
            }
            runCatching {
                val ks = KeyStore.getInstance("AndroidKeyStore")
                ks.load(null)
                if (ks.containsAlias(MASTER_KEY_ALIAS)) ks.deleteEntry(MASTER_KEY_ALIAS)
            }
            runCatching { appContext.deleteSharedPreferences(fileName) }

            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun savePrivateKey(
        pem: String,
        passphrase: String?,
        publicKey: String? = null,
        source: String = SOURCE_IMPORTED
    ) {
        prefs.edit().apply {
            putString(KEY_PRIVATE_PEM, pem)
            putString(KEY_KEY_SOURCE, source)
            if (publicKey.isNullOrBlank()) {
                remove(KEY_PUBLIC_OPENSSH)
                remove(KEY_REMOTE_KEY_ID)
                remove(KEY_REMOTE_FINGERPRINT)
                remove(KEY_REMOTE_NAME)
            } else {
                putString(KEY_PUBLIC_OPENSSH, publicKey)
            }
            if (passphrase.isNullOrBlank()) {
                remove(KEY_PASSPHRASE)
            } else {
                putString(KEY_PASSPHRASE, passphrase)
            }
        }.apply()
    }

    fun saveGeneratedKey(key: GeneratedSshKey) {
        savePrivateKey(
            pem = key.privateKeyPem,
            passphrase = null,
            publicKey = key.publicKeyOpenSsh,
            source = SOURCE_APP_MANAGED
        )
    }

    fun getPrivateKeyBytes(): ByteArray? {
        val pem = prefs.getString(KEY_PRIVATE_PEM, null) ?: return null
        return pem.toByteArray()
    }

    fun getPublicKey(): String? = prefs.getString(KEY_PUBLIC_OPENSSH, null)

    fun getPassphrase(): String? = prefs.getString(KEY_PASSPHRASE, null)

    fun hasKey(): Boolean = prefs.getString(KEY_PRIVATE_PEM, null).isNullOrEmpty().not()

    fun isAppManagedKey(): Boolean = prefs.getString(KEY_KEY_SOURCE, null) == SOURCE_APP_MANAGED

    fun saveRemoteKeyMetadata(id: Int, name: String, fingerprint: String?) {
        prefs.edit().apply {
            putInt(KEY_REMOTE_KEY_ID, id)
            putString(KEY_REMOTE_NAME, name)
            if (fingerprint.isNullOrBlank()) {
                remove(KEY_REMOTE_FINGERPRINT)
            } else {
                putString(KEY_REMOTE_FINGERPRINT, fingerprint)
            }
        }.apply()
    }

    fun getRemoteKeyId(): Int? {
        return if (prefs.contains(KEY_REMOTE_KEY_ID)) prefs.getInt(KEY_REMOTE_KEY_ID, -1).takeIf { it > 0 } else null
    }

    fun getRemoteKeyName(): String? = prefs.getString(KEY_REMOTE_NAME, null)

    fun getRemoteFingerprint(): String? = prefs.getString(KEY_REMOTE_FINGERPRINT, null)

    fun clear() {
        prefs.edit()
            .remove(KEY_PRIVATE_PEM)
            .remove(KEY_PUBLIC_OPENSSH)
            .remove(KEY_PASSPHRASE)
            .remove(KEY_REMOTE_KEY_ID)
            .remove(KEY_REMOTE_FINGERPRINT)
            .remove(KEY_REMOTE_NAME)
            .remove(KEY_KEY_SOURCE)
            .apply()
    }

    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
}
