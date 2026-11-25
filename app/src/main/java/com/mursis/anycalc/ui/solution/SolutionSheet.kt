package com.mursis.anycalc.ui.solution

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mursis.anycalc.model.Solution
import com.mursis.anycalc.model.SolutionSource
import com.mursis.anycalc.ui.components.CapsuleButton
import com.mursis.anycalc.ui.components.GlassCard
import com.mursis.anycalc.ui.components.PromptField
import com.mursis.anycalc.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolutionSheet(
    solution: Solution?,
    prompt: String,
    onPromptChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onRecalculate: () -> Unit,
    onResetPrompt: () -> Unit,
    onSave: () -> Unit,
    reduceMotion: Boolean,
) {
    if (solution == null) return
    var showPrompt by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(40.dp, 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outline),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text("Решение", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(14.dp))
            Text(solution.short, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaChip(solution.provider)
                Spacer(Modifier.width(8.dp))
                MetaChip(
                    when (solution.source) {
                        SolutionSource.LOCAL -> "Локально"
                        SolutionSource.CACHE -> "Из кэша"
                        SolutionSource.AI -> "ИИ"
                    },
                )
                Spacer(Modifier.width(8.dp))
                MetaChip("уверенность ${(solution.confidence * 100).toInt()}%")
            }
            Spacer(Modifier.height(20.dp))

            SectionLabel("Пошаговое решение")
            val steps = solution.detailed.lines().filter { it.isNotBlank() }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                steps.forEachIndexed { index, step ->
                    GlassCard(radius = 20, padding = 16) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.outline),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                step.replace(Regex("^\\d+[.)]\\s*"), ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CapsuleButton(
                    "Уточнить",
                    onClick = { showPrompt = true },
                    modifier = Modifier.weight(1f),
                    primary = false,
                )
                CapsuleButton(
                    "Сохранить",
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    primary = false,
                )
            }
            Spacer(Modifier.height(10.dp))
            CapsuleButton(
                "Изменить промпт и пересчитать",
                onClick = { showPrompt = !showPrompt },
                modifier = Modifier.fillMaxWidth(),
            )

            androidx.compose.animation.AnimatedVisibility(visible = showPrompt) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    SectionLabel("Системный промпт")
                    PromptField(value = prompt, onValueChange = onPromptChange)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CapsuleButton(
                            "Пересчитать",
                            onClick = onRecalculate,
                            modifier = Modifier.weight(1f),
                        )
                        CapsuleButton(
                            "Сбросить",
                            onClick = onResetPrompt,
                            modifier = Modifier.weight(1f),
                            primary = false,
                        )
                    }
                }
            }
            if (reduceMotion) Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
