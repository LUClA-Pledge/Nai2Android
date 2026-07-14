package cn.sta1n.nai2android

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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
    var generationTasks by mutableStateOf<List<GenerationTask>>(emptyList())
        private set
    var generatedImages by mutableStateOf<List<ImageRecord>>(emptyList())
        private set
    var archivedGeneratedImageIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var selectedGeneratedImageId by mutableStateOf<String?>(null)
        private set
    var isGeneratedImageActionRunning by mutableStateOf(false)
        private set
    private val notificationChannel = Channel<String>(Channel.BUFFERED)
    val notifications = notificationChannel.receiveAsFlow()
    private var statusMessage: String = ""
        set(value) {
            field = value
            if (value.isNotBlank()) notificationChannel.trySend(value)
        }
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
    private var generationBatchJob: Job? = null
    private var activeGenerationClient: NaiApiClient? = null
    private var activeGenerationSessionId = 0L

    val currentGeneratedImage: ImageRecord?
        get() = generatedImages.firstOrNull { it.id == selectedGeneratedImageId }
            ?: generatedImages.firstOrNull()

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

    fun applyPreset(preset: Preset) {
        form = form.applyPreset(preset)
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

        val requestForm = form.copy(batchCount = form.normalizedBatchCount())
        val token = accessToken
        val client = NaiApiClient(baseUrl)
        val model = serviceSettings?.defaultModel ?: "nai-diffusion-4-5-full"
        val sessionId = ++activeGenerationSessionId
        activeGenerationClient = client
        val tasks = List(requestForm.batchCount) { index ->
            GenerationTask(id = UUID.randomUUID().toString(), ordinal = index + 1)
        }
        generationTasks = tasks
        isGenerating = true
        statusMessage = "正在提交 ${tasks.size} 个生成任务……"

        generationBatchJob = viewModelScope.launch {
            try {
                val successfulCount = supervisorScope {
                    tasks.map { task ->
                        async {
                            generatePreview(
                                sessionId = sessionId,
                                task = task,
                                requestForm = requestForm,
                                token = token,
                                client = client,
                                model = model
                            )
                        }
                    }.map { it.await() }.count { it }
                }
                if (isCurrentGenerationSession(sessionId)) {
                    refreshBalance()
                    statusMessage = when (successfulCount) {
                        tasks.size -> "已生成 $successfulCount 张预览图，选择后可归档到图库"
                        0 -> "生成任务全部失败，请检查网络或参数后重试"
                        else -> "已生成 $successfulCount / ${tasks.size} 张预览图"
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                if (isCurrentGenerationSession(sessionId)) {
                    isGenerating = false
                    generationBatchJob = null
                    activeGenerationClient = null
                }
            }
        }
    }

    fun cancelGeneration() {
        val batchJob = generationBatchJob ?: return
        if (!isGenerating) return
        activeGenerationSessionId++
        generationTasks = generationTasks.map { task ->
            if (task.isTerminal()) task else task.copy(state = GenerationTaskState.CANCELLED)
        }
        isGenerating = false
        generationBatchJob = null
        activeGenerationClient?.cancelActiveRequests()
        activeGenerationClient = null
        batchJob.cancel()
        statusMessage = "已取消当前生成批次"
    }

    fun selectGeneratedImage(id: String) {
        selectedGeneratedImageId = id
    }

    fun isGeneratedImageArchived(image: ImageRecord): Boolean = image.id in archivedGeneratedImageIds

    fun archiveGeneratedImage(image: ImageRecord) {
        if (isGeneratedImageActionRunning) return
        if (isGeneratedImageArchived(image)) {
            statusMessage = "这张预览图已归档到应用图库"
            return
        }
        viewModelScope.launch {
            isGeneratedImageActionRunning = true
            statusMessage = "正在归档到应用图库……"
            var galleryUri: Uri? = null
            var archivedPersisted = false
            try {
                val copiedUri = imageStorage.copyGenerationPreviewToAppGallery(
                    source = Uri.parse(image.localUri),
                    displayName = "nai_${image.id}.png"
                )
                galleryUri = copiedUri
                val archived = image.copy(localUri = copiedUri.toString())
                withContext(Dispatchers.IO) { database.insertImage(archived) }
                archivedPersisted = true
                imageStorage.deleteGenerationPreview(Uri.parse(image.localUri))
                generatedImages = generatedImages.map { current ->
                    if (current.id == image.id) archived else current
                }
                archivedGeneratedImageIds += image.id
                loadGallery()
                statusMessage = "已归档到应用图库"
            } catch (error: Throwable) {
                if (!archivedPersisted) {
                    galleryUri?.let { imageStorage.deleteFromAppGallery(it) }
                }
                statusMessage = "归档到应用图库失败：${error.message.orEmpty()}"
            } finally {
                isGeneratedImageActionRunning = false
            }
        }
    }

    fun saveGeneratedImageToDevice(image: ImageRecord) {
        if (isGeneratedImageActionRunning) return
        viewModelScope.launch {
            isGeneratedImageActionRunning = true
            statusMessage = "正在导出到系统图库……"
            runCatching {
                imageStorage.exportToSystemGallery(
                    source = Uri.parse(image.localUri),
                    displayName = "nai_${image.id}_${image.exportCount + 1}.png"
                )
                val updated = image.copy(savedToDevice = true, exportCount = image.exportCount + 1)
                generatedImages = generatedImages.map { current ->
                    if (current.id == image.id) updated else current
                }
                if (isGeneratedImageArchived(image)) {
                    withContext(Dispatchers.IO) { database.recordExport(image.id) }
                    loadGallery()
                }
                updated.exportCount
            }.onSuccess { count ->
                statusMessage = "已导出到系统图库，这是第 $count 次导出"
            }.onFailure {
                statusMessage = "导出到系统图库失败：${it.message.orEmpty()}"
            }
            isGeneratedImageActionRunning = false
        }
    }

    private suspend fun generatePreview(
        sessionId: Long,
        task: GenerationTask,
        requestForm: GenerationForm,
        token: String,
        client: NaiApiClient,
        model: String
    ): Boolean {
        return try {
            val payload = requestForm.toJobPayload(token = token, model = model)
            val initial = client.submitJob(payload)
            val completed = client.waitForCompletion(initial, token) { job ->
                updateGenerationTask(sessionId, task.id) { current ->
                    when (job.status) {
                        JobStatus.QUEUED -> current.copy(
                            state = GenerationTaskState.QUEUED,
                            message = if (job.queuePosition > 0) {
                                "排队：${job.queuePosition} / ${job.queuedCount.coerceAtLeast(1)}"
                            } else {
                                "等待可用账号"
                            }
                        )
                        JobStatus.RUNNING -> current.copy(
                            state = GenerationTaskState.RUNNING,
                            progress = job.progress.percent.coerceIn(0, 100)
                        )
                        JobStatus.DONE -> current.copy(state = GenerationTaskState.DOWNLOADING)
                        JobStatus.FAILED -> current.copy(
                            state = GenerationTaskState.FAILED,
                            message = job.error
                        )
                        JobStatus.UNKNOWN -> current
                    }
                }
            }
            if (completed.imageUrl.isBlank()) throw NaiApiException("服务未返回图片地址")

            updateGenerationTask(sessionId, task.id) {
                it.copy(state = GenerationTaskState.DOWNLOADING)
            }
            val imageId = UUID.randomUUID().toString()
            val previewUri = imageStorage.saveRemoteImageToGenerationPreview(
                client = client,
                imageUrl = completed.imageUrl,
                token = token,
                displayName = "nai_${timestampForFileName()}_${imageId.take(8)}.png"
            )
            if (!isCurrentGenerationSession(sessionId)) {
                imageStorage.deleteGenerationPreview(previewUri)
                return false
            }

            val record = ImageRecord(
                id = imageId,
                localUri = previewUri.toString(),
                createdAt = System.currentTimeMillis(),
                prompt = requestForm.prompt.trim(),
                archiveTags = defaultArchiveTags(requestForm),
                artist = requestForm.artist.trim(),
                negativePrompt = requestForm.negativePrompt.trim(),
                presetName = requestForm.presetName,
                favorite = false,
                generation = requestForm.toGenerationMetadata(model)
            )
            generatedImages = listOf(record) + generatedImages
            selectedGeneratedImageId = record.id
            updateGenerationTask(sessionId, task.id) {
                it.copy(state = GenerationTaskState.COMPLETED, progress = 100)
            }
            true
        } catch (error: CancellationException) {
            updateGenerationTask(sessionId, task.id) {
                it.copy(state = GenerationTaskState.CANCELLED)
            }
            throw error
        } catch (error: Throwable) {
            updateGenerationTask(sessionId, task.id) {
                it.copy(state = GenerationTaskState.FAILED, message = error.message.orEmpty())
            }
            false
        }
    }

    private fun updateGenerationTask(
        sessionId: Long,
        taskId: String,
        transform: (GenerationTask) -> GenerationTask
    ) {
        if (!isCurrentGenerationSession(sessionId)) return
        generationTasks = generationTasks.map { task ->
            if (task.id == taskId) transform(task) else task
        }
    }

    private fun isCurrentGenerationSession(sessionId: Long): Boolean =
        activeGenerationSessionId == sessionId

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
                            displayName = "nai_${image.id}_${image.exportCount + 1}.png"
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
                    displayName = "nai_${image.id}_${image.exportCount + 1}.png"
                )
                withContext(Dispatchers.IO) { database.recordExport(image.id) }
                loadGallery()
            }.onSuccess {
                statusMessage = "已保存到系统图库，这是第 ${image.exportCount + 1} 次导出"
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
        generationBatchJob?.cancel()
        activeGenerationClient?.cancelActiveRequests()
        imageStorage.clearGenerationPreviews()
        notificationChannel.close()
        database.closeDatabase()
        super.onCleared()
    }
}
