package com.ripp3r.splitkiller.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val icon: Drawable?,
    val apkPath: String,
    val isSystemApp: Boolean
)
