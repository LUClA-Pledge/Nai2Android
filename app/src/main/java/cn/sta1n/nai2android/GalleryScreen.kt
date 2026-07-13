�r�^�f��ئ{^�y�'vî���package cn.sta1n.nai2android

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

@Composable
fun GalleryScreen(viewModel: NaiViewModel, modifier: Modifier = Modifier) {
    var selectedImage by remember { mutableStateOf<ImageRecord?>(null) }

    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("应用图库", fontSize = 24.sp)
                Text("${viewModel.galleryImages.size} 张图片", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = viewModel.sortOrder == SortOrder.NEWEST_FIRST,
                    onClick = { viewModel.updateSortOrder(SortOrder.NEWEST_FIRST) },
                    label = { Text("最新") }
                )
                FilterChip(
                    selected = viewModel.sortOrder == SortOrder.OLDEST_FIRST,
                    onClick = { viewModel.updateSortOrder(SortOrder.OLDEST_FIRST) },
                    label = { Text("最早") }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = viewModel.favoriteOnly,
                onClick = { viewModel.updateFavoriteOnly(!viewModel.favoriteOnly) },
                label = { Text("只看收藏") }
            )
            if (viewModel.selectedArchiveTag != null) {
                FilterChip(
                    selected = true,
                    onClick = { viewModel.setArchiveTagFilter(null) },
                    label = { Text("${viewModel.selectedArchiveTag} ×") }
                )
            }
        }
        if (viewModel.availableArchiveTags.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = viewModel.selectedArchiveTag == null,
                        onClick = { viewModel.setArchiveTagFilter(null) },
                        label = { Text("全部 tag") }
                    )
                }
                items(viewModel.availableArchiveTags) { tag ->
                    FilterChip(
                        selected = viewModel.selectedArchiveTag.equals(tag, ignoreCase = true),
                        onClick = { viewModel.setArchiveTagFilter(tag) },
                        label = { Text(tag) }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (viewModel.galleryImages.isEmpty()) {
            EmptyGalleryState(modifier = Modifier.fillMaxSize())
        } else {
            val resolver = LocalContext.current.contentResolver
            LazyVerticalGrid(
                columns = GridCells.Adaptive(132.dp),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                gridItems(viewModel.galleryImages, key = { it.id }) { image ->
                    GalleryTile(
                        image = image,
                        resolver = resolver,
                        onClick = { selectedImage = image },
                        onToggleFavorite = { viewModel.toggleFavorite(image) }
                    )
                }
            }
        }
    }

    selectedImage?.let { image ->
        ImageDetailDialog(
            image = image,
            resolver = LocalContext.current.contentResolver,
            onDismiss = { selectedImage = null },
            onToggleFavorite = { viewModel.toggleFavorite(image) },
            onSaveTags = { rawTags -> viewModel.updateImageTags(image, rawTags) },
            onSaveToDevice = {
                viewModel.saveImageToDevice(image)
                selectedImage = null
            }
        )
    }
}

@Composable
private fun EmptyGalleryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("图库还没有图片", fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "生成后的图片会先放在应用图库，打开详情后再保存到系统图库",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GalleryTile(
    image: ImageRecord,
    resolver: ContentResolver,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
                LocalImage(
                    uri = image.localUri,
                    resolver = resolver,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                TextButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(if (image.favorite) "★" else "☆", fontSize = 22.sp)
                }
            }
            Text(
                text = image.archiveTags.firstOrNull() ?: "未分类",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LocalImage(
    uri: String,
    resolver: ContentResolver,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                openImageInputStream(Uri.parse(uri), resolver)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "生成图片",
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text("读取中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun openImageInputStream(uri: Uri, resolver: ContentResolver): InputStream? = when (uri.scheme) {
    "file" -> uri.path?.let { FileInputStream(File(it)) }
    else -> resolver.openInputStream(uri)
}

@Composable
private fun ImageDetailDialog(
    image: ImageRecord,
    resolver: ContentResolver,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSaveTags: (String) -> Unit,
    onSaveToDevice: () -> Unit
) {
    var tags by remember(image.id) { mutableStateOf(archiveTagsText(image.archiveTags)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (image.favorite) "已收藏图片" else "图片详情") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LocalImage(
                    uri = image.localUri,
                    resolver = resolver,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                if (image.isSavedToSystemGallery()) {
                    Text(
                        "已保存到系统图库",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                } else {
                    Button(
                        onClick = onSaveToDevice,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存到系统图库")
                    }
                }
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("归档 tag") },
                    placeholder = { Text("多个 tag 用逗号分隔") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Prompt：${image.prompt}", maxLines = 5)
                if (image.artist.isNotBlank()) Text("Artist：${image.artist}", maxLines = 3)
                if (image.negativePrompt.isNotBlank()) Text("反向提示词：${image.negativePrompt}", maxLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = { onSaveTags(tags); onDismiss() }) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onToggleFavorite) { Text(if (image.favorite) "取消收藏" else "收藏") }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}
