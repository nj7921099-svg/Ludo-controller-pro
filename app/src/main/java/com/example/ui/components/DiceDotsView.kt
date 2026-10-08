package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Authentic dice-dot pattern representation for numbers 1 to 6.
 * Renders standard dice face dots.
 */
@Composable
fun DiceDots(
    number: Int,
    dotColor: Color,
    modifier: Modifier = Modifier.size(18.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val radius = w * 0.12f

        val left = w * 0.22f
        val midX = w * 0.50f
        val right = w * 0.78f

        val top = h * 0.22f
        val midY = h * 0.50f
        val bottom = h * 0.78f

        when (number) {
            1 -> {
                drawCircle(color = dotColor, radius = radius, center = Offset(midX, midY))
            }
            2 -> {
                drawCircle(color = dotColor, radius = radius, center = Offset(right, top))
                drawCircle(color = dotColor, radius = radius, center = Offset(left, bottom))
            }
            3 -> {
                drawCircle(color = dotColor, radius = radius, center = Offset(right, top))
                drawCircle(color = dotColor, radius = radius, center = Offset(midX, midY))
                drawCircle(color = dotColor, radius = radius, center = Offset(left, bottom))
            }
            4 -> {
                drawCircle(color = dotColor, radius = radius, center = Offset(left, top))
                drawCircle(color = dotColor, radius = radius, center = Offset(right, top))
                drawCircle(color = dotColor, radius = radius, center = Offset(left, bottom))
                drawCircle(color = dotColor, radius = radius, center = Offset(right, bottom))
            }
            5 -> {
                drawCircle(color = dotColor, radius = radius, center = Offset(left, top))
                drawCircle(color = dotColor, radius = radius, center = Offset(right, top))
                drawCircle(color = dotColor, radius = radius, center = Offset(midX, midY))
                drawCircle(color = dotColor, radius = radius, center = Offset(left, bottom))
                drawCircle(color = dotColor, radius = radius, center = Offset(right, bottom))
            }
            6 -> {
                drawCircle(color = dotColor, radius = radius, center = Offset(left, top))
                drawCircle(color = dotColor, radius = radius, center = Offset(right, top))
                drawCircle(color = dotColor, radius = radius, center = Offset(left, midY))
                drawCircle(color = dotColor, radius = radius, center = Offset(right, midY))
                drawCircle(color = dotColor, radius = radius, center = Offset(left, bottom))
                drawCircle(color = dotColor, radius = radius, center = Offset(right, bottom))
            }
        }
    }
}
