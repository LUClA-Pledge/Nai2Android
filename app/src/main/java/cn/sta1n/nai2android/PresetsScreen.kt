package cn.sta1n.nai2android

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
                Text("鎴戠殑棰勮", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("tag銆乤rtist銆佸弽鍚戞彁绀鸿瘝閮藉彲浠ョ户缁拷鍔?, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { editingPreset = viewModel.newPreset() }) { Text("鏂板") }
        }

        if (viewModel.presets.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text("杩樻病鏈夎嚜瀹氫箟棰勮")
                Text("鏂板涓€涓璁惧悗锛屽畠浼氬嚭鐜板湪鍒涗綔椤电殑蹇€熷鐢ㄥ尯鍩熴€?)
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
                                    TextButton(onClick = { editingPreset = preset }) { Text("缂栬緫") }
                                    TextButton(onClick = { viewModel.deletePreset(preset) }) { Text("鍒犻櫎") }
                                }
                            }
                            Text("tag锛?{preset.tag.ifBlank { "鏈～鍐? }}", maxLines = 3)
                            Text("artist锛?{preset.artist.ifBlank { "鏈～鍐? }}", maxLines = 2)
                            Text("鍙嶅悜鎻愮ず璇嶏細${preset.negativePrompt.ifBlank { "鏈～鍐? }}", maxLines = 2)
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
        title = { Text(if (preset.name.isBlank()) "鏂板棰勮" else "缂栬緫棰勮") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("棰勮鍚嶇О") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("tag / 鎻愮ず璇?) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("artist / 璐ㄩ噺鍓嶇紑") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = negative,
                    onValueChange = { negative = it },
                    label = { Text("鍙嶅悜鎻愮ず璇?) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("缂栬緫鏃剁洿鎺ュ湪鍘熷唴瀹瑰悗缁х画杈撳叆鍗冲彲杩藉姞锛涘簲鐢ㄤ笉浼氳嚜鍔ㄦ浛鎹綘鐨勬枃鏈€?)
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
            }) { Text("淇濆瓨") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("鍙栨秷") } }
    )
}


