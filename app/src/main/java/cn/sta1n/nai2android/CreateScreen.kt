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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.layout.width

@Composable
fun CreateScreen(viewModel: NaiViewModel, modifier: Modifier = Modifier) {
    val form = viewModel.form
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(
            kicker = "CREATE / NAI2 API",
            title = "创作一张图",
            subtitle = "把想法交给模型，细节可以在生成前继续调整。"
        )

        if (viewModel.presets.isNotEmpty()) {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("我的快捷预设", fontWeight = FontWeight.Bold)
                        Text(
                            "一键带入常用的 tag、artist 和反向提示词",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        "${viewModel.presets.size} 个",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.presets.forEach { preset ->
                        FilterChip(
                            selected = form.presetName == preset.name,
                            onClick = { viewModel.applyPreset(preset) },
                            label = { Text(preset.name) }
                        )
                    }
                }
            }
        }

        StudioCard {
            StudioSectionHeader("01", "画面描述", "先写清楚你想看到什么")
            MultilineField(
                label = "提示词 / NAI tag",
                value = form.prompt,
                onValueChange = { value -> viewModel.updateForm { it.copy(prompt = value) } },
                placeholder = "1girl, solo, rain, neon street",
                minLines = 5
            )
            FieldHint("这个内容会直接发送给 Nai2API，逗号和换行都可以继续追加。")
            MultilineField(
                label = "图库归档 tag",
                value = form.archiveTags,
                onValueChange = { value -> viewModel.updateForm { it.copy(archiveTags = value) } },
                placeholder = "雨夜, 银发, 赛博街道",
                minLines = 2
            )
            FieldHint("只用于 App 内分类和筛选，不会改变生图提示词。")
        }

        StudioCard {
            StudioSectionHeader("02", "风格控制", "选择一个起点，也可以继续手改")
            MultilineField(
                label = "Artist / 质量前缀",
                value = form.artist,
                onValueChange = { value -> viewModel.updateForm { it.copy(artist = value) } },
                placeholder = "artist、质量词和风格词",
                minLines = 4
            )
            Text(
                "网站 artist 预设",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WEBSITE_ARTIST_PRESETS.forEach { preset ->
                    val selected = findWebsiteArtistPreset(form.artist)?.id == preset.id
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.applyArtistPreset(preset) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        label = { Text(preset.label) }
                    )
                }
            }
            MultilineField(
                label = "反向提示词",
                value = form.negativePrompt,
                onValueChange = { value -> viewModel.updateForm { it.copy(negativePrompt = value) } },
                placeholder = "bad hands, blurry, watermark ...",
                minLines = 4
            )
        }

        StudioCard {
            StudioSectionHeader("03", "生成参数", "画幅越大，消耗的额度越高")
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
        }

        if (viewModel.statusMessage.isNotBlank()) {
            StudioCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        viewModel.statusMessage,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Button(
            onClick = viewModel::generate,
            enabled = !viewModel.isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (viewModel.isGenerating) "正在生成……" else "生成图片 · ${generationCostForSize(form.size)} 点",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MultilineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        minLines = minLines,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FieldHint(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
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
        shape = MaterialTheme.shapes.small,
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text(value, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "展开选项")
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
