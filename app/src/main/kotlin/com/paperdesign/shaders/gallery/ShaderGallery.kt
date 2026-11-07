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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
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
import com.paperdesign.shaders.specs.WarpSpec
import com.paperdesign.warp.demo.HeroHomeScreen
import com.paperdesign.warp.demo.HeroThemes
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/** Entry point: a grid of every ported shader; tapping one opens its playground. */
@Composable
fun ShaderGalleryApp(initialShaderId: String? = null) {
    var selectedId by rememberSaveable { mutableStateOf(initialShaderId) }
    if (selectedId == HERO_HOME_ID) {
        BackHandler { selectedId = null }
        HeroHomeScreen()
        return
    }
    if (selectedId == API_DEMO_ID) {
        BackHandler { selectedId = null }
        ApiDemoScreen()
        return
    }
    val selected = ShaderCatalog.all.firstOrNull { it.id == selectedId }
    if (selected == null) {
        GalleryScreen(onOpen = { selectedId = it.id }, onOpenExample = { selectedId = it })
    } else {
        BackHandler { selectedId = null }
        ShaderPlayground(spec = selected, onBack = { selectedId = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryScreen(onOpen: (ShaderSpec) -> Unit, onOpenExample: (String) -> Unit) {
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
            section("Examples", "App screens built on a shader")
            item(key = HERO_HOME_ID) { HeroHomeTile(onClick = { onOpenExample(HERO_HOME_ID) }) }
            item(key = API_DEMO_ID) { ApiDemoTile(onClick = { onOpenExample(API_DEMO_ID) }) }
            section("Interactive · touch and tilt", "Original shaders made for this app")
            items(ShaderCatalog.originals, key = { it.id }) { spec ->
                GalleryTile(spec, onClick = { onOpen(spec) })
            }
            section("Paper shaders", "Ports of paper-design/shaders. Every one supports pan/zoom gestures and tilt parallax.")
            items(ShaderCatalog.paper, key = { it.id }) { spec ->
                GalleryTile(spec, onClick = { onOpen(spec) })
            }
        }
    }
}

private fun LazyGridScope.section(title: String, subtitle: String) {
    item(key = "header-$title", span = { GridItemSpan(maxLineSpan) }) {
        Column(Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/** Thumbnail for the hero home example: a frozen frame of its Warp header. */
@Composable
private fun HeroHomeTile(onClick: () -> Unit) {
    val state = remember { ShaderState(WarpSpec, HeroThemes[0].preset) }
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("tile-$HERO_HOME_ID"),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            val clock = remember { ShaderClock(THUMBNAIL_TIME_MS) }
            PaperShader(state, Modifier.fillMaxSize(), playing = false, clock = clock)
            Text(
                "stride",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Text(
            "Hero home",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
        )
    }
}

/** Thumbnail for the Modifier/View demo: a frozen Swirl frame. */
@Composable
private fun ApiDemoTile(onClick: () -> Unit) {
    val state = remember { ShaderState(ShaderCatalog.all.first { it.id == "swirl" }) }
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("tile-$API_DEMO_ID"),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            val clock = remember { ShaderClock(state.float("frame") + THUMBNAIL_TIME_MS) }
            PaperShader(state, Modifier.fillMaxSize(), playing = false, clock = clock)
            Text(
                "</>",
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Text(
            "Modifier and View",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
        )
    }
}

/** Gallery id (and `--es shader` deep link value) for the hero home example. */
const val HERO_HOME_ID = "hero-home"

private const val THUMBNAIL_TIME_MS = 12_000f
