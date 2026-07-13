package cn.sta1n.nai2android

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
fun CreateScreen(viewModel: NaiViewModel, modifier: Modifier = Modifier) {
    val form = viewModel.form
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("创作一张图", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(
            "预设负责提供起点，生成前的每个字段都可以继续追加或修改。",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (viewModel.presets.isNotEmpty()) {
            Text("快速套用预设", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.presets.forEach { preset ->
                    AssistChip(
                        onClick = { viewModel.applyPreset(preset) },
                        label = { Text(preset.name) }
                    )
                }
            }
        }

        FormSectionTitle("01 画面描述")
        MultilineField(
            label = "提示词 / NAI tag",
            value = form.prompt,
            onValueChange = { value -> viewModel.updateForm { it.copy(prompt = value) } },
            placeholder = "例如：1girl, solo, rain, neon street"
        )
        MultilineField(
            label = "图库归档 tag（逗号分隔）",
            value = form.archiveTags,
            onValueChange = { value -> viewModel.updateForm { it.copy(archiveTags = value) } },
            placeholder = "例如：雨夜, 银发, 赛博街道"
        )

        FormSectionTitle("02 风格控制")
        MultilineField(
            label = "Artist / 质量前缀",
            value = form.artist,
            onValueChange = { value -> viewModel.updateForm { it.copy(artist = value) } },
            placeholder = "可填写 artist、质量词和风格词"
        )
        Text(
            "网站 artist 预设",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WEBSITE_ARTIST_PRESETS.forEach { preset ->
                AssistChip(
                    onClick = { viewModel.applyArtistPreset(preset) },
                    label = { Text(preset.label) }
                )
            }
        }
        MultilineField(
            label = "反向提示词",
            value = form.negativePrompt,
            onValueChange = { value -> viewModel.updateForm { it.copy(negativePrompt = value) } },
            placeholder = "bad hands, blurry, watermark ..."
        )

        FormSectionTitle("03 生成参数")
        DropdownField(
            label = "画幅 / 分辨率",
            value = form.size,
            options = listOf("竖图", "横图", "方图", "2K竖图", "2K横图", "2K方图", "4K竖图", "4K横图", "4K方图"),
            onValueChange = { value -> viewModel.updateForm { it.copy(size = value) } }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                label = "步数",
                value = form.steps.toString(),
                onValueChange = { value -> viewModel.updateForm { it.copy(steps = value.toIntOrNull() ?: it.steps) } },
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "Scale",
                value = form.scale.toString(),
                onValueChange = { value -> viewModel.updateForm { it.copy(scale = value.toDoubleOrNull() ?: it.scale) } },
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "CFG",
                value = form.cfg.toString(),
                onValueChange = { value -> viewModel.updateForm { it.copy(cfg = value.toDoubleOrNull() ?: it.cfg) } },
                modifier = Modifier.weight(1f)
            )
        }
        DropdownField(
            label = "采样器",
            value = form.sampler,
            options = listOf(
                "k_dpmpp_2m_sde",
                "k_dpmpp_2m",
                "k_dpmpp_sde",
                "k_dpmpp_2s_ancestral",
                "k_euler_ancestral",
                "k_euler"
            ),
            onValueChange = { value -> viewModel.updateForm { it.copy(sampler = value) } }
        )

        if (viewModel.statusMessage.isNotBlank()) {
            Text(
                text = viewModel.statusMessage,
                color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
        }
        Button(
            onClick = viewModel::generate,
            enabled = !viewModel.isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (viewModel.isGenerating) "生成中……" else "生成图片（${generationCostForSize(form.size)} 点）")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FormSectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
}

@Composable
private fun MultilineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Row {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(value, modifier = Modifier.weight(1f))
                Text("⌄")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
