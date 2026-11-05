package com.example.warpshader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.warpshader.ui.theme.WarpShaderTheme
import com.paperdesign.shaders.ShaderAssets
import com.paperdesign.shaders.gallery.ShaderGalleryApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ShaderAssets.init(this)
        setContent {
            WarpShaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // `adb shell am start -n com.paperdesign.warp/com.example.warpshader.MainActivity --es shader warp`
                    ShaderGalleryApp(initialShaderId = intent.getStringExtra("shader"))
                }
            }
        }
    }
}
