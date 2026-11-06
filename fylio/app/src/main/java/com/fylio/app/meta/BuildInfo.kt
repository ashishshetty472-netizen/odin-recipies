package com.fylio.app.meta

import com.fylio.app.BuildConfig

/**
 * Build metadata shown in Settings/About. Pulled from the Gradle build so the About
 * screen can never disagree with the installed APK.
 */
object BuildInfo {
    val version: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_LABEL})"
    val packageName: String = BuildConfig.APPLICATION_ID
}
