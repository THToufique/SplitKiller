package com.ripp3r.splitkiller.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.ripp3r.splitkiller.model.AppInfo
import com.ripp3r.splitkiller.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AppExtractor(private val context: Context) {
    
    private val packageManager = context.packageManager
    
    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        AppLogger.log("Loading installed apps (includeSystemApps: $includeSystemApps)")
        val apps = mutableListOf<AppInfo>()
        
        try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }
            
            AppLogger.log("Found ${packages.size} total packages")
            
            for (packageInfo in packages) {
                val applicationInfo = packageInfo.applicationInfo ?: continue
                
                val isSystem = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                
                // Filter system apps if needed
                if (!includeSystemApps && isSystem) {
                    continue
                }
                
                val appName = applicationInfo.loadLabel(packageManager).toString()
                val icon = try {
                    applicationInfo.loadIcon(packageManager)
                } catch (e: Exception) {
                    null
                }
                
                // Check if it's a split APK
                val isSplit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val splits = applicationInfo.splitSourceDirs
                    splits != null && splits.isNotEmpty()
                } else {
                    false
                }
                
                // Get app size (approximate)
                val appSize = try {
                    var totalSize = File(applicationInfo.sourceDir).length()
                    if (isSplit && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        applicationInfo.splitSourceDirs?.forEach { splitPath ->
                            totalSize += File(splitPath).length()
                        }
                    }
                    totalSize
                } catch (e: Exception) {
                    0L
                }
                
                val appInfo = AppInfo(
                    packageName = packageInfo.packageName,
                    appName = appName,
                    versionName = packageInfo.versionName ?: "Unknown",
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode
                    },
                    installDate = packageInfo.firstInstallTime,
                    appSize = appSize,
                    isSystemApp = isSystem,
                    isSplitApk = isSplit,
                    icon = icon
                )
                
                apps.add(appInfo)
            }
            
            // Sort by app name
            apps.sortBy { it.appName.lowercase() }
            AppLogger.log("Loaded ${apps.size} apps")
        } catch (e: Exception) {
            AppLogger.log("Error loading apps: ${e.message}", com.ripp3r.splitkiller.util.LogLevel.ERROR)
        }
        
        apps
    }
    
    suspend fun extractApp(appInfo: AppInfo, outputDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("Extracting ${appInfo.appName}...")
            
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val applicationInfo = packageManager.getApplicationInfo(appInfo.packageName, 0)
            val sourceApk = File(applicationInfo.sourceDir)
            
            if (appInfo.isSplitApk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Handle split APKs - create XAPK
                AppLogger.log("Creating XAPK for split app...")
                val outputFile = File(outputDir, "${appInfo.appName}_${appInfo.versionName}.xapk")
                
                ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                    // Add base APK
                    addFileToZip(zipOut, sourceApk, "base.apk")
                    
                    // Add split APKs
                    applicationInfo.splitSourceDirs?.forEachIndexed { index, splitPath ->
                        val splitFile = File(splitPath)
                        val splitName = splitFile.name
                        addFileToZip(zipOut, splitFile, splitName)
                    }
                }
                
                AppLogger.log("Successfully extracted to ${outputFile.name}")
                Result.success(outputFile)
            } else {
                // Regular APK - just copy
                val outputFile = File(outputDir, "${appInfo.appName}_${appInfo.versionName}.apk")
                sourceApk.copyTo(outputFile, overwrite = true)
                AppLogger.log("Successfully extracted to ${outputFile.name}")
                Result.success(outputFile)
            }
        } catch (e: Exception) {
            AppLogger.log("Error extracting app: ${e.message}", com.ripp3r.splitkiller.util.LogLevel.ERROR)
            Result.failure(e)
        }
    }
    
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { fis ->
            zipOut.putNextEntry(ZipEntry(entryName))
            fis.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }
}
