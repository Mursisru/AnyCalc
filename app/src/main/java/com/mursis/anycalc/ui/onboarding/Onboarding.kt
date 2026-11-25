package com.mursis.anycalc.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mursis.anycalc.R
import androidx.compose.ui.unit.sp
import com.mursis.anycalc.ui.components.CapsuleButton
import com.mursis.anycalc.ui.components.motionMs
import com.mursis.anycalc.ui.theme.BusinessBlack
import com.mursis.anycalc.ui.theme.Champagne
import com.mursis.anycalc.ui.theme.GraphiteLight
import com.mursis.anycalc.ui.theme.HairlineStrong
import com.mursis.anycalc.ui.theme.PrimaryText
import com.mursis.anycalc.ui.theme.SecondaryText

private data class Page(val glyph: String, val title: String, val body: String)

private val PAGES = listOf(
    Page("◎", "Наведите камеру", "AnyCalc распознаёт текст, формулы и условия задач прямо в кадре — без фотографий и лишних шагов."),
    Page("✦", "Получите ответ", "Короткий ответ появляется поверх задачи. Нажмите на него, чтобы открыть пошаговое решение."),
    Page("⌨", "Уточните и пересчитайте", "Измените промпт, выберите провайдера или решите простую задачу локально — без сети."),
    Page("◷", "Всё сохраняется", "История, настройки и ключи хранятся на устройстве. Ключи — в защищённом хранилище."),
)

@Composable
fun Onboarding(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val current = PAGES[page]
    val duration = motionMs(320)

    Box(Modifier.fillMaxSize().background(BusinessBlack)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("ANYCALC", style = MaterialTheme.typography.labelMedium, color = Champagne, letterSpacing = 2.sp)

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(48.dp))
                        .background(GraphiteLight)
                        .padding(1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(47.dp))
                            .background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(HairlineStrong, BusinessBlack))),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = current.glyph,
                            transitionSpec = { fadeIn(androidx.compose.animation.core.tween(duration)) togetherWith fadeOut(androidx.compose.animation.core.tween(duration)) },
                            label = "onboarding-glyph",
                        ) { glyph ->
                            Icon(
                                painter = painterResource(R.drawable.anycalc_brand),
                                contentDescription = "AnyCalc",
                                tint = androidx.compose.ui.graphics.Color.Unspecified,
                                modifier = Modifier.size(120.dp),
                            )
                            if (glyph != "◎") {
                                Text(glyph, style = MaterialTheme.typography.displayLarge, color = Champagne)
                            }
                        }
                    }
                }
            }

            Column {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = { fadeIn(androidx.compose.animation.core.tween(duration)) togetherWith fadeOut(androidx.compose.animation.core.tween(duration)) },
                    label = "onboarding-text",
                ) { index ->
                    Column {
                        Text(PAGES[index].title, style = MaterialTheme.typography.displayMedium, color = PrimaryText)
                        Spacer(Modifier.height(14.dp))
                        Text(PAGES[index].body, style = MaterialTheme.typography.bodyLarge, color = SecondaryText)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(PAGES.size) { index ->
                        Box(
                            Modifier
                                .size(if (index == page) 22.dp else 7.dp, 7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (index == page) Champagne else HairlineStrong)
                                .alpha(if (index == page) 1f else 0.6f),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                CapsuleButton(
                    text = if (page == PAGES.lastIndex) "Начать" else "Далее",
                    onClick = { if (page == PAGES.lastIndex) onDone() else page++ },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
