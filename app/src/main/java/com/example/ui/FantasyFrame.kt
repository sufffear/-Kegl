package com.example.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Quiet bronze frame with small corner ornaments; keeps the content area clear. */
internal fun Modifier.fantasyFrame(color: Color): Modifier = drawWithContent {
    drawContent()
    val inset = 2.dp.toPx()
    if (size.width > inset * 2 && size.height > inset * 2) {
        drawRoundRect(color.copy(alpha = 0.8f), Offset(inset, inset),
            Size(size.width - inset * 2, size.height - inset * 2), CornerRadius(14.dp.toPx()), style = Stroke(1.dp.toPx()))
        val edge = 8.dp.toPx()
        val length = 12.dp.toPx()
        for (x in listOf(edge, size.width - edge)) {
            val direction = if (x < size.width / 2) 1 else -1
            drawLine(color.copy(alpha = 0.65f), Offset(x, edge + length), Offset(x + direction * length, edge), 1.dp.toPx())
            drawCircle(color, 1.5.dp.toPx(), Offset(x + direction * 4.dp.toPx(), edge + 4.dp.toPx()))
        }
    }
}
