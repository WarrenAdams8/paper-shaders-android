package com.paperdesign.shaders.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.paperdesign.shaders.PaperShader
import com.paperdesign.shaders.ShaderClock
import com.paperdesign.shaders.ShaderSpec
import com.paperdesign.shaders.ShaderState
import com.paperdesign.shaders.specs.ShaderCatalog

/** Entry point: a grid of every ported shader; tapping one opens its playground. */
@Composable
fun ShaderGalleryApp(initialShaderId: String? = null) {
    var selectedId by rememberSaveable { mutableStateOf(initialShaderId) }
    val selected = ShaderCatalog.all.firstOrNull { it.id == selectedId }
    if (selected == null) {
        GalleryScreen(onOpen = { selectedId = it.id })
    } else {
        BackHandler { selectedId = null }
        ShaderPlayground(spec = selected, onBack = { selectedId = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryScreen(onOpen: (ShaderSpec) -> Unit) {
    val scroll = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Paper Shaders") },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("gallery"),
        ) {
            items(ShaderCatalog.all, key = { it.id }) { spec ->
                GalleryTile(spec, onClick = { onOpen(spec) })
            }
        }
    }
}

@Composable
private fun GalleryTile(spec: ShaderSpec, onClick: () -> Unit) {
    // Thumbnails are static frames: one draw per tile instead of 30 animations.
    val state = remember(spec) { ShaderState(spec) }
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("tile-${spec.id}"),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            // A few seconds in, so shaders that start empty (e.g. Color Panels) show something.
            val clock = remember(spec) { ShaderClock(state.float("frame") + THUMBNAIL_TIME_MS) }
            PaperShader(state, Modifier.fillMaxSize(), playing = false, clock = clock)
        }
        Text(
            spec.title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
        )
    }
}

private const val THUMBNAIL_TIME_MS = 12_000f
