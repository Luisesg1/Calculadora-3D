package com.print3d.calculator.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's one identity motif: faint horizontal "layer lines" like an FDM print's stacked
 * layers. Kept extremely subtle so it reads as texture, never decoration. Reuse this instead
 * of hand-drawing the pattern per surface so every brand moment matches.
 */
fun Modifier.layerLines(
    color: Color,
    gap: Dp = 7.dp,
    alpha: Float = 0.05f,
    strokeWidth: Float = 1f
): Modifier = drawWithContent {
    drawContent()
    val g = gap.toPx()
    val c = color.copy(alpha = alpha)
    var y = g
    while (y < size.height) {
        drawLine(c, Offset(0f, y), Offset(size.width, y), strokeWidth)
        y += g
    }
}
