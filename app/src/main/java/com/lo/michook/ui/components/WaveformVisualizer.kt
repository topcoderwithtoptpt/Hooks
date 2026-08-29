package com.lo.michook.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lo.michook.ui.theme.CyanPrimary
import com.lo.michook.ui.theme.DarkSurfaceElevated
import com.lo.michook.ui.theme.EmeraldSuccess
import com.lo.michook.ui.theme.VioletAccent

@Composable
fun WaveformVisualizer(
    samples: List<Float>,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(DarkSurfaceElevated, RoundedCornerShape(16.dp))
            .border(1.dp, if (isActive) CyanPrimary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            // Background Grid Lines
            val gridColor = Color.White.copy(alpha = 0.05f)
            drawLine(gridColor, Offset(0f, midY), Offset(width, midY), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth = 1.dp.toPx())

            if (!isActive || samples.isEmpty()) {
                // Flat idle carrier line
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                return@Canvas
            }

            // Draw Frequency spectrum bars
            val barCount = samples.size
            val barWidth = (width / barCount) * 0.65f
            val spacing = (width / barCount) * 0.35f

            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    CyanPrimary.copy(alpha = pulseAlpha),
                    VioletAccent.copy(alpha = pulseAlpha * 0.8f),
                    EmeraldSuccess.copy(alpha = pulseAlpha * 0.6f)
                ),
                startY = 0f,
                endY = height
            )

            for (i in samples.indices) {
                val sampleVal = samples[i].coerceIn(0.04f, 1f)
                val barHeight = (height * 0.85f) * sampleVal
                val left = i * (barWidth + spacing)
                val top = midY - (barHeight / 2f)

                drawRoundRect(
                    brush = gradientBrush,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // Draw smooth curve overlay connecting tops
            val path = Path()
            path.moveTo(0f, midY)
            for (i in samples.indices) {
                val sampleVal = samples[i].coerceIn(0.04f, 1f)
                val x = i * (barWidth + spacing) + (barWidth / 2f)
                val y = midY - ((height * 0.42f) * sampleVal)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = CyanPrimary.copy(alpha = 0.9f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
