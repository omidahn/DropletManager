package com.omiddd.dropletmanager.utils

import android.util.Log

object LogUtils {
    private const val DEFAULT_TAG = "DropletManager"
    private var isDebugEnabled = true

    fun init(debugEnabled: Boolean = true) {
        isDebugEnabled = debugEnabled
    }

    fun d(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.d(tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebugEnabled) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }

    fun w(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.w(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.i(tag, message)
        }
    }

    fun v(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.v(tag, message)
        }
    }
} 