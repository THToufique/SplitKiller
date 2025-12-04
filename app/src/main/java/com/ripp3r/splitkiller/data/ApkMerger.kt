package com.ripp3r.splitkiller.data

import android.content.Context
import android.net.Uri
import com.ripp3r.splitkiller.model.SignatureScheme
import com.ripp3r.splitkiller.model.SigningKey
import com.ripp3r.splitkiller.util.AppLogger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import android.os.Environment
import com.reandroid.apk.ApkModule
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.app.AndroidManifest

class ApkMerger(private val context: Context) {
    
    suspend fun mergeApk(
        uri: Uri,
        context: Context,
        signatureScheme: SignatureScheme,
        signingKey: SigningKey?,
        selectedFileName: String? = null
    ): Result<File> {
        return try {
            AppLogger.log("Step 1: Extracting XAPK/ZIP...", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            val tempDir = File(context.cacheDir, "merge_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            
            val extractedFiles = extractZip(uri, tempDir, context)
            AppLogger.log("Extracted ${extractedFiles.size} files", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            
            val apkFiles = extractedFiles.filter { it.extension == "apk" }
            if (apkFiles.isEmpty()) {
                throw Exception("No APK files found in archive")
            }
            
            AppLogger.log("Found ${apkFiles.size} APK files", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            
            val baseApk = apkFiles.find { it.name.contains("base", ignoreCase = true) }
                ?: apkFiles.maxByOrNull { it.length() }
                ?: throw Exception("Could not identify base APK")
            
            val splitApks = apkFiles.filter { it != baseApk }
            
            AppLogger.log("Base APK: ${baseApk.name} (${baseApk.length() / 1024}KB)", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            splitApks.forEach {
                AppLogger.log("Split APK: ${it.name} (${it.length() / 1024}KB)", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            }
            
            AppLogger.log("Step 2: Merging APKs with ApkBundle...", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            var usedManualMerge = false
            val mergedApk = try {
                mergeApkFilesWithBundle(baseApk, splitApks, tempDir)
            } catch (e: Exception) {
                AppLogger.log("ApkBundle merge failed: ${e.message}", com.ripp3r.splitkiller.util.LogLevel.ERROR, com.ripp3r.splitkiller.util.LogCategory.MERGE)
                AppLogger.log("Falling back to manual merge (icons may be missing)", com.ripp3r.splitkiller.util.LogLevel.WARNING, com.ripp3r.splitkiller.util.LogCategory.MERGE)
                usedManualMerge = true
                val cleanedBase = cleanManifest(baseApk, tempDir)
                mergeApkFilesManual(cleanedBase, splitApks, tempDir)
            }
            AppLogger.log("Merge complete: ${mergedApk.length() / 1024}KB", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            
            // Only clean manifest if we used ApkBundle (manual merge already cleaned)
            val cleanedApk = if (usedManualMerge) {
                mergedApk
            } else {
                AppLogger.log("Step 3: Cleaning merged manifest...", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
                cleanManifest(mergedApk, tempDir)
            }
            
            val keystoreManager = KeystoreManager(context)
            val finalApk = if (signatureScheme != SignatureScheme.UNSIGNED && signingKey != null) {
                AppLogger.log("Step 4: Signing with custom key (${signatureScheme.name})...", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
                keystoreManager.signApk(cleanedApk, signingKey, signatureScheme)
            } else {
                AppLogger.log("Step 4: Signing with default key (v1+v2+v3)...", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
                val allKeys = keystoreManager.getAllKeys()
                val defaultKey = if (allKeys.isEmpty()) {
                    keystoreManager.createKey(
                        "debug",
                        "android",
                        25,
                        "Android Debug",
                        "Android",
                        "Android",
                        "Mountain View",
                        "California",
                        "US"
                    ).getOrThrow()
                } else {
                    allKeys.first()
                }
                keystoreManager.signApk(cleanedApk, defaultKey, SignatureScheme.V1_V2_V3)
            }
            
            val outputDir = File(uri.path).parentFile ?: File(Environment.getExternalStorageDirectory(), "SplitKiller")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val appName = if (selectedFileName != null) {
                selectedFileName
                    .substringBeforeLast('.')
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    .replace(Regex("_+"), "_")
                    .trim('_')
            } else {
                "merged"
            }
            
            AppLogger.log("Output filename: ${appName}.apk", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            
            val outputFile = File(outputDir, "${appName}.apk")
            finalApk.copyTo(outputFile, overwrite = true)
            
            tempDir.deleteRecursively()
            
            AppLogger.log("Step 5: Saved to ${outputFile.absolutePath}", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
            
            Result.success(outputFile)
        } catch (e: Exception) {
            AppLogger.log("Merge error: ${e.message}", com.ripp3r.splitkiller.util.LogLevel.ERROR, com.ripp3r.splitkiller.util.LogCategory.MERGE)
            Result.failure(e)
        }
    }
    
    private fun extractZip(uri: Uri, outputDir: File, context: Context): List<File> {
        val extractedFiles = mutableListOf<File>()
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipStream ->
                var entry: ZipEntry? = zipStream.nextEntry
                
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val file = File(outputDir, entry.name)
                        file.parentFile?.mkdirs()
                        
                        FileOutputStream(file).use { output ->
                            zipStream.copyTo(output)
                        }
                        
                        extractedFiles.add(file)
                    }
                    
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        }
        
        return extractedFiles
    }
    
    private fun cleanManifest(baseApk: File, workDir: File): File {
        return try {
            val cleanedApk = File(workDir, "cleaned_base.apk")
            
            val apkModule = ApkModule.loadApkFile(baseApk)
            
            if (apkModule.hasAndroidManifest()) {
                val manifest = apkModule.androidManifest
                
                AppLogger.log("Removing split attributes from manifest...")
                
                // Remove split-specific attributes
                com.reandroid.apkeditor.common.AndroidManifestHelper.removeAttributeFromManifestById(
                    manifest, AndroidManifest.ID_requiredSplitTypes, null)
                com.reandroid.apkeditor.common.AndroidManifestHelper.removeAttributeFromManifestById(
                    manifest, AndroidManifest.ID_splitTypes, null)
                com.reandroid.apkeditor.common.AndroidManifestHelper.removeAttributeFromManifestById(
                    manifest, AndroidManifest.ID_isSplitRequired, null)
                
                com.reandroid.apkeditor.common.AndroidManifestHelper.removeAttributeFromManifestByName(
                    manifest, AndroidManifest.NAME_requiredSplitTypes, null)
                com.reandroid.apkeditor.common.AndroidManifestHelper.removeAttributeFromManifestByName(
                    manifest, AndroidManifest.NAME_splitTypes, null)
                com.reandroid.apkeditor.common.AndroidManifestHelper.removeAttributeFromManifestByName(
                    manifest, AndroidManifest.NAME_isSplitRequired, null)
                
                // Remove extractNativeLibs from both manifest and application
                com.reandroid.apkeditor.common.AndroidManifestHelper.removeAttributeFromManifestAndApplication(
                    manifest, AndroidManifest.ID_extractNativeLibs, null, AndroidManifest.NAME_extractNativeLibs)
                
                // Remove meta-data elements
                val application = manifest.applicationElement
                if (application != null) {
                    val splitMetaData = com.reandroid.apkeditor.common.AndroidManifestHelper.listSplitRequired(application)
                    for (meta in splitMetaData) {
                        AppLogger.log("Removing meta-data: ${meta.name}")
                        application.remove(meta)
                    }
                }
                
                manifest.refresh()
                
                apkModule.writeApk(cleanedApk)
                apkModule.close()
                
                AppLogger.log("Manifest cleaned successfully")
                return cleanedApk
            }
            
            AppLogger.log("No manifest found, using original", com.ripp3r.splitkiller.util.LogLevel.WARNING)
            return baseApk
            
        } catch (e: Exception) {
            AppLogger.log("Manifest cleaning failed: ${e.message}", com.ripp3r.splitkiller.util.LogLevel.WARNING)
            e.printStackTrace()
            return baseApk
        }
    }
    
    private fun removeAttribute(manifest: AndroidManifestBlock, resourceId: Int) {
        try {
            val manifestElement = manifest.manifestElement
            manifestElement?.searchAttributeByResourceId(resourceId)?.let { attr ->
                manifestElement.removeAttribute(attr)
            }
            
            // Also check application element
            manifest.applicationElement?.searchAttributeByResourceId(resourceId)?.let { attr ->
                manifest.applicationElement.removeAttribute(attr)
            }
        } catch (e: Exception) {
            // Ignore if attribute doesn't exist
        }
    }
    
    
    private fun mergeApkFilesWithBundle(baseApk: File, splitApks: List<File>, workDir: File): File {
        val mergedApk = File(workDir, "merged_bundle.apk")
        
        // Create a directory with all APKs (ApkBundle needs directory structure)
        val apksDir = File(workDir, "apks")
        apksDir.mkdirs()
        
        AppLogger.log("Copying APKs to bundle directory...")
        baseApk.copyTo(File(apksDir, "base.apk"), overwrite = true)
        splitApks.forEachIndexed { index, splitApk ->
            splitApk.copyTo(File(apksDir, "split_${index}.apk"), overwrite = true)
        }
        
        AppLogger.log("Loading APK directory into bundle...")
        val bundle = com.reandroid.apk.ApkBundle()
        bundle.loadApkDirectory(apksDir)
        
        AppLogger.log("Merging modules with resource table merge...")
        val mergedModule = bundle.mergeModules(false)
        
        AppLogger.log("Writing merged APK...")
        mergedModule.writeApk(mergedApk)
        mergedModule.close()
        
        // Cleanup
        apksDir.deleteRecursively()
        
        AppLogger.log("ApkBundle merge successful!")
        return mergedApk
    }
    
    private fun mergeApkFilesManual(cleanedBaseApk: File, splitApks: List<File>, workDir: File): File {
        val mergedApk = File(workDir, "merged.apk")
        val mergedEntries = mutableSetOf<String>()
        
        // Simple manual merge
        ZipOutputStream(FileOutputStream(mergedApk)).use { zipOut ->
            AppLogger.log("Copying base APK...")
            ZipInputStream(FileInputStream(cleanedBaseApk)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                
                while (entry != null) {
                    val entryName = entry.name
                    val isSignatureFile = entryName.matches(Regex("META-INF/.*\\.(SF|RSA|DSA|EC|MF)$", RegexOption.IGNORE_CASE))
                    
                    if (!isSignatureFile) {
                        zipOut.putNextEntry(ZipEntry(entryName))
                        zipIn.copyTo(zipOut)
                        zipOut.closeEntry()
                        mergedEntries.add(entryName)
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            // Merge splits
            splitApks.forEachIndexed { index, splitApk ->
                AppLogger.log("Merging split ${index + 1}/${splitApks.size}...")
                ZipInputStream(FileInputStream(splitApk)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    
                    while (entry != null) {
                        val entryName = entry.name
                        val isSignatureFile = entryName.matches(Regex("META-INF/.*\\.(SF|RSA|DSA|EC|MF)$", RegexOption.IGNORE_CASE))
                        
                        if (!mergedEntries.contains(entryName) && !isSignatureFile && entryName != "AndroidManifest.xml") {
                            zipOut.putNextEntry(ZipEntry(entryName))
                            zipIn.copyTo(zipOut)
                            zipOut.closeEntry()
                            mergedEntries.add(entryName)
                        }
                        
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
        }
        
        return mergedApk
    }
}
