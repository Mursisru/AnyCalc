package com.mursis.anycalc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * Растворение контента только у тех краёв, где реально есть прокрутка.
 * Оверлей размыт на 20 dp, чтобы не было резкой цветовой полосы.
 */
@Composable
fun ScrollEdgeFade(
    modifier: Modifier = Modifier,
    canScrollUp: Boolean = true,
    canScrollDown: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        content()
        val bg = MaterialTheme.colorScheme.background
        if (canScrollUp) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(24.dp)
                    .blur(20.dp)
                    .background(Brush.verticalGradient(listOf(bg, Color.Transparent))),
            )
        }
        if (canScrollDown) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(24.dp)
                    .blur(20.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, bg))),
            )
        }
    }
}
