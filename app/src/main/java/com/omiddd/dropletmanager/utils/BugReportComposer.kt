package com.omiddd.dropletmanager.utils

data class BugReportDraft(
    val subject: String,
    val body: String
)

object BugReportComposer {

    fun compose(
        appName: String,
        screenName: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        manufacturer: String,
        model: String,
        sdkInt: Int
    ): BugReportDraft {
        val subject = "$appName bug report"
        val body = """
            What happened?

            Steps to reproduce:
            1.
            2.
            3.

            Expected result:

            Actual result:

            Environment:
            - Screen: $screenName
            - App version: $versionName ($versionCode)
            - Package: $packageName
            - Device: $manufacturer $model
            - Android SDK: $sdkInt
        """.trimIndent()

        return BugReportDraft(subject = subject, body = body)
    }
}
