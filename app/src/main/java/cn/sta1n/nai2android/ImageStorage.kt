package cn.sta1n.nai2android

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class ImageStorage(private val context: Context) {
    suspend fun saveRemoteImageToGenerationPreview(
        client: NaiApiClient,
        imageUrl: String,
        token: String,
        displayName: String
    ): Uri = withContext(Dispatchers.IO) {
        val previewDirectory = File(context.filesDir, GENERATION_PREVIEW_DIRECTORY).apply { mkdirs() }
        val target = File(previewDirectory, displayName.safeFileName())
        try {
            target.outputStream().use { output ->
                client.downloadImageTo(imageUrl, token, output)
            }
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
        Uri.fromFile(target)
    }

    suspend fun copyGenerationPreviewToAppGallery(source: Uri, displayName: String): Uri =
        withContext(Dispatchers.IO) {
            val sourceFile = generationPreviewFile(source)
            val galleryDirectory = File(context.filesDir, APP_GALLERY_DIRECTORY).apply { mkdirs() }
            val target = File(galleryDirectory, displayName.safeFileName())
            sourceFile.copyTo(target, overwrite = true)
            Uri.fromFile(target)
        }

    suspend fun exportToSystemGallery(source: Uri, displayName: String): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName.safeFileName())
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Nai2API")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建系统图库文件")

        try {
            openInputStream(source)?.use { input ->
                resolver.openOutputStream(uri)?.use { output ->
                    input.copyTo(output)
                } ?: error("无法打开系统图库文件")
            } ?: error("无法读取应用图库文件")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null
            )
            uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    suspend fun deleteFromAppGallery(source: Uri): Boolean = withContext(Dispatchers.IO) {
        if (source.scheme != "file") return@withContext false
        runCatching {
            val galleryDirectory = File(context.filesDir, APP_GALLERY_DIRECTORY).canonicalFile
            val target = File(source.path ?: return@runCatching false).canonicalFile
            val galleryPath = galleryDirectory.path + File.separator
            target.path.startsWith(galleryPath) && (!target.exists() || target.delete())
        }.getOrDefault(false)
    }

    fun clearGenerationPreviews() {
        File(context.filesDir, GENERATION_PREVIEW_DIRECTORY)
            .listFiles()
            ?.forEach { file -> if (file.isFile) file.delete() }
    }

    fun deleteGenerationPreview(source: Uri): Boolean = runCatching {
        generationPreviewFile(source).delete()
    }.getOrDefault(false)

    private fun openInputStream(uri: Uri): InputStream? = when (uri.scheme) {
        "file" -> uri.path?.let { FileInputStream(File(it)) }
        else -> context.contentResolver.openInputStream(uri)
    }

    private fun generationPreviewFile(uri: Uri): File {
        require(uri.scheme == "file") { "Only generated previews can be archived" }
        val previewDirectory = File(context.filesDir, GENERATION_PREVIEW_DIRECTORY).canonicalFile
        val sourceFile = File(requireNotNull(uri.path)).canonicalFile
        require(sourceFile.path.startsWith(previewDirectory.path + File.separator)) {
            "Preview is outside the managed directory"
        }
        return sourceFile
    }

    private fun String.safeFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        const val APP_GALLERY_DIRECTORY = "gallery"
        const val GENERATION_PREVIEW_DIRECTORY = "generation-previews"
    }
}
