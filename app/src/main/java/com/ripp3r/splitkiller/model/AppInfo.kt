package com.ripp3r.splitkiller.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Int,
    val installDate: Long,
    val appSize: Long,
    val isSystemApp: Boolean,
    val isSplitApk: Boolean,
    val icon: Drawable?
)
