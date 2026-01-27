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
        private const val KEY_PASSPHRASE = "ssh_passphrase"
        private const val KEY_USERNAME = "ssh_username"
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
            Log.w(TAG, "EncryptedSharedPreferences init failed: ${t.javaClass.simpleName}. Attempting recovery…")
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

    fun savePrivateKey(pem: String, passphrase: String?) {
        prefs.edit()
            .putString(KEY_PRIVATE_PEM, pem)
            .apply()
        if (passphrase != null) {
            prefs.edit().putString(KEY_PASSPHRASE, passphrase).apply()
        } else {
            prefs.edit().remove(KEY_PASSPHRASE).apply()
        }
    }

    fun getPrivateKeyBytes(): ByteArray? {
        val pem = prefs.getString(KEY_PRIVATE_PEM, null) ?: return null
        return pem.toByteArray()
    }

    fun getPassphrase(): String? = prefs.getString(KEY_PASSPHRASE, null)

    fun hasKey(): Boolean = prefs.getString(KEY_PRIVATE_PEM, null).isNullOrEmpty().not()

    fun clear() { prefs.edit().remove(KEY_PRIVATE_PEM).remove(KEY_PASSPHRASE).apply() }

    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
}
