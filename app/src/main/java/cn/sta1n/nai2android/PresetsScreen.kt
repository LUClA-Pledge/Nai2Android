�r�^�f��ئ{^�y�'vî���package cn.sta1n.nai2android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PresetsScreen(viewModel: NaiViewModel, modifier: Modifier = Modifier) {
    var editingPreset by remember { mutableStateOf<Preset?>(null) }

    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("我的预设", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("tag、artist、反向提示词都可以继续追加", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { editingPreset = viewModel.newPreset() }) { Text("新增") }
        }

        if (viewModel.presets.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text("还没有自定义预设")
                Text("新增一个预设后，它会出现在创作页的快速套用区域。")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(viewModel.presets, key = { it.id }) { preset ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(preset.name, fontWeight = FontWeight.Bold)
                                Row {
                                    TextButton(onClick = { editingPreset = preset }) { Text("编辑") }
                                    TextButton(onClick = { viewModel.deletePreset(preset) }) { Text("删除") }
                                }
                            }
                            Text("tag：${preset.tag.ifBlank { "未填写" }}", maxLines = 3)
                            Text("artist：${preset.artist.ifBlank { "未填写" }}", maxLines = 2)
                            Text("反向提示词：${preset.negativePrompt.ifBlank { "未填写" }}", maxLines = 2)
                        }
                    }
                }
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("预设名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("tag / 提示词") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("artist / 质量前缀") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = negative,
                    onValueChange = { negative = it },
                    label = { Text("反向提示词") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("编辑时直接在原内容后继续输入即可追加；应用不会自动替换你的文本。")
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(preset.copy(
                    name = name,
                    tag = tag,
                    artist = artist,
                    negativePrompt = negative,
                    updatedAt = System.currentTimeMillis()
                ))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

