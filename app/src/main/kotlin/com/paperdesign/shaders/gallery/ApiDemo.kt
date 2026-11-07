package com.paperdesign.shaders.gallery

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.warpshader.R
import com.paperdesign.shaders.ShaderState
import com.paperdesign.shaders.paperShader
import com.paperdesign.shaders.specs.ShaderCatalog

/** Gallery id (and `--es shader` deep link value) for the API demo. */
const val API_DEMO_ID = "api-demo"

/** Shows the three ways to use any catalog shader: composable, Modifier and classic View. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiDemoScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Modifier and View") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag(API_DEMO_ID),
        ) {
            Label("Modifier.paperShader(state) behind content")
            val swirl = remember { ShaderState(ShaderCatalog.all.first { it.id == "swirl" }) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .paperShader(swirl),
                contentAlignment = Alignment.Center,
            ) {
                Text("Any composable", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            }
            Label("PaperShaderView from XML (app:shader=\"…\")")
            AndroidView(
                factory = { ctx -> FrameLayout(ctx).also { LayoutInflater.from(ctx).inflate(R.layout.paper_shader_views_demo, it, true) } },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}
