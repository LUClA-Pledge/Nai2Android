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
        statusMessage = "宸插鐢ㄩ璁撅細${preset.name}锛屽唴瀹逛粛鍙户缁拷鍔犱慨鏀?
    }

    fun saveConnection(newBaseUrl: String, newToken: String) {
        val normalizedUrl = newBaseUrl.trim().trimEnd('/').ifBlank { DEFAULT_BASE_URL }
        val normalizedToken = newToken.trim()
        baseUrl = normalizedUrl
        accessToken = normalizedToken
        settingsStore.baseUrl = normalizedUrl
        tokenStore.save(normalizedToken)
        statusMessage = "杩炴帴璁剧疆宸蹭繚瀛?
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
                .onSuccess { balance = it.balance; statusMessage = "瀵嗛挜杩炴帴鎴愬姛锛屽綋鍓嶉搴?${it.balance} 鐐? }
                .onFailure { balance = null; statusMessage = "瀵嗛挜杩炴帴澶辫触锛?{it.message.orEmpty()}" }
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
                .onFailure { statusMessage = "鏈嶅姟閰嶇疆璇诲彇澶辫触锛?{it.message.orEmpty()}" }
        }
    }

    fun generate() {
        if (isGenerating) return
        if (accessToken.isBlank()) {
            statusMessage = "璇峰厛鍦ㄨ缃腑濉啓 STA1N 璁块棶瀵嗛挜"
            screen = AppScreen.SETTINGS
            return
        }
        if (form.prompt.isBlank()) {
            statusMessage = "鎻愮ず璇嶄笉鑳戒负绌?
            return
        }

        val requestForm = form
        val token = accessToken
        val client = NaiApiClient(baseUrl)
        isGenerating = true
        statusMessage = "姝ｅ湪鎻愪氦鐢熸垚浠诲姟鈥︹€?

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
                            "鎺掗槦涓細绗?${job.queuePosition} / ${job.queuedCount.coerceAtLeast(1)} 涓?
                        } else {
                            "宸叉彁浜わ紝绛夊緟鍙敤璐﹀彿"
                        }
                        JobStatus.RUNNING -> "姝ｅ湪鐢熸垚锛?{job.progress.percent}%"
                        JobStatus.DONE -> "鍥剧墖鐢熸垚瀹屾垚锛屾鍦ㄤ繚瀛樺埌鏈湴鈥︹€?
                        else -> "姝ｅ湪澶勭悊鈥︹€?
                    }
                }
                if (completed.imageUrl.isBlank()) throw NaiApiException("鏈嶅姟鏈繑鍥炲浘鐗囧湴鍧€")

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
                statusMessage = "鐢熸垚瀹屾垚锛屽凡淇濆瓨鍒扮郴缁熷浘搴?
            } catch (error: Throwable) {
                statusMessage = "鐢熸垚澶辫触锛?{error.message ?: "鏈煡閿欒"}"
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
            statusMessage = "棰勮鍚嶇О涓嶈兘涓虹┖"
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.upsertPreset(preset.copy(name = preset.name.trim())) }
            refreshPresets()
            statusMessage = "棰勮宸蹭繚瀛?
        }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.deletePreset(preset.id) }
            refreshPresets()
            statusMessage = "棰勮宸插垹闄?
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

