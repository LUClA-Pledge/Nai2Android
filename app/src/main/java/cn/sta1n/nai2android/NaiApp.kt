package cn.sta1n.nai2android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaiApp(viewModel: NaiViewModel) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nai2Android") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationItem(AppScreen.CREATE, "创作", "✦", viewModel)
                NavigationItem(AppScreen.GALLERY, "图库", "▦", viewModel)
                NavigationItem(AppScreen.PRESETS, "预设", "☷", viewModel)
                NavigationItem(AppScreen.SETTINGS, "设置", "⚙", viewModel)
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
    glyph: String,
    viewModel: NaiViewModel
) {
    NavigationBarItem(
        selected = viewModel.screen == screen,
        onClick = { viewModel.selectScreen(screen) },
        icon = { Text(glyph) },
        label = { Text(label) }
    )
}

