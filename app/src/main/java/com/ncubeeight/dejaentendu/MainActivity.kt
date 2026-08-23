package com.ncubeeight.dejaentendu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncubeeight.dejaentendu.ui.theme.AppColors
import com.ncubeeight.dejaentendu.ui.theme.DejaEntenduTheme

/**
 * Empty scaffold — no feature code yet. This exists so the project opens
 * and runs in Android Studio as a starting point; see README.md for what's
 * actually planned (on-device transcription + Gemini Nano/AICore study
 * notes, mirroring the iOS app).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DejaEntenduTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column {
                        TitleBanner()
                        PlaceholderBody()
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(AppColors.headerGradientStart, AppColors.headerGradientEnd)
                )
            )
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Déjà Entendu",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "You heard it before.\nLet's try to remember it.",
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlaceholderBody() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .padding(20.dp),
    ) {
        Text(
            text = "Android scaffold — nothing built yet. See README.md.",
            color = AppColors.inkSoft,
        )
    }
}
