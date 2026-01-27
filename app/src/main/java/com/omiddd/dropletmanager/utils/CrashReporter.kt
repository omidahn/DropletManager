package com.omiddd.dropletmanager.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashReporter {
    fun setEnabled(enabled: Boolean) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
    }

    fun setKeys(keys: Map<String, Any>) {
        keys.forEach { (key, value) ->
            when (value) {
                is String -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
                is Int -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
                is Boolean -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
            }
        }
    }

    fun log(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }

    fun recordNonFatal(throwable: Throwable, customKeys: Map<String, String> = emptyMap()) {
        customKeys.forEach { (key, value) ->
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }
}