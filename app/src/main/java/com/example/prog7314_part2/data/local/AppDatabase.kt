package com.example.prog7314_part2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class, SyncMetadata::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao
    abstract fun syncMetadata(): SyncMetadataDao
}
