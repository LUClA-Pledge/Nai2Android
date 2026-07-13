package cn.sta1n.nai2android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PresetsScreen(viewModel: NaiViewModel, modifier: Modifier = Modifier) {
    var editingPreset by remember { mutableStateOf<Preset?>(null) }

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            ScreenHeader(
                modifier = Modifier.weight(1f),
                kicker = "PRESETS / WORKFLOW",
                title = "我的预设",
                subtitle = "把常用的 tag、artist 和反向提示词收进自己的工作流。"
            )
            Button(
                onClick = { editingPreset = viewModel.newPreset() },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("新增")
            }
        }

        if (viewModel.presets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyPresetState(onCreate = { editingPreset = viewModel.newPreset() })
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.presets, key = { it.id }) { preset ->
                    PresetCard(
                        preset = preset,
                        onEdit = { editingPreset = preset },
                        onDelete = { viewModel.deletePreset(preset) }
                    )
                }
                item { Spacer(Modifier.size(8.dp)) }
            }
        }
    }

    editingPreset?.let { preset ->
        PresetEditorDialog(
            preset = preset,
            onDismiss = { editingPreset = null },
            onSave = {
                viewModel.savePreset(it)
                editingPreset = null
            }
        )
    }
}

@Composable
private fun EmptyPresetState(onCreate: () -> Unit) {
    StudioCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Text("还没有自定义预设", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(
                "新建后，预设会出现在创作页顶部。内容可以随时继续追加，不会替换你手动输入的文字。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Button(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("创建第一个预设")
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: Preset,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    StudioCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(preset.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "自定义工作流",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑预设")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "删除预设",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        PresetPreview(label = "TAG", value = preset.tag)
        PresetPreview(label = "ARTIST", value = preset.artist)
        PresetPreview(label = "NEGATIVE", value = preset.negativePrompt)
    }
}

@Composable
private fun PresetPreview(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            label,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp
        )
        Text(
            value.ifBlank { "未设置" },
            color = if (value.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = 13.sp,
            maxLines = 3
        )
    }
}

@Composable
private fun PresetEditorDialog(
    preset: Preset,
    onDismiss: () -> Unit,
    onSave: (Preset) -> Unit
) {
    var name by remember(preset.id) { mutableStateOf(preset.name) }
    var tag by remember(preset.id) { mutableStateOf(preset.tag) }
    var artist by remember(preset.id) { mutableStateOf(preset.artist) }
    var negative by remember(preset.id) { mutableStateOf(preset.negativePrompt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preset.name.isBlank()) "新增预设" else "编辑预设") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("预设名称") },
                    placeholder = { Text("例如：夜景赛博") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("tag / 提示词") },
                    placeholder = { Text("多个 tag 可以用逗号分隔") },
                    minLines = 3,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("artist / 质量前缀") },
                    placeholder = { Text("可从创作页的网站预设复制后继续追加") },
                    minLines = 3,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = negative,
                    onValueChange = { negative = it },
                    label = { Text("反向提示词") },
                    placeholder = { Text("bad hands, blurry, watermark …") },
                    minLines = 3,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "保存后可以在创作页一键套用，套用之后仍然可以继续编辑每个字段。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    preset.copy(
                        name = name,
                        tag = tag,
                        artist = artist,
                        negativePrompt = negative,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }) { Text("保存预设") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
