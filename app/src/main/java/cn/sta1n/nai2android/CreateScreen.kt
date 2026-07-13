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
        Text("åˆ›ä½œä¸€å¼ å›¾", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(
            "é¢„è®¾è´Ÿè´£æä¾›èµ·ç‚¹ï¼Œç”Ÿæˆå‰çš„æ¯ä¸ªå­—æ®µéƒ½å¯ä»¥ç»§ç»­è¿½åŠ æˆ–ä¿®æ”¹ã€‚",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (viewModel.presets.isNotEmpty()) {
            Text("å¿«é€Ÿå¥—ç”¨é¢„è®¾", fontWeight = FontWeight.SemiBold)
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

        FormSectionTitle("01 ç”»é¢æè¿°")
        MultilineField(
            label = "æç¤ºè¯ / NAI tag",
            value = form.prompt,
            onValueChange = { value -> viewModel.updateForm { it.copy(prompt = value) } },
            placeholder = "ä¾‹å¦‚ï¼š1girl, solo, rain, neon street"
        )
        MultilineField(
            label = "å›¾åº“å½’æ¡£ tagï¼ˆé€—å·åˆ†éš”ï¼‰",
            value = form.archiveTags,
            onValueChange = { value -> viewModel.updateForm { it.copy(archiveTags = value) } },
            placeholder = "ä¾‹å¦‚ï¼šé›¨å¤œ, é“¶å‘, èµ›åšè¡—é“"
        )

        FormSectionTitle("02 é£æ ¼æ§åˆ¶")
        MultilineField(
            label = "Artist / è´¨é‡å‰ç¼€",
            value = form.artist,
            onValueChange = { value -> viewModel.updateForm { it.copy(artist = value) } },
            placeholder = "å¯å¡«å†™ artistã€è´¨é‡è¯å’Œé£æ ¼è¯"
        )
        MultilineField(
            label = "åå‘æç¤ºè¯",
            value = form.negativePrompt,
            onValueChange = { value -> viewModel.updateForm { it.copy(negativePrompt = value) } },
            placeholder = "bad hands, blurry, watermark ..."
        )

        FormSectionTitle("03 ç”Ÿæˆå‚æ•°")
        DropdownField(
            label = "ç”»å¹… / åˆ†è¾¨ç‡",
            value = form.size,
            options = listOf("ç«–å›¾", "æ¨ªå›¾", "æ–¹å›¾", "2Kç«–å›¾", "2Kæ¨ªå›¾", "2Kæ–¹å›¾", "4Kç«–å›¾", "4Kæ¨ªå›¾", "4Kæ–¹å›¾"),
            onValueChange = { value -> viewModel.updateForm { it.copy(size = value) } }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                label = "æ­¥æ•°",
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
            label = "é‡‡æ ·å™¨",
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
            Text(if (viewModel.isGenerating) "ç”Ÿæˆä¸­â€¦â€¦" else "ç”Ÿæˆå›¾ç‰‡ï¼ˆ${generationCostForSize(form.size)} ç‚¹ï¼‰")
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
                Text("âŒ„")
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
          -z÷İ­¢G§²ÚîÆ­yÒ&—fFR6ö×æ–öâö&¦V7B°¢6öç7BfÂ$TdU$Tä4U2Ò&æ“&æG&ö–E÷6V7W&U÷6WGF–æw2 ¢6öç7BfÂDô´Tåô´U’Ò&Væ7'—FVEö66W75÷Fö¶Vâ ¢6öç7BfÂ´U•ôÄ”2Ò&æ“&æG&ö–Eö66W75ö¶W’ ¢6öç7BfÂäE$ô”Eô´U•5Dõ$RÒ$æG&ö–D¶W•7F÷&R ¢6öç7BfÂÄtõ$•D„ÒÒ$U2 ¢6öç7BfÂE$å4dõ$ÔD”ôâÒ$U2ôt4ÒôæõFF–ær ¢6öç7BfÂt4ÕõDuô$•E2Ò#€¢Ğ§Ğ ¦6Æ726WGF–æw57F÷&R†6öçFW‡C¢6öçFW‡B’°¢&—fFRfÂ&VfW&Væ6W2Ò6öçFW‡BævWE6†&VE&VfW&Væ6W2…$TdU$Tä4U2Â6öçFW‡BäÔôDUõ$•dDR ¢f"&6UW&Ã¢7G&–æp¢vWB‚’Ò&VfW&Væ6W2ævWE7G&–ær„$4UõU$Åô´U’ÂDTdTÅEô$4UõU$Â’ó¢DTdTÅEô$4UõU$À¢6WB‡fÇVR’°¢&VfW&Væ6W2æVF—B‚’çWE7G&–ær„$4UõU$Åô´U’ÂfÇVRçG&–Ò‚’çG&–ÔVæB‚ròr’’æÇ’‚¢Ğ ¢&—fFR6ö×æ–öâö&¦V7B°¢6öç7BfÂ$TdU$Tä4U2Ò&æ“&æG&ö–Eö÷6WGF–æw2 ¢6öç7BfÂ$4UõU$Åô´U’Ò&&6U÷W&Â ¢Ğ§Ğ  