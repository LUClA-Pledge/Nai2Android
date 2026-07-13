package cn.sta1n.nai2android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomainRulesTest {
    @Test
    fun normalizeArchiveTags_trims_deduplicates_and_ignores_empty_values() {
        val result = normalizeArchiveTags("  rain, night\nnight,  ,Rain  ")

        assertEquals(listOf("rain", "night"), result)
    }

    @Test
    fun image_matches_tag_uses_case_insensitive_exact_tag_matching() {
        val image = testImage(tags = listOf("Rain", "night scene"))

        assertTrue(image.matchesArchiveTag("rain"))
        assertFalse(image.matchesArchiveTag("night"))
    }

    @Test
    fun sort_images_newest_first_and_oldest_first() {
        val old = testImage(id = "old", createdAt = 10)
        val newer = testImage(id = "new", createdAt = 20)

        assertEquals(listOf("new", "old"), sortImages(listOf(old, newer), SortOrder.NEWEST_FIRST).map { it.id })
        assertEquals(listOf("old", "new"), sortImages(listOf(old, newer), SortOrder.OLDEST_FIRST).map { it.id })
    }

    @Test
    fun generation_cost_matches_site_size_rules() {
        assertEquals(1, generationCostForSize("竖图"))
        assertEquals(15, generationCostForSize("2K横图"))
        assertEquals(25, generationCostForSize("4K方图"))
        assertEquals(1, generationCostForSize("unknown"))
    }

    @Test
    fun generation_form_maps_to_site_job_contract() {
        val payload = GenerationForm(
            prompt = "  1girl, rain  ",
            archiveTags = "rain",
            artist = " artist ",
            negativePrompt = " bad hands ",
            size = "2K竖图",
            steps = 99,
            scale = 30.0,
            cfg = -1.0,
            sampler = "k_euler"
        ).toJobPayload("STA1N-test")

        assertEquals("STA1N-test", payload.token)
        assertEquals("1girl, rain", payload.tag)
        assertEquals("artist", payload.artist)
        assertEquals("bad hands", payload.negative)
        assertEquals(15, payload.cost)
        assertEquals(28, payload.steps)
        assertEquals(20.0, payload.scale)
        assertEquals(0.0, payload.cfg)
        assertEquals("karras", payload.noiseSchedule)
    }

    private fun testImage(
        id: String = "image",
        createdAt: Long = 1,
        tags: List<String> = listOf("default")
    ) = ImageRecord(
        id = id,
        localUri = "content://media/$id",
        createdAt = createdAt,
        prompt = "1girl",
        archiveTags = tags,
        artist = "",
        negativePrompt = "",
        presetName = "",
        favorite = false
    )
}
