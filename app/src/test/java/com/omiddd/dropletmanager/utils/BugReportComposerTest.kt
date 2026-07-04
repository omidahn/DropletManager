package com.omiddd.dropletmanager.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BugReportComposerTest {

    @Test
    fun `compose includes screen app and device metadata`() {
        val draft = BugReportComposer.compose(
            appName = "Droplet Manager",
            screenName = "Droplets",
            packageName = "com.omiddd.dropletmanager",
            versionName = "1.0.9",
            versionCode = 12,
            manufacturer = "Samsung",
            model = "SM-G986B",
            sdkInt = 33
        )

        assertEquals("Droplet Manager bug report", draft.subject)
        assertTrue(draft.body.contains("- Screen: Droplets"))
        assertTrue(draft.body.contains("- App version: 1.0.9 (12)"))
        assertTrue(draft.body.contains("- Package: com.omiddd.dropletmanager"))
        assertTrue(draft.body.contains("- Device: Samsung SM-G986B"))
        assertTrue(draft.body.contains("- Android SDK: 33"))
    }
}
