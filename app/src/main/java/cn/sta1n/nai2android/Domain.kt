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
    val favorite: Boolean,
    val savedToDevice: Boolean = false,
    val exportCount: Int = 0,
    val generation: GenerationMetadata = GenerationMetadata()
) {
    fun matchesArchiveTag(tag: String): Boolean = archiveTags.any {
        it.trim().equals(tag.trim(), ignoreCase = true)
    }

    fun isSavedToSystemGallery(): Boolean =
        exportCount > 0 || savedToDevice || localUri.startsWith("content://")
}

data class GenerationMetadata(
    val model: String = "",
    val size: String = "",
    val steps: Int = 0,
    val scale: Double = 0.0,
    val cfg: Double = 0.0,
    val sampler: String = "",
    val cost: Int = 0,
    val nocache: String = "",
    val noiseSchedule: String = ""
) {
    fun isEmpty(): Boolean = model.isBlank() && size.isBlank() && sampler.isBlank()
}

enum class GenerationTaskState {
    SUBMITTING,
    QUEUED,
    RUNNING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class GenerationTask(
    val id: String,
    val ordinal: Int,
    val state: GenerationTaskState = GenerationTaskState.SUBMITTING,
    val progress: Int = 0,
    val message: String = ""
) {
    fun isTerminal(): Boolean = state in setOf(
        GenerationTaskState.COMPLETED,
        GenerationTaskState.FAILED,
        GenerationTaskState.CANCELLED
    )
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
    val batchCount: Int = 1,
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

data class GallerySelection(val ids: Set<String> = emptySet()) {
    fun toggle(id: String): GallerySelection = copy(
        ids = if (id in ids) ids - id else ids + id
    )

    fun selectAll(images: List<ImageRecord>): GallerySelection = copy(
        ids = images.mapTo(linkedSetOf()) { it.id }
    )

    fun remove(idsToRemove: Set<String>): GallerySelection = copy(
        ids = ids - idsToRemove
    )
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

fun GenerationForm.normalizedBatchCount(): Int = batchCount.coerceIn(1, MAX_CONCURRENT_GENERATIONS)

fun GenerationForm.applyPreset(preset: Preset): GenerationForm = copy(
    prompt = preset.tag,
    archiveTags = preset.name,
    artist = preset.artist,
    negativePrompt = preset.negativePrompt,
    presetName = preset.name
)

fun GenerationForm.toGenerationMetadata(model: String): GenerationMetadata = GenerationMetadata(
    model = model.trim(),
    size = size,
    steps = steps.coerceIn(1, 28),
    scale = scale.coerceIn(1.0, 20.0),
    cfg = cfg.coerceIn(0.0, 1.0),
    sampler = sampler,
    cost = generationCostForSize(size),
    nocache = "1",
    noiseSchedule = "karras"
)

fun GenerationForm.toJobPayload(
    token: String,
    model: String = "nai-diffusion-4-5-full"
): JobPayload {
    val metadata = toGenerationMetadata(model)
    return JobPayload(
        token = token,
        tag = prompt.trim(),
        model = metadata.model,
        artist = artist.trim(),
        size = metadata.size,
        cost = metadata.cost,
        steps = metadata.steps,
        scale = metadata.scale,
        cfg = metadata.cfg,
        sampler = metadata.sampler,
        negative = negativePrompt.trim(),
        nocache = metadata.nocache,
        noiseSchedule = metadata.noiseSchedule
    )
}

fun defaultArchiveTags(form: GenerationForm): List<String> {
    val explicit = normalizeArchiveTags(form.archiveTags)
    if (explicit.isNotEmpty()) return explicit
    val presetTag = normalizeArchiveTags(form.prompt).firstOrNull()
    return listOfNotNull(form.presetName.trim().takeIf(String::isNotEmpty), presetTag)
        .distinctBy { it.lowercase(Locale.ROOT) }
}

const val MAX_CONCURRENT_GENERATIONS = 4
