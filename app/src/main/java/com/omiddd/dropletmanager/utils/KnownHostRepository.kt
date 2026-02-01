package com.omiddd.dropletmanager.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

class KnownHostRepository(context: Context) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences = createEncryptedPrefsWithRecovery()

    data class StoredHostKey(
        val algorithm: String,
        val keyBase64: String,
        val fingerprint: String
    ) {
        fun matches(algorithm: String, keyBase64: String): Boolean {
            return this.algorithm == algorithm && this.keyBase64 == keyBase64
        }

        fun matchesKey(keyBase64: String): Boolean {
            return this.keyBase64 == keyBase64
        }
    }

    companion object {
        private const val TAG = "KnownHostRepository"
        private const val PREFS_FILE_NAME = "secure_known_hosts"
        private const val MASTER_KEY_ALIAS = "androidx_security_master_key"
        private const val KEYSET_PREFS = "androidx.security.crypto.master_key_keyset_prefs"

        private fun prefKeyForHost(host: String, port: Int): String = "host_${host}_$port"
        private fun legacyPrefKeyForHost(host: String): String = "host_$host"
    }

    private fun createEncryptedPrefsWithRecovery(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (t: Throwable) {
            Log.w(TAG, "EncryptedSharedPreferences init failed: ${t.javaClass.simpleName}. Attempting recovery…")
            CrashReporter.recordNonFatal(t, mapOf("where" to "KnownHostRepository.init", "file" to PREFS_FILE_NAME))
            runCatching {
                appContext.getSharedPreferences(KEYSET_PREFS, Context.MODE_PRIVATE)
                    .edit(commit = true) { clear() }
            }
            runCatching {
                val ks = KeyStore.getInstance("AndroidKeyStore")
                ks.load(null)
                if (ks.containsAlias(MASTER_KEY_ALIAS)) ks.deleteEntry(MASTER_KEY_ALIAS)
            }
            runCatching { appContext.deleteSharedPreferences(PREFS_FILE_NAME) }

            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun getHostKey(host: String, port: Int): StoredHostKey? {
        val raw = prefs.getString(prefKeyForHost(host, port), null)
            ?: prefs.getString(legacyPrefKeyForHost(host), null)
            ?: return null
        val parts = raw.split('|')
        if (parts.size != 3) return null
        return StoredHostKey(parts[0], parts[1], parts[2])
    }

    fun saveHostKey(host: String, port: Int, algorithm: String, keyBytes: ByteArray, fingerprint: String) {
        val keyBase64 = Base64.encodeToString(keyBytes, Base64.NO_WRAP)
        prefs.edit {
            putString(prefKeyForHost(host, port), listOf(algorithm, keyBase64, fingerprint).joinToString("|"))
        }
    }

    fun removeHostKey(host: String, port: Int) {
        prefs.edit { remove(prefKeyForHost(host, port)) }
    }
}
