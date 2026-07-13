package cn.sta1n.nai2android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaiApp(viewModel: NaiViewModel) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Surface(
                        modifier = Modifier.padding(start = 16.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                },
                title = {
                    Text(
                        screenTitle(viewModel.screen),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                },
                actions = {
                    if (viewModel.balance != null) {
                        Text(
                            "${viewModel.balance} 点",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationItem(AppScreen.CREATE, "创作", Icons.Filled.AutoAwesome, viewModel, Modifier.weight(1f))
                NavigationItem(AppScreen.GALLERY, "图库", Icons.Filled.Collections, viewModel, Modifier.weight(1f))
                NavigationItem(AppScreen.PRESETS, "预设", Icons.Filled.Tune, viewModel, Modifier.weight(1f))
                NavigationItem(AppScreen.SETTINGS, "设置", Icons.Filled.Settings, viewModel, Modifier.weight(1f))
            }
        }
    ) { paddingValues ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        when (viewModel.screen) {
            AppScreen.CREATE -> CreateScreen(viewModel, contentModifier)
            AppScreen.GALLERY -> GalleryScreen(viewModel, contentModifier)
            AppScreen.PRESETS -> PresetsScreen(viewModel, contentModifier)
            AppScreen.SETTINGS -> SettingsScreen(viewModel, contentModifier)
        }
    }
}

@Composable
private fun NavigationItem(
    screen: AppScreen,
    label: String,
    icon: ImageVector,
    viewModel: NaiViewModel,
    modifier: Modifier
) {
    val selected = viewModel.screen == screen
    Surface(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .height(58.dp)
            .clickable { viewModel.selectScreen(screen) },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) {
                    androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                label,
                color = if (selected) {
                    androidx.compose.material3.MaterialTheme.colorScheme.primary
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 11.sp
            )
        )
    }
}

private fun screenTitle(screen: AppScreen): String = when (screen) {
    AppScreen.CREATE -> "Nai2 Studio"
    AppScreen.GALLERY -> "应用图库"
    AppScreen.PRESETS -> "我的预设"
    AppScreen.SETTINGS -> "连接设置"
}
