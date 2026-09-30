package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val itemType: ItemType,
    val content: String,
    val context: String,
    val tag: String? = null,
    val createdTime: Long = System.currentTimeMillis(),
    val updatedTime: Long = System.currentTimeMillis()
)
