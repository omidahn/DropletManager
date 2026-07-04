package com.omiddd.dropletmanager.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReporterTest {

    @Test
    fun `recordNonFatal forwards keys and exception to delegate`() {
        val delegate = RecordingDelegate()
        val previous = CrashReporter.delegate
        CrashReporter.delegate = delegate
        val error = IllegalStateException("boom")

        try {
            CrashReporter.recordNonFatal(error, mapOf("screen" to "main"))
        } finally {
            CrashReporter.delegate = previous
        }

        assertEquals(mapOf("screen" to "main"), delegate.stringKeys)
        assertEquals(error, delegate.recordedException)
    }

    @Test
    fun `setKeys ignores unsupported values and does not throw when delegate fails`() {
        val delegate = object : CrashReporter.Delegate {
            override fun setEnabled(enabled: Boolean) = error("fail")
            override fun setCustomKey(key: String, value: String) = error("fail")
            override fun setCustomKey(key: String, value: Int) = error("fail")
            override fun setCustomKey(key: String, value: Boolean) = error("fail")
            override fun log(message: String) = error("fail")
            override fun recordException(throwable: Throwable) = error("fail")
        }
        val previous = CrashReporter.delegate
        CrashReporter.delegate = delegate

        try {
            CrashReporter.setEnabled(true)
            CrashReporter.setKeys(mapOf("name" to "value", "count" to 3, "flag" to true, "ignored" to 4.5))
            CrashReporter.log("hello")
            CrashReporter.recordNonFatal(IllegalStateException("boom"))
        } finally {
            CrashReporter.delegate = previous
        }

        assertTrue(true)
    }

    private class RecordingDelegate : CrashReporter.Delegate {
        val stringKeys = linkedMapOf<String, String>()
        var recordedException: Throwable? = null

        override fun setEnabled(enabled: Boolean) = Unit

        override fun setCustomKey(key: String, value: String) {
            stringKeys[key] = value
        }

        override fun setCustomKey(key: String, value: Int) {
            stringKeys[key] = value.toString()
        }

        override fun setCustomKey(key: String, value: Boolean) {
            stringKeys[key] = value.toString()
        }

        override fun log(message: String) = Unit

        override fun recordException(throwable: Throwable) {
            recordedException = throwable
        }
    }
}
