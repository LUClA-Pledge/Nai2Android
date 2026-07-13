package cn.sta1n.nai2android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(viewModel: NaiViewModel, modifier: Modifier = Modifier) {
    var url by remember { mutableStateOf(viewModel.baseUrl) }
    var token by remember { mutableStateOf(viewModel.accessToken) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("连接设置", fontSize = 24.sp)
        Text(
            "访问密钥只保存在本机的 Android Keystore 加密存储中。请把它当作余额凭证，不要放进截图或代码仓库。",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Nai2API 服务地址") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("STA1N 访问密钥") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.saveConnection(url, token) }) { Text("保存并连接") }
            TextButton(onClick = viewModel::refreshBalance) { Text("刷新额度") }
        }
        Text(
            text = viewModel.balance?.let { "当前额度：$it 点" } ?: "当前未连接",
            fontSize = 18.sp
        )
        if (viewModel.statusMessage.isNotBlank()) {
            Text(viewModel.statusMessage, color = MaterialTheme.colorScheme.secondary)
        }
        Text("当前默认模型：${viewModel.serviceSettings?.defaultModel ?: "nai-diffusion-4-5-full"}")
        Text("生成图片会自动保存到系统相册的 Pictures/Nai2API 文件夹。")
    }
}

