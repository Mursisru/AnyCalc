package com.mursis.anycalc.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mursis.anycalc.data.ScanEntity
import com.mursis.anycalc.ui.components.CapsuleButton
import com.mursis.anycalc.ui.components.GlassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryRoute(onBack: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("История", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                CapsuleButton("Назад", onClick = onBack, primary = false, modifier = Modifier.width(110.dp))
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQuery,
                placeholder = { Text("Поиск по задачам и ответам", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    cursorColor = MaterialTheme.colorScheme.secondary,
                ),
            )
            Spacer(Modifier.height(14.dp))
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("◇", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(10.dp))
                    Text("Здесь появятся решенные задачи", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            com.mursis.anycalc.ui.components.ScrollEdgeFade(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 0.dp, 20.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    SwipeItem(item = item, onDelete = { viewModel.delete(item) })
                }
                item {
                    Spacer(Modifier.height(6.dp))
                    CapsuleButton("Очистить историю", onClick = viewModel::clear, primary = false, modifier = Modifier.fillMaxWidth())
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeItem(item: ScanEntity, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.22f)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Удалить", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 24.dp))
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        GlassCard(radius = 24, padding = 16, tint = MaterialTheme.colorScheme.surface) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                            ),
                        ),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.text.replace('\n', ' '),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(item.short, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${formatDate(item.createdAt)} · ${item.provider}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun formatDate(ts: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(ts))
