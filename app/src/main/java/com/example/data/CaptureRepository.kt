package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.model.CaptureEntity
import com.example.data.model.ItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class CaptureRepository(
    private val dao: CaptureDao,
    private val imageStorage: ImageStorageManager
) {
    val allCaptures: Flow<List<CaptureEntity>> = dao.getAllCaptures()
    val allTags: Flow<List<String>> = dao.getAllTags()

    suspend fun saveCapture(
        itemType: ItemType,
        content: String,
        contextText: String,
        tag: String? = null
    ): CaptureEntity = withContext(Dispatchers.IO) {
        val cleanTag = tag?.trim()?.removePrefix("#")?.ifEmpty { null }
        val now = System.currentTimeMillis()
        val entity = CaptureEntity(
            id = UUID.randomUUID().toString(),
            itemType = itemType,
            content = content.trim(),
            context = contextText.trim(),
            tag = cleanTag,
            createdTime = now,
            updatedTime = now
        )
        dao.insert(entity)
        entity
    }

    suspend fun saveBitmapCapture(
        bitmap: Bitmap,
        contextText: String,
        tag: String? = null
    ): CaptureEntity = withContext(Dispatchers.IO) {
        val imagePath = imageStorage.saveBitmap(bitmap)
        val cleanTag = tag?.trim()?.removePrefix("#")?.ifEmpty { null }
        val now = System.currentTimeMillis()
        val entity = CaptureEntity(
            id = UUID.randomUUID().toString(),
            itemType = ItemType.IMAGE,
            content = imagePath,
            context = contextText.trim(),
            tag = cleanTag,
            createdTime = now,
            updatedTime = now
        )
        dao.insert(entity)
        entity
    }

    suspend fun saveMultipleImageCaptures(
        uris: List<Uri>,
        contextText: String,
        tag: String? = null
    ): List<CaptureEntity> = withContext(Dispatchers.IO) {
        val cleanTag = tag?.trim()?.removePrefix("#")?.ifEmpty { null }
        val now = System.currentTimeMillis()
        val entities = mutableListOf<CaptureEntity>()
        for (uri in uris) {
            val imagePath = imageStorage.saveImageUri(uri)
            if (imagePath != null) {
                entities.add(
                    CaptureEntity(
                        id = UUID.randomUUID().toString(),
                        itemType = ItemType.IMAGE,
                        content = imagePath,
                        context = contextText.trim(),
                        tag = cleanTag,
                        createdTime = now,
                        updatedTime = now
                    )
                )
            }
        }
        if (entities.isNotEmpty()) {
            dao.insertAll(entities)
        }
        entities
    }

    suspend fun updateCapture(
        capture: CaptureEntity,
        newContext: String,
        newTag: String?
    ) = withContext(Dispatchers.IO) {
        val cleanTag = newTag?.trim()?.removePrefix("#")?.ifEmpty { null }
        val updated = capture.copy(
            context = newContext.trim(),
            tag = cleanTag,
            updatedTime = System.currentTimeMillis()
        )
        dao.update(updated)
    }

    suspend fun deleteCapture(capture: CaptureEntity) = withContext(Dispatchers.IO) {
        if (capture.itemType == ItemType.IMAGE) {
            imageStorage.deleteImageFile(capture.content)
        }
        dao.delete(capture)
    }

    suspend fun deleteCaptures(captures: List<CaptureEntity>) = withContext(Dispatchers.IO) {
        for (capture in captures) {
            if (capture.itemType == ItemType.IMAGE) {
                imageStorage.deleteImageFile(capture.content)
            }
        }
        dao.deleteByIds(captures.map { it.id })
    }

    /**
     * Startup Cleanup:
     * Scan image directory, compare against Room records, delete orphaned files
     */
    suspend fun performStartupCleanup() = withContext(Dispatchers.IO) {
        val pathsInDb = dao.getAllImagePaths().toSet()
        imageStorage.cleanupOrphanedImages(pathsInDb)
    }

    companion object {
        @Volatile
        private var INSTANCE: CaptureRepository? = null

        fun getInstance(context: Context): CaptureRepository {
            return INSTANCE ?: synchronized(this) {
                val db = CaptureDatabase.getInstance(context)
                val imageStorage = ImageStorageManager(context)
                val instance = CaptureRepository(db.captureDao(), imageStorage)
                INSTANCE = instance
                instance
            }
        }
    }
}
