package com.omiddd.dropletmanager.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashReporter {
    internal interface Delegate {
        fun setEnabled(enabled: Boolean)
        fun setCustomKey(key: String, value: String)
        fun setCustomKey(key: String, value: Int)
        fun setCustomKey(key: String, value: Boolean)
        fun log(message: String)
        fun recordException(throwable: Throwable)
    }

    private object FirebaseDelegate : Delegate {
        override fun setEnabled(enabled: Boolean) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
        }

        override fun setCustomKey(key: String, value: String) {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }

        override fun setCustomKey(key: String, value: Int) {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }

        override fun setCustomKey(key: String, value: Boolean) {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }

        override fun log(message: String) {
            FirebaseCrashlytics.getInstance().log(message)
        }

        override fun recordException(throwable: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }

    @Volatile
    internal var delegate: Delegate = FirebaseDelegate

    private inline fun runSafely(action: () -> Unit) {
        runCatching(action).onFailure { error ->
            runCatching {
                System.err.println("CrashReporter failure: ${error.javaClass.simpleName}")
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        runSafely { delegate.setEnabled(enabled) }
    }

    fun setKeys(keys: Map<String, Any>) {
        runSafely {
            keys.forEach { (key, value) ->
                when (value) {
                    is String -> delegate.setCustomKey(key, value)
                    is Int -> delegate.setCustomKey(key, value)
                    is Boolean -> delegate.setCustomKey(key, value)
                }
            }
        }
    }

    fun log(message: String) {
        runSafely { delegate.log(message) }
    }

    fun recordNonFatal(throwable: Throwable, customKeys: Map<String, String> = emptyMap()) {
        runSafely {
            customKeys.forEach { (key, value) ->
                delegate.setCustomKey(key, value)
            }
            delegate.recordException(throwable)
        }
    }
}
