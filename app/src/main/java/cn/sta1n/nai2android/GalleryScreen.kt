package cn.sta1n.nai2android

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withContext

@Composable
fun GalleryScreen(viewModel: NaiViewModel, modifier: Modifier = Modifier) {
    var selectedImage by remember { mutableStateOf<ImageRecord?>(null) }
    val resolver = LocalContext.current.contentResolver

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                ScreenHeader(
                    modifier = Modifier.weight(1f),
                    kicker = "LIBRARY / LOCAL FIRST",
                    title = "应用图库",
                    subtitle = "生成结果先在这里整理，再决定哪些导出到系统相册。"
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            viewModel.galleryImages.size.toString(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "张图片",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = viewModel.sortOrder == SortOrder.NEWEST_FIRST,
                        onClick = { viewModel.updateSortOrder(SortOrder.NEWEST_FIRST) },
                        label = { Text("最新在前") }
                    )
                }
                item {
                    FilterChip(
                        selected = viewModel.sortOrder == SortOrder.OLDEST_FIRST,
                        onClick = { viewModel.updateSortOrder(SortOrder.OLDEST_FIRST) },
                        label = { Text("最早在前") }
                    )
                }
                item {
                    FilterChip(
                        selected = viewModel.favoriteOnly,
                        onClick = { viewModel.updateFavoriteOnly(!viewModel.favoriteOnly) },
                        leadingIcon = {
                            Icon(
                                if (viewModel.favoriteOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null
                            )
                        },
                        label = { Text("只看收藏") }
                    )
                }
            }
        }

        if (viewModel.availableArchiveTags.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "按归档 tag 浏览",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = viewModel.selectedArchiveTag == null,
                                onClick = { viewModel.setArchiveTagFilter(null) },
                                label = { Text("全部") }
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
            }
        }

        if (viewModel.galleryImages.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyGalleryState()
                }
            }
        } else {
            gridItems(viewModel.galleryImages, key = { it.id }) { image ->
                GalleryTile(
                    image = image,
                    resolver = resolver,
                    onClick = { selectedImage = image },
                    onToggleFavorite = { viewModel.toggleFavorite(image) }
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(8.dp)) }
    }

    selectedImage?.let { image ->
        ImageDetailDialog(
            image = image,
            resolver = resolver,
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
private fun EmptyGalleryState() {
    StudioCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Collections,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text("图库还没有图片", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(
                "完成一次生成后，图片会先进入应用图库。打开详情，可以单独收藏、补充 tag 或导出到系统相册。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun GalleryTile(
    image: ImageRecord,
    resolver: ContentResolver,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LocalImage(
                    uri = image.localUri,
                    resolver = resolver,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.82f)
                ) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (image.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (image.favorite) "取消收藏" else "收藏",
                            tint = if (image.favorite) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    image.archiveTags.firstOrNull() ?: "未分类",
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (image.isSavedToSystemGallery()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        if (image.isSavedToSystemGallery()) "已导出到系统相册" else "仅存应用图库",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalImage(
    uri: String,
    resolver: ContentResolver,
    modifier: Modifier = Modifier,
    preserveAspectRatio: Boolean = false
) {
    val thumbnail = !preserveAspectRatio
    val cacheKey = if (thumbnail) "thumbnail:$uri" else "full:$uri"
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = cacheKey) {
        val cached = if (thumbnail) GalleryThumbnailCache.get(uri) else null
        value = cached ?: withContext(galleryImageDecodeDispatcher) {
            runCatching {
                decodeGalleryBitmap(
                    uri = Uri.parse(uri),
                    resolver = resolver,
                    maxDimension = GALLERY_THUMBNAIL_MAX_DIMENSION.takeIf { thumbnail }
                )
            }.getOrNull()
        }.also { decoded ->
            if (thumbnail && decoded != null) {
                GalleryThumbnailCache.put(uri, decoded)
            }
        }
    }
    if (bitmap != null) {
        if (preserveAspectRatio) {
            BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
                val ratio = bitmap!!.width.toFloat() / bitmap!!.height.coerceAtLeast(1)
                val imageHeight = (maxWidth / ratio).coerceIn(160.dp, 420.dp)
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "生成图片",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                )
            }
        } else {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "生成图片",
                contentScale = ContentScale.Fit,
                modifier = modifier
            )
        }
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("读取中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("图片详情", fontWeight = FontWeight.Bold)
                if (image.favorite) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "已收藏",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LocalImage(
                    uri = image.localUri,
                    resolver = resolver,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    preserveAspectRatio = true
                )
                if (image.isSavedToSystemGallery()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "已导出到系统相册 Pictures/Nai2API",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onSaveToDevice,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("导出到系统相册")
                    }
                }
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("归档 tag") },
                    placeholder = { Text("多个 tag 用逗号分隔") },
                    minLines = 2,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                DetailText("Prompt", image.prompt)
                if (image.artist.isNotBlank()) DetailText("Artist", image.artist)
                if (image.negativePrompt.isNotBlank()) DetailText("反向提示词", image.negativePrompt)
            }
        },
        confirmButton = {
            Button(onClick = { onSaveTags(tags); onDismiss() }) { Text("保存 tag") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onToggleFavorite) {
                    Text(if (image.favorite) "取消收藏" else "收藏")
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

@Composable
private fun DetailText(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, maxLines = 6, fontSize = 13.sp)
    }
}
