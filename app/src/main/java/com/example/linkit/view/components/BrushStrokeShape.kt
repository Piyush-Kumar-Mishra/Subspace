package com.example.linkit.view.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.random.Random

class BrushStrokeShape(
    private val brushSide: Side = Side.Right,
    private val jaggedness: Float = 25f,
    private val cornerRadius: Float = 20f,
    private val variation: BrushVariation = BrushVariation.JAGGED
) : Shape {

    enum class Side { Left, Right }
    enum class BrushVariation { JAGGED, WAVED }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val random = Random(42)
            val r = cornerRadius
            val diameter = r * 2

            if (brushSide == Side.Right) {
                arcTo(
                    rect = Rect(0f, 0f, diameter, diameter),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = true
                )

                lineTo(size.width - jaggedness, 0f)

                var currentY = 0f
                val steps = if (variation == BrushVariation.JAGGED) 15 else 8
                val stepSize = size.height / steps

                while (currentY < size.height) {
                    currentY += stepSize
                    val xVar = size.width - (random.nextFloat() * jaggedness)

                    if (variation == BrushVariation.WAVED) {
                        // Smooth waved brush effect using cubic bezier
                        val prevY = currentY - stepSize
                        cubicTo(
                            x1 = size.width, y1 = prevY + (stepSize / 3),
                            x2 = xVar, y2 = currentY - (stepSize / 3),
                            x3 = xVar, y3 = currentY
                        )
                    } else {
                        lineTo(xVar, currentY)
                    }
                }
                lineTo(r, size.height)

                arcTo(
                    rect = Rect(0f, size.height - diameter, diameter, size.height),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            } else {
                arcTo(
                    rect = Rect(size.width - diameter, 0f, size.width, diameter),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = true
                )

                lineTo(size.width, size.height - r)

                arcTo(
                    rect = Rect(size.width - diameter, size.height - diameter, size.width, size.height),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                lineTo(jaggedness, size.height)
                var currentY = size.height
                val steps = if (variation == BrushVariation.JAGGED) 15 else 8
                val stepSize = size.height / steps

                while (currentY > 0) {
                    currentY -= stepSize
                    val xVar = random.nextFloat() * jaggedness

                    if (variation == BrushVariation.WAVED) {
                        val prevY = currentY + stepSize
                        cubicTo(
                            x1 = 0f, y1 = prevY - (stepSize / 3),
                            x2 = xVar, y2 = currentY + (stepSize / 3),
                            x3 = xVar, y3 = currentY
                        )
                    } else {
                        lineTo(xVar, currentY)
                    }
                }
                close()
            }
        }
        return Outline.Generic(path)
    }
}