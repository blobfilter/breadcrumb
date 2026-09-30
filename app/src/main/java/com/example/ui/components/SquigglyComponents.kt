package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CrustBrown
import com.example.ui.theme.MascotBlushPink
import com.example.ui.theme.MascotCream
import com.example.ui.theme.MascotEyeNavy
import com.example.ui.theme.MascotSmileSlate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders the Breadcrumb Mascot from the SVG with a polished,
 * corner-damped hand-inked outline:
 * - Face: rect x=14, y=14, width=100, height=100, rx=24, fill=#F7F3E8, stroke=#8D675A, stroke-width=8
 * - Eyes: ellipse cx=48, cy=58, rx=5, ry=8 & cx=80, cy=58, rx=5, ry=8, fill=#1E2742
 * - Blush: ellipse cx=36, cy=78, rx=10, ry=6 & cx=92, cy=78, rx=10, ry=6, fill=#F6B1B1
 * - Smile: path M48 84 Q64 98 80 84, stroke=#55555F, stroke-width=6, stroke-linecap=round
 */
@Composable
fun BreadcrumbMascot(
    modifier: Modifier = Modifier.size(38.dp),
    squigglyOutline: Boolean = true
) {
    Canvas(modifier = modifier) {
        val scaleX = size.width / 128f
        val scaleY = size.height / 128f
        val scale = min(scaleX, scaleY)

        val faceLeft = 14f * scaleX
        val faceTop = 14f * scaleY
        val faceW = 100f * scaleX
        val faceH = 100f * scaleY
        val rx = 24f * scale

        // 1. Face fill
        drawRoundRect(
            color = MascotCream,
            topLeft = Offset(faceLeft, faceTop),
            size = Size(faceW, faceH),
            cornerRadius = CornerRadius(rx, rx)
        )

        // 2. Face stroke (polished hand-drawn outline with secondary ink pass)
        if (squigglyOutline) {
            val primaryPath = buildSquigglyRoundRectPath(
                left = faceLeft,
                top = faceTop,
                width = faceW,
                height = faceH,
                cornerRadius = rx,
                wavelengthPx = max(28f, 64f * scale),
                amplitudePx = 0.85f * scale,
                seed = 1.2f,
                rawValues = true
            )
            val secondaryPath = buildSquigglyRoundRectPath(
                left = faceLeft,
                top = faceTop,
                width = faceW,
                height = faceH,
                cornerRadius = rx,
                wavelengthPx = max(32f, 76f * scale),
                amplitudePx = 0.55f * scale,
                seed = 3.6f,
                rawValues = true
            )
            drawPath(
                path = primaryPath,
                color = CrustBrown,
                style = Stroke(
                    width = 7.8f * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            drawPath(
                path = secondaryPath,
                color = CrustBrown.copy(alpha = 0.32f),
                style = Stroke(
                    width = 3.5f * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        } else {
            drawRoundRect(
                color = CrustBrown,
                topLeft = Offset(faceLeft, faceTop),
                size = Size(faceW, faceH),
                cornerRadius = CornerRadius(rx, rx),
                style = Stroke(width = 8f * scale)
            )
        }

        // 3. Eyes: cx=48, cy=58, rx=5, ry=8 and cx=80, cy=58, rx=5, ry=8
        val eyeRx = 5f * scaleX
        val eyeRy = 8f * scaleY
        drawOval(
            color = MascotEyeNavy,
            topLeft = Offset(48f * scaleX - eyeRx, 58f * scaleY - eyeRy),
            size = Size(eyeRx * 2f, eyeRy * 2f)
        )
        drawOval(
            color = MascotEyeNavy,
            topLeft = Offset(80f * scaleX - eyeRx, 58f * scaleY - eyeRy),
            size = Size(eyeRx * 2f, eyeRy * 2f)
        )

        // 4. Blush: cx=36, cy=78, rx=10, ry=6 and cx=92, cy=78, rx=10, ry=6
        val blushRx = 10f * scaleX
        val blushRy = 6f * scaleY
        drawOval(
            color = MascotBlushPink,
            topLeft = Offset(36f * scaleX - blushRx, 78f * scaleY - blushRy),
            size = Size(blushRx * 2f, blushRy * 2f)
        )
        drawOval(
            color = MascotBlushPink,
            topLeft = Offset(92f * scaleX - blushRx, 78f * scaleY - blushRy),
            size = Size(blushRx * 2f, blushRy * 2f)
        )

        // 5. Smile: M48 84 Q64 98 80 84
        val smilePath = Path().apply {
            moveTo(48f * scaleX, 84f * scaleY)
            quadraticTo(
                64f * scaleX,
                98f * scaleY,
                80f * scaleX,
                84f * scaleY
            )
        }
        drawPath(
            path = smilePath,
            color = MascotSmileSlate,
            style = Stroke(
                width = 6f * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Hand-drawn Crown icon matching the top-right Upgrade action
 */
@Composable
fun CrownIcon(
    modifier: Modifier = Modifier.size(22.dp),
    color: Color = CrustBrown
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = max(2f, w * 0.095f)

        val crownPath = Path().apply {
            moveTo(w * 0.18f, h * 0.72f)
            lineTo(w * 0.12f, h * 0.30f)
            lineTo(w * 0.36f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.22f)
            lineTo(w * 0.64f, h * 0.50f)
            lineTo(w * 0.88f, h * 0.30f)
            lineTo(w * 0.82f, h * 0.72f)
            close()
        }
        drawPath(
            path = crownPath,
            color = color,
            style = Stroke(
                width = strokeW,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Base underline
        drawLine(
            color = color,
            start = Offset(w * 0.18f, h * 0.86f),
            end = Offset(w * 0.82f, h * 0.86f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Polished hand-drawn horizontal divider with organic low-frequency pen drift,
 * edge tapering, and a delicate secondary ink pass.
 */
@Composable
fun SquigglyDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    strokeWidth: Dp = 1.25.dp,
    wavelength: Dp = 76.dp,
    amplitude: Dp = 0.55.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
    ) {
        val primaryPath = Path()
        val secondaryPath = Path()

        // Enforce a long, graceful hand-drawn wavelength even if a smaller value was passed
        val wavePx = max(wavelength.toPx(), 64.dp.toPx())
        val ampPx = min(amplitude.toPx(), 0.65.dp.toPx())
        val midY = size.height / 2f
        val steps = (size.width / 8f).toInt().coerceAtLeast(16)
        val dx = size.width / steps

        var prevX1 = 0f
        var prevY1 = midY
        var prevX2 = 0f
        var prevY2 = midY

        for (i in 0..steps) {
            val x = i * dx
            val t = i.toFloat() / steps.toFloat()
            // Smooth edge envelope so the ends of the divider settle naturally
            val edgeEnvelope = sin(t * PI.toFloat()).coerceIn(0.15f, 1f)

            val angle = (x / wavePx) * (2f * PI.toFloat())
            val y1 = midY + ampPx * edgeEnvelope * (
                0.58f * sin(angle + 0.4f) +
                    0.28f * cos(angle * 1.7f + 1.3f) +
                    0.14f * sin(angle * 2.9f + 2.5f)
                )
            val y2 = midY + (ampPx * 0.62f * edgeEnvelope) * (
                0.55f * cos(angle + 1.9f) +
                    0.45f * sin(angle * 2.1f + 0.7f)
                )

            if (i == 0) {
                primaryPath.moveTo(x, y1)
                secondaryPath.moveTo(x, y2)
            } else {
                primaryPath.quadraticTo(prevX1, prevY1, (prevX1 + x) * 0.5f, (prevY1 + y1) * 0.5f)
                secondaryPath.quadraticTo(prevX2, prevY2, (prevX2 + x) * 0.5f, (prevY2 + y2) * 0.5f)
            }

            prevX1 = x
            prevY1 = y1
            prevX2 = x
            prevY2 = y2
        }

        val sw = strokeWidth.toPx()
        drawPath(
            path = primaryPath,
            color = color,
            style = Stroke(
                width = sw,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = secondaryPath,
            color = color.copy(alpha = (color.alpha * 0.28f).coerceIn(0f, 1f)),
            style = Stroke(
                width = sw * 0.6f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Polished hand-drawn sketchbook border modifier
 */
fun Modifier.squigglyBorder(
    color: Color,
    cornerRadius: Dp = 16.dp,
    strokeWidth: Dp = 1.5.dp,
    wavelength: Dp = 68.dp,
    amplitude: Dp = 0.58.dp,
    seed: Float = 0f
): Modifier = this.drawWithCache {
    val swPx = strokeWidth.toPx()
    // Keep border centered right on the shape perimeter so background fills never poke out
    val inset = swPx * 0.45f
    val rectW = (size.width - inset * 2f).coerceAtLeast(4f)
    val rectH = (size.height - inset * 2f).coerceAtLeast(4f)
    val radiusPx = min(cornerRadius.toPx(), min(rectW, rectH) / 2f)

    // Calibrate wavelength & amplitude for a refined, organic hand-inked aesthetic
    val minDimension = min(rectW, rectH)
    val refinedWavelengthPx = max(wavelength.toPx(), 58.dp.toPx())
    val maxPolishedAmpPx = min(0.68.dp.toPx(), minDimension * 0.016f).coerceAtLeast(0.28.dp.toPx())
    val refinedAmpPx = min(amplitude.toPx() * 0.54f, maxPolishedAmpPx)

    val primaryPath = buildSquigglyRoundRectPath(
        left = inset,
        top = inset,
        width = rectW,
        height = rectH,
        cornerRadius = radiusPx,
        wavelengthPx = refinedWavelengthPx,
        amplitudePx = refinedAmpPx,
        seed = seed,
        rawValues = true
    )

    // Second ultra-subtle fineliner pass with shifted phase for hand-inked richness
    val secondaryPath = buildSquigglyRoundRectPath(
        left = inset,
        top = inset,
        width = rectW,
        height = rectH,
        cornerRadius = radiusPx,
        wavelengthPx = refinedWavelengthPx * 1.22f,
        amplitudePx = refinedAmpPx * 0.65f,
        seed = seed + 2.37f,
        rawValues = true
    )

    val primaryStroke = Stroke(
        width = swPx,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    val secondaryStroke = Stroke(
        width = (swPx * 0.55f).coerceAtLeast(0.8f),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    val shadowOffsetX = 0.6.dp.toPx()
    val shadowOffsetY = 1.0.dp.toPx()

    onDrawWithContent {
        drawContent()

        // 1. Subtle warm paper depth under-stroke
        translate(left = shadowOffsetX, top = shadowOffsetY) {
            drawPath(
                path = primaryPath,
                color = CrustBrown.copy(alpha = (color.alpha * 0.10f).coerceIn(0f, 0.14f)),
                style = primaryStroke
            )
        }

        // 2. Primary hand-inked stroke
        drawPath(
            path = primaryPath,
            color = color,
            style = primaryStroke
        )

        // 3. Delicate secondary sketch whisper pass
        drawPath(
            path = secondaryPath,
            color = color.copy(alpha = (color.alpha * 0.28f).coerceIn(0f, 1f)),
            style = secondaryStroke
        )
    }
}

private data class PerimeterSample(
    val point: Offset,
    val normal: Offset,
    val cornerDamping: Float
)

/**
 * Builds a closed rounded-rectangle path with a continuous, corner-damped organic hand-drawn wave
 * along its perimeter.
 */
fun buildSquigglyRoundRectPath(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    cornerRadius: Float,
    wavelengthPx: Float,
    amplitudePx: Float,
    seed: Float = 0f,
    rawValues: Boolean = false
): Path {
    val r = min(cornerRadius, min(width, height) / 2f)
    val right = left + width
    val bottom = top + height
    val topStraight = max(0f, width - 2f * r)
    val sideStraight = max(0f, height - 2f * r)
    val cornerArcLen = (0.5f * PI.toFloat() * r)
    val totalPerimeter = 2f * topStraight + 2f * sideStraight + 4f * cornerArcLen
    if (totalPerimeter <= 1f) return Path()

    val effectiveWavelength = if (rawValues) wavelengthPx else max(wavelengthPx, 120f)
    val effectiveAmplitude = if (rawValues) amplitudePx else min(amplitudePx * 0.55f, 1.8f)

    // Integer number of base cycles around the closed loop so start & end meet with C1 continuity
    val numWaves = max(3, (totalPerimeter / effectiveWavelength).toInt())
    val stepPx = 4.5f
    val totalSteps = max(48, (totalPerimeter / stepPx).toInt())

    val points = ArrayList<Offset>(totalSteps + 1)
    for (i in 0 until totalSteps) {
        val dist = (i.toFloat() / totalSteps.toFloat()) * totalPerimeter
        val sample = pointAndNormalOnRoundRect(
            dist = dist,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            r = r,
            topStraight = topStraight,
            sideStraight = sideStraight,
            cornerArcLen = cornerArcLen
        )

        val phase = (i.toFloat() / totalSteps.toFloat()) * numWaves * 2f * PI.toFloat()
        // 3-harmonic organic hand drift (avoids mechanical sine-wave appearance)
        val rawWave = 0.55f * sin(phase + seed) +
            0.30f * cos(phase * 2f + seed * 1.73f + 0.6f) +
            0.15f * sin(phase * 3f - seed * 1.31f + 1.4f)
        val wave = effectiveAmplitude * sample.cornerDamping * rawWave

        points.add(
            Offset(
                x = sample.point.x + sample.normal.x * wave,
                y = sample.point.y + sample.normal.y * wave
            )
        )
    }

    val path = Path()
    if (points.isNotEmpty()) {
        val firstMid = Offset(
            (points.last().x + points.first().x) * 0.5f,
            (points.last().y + points.first().y) * 0.5f
        )
        path.moveTo(firstMid.x, firstMid.y)
        for (i in points.indices) {
            val current = points[i]
            val next = points[(i + 1) % points.size]
            val mid = Offset((current.x + next.x) * 0.5f, (current.y + next.y) * 0.5f)
            path.quadraticTo(current.x, current.y, mid.x, mid.y)
        }
        path.close()
    }
    return path
}

/**
 * Smoothly attenuates wave amplitude around rounded corners so corners never pinch or dent,
 * while straight edges exhibit natural hand-drawn character.
 */
private fun arcCornerDamping(localArcDist: Float, cornerArcLen: Float): Float {
    if (cornerArcLen <= 0.01f) return 1f
    // u = 0 at arc start/end (meets straight edge), u = 1 at arc midpoint (45° corner apex)
    val normalized = (localArcDist / cornerArcLen).coerceIn(0f, 1f)
    val distFromCenter = kotlin.math.abs(normalized - 0.5f) * 2f // 0 at apex, 1 at ends
    val smooth = distFromCenter * distFromCenter * (3f - 2f * distFromCenter)
    return 0.22f + 0.78f * smooth
}

private fun pointAndNormalOnRoundRect(
    dist: Float,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    r: Float,
    topStraight: Float,
    sideStraight: Float,
    cornerArcLen: Float
): PerimeterSample {
    var d = dist

    // 1. Top edge: (left + r, top) -> (right - r, top), normal = (0, -1)
    if (d <= topStraight) {
        return PerimeterSample(Offset(left + r + d, top), Offset(0f, -1f), 1f)
    }
    d -= topStraight

    // 2. Top-right arc: center (right - r, top + r), angle -PI/2 -> 0
    if (d <= cornerArcLen && r > 0f) {
        val angle = (-0.5f * PI.toFloat()) + (d / cornerArcLen) * (0.5f * PI.toFloat())
        val nx = cos(angle)
        val ny = sin(angle)
        return PerimeterSample(
            Offset(right - r + r * nx, top + r + r * ny),
            Offset(nx, ny),
            arcCornerDamping(d, cornerArcLen)
        )
    }
    d -= cornerArcLen

    // 3. Right edge: (right, top + r) -> (right, bottom - r), normal = (1, 0)
    if (d <= sideStraight) {
        return PerimeterSample(Offset(right, top + r + d), Offset(1f, 0f), 1f)
    }
    d -= sideStraight

    // 4. Bottom-right arc: center (right - r, bottom - r), angle 0 -> PI/2
    if (d <= cornerArcLen && r > 0f) {
        val angle = (d / cornerArcLen) * (0.5f * PI.toFloat())
        val nx = cos(angle)
        val ny = sin(angle)
        return PerimeterSample(
            Offset(right - r + r * nx, bottom - r + r * ny),
            Offset(nx, ny),
            arcCornerDamping(d, cornerArcLen)
        )
    }
    d -= cornerArcLen

    // 5. Bottom edge: (right - r, bottom) -> (left + r, bottom), normal = (0, 1)
    if (d <= topStraight) {
        return PerimeterSample(Offset(right - r - d, bottom), Offset(0f, 1f), 1f)
    }
    d -= topStraight

    // 6. Bottom-left arc: center (left + r, bottom - r), angle PI/2 -> PI
    if (d <= cornerArcLen && r > 0f) {
        val angle = (0.5f * PI.toFloat()) + (d / cornerArcLen) * (0.5f * PI.toFloat())
        val nx = cos(angle)
        val ny = sin(angle)
        return PerimeterSample(
            Offset(left + r + r * nx, bottom - r + r * ny),
            Offset(nx, ny),
            arcCornerDamping(d, cornerArcLen)
        )
    }
    d -= cornerArcLen

    // 7. Left edge: (left, bottom - r) -> (left, top + r), normal = (-1, 0)
    if (d <= sideStraight) {
        return PerimeterSample(Offset(left, bottom - r - d), Offset(-1f, 0f), 1f)
    }
    d -= sideStraight

    // 8. Top-left arc: center (left + r, top + r), angle PI -> 3*PI/2
    if (r > 0f && cornerArcLen > 0f) {
        val clampedD = d.coerceIn(0f, cornerArcLen)
        val angle = PI.toFloat() + (clampedD / cornerArcLen) * (0.5f * PI.toFloat())
        val nx = cos(angle)
        val ny = sin(angle)
        return PerimeterSample(
            Offset(left + r + r * nx, top + r + r * ny),
            Offset(nx, ny),
            arcCornerDamping(clampedD, cornerArcLen)
        )
    }

    return PerimeterSample(Offset(left + r, top), Offset(0f, -1f), 1f)
}
