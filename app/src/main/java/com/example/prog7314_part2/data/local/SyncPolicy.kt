package com.example.prog7314_part2.data.local

/** Auth and temporary server failures retain the queue; rejected input needs user action. */
object SyncPolicy {
    const val PENDING = "pending"
    const val SYNCED = "synced"
    const val FAILED = "failed"

    fun isPermanentFailure(code: Int): Boolean =
        code in 400..499 && code !in setOf(401, 403, 408, 425, 429)
}
