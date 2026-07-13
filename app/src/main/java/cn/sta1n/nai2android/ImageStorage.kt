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
    suspend fun saveRemoteImageToAppGallery(
        client: NaiApiClient,
        imageUrl: String,
        token: String,
        displayName: String
    ): Uri = withContext(Dispatchers.IO) {
        val galleryDirectory = File(context.filesDir, APP_GALLERY_DIRECTORY).apply { mkdirs() }
        val target = File(galleryDirectory, displayName.safeFileName())
        target.outputStream().use { output ->
            client.downloadImageTo(imageUrl, token, output)
        }
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

    private fun openInputStream(uri: Uri): InputStream? = when (uri.scheme) {
        "file" -> uri.path?.let { FileInputStream(File(it)) }
        else -> context.contentResolver.openInputStream(uri)
    }

    private fun String.safeFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        const val APP_GALLERY_DIRECTORY = "gallery"
    }
}
