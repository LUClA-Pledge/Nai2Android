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

    @Test
    fun applying_a_preset_updates_the_archive_tag_to_the_preset_name() {
        val now = 100L
        val preset = Preset(
            id = "preset-2",
            name = "雨夜霓虹",
            tag = "1girl, rain, neon",
            artist = "artist",
            negativePrompt = "bad hands",
            createdAt = now,
            updatedAt = now
        )

        val result = GenerationForm(archiveTags = "旧预设").withPreset(preset)

        assertEquals("雨夜霓虹", result.archiveTags)
        assertEquals("雨夜霓虹", result.presetName)
        assertEquals("1girl, rain, neon", result.prompt)
    }

    @Test
    fun generation_parameters_keep_every_value_needed_by_image_details() {
        val form = GenerationForm(
            size = "2K横图",
            steps = 24,
            scale = 7.5,
            cfg = 0.4,
            sampler = "k_euler"
        )

        val parameters = form.toGenerationParameters("nai-diffusion-4-5-full")

        assertEquals("nai-diffusion-4-5-full", parameters.model)
        assertEquals("2K横图", parameters.size)
        assertEquals(15, parameters.cost)
        assertEquals(24, parameters.steps)
        assertEquals(7.5, parameters.scale)
        assertEquals(0.4, parameters.cfg)
        assertEquals("k_euler", parameters.sampler)
        assertEquals("karras", parameters.noiseSchedule)
        assertTrue(parameters.noCache)
    }

    @Test
    fun recording_an_export_increments_the_count_every_time() {
        val once = testImage().recordExport()
        val twice = once.recordExport()

        assertEquals(1, once.exportCount)
        assertEquals(2, twice.exportCount)
        assertTrue(twice.isSavedToSystemGallery())
    }

    @Test
    fun batch_count_is_clamped_to_supported_concurrency_range() {
        assertEquals(1, normalizedBatchCount(0))
        assertEquals(3, normalizedBatchCount(3))
        assertEquals(4, normalizedBatchCount(99))
    }

    @Test
    fun website_artist_presets_match_the_create_page_defaults() {
        assertEquals(6, WEBSITE_ARTIST_PRESETS.size)
        assertEquals("韩漫小清新风", WEBSITE_ARTIST_PRESETS.first().label)
        assertEquals("2.5d", DEFAULT_WEBSITE_ARTIST_PRESET.id)
        assertTrue(DEFAULT_WEBSITE_ARTIST_PRESET.value.isNotBlank())
        assertEquals(DEFAULT_WEBSITE_ARTIST_PRESET, findWebsiteArtistPreset(DEFAULT_WEBSITE_ARTIST_PRESET.value))
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
        favorite = false,
        savedToDevice = false
    )
}

