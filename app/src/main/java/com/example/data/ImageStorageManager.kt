package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ImageStorageManager(private val context: Context) {

    /**
     * Storage location: /files/images/
     */
    val imagesDir: File
        get() {
            val dir = File(context.filesDir, "images")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Store image directly without re-compression or alteration
     */
    suspend fun saveImageUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // Determine extension or default to .jpg
            val mimeType = context.contentResolver.getType(uri)
            val extension = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                else -> "jpg"
            }
            val fileName = "${UUID.randomUUID()}.$extension"
            val destFile = File(imagesDir, fileName)

            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Save bitmap (used for Snip Tool)
     */
    suspend fun saveBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val fileName = "${UUID.randomUUID()}.png"
        val destFile = File(imagesDir, fileName)
        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        destFile.absolutePath
    }

    /**
     * Delete image file on disk
     */
    suspend fun deleteImageFile(path: String?) = withContext(Dispatchers.IO) {
        if (!path.isNullOrBlank()) {
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {
                // Ignore failure
            }
        }
    }

    /**
     * Startup Cleanup:
     * Scan image directory
     * Compare against Room records
     * Delete orphaned files
     */
    suspend fun cleanupOrphanedImages(activeImagePaths: Set<String>) = withContext(Dispatchers.IO) {
        try {
            val files = imagesDir.listFiles() ?: return@withContext
            for (file in files) {
                if (file.isFile && !activeImagePaths.contains(file.absolutePath)) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Ignore failure
        }
    }
}
