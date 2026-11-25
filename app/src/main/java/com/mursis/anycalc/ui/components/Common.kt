package com.mursis.anycalc.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mursis.anycalc.ui.theme.Champagne
import com.mursis.anycalc.ui.theme.Glass
import com.mursis.anycalc.ui.theme.Graphite
import com.mursis.anycalc.ui.theme.GraphiteLight
import com.mursis.anycalc.ui.theme.Hairline
import com.mursis.anycalc.ui.theme.HairlineStrong
import com.mursis.anycalc.ui.theme.LocalReduceMotion
import com.mursis.anycalc.ui.theme.Platinum
import com.mursis.anycalc.ui.theme.PrimaryText
import com.mursis.anycalc.ui.theme.SecondaryText
import com.mursis.anycalc.ui.theme.Success

@Composable
fun motionMs(value: Int): Int = if (LocalReduceMotion.current) 0 else value

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Int = 24,
    padding: Int = 18,
    tint: Color = Glass,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = tint,
        shape = RoundedCornerShape(radius.dp),
        border = BorderStroke(1.dp, Hairline),
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(padding.dp), content = content)
    }
}

@Composable
fun CapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    leading: String? = null,
) {
    val shape = RoundedCornerShape(50)
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(52.dp),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Platinum,
                contentColor = com.mursis.anycalc.ui.theme.BusinessBlack,
                disabledContainerColor = GraphiteLight,
                disabledContentColor = SecondaryText,
            ),
        ) {
            if (leading != null) {
                Text(leading, fontFamily = FontFamily.Default)
                Spacer(Modifier.width(8.dp))
            }
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(52.dp),
            shape = shape,
            border = BorderStroke(1.dp, HairlineStrong),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PrimaryText,
                disabledContentColor = SecondaryText,
            ),
        ) {
            if (leading != null) {
                Text(leading)
                Spacer(Modifier.width(8.dp))
            }
            Text(text)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        color = SecondaryText,
        modifier = modifier.padding(start = 4.dp, bottom = 10.dp),
    )
}

@Composable
fun FloatingCapsule(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Glass.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, if (isError) com.mursis.anycalc.ui.theme.Danger.copy(alpha = 0.5f) else HairlineStrong),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                color = if (isError) com.mursis.anycalc.ui.theme.Danger else PrimaryText,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun ShimmerLines(lines: Int = 3) {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "shimmer-alpha",
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(lines) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == lines - 1) 0.6f else 1f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                GraphiteLight.copy(alpha = if (reduce) 0.7f else alpha),
                                HairlineStrong.copy(alpha = if (reduce) 0.7f else alpha),
                                GraphiteLight.copy(alpha = if (reduce) 0.7f else alpha),
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
fun StatusDot(active: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size6()
                .clip(RoundedCornerShape(50))
                .background(if (active) Success else SecondaryText.copy(alpha = 0.5f)),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = SecondaryText)
    }
}

private fun Modifier.size6(): Modifier = this.then(Modifier.width(6.dp).height(6.dp))

@Composable
fun PromptField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        minLines = 5,
        maxLines = 10,
        textStyle = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PrimaryText,
            unfocusedTextColor = SecondaryText,
            focusedBorderColor = HairlineStrong,
            unfocusedBorderColor = Hairline,
            focusedContainerColor = Graphite,
            unfocusedContainerColor = Graphite,
            cursorColor = Platinum,
        ),
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
    )
}

@Composable
fun KeyField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PrimaryText,
            unfocusedTextColor = SecondaryText,
            focusedBorderColor = HairlineStrong,
            unfocusedBorderColor = Hairline,
            focusedContainerColor = Graphite,
            unfocusedContainerColor = Graphite,
            focusedLabelColor = Champagne,
            cursorColor = Platinum,
        ),
        shape = RoundedCornerShape(18.dp),
    )
}

@Composable
fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = visualTransformation,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PrimaryText,
            unfocusedTextColor = SecondaryText,
            focusedBorderColor = HairlineStrong,
            unfocusedBorderColor = Hairline,
            focusedContainerColor = Graphite,
            unfocusedContainerColor = Graphite,
            focusedLabelColor = Champagne,
            cursorColor = Platinum,
        ),
        shape = RoundedCornerShape(18.dp),
    )
}
