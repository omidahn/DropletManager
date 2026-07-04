package com.omiddd.dropletmanager

import android.app.Application
import com.omiddd.dropletmanager.utils.CrashReporter
import com.omiddd.dropletmanager.utils.LogUtils
import com.google.firebase.FirebaseApp

class DropletManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LogUtils.init(BuildConfig.DEBUG)

        // Initialize Firebase services when google-services.json is present.
        try {
            val app = FirebaseApp.initializeApp(this)
            if (app != null) {
                // Configure Crashlytics via the app-level abstraction.
                CrashReporter.setEnabled(true)
                CrashReporter.setKeys(
                    mapOf(
                        "build_type" to BuildConfig.BUILD_TYPE,
                        "version_name" to BuildConfig.VERSION_NAME,
                        "version_code" to BuildConfig.VERSION_CODE
                    )
                )
                val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                    try {
                        CrashReporter.log("Uncaught exception in thread ${thread.name}")
                        CrashReporter.recordNonFatal(throwable, mapOf("thread" to thread.name, "build_type" to BuildConfig.BUILD_TYPE))
                    } catch (_: Throwable) {}
                    previousHandler?.uncaughtException(thread, throwable)
                }
                LogUtils.d("DropletManagerApp", "Firebase initialized and CrashReporter ready")
            } else {
                LogUtils.w("DropletManagerApp", "Firebase not initialized (missing google-services.json?)")
            }
        } catch (e: Exception) {
            LogUtils.w("DropletManagerApp", "Firebase init failed: ${e.message}")
        }
    }
}
