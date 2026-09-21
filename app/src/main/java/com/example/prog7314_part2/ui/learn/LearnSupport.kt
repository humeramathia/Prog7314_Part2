package com.example.prog7314_part2.ui.learn

import com.example.prog7314_part2.data.remote.LearnGuideDto

/**
 * Small pure-Kotlin helpers pulled out of LearnFragment/LearnDetailFragment
 * so they can be unit tested without an Android runtime.
 */
object LearnSupport {

    val CATEGORIES = listOf("RULES", "TECHNIQUES", "TRAINING", "SAFETY")

    /** Chip label shown for a raw category from the API, e.g. "RULES" -> "Rules". */
    fun categoryLabel(category: String): String =
        category.lowercase().replaceFirstChar { it.uppercase() }

    /** One-line preview of a guide's body for the list row. */
    fun bodyPreview(body: String, maxLength: Int = 90): String {
        val trimmed = body.trim()
        if (trimmed.length <= maxLength) return trimmed
        return trimmed.take(maxLength).trimEnd() + "…"
    }

    /**
     * Client-side safety net: guides are already filtered by sport and
     * category via API query params, but if a stale list is re-rendered
     * (e.g. after a chip flips before the response for the previous
     * selection lands) this keeps the row list honest.
     */
    fun filter(guides: List<LearnGuideDto>, sportId: String, category: String?): List<LearnGuideDto> =
        guides.filter { it.sportId == sportId && (category == null || it.category == category) }

    /** Whether the empty-state message should be shown. */
    fun shouldShowEmpty(guides: List<LearnGuideDto>, isLoading: Boolean, hasError: Boolean): Boolean =
        !isLoading && !hasError && guides.isEmpty()

    private val VIDEO_EXTENSIONS = listOf(".mp4", ".mov", ".m4v", ".webm", ".3gp")
    private val VIDEO_HOSTS = listOf("youtube.com", "youtu.be", "vimeo.com")

    /** True when a mediaUrl looks like a playable video rather than a still image. */
    fun isVideoUrl(mediaUrl: String?): Boolean {
        if (mediaUrl.isNullOrBlank()) return false
        val lower = mediaUrl.lowercase()
        if (VIDEO_HOSTS.any { lower.contains(it) }) return true
        return VIDEO_EXTENSIONS.any { lower.substringBefore('?').endsWith(it) }
    }

    /** True when a mediaUrl should be rendered as a still image. */
    fun isImageUrl(mediaUrl: String?): Boolean =
        !mediaUrl.isNullOrBlank() && !isVideoUrl(mediaUrl)
}
