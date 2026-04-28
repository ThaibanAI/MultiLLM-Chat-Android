package com.thaibanai.multillmchat.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object TempFileUtil {
    /**
     * Save a bitmap to cache directory and return a content URI.
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "attachments")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val file = File(cacheDir, "IMG_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Return file URI directly (uses FileProvider in production)
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    fun cleanCache(context: Context) {
        val cacheDir = File(context.cacheDir, "attachments")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }
}
