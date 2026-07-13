package cn.sta1n.nai2android

import java.util.Locale

data class ImageRecord(
    val id: String,
    val localUri: String,
    val createdAt: Long,
    val prompt: String,
    val archiveTags: List<String>,
    val artist: String,
    val negativePrompt: String,
    val presetName: String,
    val favorite: Boolean
) {
    fun matchesArchiveTag(tag: String): Boolean = archiveTags.any {
        it.trim().equals(tag.trim(), ignoreCase = true)
    }
}

data class Preset(
    val id: String,
    val name: String,
    val tag: String,
    val artist: String,
    val negativePrompt: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class GenerationForm(
    val prompt: String = "",
    val archiveTags: String = "",
    val artist: String = "",
    val negativePrompt: String = "",
    val size: String = "竖图",
    val steps: Int = 28,
    val scale: Double = 6.0,
    val cfg: Double = 0.0,
    val sampler: String = "k_dpmpp_2m_sde",
    val presetName: String = ""
)

data class JobPayload(
    val token: String,
    val tag: String,
    val model: String,
    val artist: String,
    val size: String,
    val cost: Int,
    val steps: Int,
    val scale: Double,
    val cfg: Double,
    val sampler: String,
    val negative: String,
    val nocache: String = "1",
    val noiseSchedule: String = "karras"
)

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

fun normalizeArchiveTags(raw: String): List<String> {
    return raw
        .split(',', '\n', '，', ';', '；')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy { it.lowercase(Locale.ROOT) }
}

fun archiveTagsText(tags: List<String>): String = tags.joinToString(", ")

fun ImageRecord.withArchiveTags(raw: String): ImageRecord = copy(
    archiveTags = normalizeArchiveTags(raw)
)

fun sortImages(images: List<ImageRecord>, order: SortOrder): List<ImageRecord> {
    val comparator = compareBy<ImageRecord> { it.createdAt }
    return when (order) {
        SortOrder.NEWEST_FIRST -> images.sortedWith(comparator.reversed())
        SortOrder.OLDEST_FIRST -> images.sortedWith(comparator)
    }
}

fun generationCostForSize(size: String): Int = when (size) {
    "2K竖图", "2K横图", "2K方图" -> 15
    "4K竖图", "4K横图", "4K方图" -> 25
    else -> 1
}

fun GenerationForm.toJobPayload(
    token: String,
    model: String = "nai-diffusion-4-5-full"
): JobPayload = JobPayload(
    token = token,
    tag = prompt.trim(),
    model = model,
    artist = artist.trim(),
    size = size,
    cost = generationCostForSize(size),
    steps = steps.coerceIn(1, 28),
    scale = scale.coerceIn(1.0, 20.0),
    cfg = cfg.coerceIn(0.0, 1.0),
    sampler = sampler,
    negative = negativePrompt.trim()
)

fun defaultArchiveTags(form: GenerationForm): List<String> {
    val explicit = normalizeArchiveTags(form.archiveTags)
    if (explicit.isNotEmpty()) return explicit
    val presetTag = normalizeArchiveTags(form.prompt).firstOrNull()
    return listOfNotNull(form.presetName.trim().takeIf(String::isNotEmpty), presetTag)
        .distinctBy { it.lowercase(Locale.ROOT) }
}
