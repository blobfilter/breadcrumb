package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.CaptureEntity
import com.example.data.model.ItemType

class ItemTypeConverter {
    @TypeConverter
    fun fromItemType(type: ItemType?): String = (type ?: ItemType.TEXT).name

    @TypeConverter
    fun toItemType(value: String?): ItemType = try {
        if (value != null) ItemType.valueOf(value) else ItemType.TEXT
    } catch (_: Exception) {
        ItemType.TEXT
    }
}

@Database(entities = [CaptureEntity::class], version = 1, exportSchema = false)
@TypeConverters(ItemTypeConverter::class)
abstract class CaptureDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao

    companion object {
        @Volatile
        private var INSTANCE: CaptureDatabase? = null

        fun getInstance(context: Context): CaptureDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CaptureDatabase::class.java,
                    "breadcrumb_captures.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
