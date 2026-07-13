package cn.sta1n.nai2android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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

    fun applyPreset(preset: Preset) {
        form = form.copy(
            prompt = preset.tag,
            artist = preset.artist,
            negativePrompt = preset.negativePrompt,
            presetName = preset.name
        )
        statusMessage = "已套用预设：${preset.name}，内容仍可继续追加修改"
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

        val requestForm = form
        val token = accessToken
        val client = NaiApiClient(baseUrl)
        isGenerating = true
        statusMessage = "正在提交生成任务……"

        viewModelScope.launch {
            try {
                val initial = client.submitJob(
                    requestForm.toJobPayload(
                        token = token,
                        model = serviceSettings?.defaultModel ?: "nai-diffusion-4-5-full"
                    )
                )
                val completed = client.waitForCompletion(initial, token) { job ->
                    statusMessage = when (job.status) {
                        JobStatus.QUEUED -> if (job.queuePosition > 0) {
                            "排队中：第 ${job.queuePosition} / ${job.queuedCount.coerceAtLeast(1)} 个"
                        } else {
                            "已提交，等待可用账号"
                        }
                        JobStatus.RUNNING -> "正在生成：${job.progress.percent}%"
                        JobStatus.DONE -> "图片生成完成，正在保存到本地……"
                        else -> "正在处理……"
                    }
                }
                if (completed.imageUrl.isBlank()) throw NaiApiException("服务未返回图片地址")

                val fileName = "nai_${timestampForFileName()}.png"
                val localUri = imageStorage.saveRemoteImage(client, completed.imageUrl, token, fileName)
                val record = ImageRecord(
                    id = UUID.randomUUID().toString(),
                    localUri = localUri.toString(),
                    createdAt = System.currentTimeMillis(),
                    prompt = requestForm.prompt.trim(),
                    archiveTags = defaultArchiveTags(requestForm),
                    artist = requestForm.artist.trim(),
                    negativePrompt = requestForm.negativePrompt.trim(),
                    presetName = requestForm.presetName,
                    favorite = false
                )
                withContext(Dispatchers.IO) { database.insertImage(record) }
                refreshGallery()
                refreshBalance()
                statusMessage = "生成完成，已保存到系统图库"
            } catch (error: Throwable) {
                statusMessage = "生成失败：${error.message ?: "未知错误"}"
            } finally {
                isGenerating = false
            }
        }
    }

    fun refreshGallery() {
        viewModelScope.launch { loadGallery() }
    }

    fun setSortOrder(value: SortOrder) {
        sortOrder = value
        applyGalleryFilters()
    }

    fun setFavoriteOnly(value: Boolean) {
        favoriteOnly = value
        applyGalleryFilters()
    }

    fun setArchiveTagFilter(value: String?) {
        selectedArchiveTag = value
        applyGalleryFilters()
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

    private fun timestampForFileName(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())

    override fun onCleared() {
        database.closeDatabase()
        super.onCleared()
    }
}
