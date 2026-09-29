package com.swill.vpn.core

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object AssetExtractor {

    private const val TAG = "AssetExtractor"

    fun extractAsset(context: Context, assetName: String, targetName: String): File? {
        return try {
            val cacheDir = context.cacheDir
            val targetFile = File(cacheDir, targetName)

            if (targetFile.exists() && targetFile.canExecute()) {
                return targetFile
            }

            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            targetFile.setExecutable(true, false)
            targetFile
        } catch (e: Exception) {
            null
        }
    }

    fun extractIfNeeded(context: Context, assetName: String, targetName: String): Boolean {
        return extractAsset(context, assetName, targetName) != null
    }

    fun getBinaryPath(context: Context, binaryName: String): String? {
        val file = File(context.cacheDir, binaryName)
        return if (file.exists() && file.canExecute()) file.absolutePath else null
    }
}
