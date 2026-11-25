package com.mursis.anycalc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/** Плавное «профессиональное» растворение контента у верхней и нижней кромки скролла. */
@Composable
fun ScrollEdgeFade(
    modifier: Modifier = Modifier,
    top: Boolean = true,
    bottom: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        content()
        val bg = MaterialTheme.colorScheme.background
        if (top) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Brush.verticalGradient(listOf(bg, Color.Transparent))),
            )
        }
        if (bottom) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, bg))),
            )
        }
    }
}
