package com.example.prog7314_part2.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sync_metadata")
data class SyncMetadata(@PrimaryKey val userId: String, val uploadedAt: Long)

@Dao
interface SyncMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(metadata: SyncMetadata)

    @Query("SELECT uploadedAt FROM sync_metadata WHERE userId = :uid")
    fun observe(uid: String): Flow<Long?>
}
