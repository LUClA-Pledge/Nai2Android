package cn.sta1n.nai2android

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppScreen {
    CREATE,
    GALLERY,
    PRESETS,
    SETTINGS
}

enum class GenerationTaskStatus {
    SUBMITTING,
    QUEUED,
    RUNNING,
    SAVING,
    DONE,
    FAILED
}

data class GenerationTaskUiState(
    val id: String,
    val sequence: Int,
    val status: GenerationTaskStatus = GenerationTaskStatus.SUBMITTING,
    val progress: Int = 0,
    val message: String = "正在提交任务",
    val imageId: String? = null
)

class NaiViewModel(application: Application) : AndroidViewModel(application) {
    private val database = LocalDatabase(application)
    private val imageStorage = ImageStorage(application)
    private val tokenStore = SecureTokenStore(application)
    private val settingsStore = AppSettingsStore(application)

    var screen by mutableStateOf(AppScreen.CREATE)
        private set
    var accessToken by mutableStateOf(tokenStore.read())
        private set
    var baseUrl by mutableStateOf(settingsStore.baseUrl)
        private set
    var serviceSettings by mutableStateOf<ServiceSettings?>(null)
        private set
    var balance by mutableStateOf<Int?>(null)
        private set
    var form by mutableStateOf(GenerationForm())
        private set
    var isGenerating by mutableStateOf(false)
        private set
    var generationTasks by mutableStateOf<List<GenerationTaskUiState>>(emptyList())
        private set
    var latestGeneratedImage by mutableStateOf<ImageRecord?>(null)
        private set
    var statusMessage by mutableStateOf("")
        private set
    var presets by mutableStateOf<List<Preset>>(emptyList())
        private set
    var galleryImages by mutableStateOf<List<ImageRecord>>(emptyList())
        private set
    var availableArchiveTags by mutableStateOf<List<String>>(emptyList())
        private set
    var sortOrder by mutableStateOf(SortOrder.NEWEST_FIRST)
        private set
    var favoriteOnly by mutableStateOf(false)
        private set
    var selectedArchiveTag by mutableStateOf<String?>(null)
        private set
    var gallerySelectionMode by mutableStateOf(false)
        private set
    var gallerySelection by mutableStateOf(GallerySelection())
        private set
    var isGalleryActionRunning by mutableStateOf(false)
        private set

    private var allGalleryImages: List<ImageRecord> = emptyList()

    init {
        viewModelScope.launch {
            loadLocalData()
            loadServiceSettings()
            if (accessToken.isNotBlank()) refreshBalance()
        }
    }

    fun selectScreen(value: AppScreen) {
        screen = value
        if (value == AppScreen.GALLERY) refreshGallery()
        if (value == AppScreen.PRESETS) refreshPresets()
    }

    fun updateForm(transform: (GenerationForm) -> GenerationForm) {
        form = transform(form)
    }

    fun clearStatusMessage() {
        statusMessage = ""
    }

    fun applyPreset(preset: Preset) {
        form = form.withPreset(preset)
        statusMessage = "已套用预设：${preset.name}，内容仍可继续追加修改"
    }

    fun applyArtistPreset(preset: ArtistPreset) {
        form = form.copy(artist = preset.value)
        statusMessage = "已套用 artist：${preset.label}，内容仍可继续追加修改"
    }

    fun saveConnection(newBaseUrl: String, newToken: String) {
        val normalizedUrl = newBaseUrl.trim().trimEnd('/').ifBlank { DEFAULT_BASE_URL }
        val normalizedToken = newToken.trim()
        baseUrl = normalizedUrl
        accessToken = normalizedToken
        settingsStore.baseUrl = normalizedUrl
        tokenStore.save(normalizedToken)
        statusMessage = "连接设置已保存"
        loadServiceSettings()
        if (normalizedToken.isNotBlank()) refreshBalance()
    }

    fun refreshBalance() {
        if (accessToken.isBlank()) {
            balance = null
            return
        }
        viewModelScope.launch {
            runCatching { NaiApiClient(baseUrl).getMe(accessToken) }
                .onSuccess { balance = it.balance; statusMessage = "密钥连接成功，当前额度 ${it.balance} 点" }
                .onFailure { balance = null; statusMessage = "密钥连接失败：${it.message.orEmpty()}" }
        }
    }

    fun loadServiceSettings() {
        viewModelScope.launch {
            runCatching { NaiApiClient(baseUrl).getSettings() }
                .onSuccess { settings ->
                    serviceSettings = settings
                    if (form.artist.isBlank() && settings.defaultArtist.isNotBlank()) {
                        form = form.copy(artist = settings.defaultArtist)
                    }
                    if (form.negativePrompt.isBlank() && settings.defaultNegative.isNotBlank()) {
                        form = form.copy(negativePrompt = settings.defaultNegative)
                    }
                }
                .onFailure { statusMessage = "服务配置读取失败：${it.message.orEmpty()}" }
        }
    }

    fun generate() {
        if (isGenerating) return
        if (accessToken.isBlank()) {
            statusMessage = "请先在设置中填写 STA1N 访问密钥"
            screen = AppScreen.SETTINGS
            return
        }
        if (form.prompt.isBlank()) {
            statusMessage = "提示词不能为空"
            return
        }

        val requestForm = form.copy(batchCount = normalizedBatchCount(form.batchCount))
        val token = accessToken
        val client = NaiApiClient(baseUrl)
        val model = serviceSettings?.defaultModel ?: "nai-diffusion-4-5-full"
        val taskIds = (1..requestForm.batchCount).map { UUID.randomUUID().toString() }
        isGenerating = true
        generationTasks = taskIds.mapIndexed { index, id ->
            GenerationTaskUiState(id = id, sequence = index + 1)
        }
        statusMessage = "已开始并发生成 ${requestForm.batchCount} 张图片"

        viewModelScope.launch {
            val succeeded = taskIds.mapIndexed { index, taskId ->
                async {
                    generateOne(taskId, index + 1, requestForm, token, client, model)
                }
            }.awaitAll().count { it }
            loadGallery()
            refreshBalance()
            isGenerating = false
            val failed = requestForm.batchCount - succeeded
            statusMessage = if (failed == 0) {
                "$succeeded 张图片已生成并存入应用图库"
            } else {
                "生成完成：成功 $succeeded 张，失败 $failed 张"
            }
        }
    }

    private suspend fun generateOne(
        taskId: String,
        sequence: Int,
        requestForm: GenerationForm,
        token: String,
        client: NaiApiClient,
        model: String
    ): Boolean = runCatching {
        val initial = client.submitJob(requestForm.toJobPayload(token = token, model = model))
        val completed = client.waitForCompletion(initial, token) { job ->
            updateGenerationTask(taskId) { task ->
                when (job.status) {
                    JobStatus.QUEUED -> task.copy(
                        status = GenerationTaskStatus.QUEUED,
                        message = if (job.queuePosition > 0) {
                            "排队第 ${job.queuePosition} / ${job.queuedCount.coerceAtLeast(1)} 位"
                        } else {
                            "等待可用账号"
                        }
                    )
                    JobStatus.RUNNING -> task.copy(
                        status = GenerationTaskStatus.RUNNING,
                        progress = job.progress.percent.coerceIn(0, 100),
                        message = "生成中 ${job.progress.percent.coerceIn(0, 100)}%"
                    )
                    JobStatus.DONE -> task.copy(
                        status = GenerationTaskStatus.SAVING,
                        progress = 100,
                        message = "正在保存到应用图库"
                    )
                    else -> task
                }
            }
        }
        if (completed.imageUrl.isBlank()) throw NaiApiException("服务未返回图片地址")

        val localUri = imageStorage.saveRemoteImageToAppGallery(
            client = client,
            imageUrl = completed.imageUrl,
            token = token,
            displayName = "nai_${timestampForFileName()}_${sequence}.png"
        )
        val record = ImageRecord(
            id = UUID.randomUUID().toString(),
            localUri = localUri.toString(),
            createdAt = System.currentTimeMillis(),
            prompt = requestForm.prompt.trim(),
            archiveTags = defaultArchiveTags(requestForm),
            artist = requestForm.artist.trim(),
            negativePrompt = requestForm.negativePrompt.trim(),
            presetName = requestForm.presetName,
            favorite = false,
            savedToDevice = false,
            parameters = requestForm.toGenerationParameters(model).copy(jobId = completed.id)
        )
        withContext(Dispatchers.IO) { database.insertImage(record) }
        latestGeneratedImage = record
        updateGenerationTask(taskId) {
            it.copy(
                status = GenerationTaskStatus.DONE,
                progress = 100,
                message = "已保存到应用图库",
                imageId = record.id
            )
        }
        true
    }.getOrElse { error ->
        updateGenerationTask(taskId) {
            it.copy(
                status = GenerationTaskStatus.FAILED,
                message = error.message ?: "未知错误"
            )
        }
        false
    }

    private fun updateGenerationTask(
        taskId: String,
        transform: (GenerationTaskUiState) -> GenerationTaskUiState
    ) {
        generationTasks = generationTasks.map { task ->
            if (task.id == taskId) transform(task) else task
        }
    }

    fun refreshGallery() {
        viewModelScope.launch { loadGallery() }
    }

    fun updateSortOrder(value: SortOrder) {
        sortOrder = value
        applyGalleryFilters()
    }

    fun updateFavoriteOnly(value: Boolean) {
        favoriteOnly = value
        applyGalleryFilters()
    }

    fun setArchiveTagFilter(value: String?) {
        selectedArchiveTag = value
        applyGalleryFilters()
    }

    fun enterGallerySelectionMode() {
        gallerySelectionMode = true
    }

    fun exitGallerySelectionMode() {
        gallerySelectionMode = false
        gallerySelection = GallerySelection()
    }

    fun toggleGallerySelection(image: ImageRecord) {
        gallerySelection = gallerySelection.toggle(image.id)
    }

    fun selectAllVisibleGalleryImages() {
        gallerySelection = gallerySelection.selectAll(galleryImages)
    }

    fun clearGallerySelection() {
        gallerySelection = GallerySelection()
    }

    fun exportSelectedGalleryImages() {
        val selected = selectedGalleryImages()
        if (selected.isEmpty() || isGalleryActionRunning) {
            if (selected.isEmpty()) statusMessage = "请先选择要导出的图片"
            return
        }
        isGalleryActionRunning = true
        statusMessage = "正在批量导出图片……"
        viewModelScope.launch {
            try {
                var exportedCount = 0
                var failedCount = 0
                val exportedIds = mutableSetOf<String>()
                selected.forEach { image ->
                    runCatching {
                        imageStorage.exportToSystemGallery(
                            source = Uri.parse(image.localUri),
                            displayName = "nai_${image.id}.png"
                        )
                    }.onSuccess {
                        exportedIds += image.id
                        exportedCount++
                    }.onFailure {
                        failedCount++
                    }
                }
                withContext(Dispatchers.IO) { database.recordExports(exportedIds) }
                loadGallery()
                gallerySelection = GallerySelection()
                gallerySelectionMode = false
                statusMessage = when {
                    failedCount > 0 -> "已导出 $exportedCount 张，$failedCount 张失败"
                    else -> "已批量导出 $exportedCount 张图片到系统相册"
                }
            } catch (error: Throwable) {
                statusMessage = "批量导出失败：${error.message.orEmpty()}"
            } finally {
                isGalleryActionRunning = false
            }
        }
    }

    fun deleteSelectedGalleryImages() {
        val selected = selectedGalleryImages()
        if (selected.isEmpty() || isGalleryActionRunning) {
            if (selected.isEmpty()) statusMessage = "请先选择要删除的图片"
            return
        }
        isGalleryActionRunning = true
        statusMessage = "正在删除所选图片……"
        viewModelScope.launch {
            try {
                val selectedIds = selected.mapTo(mutableSetOf()) { it.id }
                withContext(Dispatchers.IO) {
                    selected.forEach { imageStorage.deleteFromAppGallery(Uri.parse(it.localUri)) }
                    database.deleteImages(selectedIds)
                }
                gallerySelection = GallerySelection()
                gallerySelectionMode = false
                loadGallery()
                statusMessage = "已从应用图库删除 ${selected.size} 张图片；系统相册中的导出副本不会被删除"
            } catch (error: Throwable) {
                statusMessage = "批量删除失败：${error.message.orEmpty()}"
            } finally {
                isGalleryActionRunning = false
            }
        }
    }

    fun toggleFavorite(image: ImageRecord) {
        val next = !image.favorite
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.setFavorite(image.id, next) }
            loadGallery()
        }
    }

    fun updateImageTags(image: ImageRecord, rawTags: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.updateArchiveTags(image.id, normalizeArchiveTags(rawTags))
            }
            loadGallery()
        }
    }

    fun saveImageToDevice(image: ImageRecord) {
        viewModelScope.launch {
            statusMessage = "正在保存到系统图库……"
            runCatching {
                imageStorage.exportToSystemGallery(
                    source = Uri.parse(image.localUri),
                    displayName = "nai_${image.id}.png"
                )
                withContext(Dispatchers.IO) { database.recordExport(image.id) }
                loadGallery()
            }.onSuccess {
                statusMessage = "已保存到系统图库 Pictures/Nai2API"
            }.onFailure {
                statusMessage = "保存到系统图库失败：${it.message.orEmpty()}"
            }
        }
    }

    fun refreshPresets() {
        viewModelScope.launch {
            presets = withContext(Dispatchers.IO) { database.listPresets() }
        }
    }

    fun savePreset(preset: Preset) {
        if (preset.name.isBlank()) {
            statusMessage = "预设名称不能为空"
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.upsertPreset(preset.copy(name = preset.name.trim())) }
            refreshPresets()
            statusMessage = "预设已保存"
        }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.deletePreset(preset.id) }
            refreshPresets()
            statusMessage = "预设已删除"
        }
    }

    fun newPreset(): Preset {
        val now = System.currentTimeMillis()
        return Preset(
            id = UUID.randomUUID().toString(),
            name = "",
            tag = "",
            artist = "",
            negativePrompt = "",
            createdAt = now,
            updatedAt = now
        )
    }

    private suspend fun loadLocalData() {
        presets = withContext(Dispatchers.IO) { database.listPresets() }
        loadGallery()
    }

    private suspend fun loadGallery() {
        allGalleryImages = withContext(Dispatchers.IO) { database.listImages() }
        val availableIds = allGalleryImages.mapTo(mutableSetOf()) { it.id }
        gallerySelection = GallerySelection(gallerySelection.ids.intersect(availableIds))
        availableArchiveTags = allGalleryImages
            .flatMap { it.archiveTags }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
        applyGalleryFilters()
    }

    private fun applyGalleryFilters() {
        galleryImages = sortImages(
            allGalleryImages.filter { image ->
                (!favoriteOnly || image.favorite) &&
                    (selectedArchiveTag == null || image.matchesArchiveTag(selectedArchiveTag!!))
            },
            sortOrder
        )
    }

    private fun selectedGalleryImages(): List<ImageRecord> = allGalleryImages.filter {
        it.id in gallerySelection.ids
    }

    private fun timestampForFileName(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())

    override fun onCleared() {
        database.closeDatabase()
        super.onCleared()
    }
}

