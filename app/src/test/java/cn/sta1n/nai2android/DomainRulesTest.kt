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
    fun generation_form_snapshots_every_gallery_visible_parameter() {
        val form = GenerationForm(
            prompt = "subject",
            artist = "artist",
            negativePrompt = "bad hands",
            size = "2Ksize",
            steps = 24,
            scale = 7.5,
            cfg = 0.4,
            sampler = "k_euler"
        )
        val payload = form.toJobPayload("STA1N-test", model = "nai-test-model")
        val metadata = form.toGenerationMetadata(model = "nai-test-model")

        assertEquals(payload.model, metadata.model)
        assertEquals(payload.size, metadata.size)
        assertEquals(payload.steps, metadata.steps)
        assertEquals(payload.scale, metadata.scale)
        assertEquals(payload.cfg, metadata.cfg)
        assertEquals(payload.sampler, metadata.sampler)
        assertEquals(payload.cost, metadata.cost)
        assertEquals(payload.nocache, metadata.nocache)
        assertEquals(payload.noiseSchedule, metadata.noiseSchedule)
    }

    @Test
    fun applying_a_preset_replaces_the_archive_tag_with_the_preset_name() {
        val preset = Preset(
            id = "preset",
            name = "Night scene",
            tag = "night, rain",
            artist = "artist",
            negativePrompt = "day",
            createdAt = 1,
            updatedAt = 1
        )

        val applied = GenerationForm(archiveTags = "old tag").applyPreset(preset)

        assertEquals("Night scene", applied.archiveTags)
        assertEquals("Night scene", applied.presetName)
        assertEquals("night, rain", applied.prompt)
        assertEquals(listOf("Night scene"), defaultArchiveTags(applied))
    }

    @Test
    fun batch_count_is_limited_to_a_bounded_concurrency_range() {
        assertEquals(1, GenerationForm(batchCount = 0).normalizedBatchCount())
        assertEquals(3, GenerationForm(batchCount = 3).normalizedBatchCount())
        assertEquals(4, GenerationForm(batchCount = 99).normalizedBatchCount())
    }

    @Test
    fun export_count_keeps_an_image_exportable_after_the_first_export() {
        val image = testImage().copy(savedToDevice = false, exportCount = 2)

        assertTrue(image.isSavedToSystemGallery())
        assertEquals(2, image.exportCount)
    }

    @Test
    fun cancelled_generation_task_is_terminal() {
        assertFalse(GenerationTask(id = "pending", ordinal = 1).isTerminal())
        assertTrue(
            GenerationTask(
                id = "cancelled",
                ordinal = 1,
                state = GenerationTaskState.CANCELLED
            ).isTerminal()
        )
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
