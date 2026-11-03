package com.example.prog7314_part2.data.local

import androidx.room.Entity
import androidx.room.Index
import com.example.prog7314_part2.data.PerformanceSession
import com.example.prog7314_part2.data.remote.CreatePerformanceRequest
import com.example.prog7314_part2.data.remote.PerformanceSessionDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(
    tableName = "performance_sessions",
    primaryKeys = ["userId", "localId"],
    indices = [Index(value = ["userId", "serverId"], unique = true)]
)
data class SessionEntity(
    val userId: String,
    val localId: String,
    val serverId: String? = null,
    val sportId: String,
    val recordedAt: Long,
    val notes: String,
    val metricsJson: String,
    val updatedAt: Long,
    val syncState: String = SyncPolicy.PENDING
) {
    fun metrics(): Map<String, Double> = Gson().fromJson(
        metricsJson, object : TypeToken<Map<String, Double>>() {}.type
    )

    fun toRequest() = CreatePerformanceRequest(sportId, recordedAt, notes, metrics(), localId)

    fun toLocal() = PerformanceSession(
        localId, sportId, recordedAt, metrics(), notes, syncState
    )

    companion object {
        fun fromRemote(dto: PerformanceSessionDto, localId: String = dto.clientId ?: dto.id) = SessionEntity(
            userId = dto.userId, localId = localId, serverId = dto.id,
            sportId = dto.sportId, recordedAt = dto.recordedAt, notes = dto.notes,
            metricsJson = Gson().toJson(dto.metrics), updatedAt = dto.recordedAt,
            syncState = SyncPolicy.SYNCED
        )
    }
}
