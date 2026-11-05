package com.example.prog7314_part2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM performance_sessions WHERE userId = :uid AND sportId = :sportId ORDER BY recordedAt DESC")
    fun observe(uid: String, sportId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM performance_sessions WHERE userId = :uid AND syncState = 'pending' ORDER BY updatedAt")
    suspend fun pending(uid: String): List<SessionEntity>

    @Query("SELECT DISTINCT sportId FROM performance_sessions WHERE userId = :uid")
    suspend fun sports(uid: String): List<String>

    @Query("SELECT * FROM performance_sessions WHERE userId = :uid AND localId = :localId")
    suspend fun get(uid: String, localId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(session: SessionEntity)

    @Query("DELETE FROM performance_sessions WHERE userId = :uid AND sportId = :sportId AND syncState = 'synced'")
    suspend fun clearSynced(uid: String, sportId: String)

    @Query("DELETE FROM performance_sessions WHERE userId = :uid AND localId = :localId")
    suspend fun delete(uid: String, localId: String)

    @Query("UPDATE performance_sessions SET syncState = :state, serverId = :serverId WHERE userId = :uid AND localId = :localId")
    suspend fun setState(uid: String, localId: String, state: String, serverId: String?)
}
