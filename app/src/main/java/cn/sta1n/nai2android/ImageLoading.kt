package cn.sta1n.nai2android

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

internal const val GALLERY_THUMBNAIL_MAX_DIMENSION = 768
internal val galleryImageDecodeDispatcher = Dispatchers.IO.limitedParallelism(2)

private const val THUMBNAIL_CACHE_SIZE_KIB = 24 * 1024

internal object GalleryThumbnailCache {
    private val cache = object : LruCache<String, Bitmap>(THUMBNAIL_CACHE_SIZE_KIB) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }

    fun get(uri: String): Bitmap? = cache.get(uri)

    fun put(uri: String, bitmap: Bitmap) {
        cache.put(uri, bitmap)
    }
}

internal fun decodeGalleryBitmap(
    uri: Uri,
    resolver: ContentResolver,
    maxDimension: Int? = null
): Bitmap? {
    if (maxDimension == null) {
        return openImageInputStream(uri, resolver)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openImageInputStream(uri, resolver)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            targetDimension = maxDimension
        )
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return openImageInputStream(uri, resolver)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}

internal fun calculateInSampleSize(
    width: Int,
    height: Int,
    targetDimension: Int
): Int {
    if (width <= 0 || height <= 0 || targetDimension <= 0) return 1

    val longestSide = maxOf(width, height)
    var sampleSize = 1
    while (longestSide / sampleSize > targetDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun openImageInputStream(uri: Uri, resolver: ContentResolver): InputStream? = when (uri.scheme) {
    "file" -> uri.path?.let { FileInputStream(File(it)) }
    else -> resolver.openInputStream(uri)
}
