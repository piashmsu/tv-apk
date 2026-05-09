package com.piashmsu.tvapk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.piashmsu.tvapk.ui.TvApkRoot
import com.piashmsu.tvapk.ui.theme.TvApkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TvApkTheme {
                Surface(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    TvApkRoot()
                }
            }
        }
    }
}
