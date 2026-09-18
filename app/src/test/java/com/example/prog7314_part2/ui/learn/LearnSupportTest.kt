package com.example.prog7314_part2.ui.learn

import com.example.prog7314_part2.data.remote.LearnGuideDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnSupportTest {

    private fun guide(
        id: String,
        sportId: String = "tennis",
        category: String = "RULES",
        mediaUrl: String? = null
    ) = LearnGuideDto(
        id = id,
        sportId = sportId,
        category = category,
        title = "$sportId $category",
        body = "Body for $id",
        mediaUrl = mediaUrl
    )

    // --- categoryLabel ---

    @Test
    fun categoryLabel_capitalisesOnlyFirstLetter() {
        assertEquals("Rules", LearnSupport.categoryLabel("RULES"))
        assertEquals("Safety", LearnSupport.categoryLabel("SAFETY"))
    }

    // --- bodyPreview ---

    @Test
    fun bodyPreview_returnsShortBodyUnchanged() {
        assertEquals("Short body.", LearnSupport.bodyPreview("Short body."))
    }

    @Test
    fun bodyPreview_truncatesLongBodyWithEllipsis() {
        val longBody = "a".repeat(200)
        val preview = LearnSupport.bodyPreview(longBody, maxLength = 90)
        assertEquals(91, preview.length) // 90 chars + ellipsis
        assertTrue(preview.endsWith("…"))
    }

    // --- filter (sport + category) ---

    @Test
    fun filter_keepsOnlyMatchingSport() {
        val guides = listOf(
            guide("t1", sportId = "tennis"),
            guide("f1", sportId = "football")
        )
        val result = LearnSupport.filter(guides, sportId = "tennis", category = null)
        assertEquals(listOf("t1"), result.map { it.id })
    }

    @Test
    fun filter_appliesCategoryWhenGiven() {
        val guides = listOf(
            guide("t-rules", category = "RULES"),
            guide("t-safety", category = "SAFETY")
        )
        val result = LearnSupport.filter(guides, sportId = "tennis", category = "SAFETY")
        assertEquals(listOf("t-safety"), result.map { it.id })
    }

    @Test
    fun filter_returnsEverySportMatchWhenCategoryIsNull() {
        val guides = listOf(
            guide("t-rules", category = "RULES"),
            guide("t-safety", category = "SAFETY")
        )
        val result = LearnSupport.filter(guides, sportId = "tennis", category = null)
        assertEquals(2, result.size)
    }

    @Test
    fun filter_excludesOtherSportsEvenWhenNoOtherSportGuidesShareTheSport() {
        // A tennis user must never see rugby's guides, whatever the category filter.
        val guides = listOf(guide("r-safety", sportId = "rugby", category = "SAFETY"))
        val result = LearnSupport.filter(guides, sportId = "tennis", category = "SAFETY")
        assertTrue(result.isEmpty())
    }

    // --- shouldShowEmpty ---

    @Test
    fun shouldShowEmpty_trueOnlyWhenIdleWithNoGuidesAndNoError() {
        assertTrue(LearnSupport.shouldShowEmpty(emptyList(), isLoading = false, hasError = false))
    }

    @Test
    fun shouldShowEmpty_falseWhileLoading() {
        assertFalse(LearnSupport.shouldShowEmpty(emptyList(), isLoading = true, hasError = false))
    }

    @Test
    fun shouldShowEmpty_falseOnError() {
        assertFalse(LearnSupport.shouldShowEmpty(emptyList(), isLoading = false, hasError = true))
    }

    @Test
    fun shouldShowEmpty_falseWhenGuidesArePresent() {
        assertFalse(LearnSupport.shouldShowEmpty(listOf(guide("t1")), isLoading = false, hasError = false))
    }

    // --- isVideoUrl / isImageUrl ---

    @Test
    fun isVideoUrl_trueForVideoFileExtensions() {
        assertTrue(LearnSupport.isVideoUrl("https://cdn.example.com/clip.mp4"))
        assertTrue(LearnSupport.isVideoUrl("https://cdn.example.com/clip.mp4?token=abc"))
    }

    @Test
    fun isVideoUrl_trueForKnownVideoHosts() {
        assertTrue(LearnSupport.isVideoUrl("https://www.youtube.com/watch?v=abc123"))
        assertTrue(LearnSupport.isVideoUrl("https://youtu.be/abc123"))
    }

    @Test
    fun isVideoUrl_falseForImagesOrBlank() {
        assertFalse(LearnSupport.isVideoUrl("https://cdn.example.com/photo.jpg"))
        assertFalse(LearnSupport.isVideoUrl(null))
        assertFalse(LearnSupport.isVideoUrl(""))
    }

    @Test
    fun isImageUrl_trueForNonVideoMediaUrl() {
        assertTrue(LearnSupport.isImageUrl("https://cdn.example.com/photo.jpg"))
    }

    @Test
    fun isImageUrl_falseForVideoOrBlank() {
        assertFalse(LearnSupport.isImageUrl("https://cdn.example.com/clip.mp4"))
        assertFalse(LearnSupport.isImageUrl(null))
    }
}
