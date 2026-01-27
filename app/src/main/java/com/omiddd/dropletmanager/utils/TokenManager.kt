package com.omiddd.dropletmanager.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

class TokenManager(context: Context) {
    private val appContext = context.applicationContext

    private val sharedPreferences: SharedPreferences = createEncryptedPrefsWithRecovery(
        fileName = "secure_prefs"
    )

    companion object {
        private const val KEY_API_TOKEN = "api_token"
        private const val TAG = "TokenManager"
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
            CrashReporter.recordNonFatal(t, mapOf("where" to "TokenManager.init", "file" to fileName))
            // Wipe keyset prefs that hold Tink keyset encrypted by the master key.
            runCatching {
                appContext.getSharedPreferences(KEYSET_PREFS, Context.MODE_PRIVATE)
                    .edit().clear().commit()
            }
            // Delete the master key from Android Keystore so a fresh one is generated.
            runCatching {
                val ks = KeyStore.getInstance("AndroidKeyStore")
                ks.load(null)
                if (ks.containsAlias(MASTER_KEY_ALIAS)) ks.deleteEntry(MASTER_KEY_ALIAS)
            }
            // Remove the app-level encrypted prefs file to avoid stale ciphertext.
            runCatching { appContext.deleteSharedPreferences(fileName) }

            // Try again with a fresh key
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

    fun saveToken(token: String) {
        LogUtils.d(TAG, "Saving token (length: ${token.length})")
        sharedPreferences.edit().putString(KEY_API_TOKEN, token).apply()
        LogUtils.d(TAG, "Token saved successfully")
    }

    fun getToken(): String? {
        val token = sharedPreferences.getString(KEY_API_TOKEN, null)
        LogUtils.d(TAG, "Retrieved token: ${if (token != null) "present (length: ${token.length})" else "null"}")
        return token
    }

    fun clearToken() {
        LogUtils.d(TAG, "Clearing token")
        sharedPreferences.edit().remove(KEY_API_TOKEN).apply()
        LogUtils.d(TAG, "Token cleared successfully")
    }

    fun hasToken(): Boolean {
        val hasToken = !getToken().isNullOrEmpty()
        LogUtils.d(TAG, "Checking for token: $hasToken")
        return hasToken
    }
} 
