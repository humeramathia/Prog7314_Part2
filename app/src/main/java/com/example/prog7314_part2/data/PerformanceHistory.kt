package com.example.prog7314_part2.data

/**
 * Pure merge rules for the Performance history list.
 *
 * Fragments never talk to Room themselves. MO's offline layer can keep
 * feeding [pending] rows into [merge]; once a row exists on the server
 * it drops out of pending and the graph is allowed to include it.
 */
object PerformanceHistory {

    fun merge(
        remote: List<PerformanceSession>,
        pending: List<PerformanceSession>
    ): List<PerformanceSession> {
        val remoteIds = remote.map { it.id }.toSet()
        val stillPending = pending.filter { it.sportId.isNotBlank() && it.id !in remoteIds }
        return (remote + stillPending).sortedByDescending { it.recordedAt }
    }

    fun latest(
        remote: List<PerformanceSession>,
        pending: List<PerformanceSession>
    ): PerformanceSession? = merge(remote, pending).firstOrNull()

    /** Pending rows stay off the monthly graph until they sync. */
    fun graphSessions(remote: List<PerformanceSession>): List<PerformanceSession> =
        remote.filter { !it.pending }
}
