package com.omiddd.dropletmanager

import android.app.Application
import com.omiddd.dropletmanager.utils.LogUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import android.os.Bundle
import com.omiddd.dropletmanager.utils.CrashReporter

class DropletManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LogUtils.init(BuildConfig.DEBUG)

        // Initialize Firebase Analytics if google-services.json is provided
        try {
            val app = FirebaseApp.initializeApp(this)
            if (app != null) {
                // App Check: Use Debug provider in debug builds, Play Integrity in release
                val appCheck = FirebaseAppCheck.getInstance()
                if (BuildConfig.DEBUG) {
                    try {
                        val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                        val getInstance = clazz.getMethod("getInstance")
                        val factory = getInstance.invoke(null)
                        val appCheckProviderFactoryClass = Class.forName("com.google.firebase.appcheck.AppCheckProviderFactory")
                        if (appCheckProviderFactoryClass.isInstance(factory)) {
                            @Suppress("UNCHECKED_CAST")
                            appCheck.installAppCheckProviderFactory(factory as com.google.firebase.appcheck.AppCheckProviderFactory)
                            LogUtils.d("DropletManagerApp", "Firebase App Check initialized with Debug provider")
                        } else {
                            LogUtils.w("DropletManagerApp", "Debug App Check factory not available or wrong type")
                        }
                    } catch (e: Exception) {
                        LogUtils.w("DropletManagerApp", "Debug App Check init failed: ${e.message}")
                    }
                } else {
                    appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
                    LogUtils.d("DropletManagerApp", "Firebase App Check initialized with Play Integrity provider")
                }

                // Configure Crashlytics (if present) via CrashReporter abstraction
                // Enable during debug so test crashes are reported while setting up Crashlytics
                CrashReporter.setEnabled(true)
                CrashReporter.setKeys(
                    mapOf(
                        "build_type" to BuildConfig.BUILD_TYPE,
                        "version_name" to BuildConfig.VERSION_NAME,
                        "version_code" to BuildConfig.VERSION_CODE
                    )
                )
                // Install a global uncaught exception handler that reports to Crashlytics
                val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                    try {
                        CrashReporter.log("Uncaught exception in thread ${thread.name}")
                        CrashReporter.recordNonFatal(throwable, mapOf("thread" to thread.name, "build_type" to BuildConfig.BUILD_TYPE))
                    } catch (_: Throwable) {}
                    // invoke previous handler to allow normal termination / system dialogs
                    previousHandler?.uncaughtException(thread, throwable)
                }

                val analytics = FirebaseAnalytics.getInstance(this)
                // Log a lightweight event to verify wiring
                analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, Bundle())
                LogUtils.d("DropletManagerApp", "Firebase initialized and Analytics ready")
            } else {
                LogUtils.w("DropletManagerApp", "Firebase not initialized (missing google-services.json?)")
            }
        } catch (e: Exception) {
            LogUtils.w("DropletManagerApp", "Firebase init failed: ${e.message}")
        }
    }
}
