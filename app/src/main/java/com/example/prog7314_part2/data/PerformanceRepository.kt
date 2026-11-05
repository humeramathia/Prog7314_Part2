package com.example.prog7314_part2.data

import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.CreatePerformanceRequest
import com.example.prog7314_part2.data.remote.MonthlyPerformanceDto
import com.example.prog7314_part2.data.remote.PerformanceSessionDto
import com.example.prog7314_part2.data.remote.SportSphereApi
import com.example.prog7314_part2.data.remote.toLocal
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Persistence seam the Performance screens call.
 *
 * Today this talks to the hosted API and keeps unsynced rows in memory
 * so a save in airplane mode still shows in History as pending.
 *
 * MO's Room / WorkManager layer should replace [pending] and keep these
 * method names (`save`, `history`, `delete`, `monthly`, `latest`).
 */
object PerformanceRepository {

    private val api: SportSphereApi get() = ApiClient.service
    private val pending = CopyOnWriteArrayList<PerformanceSession>()

    fun save(
        sportId: String,
        recordedAt: Long,
        notes: String,
        metrics: Map<String, Double>,
        onSuccess: (PerformanceSession) -> Unit,
        onError: (Boolean) -> Unit
    ): Call<PerformanceSessionDto> {
        val request = CreatePerformanceRequest(
            sportId = sportId,
            recordedAt = recordedAt,
            notes = notes,
            metrics = metrics
        )
        val call = api.createPerformance(request)
        call.enqueue(object : Callback<PerformanceSessionDto> {
            override fun onResponse(
                call: Call<PerformanceSessionDto>,
                response: Response<PerformanceSessionDto>
            ) {
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    onError(false)
                    return
                }
                onSuccess(body.toLocal())
            }

            override fun onFailure(call: Call<PerformanceSessionDto>, error: Throwable) {
                if (call.isCanceled) return
                val local = PerformanceSession(
                    id = "pending-${UUID.randomUUID()}",
                    sportId = sportId,
                    recordedAt = recordedAt,
                    metrics = metrics,
                    notes = notes,
                    pending = true
                )
                pending.add(0, local)
                onSuccess(local)
            }
        })
        return call
    }

    fun history(
        sportId: String,
        onResult: (sessions: List<PerformanceSession>, remoteFailed: Boolean) -> Unit
    ): Call<List<PerformanceSessionDto>>? {
        if (sportId.isBlank()) {
            onResult(emptyList(), false)
            return null
        }
        flushPending(sportId)
        val call = api.getPerformance(sportId)
        call.enqueue(object : Callback<List<PerformanceSessionDto>> {
            override fun onResponse(
                call: Call<List<PerformanceSessionDto>>,
                response: Response<List<PerformanceSessionDto>>
            ) {
                if (!response.isSuccessful) {
                    onResult(pendingFor(sportId), true)
                    return
                }
                val remote = response.body().orEmpty()
                    .sortedByDescending { it.recordedAt }
                    .map { it.toLocal() }
                onResult(PerformanceHistory.merge(remote, pendingFor(sportId)), false)
            }

            override fun onFailure(
                call: Call<List<PerformanceSessionDto>>,
                error: Throwable
            ) {
                if (call.isCanceled) return
                onResult(pendingFor(sportId), true)
            }
        })
        return call
    }

    fun delete(
        session: PerformanceSession,
        onSuccess: () -> Unit,
        onError: (Boolean) -> Unit
    ): Call<Void>? {
        if (session.pending) {
            pending.removeAll { it.id == session.id }
            onSuccess()
            return null
        }
        val call = api.deletePerformance(session.id)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) onSuccess() else onError(false)
            }

            override fun onFailure(call: Call<Void>, error: Throwable) {
                if (call.isCanceled) return
                onError(true)
            }
        })
        return call
    }

    fun monthly(
        sportId: String,
        year: Int,
        month: Int,
        metric: String?,
        onSuccess: (MonthlyPerformanceDto) -> Unit,
        onError: (Boolean) -> Unit
    ): Call<MonthlyPerformanceDto> {
        val call = api.getMonthlyPerformance(sportId, year, month, metric)
        call.enqueue(object : Callback<MonthlyPerformanceDto> {
            override fun onResponse(
                call: Call<MonthlyPerformanceDto>,
                response: Response<MonthlyPerformanceDto>
            ) {
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    onError(false)
                    return
                }
                onSuccess(body)
            }

            override fun onFailure(call: Call<MonthlyPerformanceDto>, error: Throwable) {
                if (call.isCanceled) return
                onError(true)
            }
        })
        return call
    }

    fun latest(
        sportId: String,
        onResult: (PerformanceSession?) -> Unit
    ): Call<List<PerformanceSessionDto>>? {
        return history(sportId) { sessions, _ ->
            onResult(sessions.firstOrNull())
        }
    }

    private fun pendingFor(sportId: String): List<PerformanceSession> =
        pending.filter { it.sportId == sportId }

    /**
     * Best-effort POST of in-memory pending rows. MO should swap this
     * for WorkManager once Room is in place.
     */
    private fun flushPending(sportId: String) {
        pendingFor(sportId).forEach { row ->
            val request = CreatePerformanceRequest(
                sportId = row.sportId,
                recordedAt = row.recordedAt,
                notes = row.notes,
                metrics = row.metrics
            )
            api.createPerformance(request).enqueue(object : Callback<PerformanceSessionDto> {
                override fun onResponse(
                    call: Call<PerformanceSessionDto>,
                    response: Response<PerformanceSessionDto>
                ) {
                    if (response.isSuccessful) {
                        pending.removeAll { it.id == row.id }
                    }
                }

                override fun onFailure(call: Call<PerformanceSessionDto>, error: Throwable) = Unit
            })
        }
    }
}
