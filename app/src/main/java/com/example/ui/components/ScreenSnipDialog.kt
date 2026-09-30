package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.BreadcrumbAccessibilityService
import com.example.ui.theme.CrustBrown
import com.example.ui.theme.SquiggleBorderLight
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class DragHandle {
    NONE,
    MOVE_BOX,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    EDGE_TOP,
    EDGE_BOTTOM,
    EDGE_LEFT,
    EDGE_RIGHT
}

@Composable
fun ScreenSnipDialog(
    initialBitmap: Bitmap? = null,
    onDismiss: () -> Unit,
    onSnipConfirmed: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var sourceBitmap by remember(initialBitmap) {
        mutableStateOf<Bitmap?>(
            initialBitmap
                ?: BreadcrumbAccessibilityService.latestCapturedBitmap
                ?: generateSampleScreenBitmap()
        )
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Normalized coordinates (0.0f .. 1.0f) relative to the drawn image bounds
    var normLeft by remember { mutableFloatStateOf(0.10f) }
    var normTop by remember { mutableFloatStateOf(0.18f) }
    var normRight by remember { mutableFloatStateOf(0.90f) }
    var normBottom by remember { mutableFloatStateOf(0.72f) }

    var activeDragHandle by remember { mutableStateOf(DragHandle.NONE) }

    // Snapshot of normalized rect at the exact moment a drag starts
    var startNormLeft by remember { mutableFloatStateOf(0.10f) }
    var startNormTop by remember { mutableFloatStateOf(0.18f) }
    var startNormRight by remember { mutableFloatStateOf(0.90f) }
    var startNormBottom by remember { mutableFloatStateOf(0.72f) }
    var totalDragDx by remember { mutableFloatStateOf(0f) }
    var totalDragDy by remember { mutableFloatStateOf(0f) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bmp != null) {
                    sourceBitmap = bmp
                    normLeft = 0.10f
                    normTop = 0.15f
                    normRight = 0.90f
                    normBottom = 0.80f
                }
            } catch (_: Exception) {}
        }
    }

    val bmpForDims = sourceBitmap
    val cropWidthPx = if (bmpForDims != null) {
        ((normRight - normLeft) * bmpForDims.width).roundToInt().coerceAtLeast(1)
    } else 0
    val cropHeightPx = if (bmpForDims != null) {
        ((normBottom - normTop) * bmpForDims.height).roundToInt().coerceAtLeast(1)
    } else 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .squigglyBorder(
                    color = CrustBrown,
                    cornerRadius = 22.dp,
                    strokeWidth = 2.dp,
                    seed = 2.7f
                )
                .testTag("screen_snip_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Compact Header with Mascot & Live Dimensions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BreadcrumbMascot(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Snip Screen",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (cropWidthPx > 0 && cropHeightPx > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "$cropWidthPx × $cropHeightPx px",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Drag to move • Corners to resize",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Framing Presets & Image Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = normLeft <= 0.02f && normTop <= 0.02f && normRight >= 0.98f && normBottom >= 0.98f,
                        onClick = {
                            normLeft = 0f
                            normTop = 0f
                            normRight = 1f
                            normBottom = 1f
                        },
                        label = { Text("Full", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            normLeft = 0.10f
                            normTop = 0.22f
                            normRight = 0.90f
                            normBottom = 0.78f
                        },
                        label = { Text("Center", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            normLeft = 0.06f
                            normTop = 0.06f
                            normRight = 0.94f
                            normBottom = 0.48f
                        },
                        label = { Text("Top", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            normLeft = 0.06f
                            normTop = 0.52f
                            normRight = 0.94f
                            normBottom = 0.94f
                        },
                        label = { Text("Bottom", fontSize = 11.sp) }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Pick Image", modifier = Modifier.size(18.dp), tint = CrustBrown)
                    }

                    IconButton(
                        onClick = {
                            normLeft = 0.10f
                            normTop = 0.18f
                            normRight = 0.90f
                            normBottom = 0.72f
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Selection", modifier = Modifier.size(18.dp), tint = CrustBrown)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Snipping Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .squigglyBorder(
                            color = SquiggleBorderLight,
                            cornerRadius = 16.dp,
                            strokeWidth = 1.5.dp
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161D31))
                        .onSizeChanged { canvasSize = it }
                ) {
                    val bmp = sourceBitmap
                    if (bmp != null && canvasSize.width > 0 && canvasSize.height > 0) {
                        val padPx = with(density) { 14.dp.toPx() }
                        val availW = (canvasSize.width.toFloat() - padPx * 2f).coerceAtLeast(100f)
                        val availH = (canvasSize.height.toFloat() - padPx * 2f).coerceAtLeast(100f)

                        val srcAspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                        val dstAspect = availW / availH

                        val drawnW: Float
                        val drawnH: Float
                        val offsetX: Float
                        val offsetY: Float

                        if (srcAspect > dstAspect) {
                            drawnW = availW
                            drawnH = availW / srcAspect
                            offsetX = padPx
                            offsetY = padPx + (availH - drawnH) / 2f
                        } else {
                            drawnH = availH
                            drawnW = availH * srcAspect
                            offsetX = padPx + (availW - drawnW) / 2f
                            offsetY = padPx
                        }

                        val primaryColor = MaterialTheme.colorScheme.primary
                        val cornerTouchRadiusPx = with(density) { 30.dp.toPx() }
                        val edgeTouchBandPx = with(density) { 22.dp.toPx() }
                        val centerMoveRadiusPx = with(density) { 26.dp.toPx() }
                        val movePillHalfW = with(density) { 58.dp.toPx() }
                        val movePillHalfH = with(density) { 20.dp.toPx() }
                        val movePillGapPx = with(density) { 24.dp.toPx() }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(drawnW, drawnH, offsetX, offsetY) {
                                    detectDragGestures(
                                        onDragStart = { startPos ->
                                            startNormLeft = normLeft
                                            startNormTop = normTop
                                            startNormRight = normRight
                                            startNormBottom = normBottom
                                            totalDragDx = 0f
                                            totalDragDy = 0f

                                            val currentLeftPx = offsetX + normLeft * drawnW
                                            val currentRightPx = offsetX + normRight * drawnW
                                            val currentTopPx = offsetY + normTop * drawnH
                                            val currentBottomPx = offsetY + normBottom * drawnH

                                            val boxCenterX = (currentLeftPx + currentRightPx) / 2f
                                            val boxCenterY = (currentTopPx + currentBottomPx) / 2f

                                            val pillPlaceBelow = (currentBottomPx + movePillGapPx + movePillHalfH) < (size.height - 8f)
                                            val pillCenterY = if (pillPlaceBelow) {
                                                currentBottomPx + movePillGapPx
                                            } else {
                                                (currentTopPx - movePillGapPx).coerceAtLeast(movePillHalfH)
                                            }

                                            val isTouchingMovePill =
                                                abs(startPos.x - boxCenterX) <= movePillHalfW &&
                                                    abs(startPos.y - pillCenterY) <= movePillHalfH

                                            val isTouchingCenterBadge =
                                                hypot(startPos.x - boxCenterX, startPos.y - boxCenterY) <= centerMoveRadiusPx

                                            val dTopLeft = hypot(startPos.x - currentLeftPx, startPos.y - currentTopPx)
                                            val dTopRight = hypot(startPos.x - currentRightPx, startPos.y - currentTopPx)
                                            val dBottomLeft = hypot(startPos.x - currentLeftPx, startPos.y - currentBottomPx)
                                            val dBottomRight = hypot(startPos.x - currentRightPx, startPos.y - currentBottomPx)
                                            val minCornerDist = min(min(dTopLeft, dTopRight), min(dBottomLeft, dBottomRight))

                                            activeDragHandle = when {
                                                isTouchingMovePill || isTouchingCenterBadge ->
                                                    DragHandle.MOVE_BOX
                                                minCornerDist <= cornerTouchRadiusPx -> {
                                                    when (minCornerDist) {
                                                        dTopLeft -> DragHandle.TOP_LEFT
                                                        dTopRight -> DragHandle.TOP_RIGHT
                                                        dBottomLeft -> DragHandle.BOTTOM_LEFT
                                                        else -> DragHandle.BOTTOM_RIGHT
                                                    }
                                                }
                                                abs(startPos.y - currentTopPx) <= edgeTouchBandPx &&
                                                    startPos.x in (currentLeftPx - edgeTouchBandPx)..(currentRightPx + edgeTouchBandPx) ->
                                                    DragHandle.EDGE_TOP
                                                abs(startPos.y - currentBottomPx) <= edgeTouchBandPx &&
                                                    startPos.x in (currentLeftPx - edgeTouchBandPx)..(currentRightPx + edgeTouchBandPx) ->
                                                    DragHandle.EDGE_BOTTOM
                                                abs(startPos.x - currentLeftPx) <= edgeTouchBandPx &&
                                                    startPos.y in (currentTopPx - edgeTouchBandPx)..(currentBottomPx + edgeTouchBandPx) ->
                                                    DragHandle.EDGE_LEFT
                                                abs(startPos.x - currentRightPx) <= edgeTouchBandPx &&
                                                    startPos.y in (currentTopPx - edgeTouchBandPx)..(currentBottomPx + edgeTouchBandPx) ->
                                                    DragHandle.EDGE_RIGHT
                                                else -> DragHandle.MOVE_BOX
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (drawnW <= 0f || drawnH <= 0f) return@detectDragGestures

                                            totalDragDx += dragAmount.x
                                            totalDragDy += dragAmount.y
                                            val dNormX = totalDragDx / drawnW
                                            val dNormY = totalDragDy / drawnH
                                            val minBoxSize = 0.06f

                                            when (activeDragHandle) {
                                                DragHandle.MOVE_BOX -> {
                                                    val boxW = startNormRight - startNormLeft
                                                    val boxH = startNormBottom - startNormTop
                                                    val newLeft = (startNormLeft + dNormX).coerceIn(0f, (1f - boxW).coerceAtLeast(0f))
                                                    val newTop = (startNormTop + dNormY).coerceIn(0f, (1f - boxH).coerceAtLeast(0f))
                                                    normLeft = newLeft
                                                    normRight = (newLeft + boxW).coerceAtMost(1f)
                                                    normTop = newTop
                                                    normBottom = (newTop + boxH).coerceAtMost(1f)
                                                }
                                                DragHandle.TOP_LEFT -> {
                                                    normLeft = (startNormLeft + dNormX).coerceIn(0f, startNormRight - minBoxSize)
                                                    normTop = (startNormTop + dNormY).coerceIn(0f, startNormBottom - minBoxSize)
                                                }
                                                DragHandle.TOP_RIGHT -> {
                                                    normRight = (startNormRight + dNormX).coerceIn(startNormLeft + minBoxSize, 1f)
                                                    normTop = (startNormTop + dNormY).coerceIn(0f, startNormBottom - minBoxSize)
                                                }
                                                DragHandle.BOTTOM_LEFT -> {
                                                    normLeft = (startNormLeft + dNormX).coerceIn(0f, startNormRight - minBoxSize)
                                                    normBottom = (startNormBottom + dNormY).coerceIn(startNormTop + minBoxSize, 1f)
                                                }
                                                DragHandle.BOTTOM_RIGHT -> {
                                                    normRight = (startNormRight + dNormX).coerceIn(startNormLeft + minBoxSize, 1f)
                                                    normBottom = (startNormBottom + dNormY).coerceIn(startNormTop + minBoxSize, 1f)
                                                }
                                                DragHandle.EDGE_TOP -> {
                                                    normTop = (startNormTop + dNormY).coerceIn(0f, startNormBottom - minBoxSize)
                                                }
                                                DragHandle.EDGE_BOTTOM -> {
                                                    normBottom = (startNormBottom + dNormY).coerceIn(startNormTop + minBoxSize, 1f)
                                                }
                                                DragHandle.EDGE_LEFT -> {
                                                    normLeft = (startNormLeft + dNormX).coerceIn(0f, startNormRight - minBoxSize)
                                                }
                                                DragHandle.EDGE_RIGHT -> {
                                                    normRight = (startNormRight + dNormX).coerceIn(startNormLeft + minBoxSize, 1f)
                                                }
                                                DragHandle.NONE -> {}
                                            }
                                        },
                                        onDragEnd = { activeDragHandle = DragHandle.NONE },
                                        onDragCancel = { activeDragHandle = DragHandle.NONE }
                                    )
                                }
                                .testTag("snip_canvas")
                        ) {
                            val dstLeftInt = offsetX.roundToInt()
                            val dstTopInt = offsetY.roundToInt()
                            val dstWidthInt = drawnW.roundToInt().coerceAtLeast(1)
                            val dstHeightInt = drawnH.roundToInt().coerceAtLeast(1)

                            // 1. Draw the full captured screenshot
                            drawImage(
                                image = bmp.asImageBitmap(),
                                dstOffset = IntOffset(dstLeftInt, dstTopInt),
                                dstSize = IntSize(dstWidthInt, dstHeightInt)
                            )

                            val selLeft = dstLeftInt + normLeft * dstWidthInt
                            val selTop = dstTopInt + normTop * dstHeightInt
                            val selRight = dstLeftInt + normRight * dstWidthInt
                            val selBottom = dstTopInt + normBottom * dstHeightInt
                            val selW = max(10f, selRight - selLeft)
                            val selH = max(10f, selBottom - selTop)
                            val selCenterX = (selLeft + selRight) / 2f
                            val selCenterY = (selTop + selBottom) / 2f

                            // 2. Dim outer non-selected area
                            val cutoutPath = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        rect = Rect(selLeft, selTop, selRight, selBottom),
                                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                    )
                                )
                            }
                            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.58f),
                                    topLeft = Offset.Zero,
                                    size = size
                                )
                            }

                            // 3. Rule-of-thirds grid
                            val gridColor = Color.White.copy(alpha = 0.42f)
                            val gridStrokeWidth = 1.dp.toPx()
                            drawLine(gridColor, Offset(selLeft + selW / 3f, selTop), Offset(selLeft + selW / 3f, selBottom), gridStrokeWidth)
                            drawLine(gridColor, Offset(selLeft + (2f * selW) / 3f, selTop), Offset(selLeft + (2f * selW) / 3f, selBottom), gridStrokeWidth)
                            drawLine(gridColor, Offset(selLeft, selTop + selH / 3f), Offset(selRight, selTop + selH / 3f), gridStrokeWidth)
                            drawLine(gridColor, Offset(selLeft, selTop + (2f * selH) / 3f), Offset(selRight, selTop + (2f * selH) / 3f), gridStrokeWidth)

                            // 4. Squiggly selection border
                            val isMoving = activeDragHandle == DragHandle.MOVE_BOX
                            val borderColor = if (isMoving) Color(0xFFF7F3E8) else primaryColor
                            val squiggleCropPath = buildSquigglyRoundRectPath(
                                left = selLeft,
                                top = selTop,
                                width = selW,
                                height = selH,
                                cornerRadius = 8.dp.toPx(),
                                wavelengthPx = 22.dp.toPx(),
                                amplitudePx = 1.1.dp.toPx(),
                                seed = 1.5f
                            )
                            drawPath(
                                path = squiggleCropPath,
                                color = borderColor,
                                style = Stroke(width = 2.6.dp.toPx())
                            )

                            // 5. Tactile L-bracket handles
                            val bracketLen = min(20.dp.toPx(), min(selW, selH) * 0.32f).coerceAtLeast(10.dp.toPx())
                            val bracketStroke = 5.dp.toPx()
                            val bracketColor = Color(0xFFF7F3E8)
                            val bracketAccent = CrustBrown

                            // Corners
                            drawLine(bracketAccent, Offset(selLeft, selTop), Offset(selLeft + bracketLen, selTop), bracketStroke + 3.dp.toPx(), StrokeCap.Round)
                            drawLine(bracketAccent, Offset(selLeft, selTop), Offset(selLeft, selTop + bracketLen), bracketStroke + 3.dp.toPx(), StrokeCap.Round)
                            drawLine(bracketColor, Offset(selLeft, selTop), Offset(selLeft + bracketLen, selTop), bracketStroke, StrokeCap.Round)
                            drawLine(bracketColor, Offset(selLeft, selTop), Offset(selLeft, selTop + bracketLen), bracketStroke, StrokeCap.Round)

                            drawLine(bracketAccent, Offset(selRight, selTop), Offset(selRight - bracketLen, selTop), bracketStroke + 3.dp.toPx(), StrokeCap.Round)
                            drawLine(bracketAccent, Offset(selRight, selTop), Offset(selRight, selTop + bracketLen), bracketStroke + 3.dp.toPx(), StrokeCap.Round)
                            drawLine(bracketColor, Offset(selRight, selTop), Offset(selRight - bracketLen, selTop), bracketStroke, StrokeCap.Round)
                            drawLine(bracketColor, Offset(selRight, selTop), Offset(selRight, selTop + bracketLen), bracketStroke, StrokeCap.Round)

                            drawLine(bracketAccent, Offset(selLeft, selBottom), Offset(selLeft + bracketLen, selBottom), bracketStroke + 3.dp.toPx(), StrokeCap.Round)
                            drawLine(bracketAccent, Offset(selLeft, selBottom), Offset(selLeft, selBottom - bracketLen), bracketStroke + 3.dp.toPx(), StrokeCap.Round)
                            drawLine(bracketColor, Offset(selLeft, selBottom), Offset(selLeft + bracketLen, selBottom), bracketStroke, StrokeCap.Round)
                            drawLine(bracketColor, Offset(selLeft, selBottom), Offset(selLeft, selBottom - bracketLen), bracketStroke, StrokeCap.Round)

                            drawLine(bracketAccent, Offset(selRight, selBottom), Offset(selRight - bracketLen, selBottom), bracketStroke + 3.dp.toPx(), StrokeCap.Round)
                            drawLine(bracketAccent, Offset(selRight, selBottom), Offset(selRight, selBottom - bracketLen), bracketStroke + 3.dp.toPx(), StrokeCap.Round)
                            drawLine(bracketColor, Offset(selRight, selBottom), Offset(selRight - bracketLen, selBottom), bracketStroke, StrokeCap.Round)
                            drawLine(bracketColor, Offset(selRight, selBottom), Offset(selRight, selBottom - bracketLen), bracketStroke, StrokeCap.Round)

                            // Edge pills
                            val edgePillLen = min(26.dp.toPx(), min(selW, selH) * 0.28f)
                            if (edgePillLen >= 10.dp.toPx()) {
                                drawLine(bracketAccent, Offset(selCenterX - edgePillLen / 2f, selTop), Offset(selCenterX + edgePillLen / 2f, selTop), 6.dp.toPx(), StrokeCap.Round)
                                drawLine(bracketColor, Offset(selCenterX - edgePillLen / 2f, selTop), Offset(selCenterX + edgePillLen / 2f, selTop), 3.5.dp.toPx(), StrokeCap.Round)
                                drawLine(bracketAccent, Offset(selCenterX - edgePillLen / 2f, selBottom), Offset(selCenterX + edgePillLen / 2f, selBottom), 6.dp.toPx(), StrokeCap.Round)
                                drawLine(bracketColor, Offset(selCenterX - edgePillLen / 2f, selBottom), Offset(selCenterX + edgePillLen / 2f, selBottom), 3.5.dp.toPx(), StrokeCap.Round)

                                drawLine(bracketAccent, Offset(selLeft, selCenterY - edgePillLen / 2f), Offset(selLeft, selCenterY + edgePillLen / 2f), 6.dp.toPx(), StrokeCap.Round)
                                drawLine(bracketColor, Offset(selLeft, selCenterY - edgePillLen / 2f), Offset(selLeft, selCenterY + edgePillLen / 2f), 3.5.dp.toPx(), StrokeCap.Round)
                                drawLine(bracketAccent, Offset(selRight, selCenterY - edgePillLen / 2f), Offset(selRight, selCenterY + edgePillLen / 2f), 6.dp.toPx(), StrokeCap.Round)
                                drawLine(bracketColor, Offset(selRight, selCenterY - edgePillLen / 2f), Offset(selRight, selCenterY + edgePillLen / 2f), 3.5.dp.toPx(), StrokeCap.Round)
                            }

                            // 6. Central Move Handle
                            val badgeRadius = 17.dp.toPx()
                            drawCircle(CrustBrown.copy(alpha = 0.90f), badgeRadius + 2.dp.toPx(), Offset(selCenterX, selCenterY))
                            drawCircle(if (isMoving) primaryColor else Color(0xFFF7F3E8).copy(alpha = 0.92f), badgeRadius, Offset(selCenterX, selCenterY))

                            val arrowColor = if (isMoving) Color.White else Color(0xFF1E2742)
                            val arm = 8.dp.toPx()
                            val head = 3.5.dp.toPx()
                            val arrowStroke = 2.dp.toPx()

                            drawLine(arrowColor, Offset(selCenterX - arm, selCenterY), Offset(selCenterX + arm, selCenterY), arrowStroke, StrokeCap.Round)
                            drawLine(arrowColor, Offset(selCenterX, selCenterY - arm), Offset(selCenterX, selCenterY + arm), arrowStroke, StrokeCap.Round)
                            drawLine(arrowColor, Offset(selCenterX - arm, selCenterY), Offset(selCenterX - arm + head, selCenterY - head), arrowStroke, StrokeCap.Round)
                            drawLine(arrowColor, Offset(selCenterX - arm, selCenterY), Offset(selCenterX - arm + head, selCenterY + head), arrowStroke, StrokeCap.Round)
                            drawLine(arrowColor, Offset(selCenterX + arm, selCenterY), Offset(selCenterX + arm - head, selCenterY - head), arrowStroke, StrokeCap.Round)
                            drawLine(arrowColor, Offset(selCenterX + arm, selCenterY), Offset(selCenterX + arm - head, selCenterY + head), arrowStroke, StrokeCap.Round)
                            drawLine(arrowColor, Offset(selCenterX, selCenterY - arm), Offset(selCenterX - head, selCenterY - arm + head), arrowStroke, StrokeCap.Round)
                            drawLine(arrowColor, Offset(selCenterX, selCenterY - arm), Offset(selCenterX + head, selCenterY - arm + head), arrowStroke, StrokeCap.Round)
                            drawLine(arrowColor, Offset(selCenterX, selCenterY + arm), Offset(selCenterX - head, selCenterY + arm - head), arrowStroke, StrokeCap.Round)
                            drawLine(arrowColor, Offset(selCenterX, selCenterY + arm), Offset(selCenterX + head, selCenterY + arm - head), arrowStroke, StrokeCap.Round)

                            // 7. Attached Grab Pill
                            val pillPlaceBelow = (selBottom + movePillGapPx + movePillHalfH) < (size.height - 8f)
                            val pillY = if (pillPlaceBelow) {
                                selBottom + movePillGapPx
                            } else {
                                (selTop - movePillGapPx).coerceAtLeast(movePillHalfH)
                            }
                            val pillLeft = (selCenterX - 46.dp.toPx()).coerceIn(8f, size.width - 92.dp.toPx() - 8f)
                            val pillTop = pillY - 13.dp.toPx()
                            val pillW = 92.dp.toPx()
                            val pillH = 26.dp.toPx()

                            drawRoundRect(
                                color = CrustBrown,
                                topLeft = Offset(pillLeft - 1.5.dp.toPx(), pillTop - 1.5.dp.toPx()),
                                size = Size(pillW + 3.dp.toPx(), pillH + 3.dp.toPx()),
                                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                            )
                            drawRoundRect(
                                color = if (isMoving) primaryColor else Color(0xFFF7F3E8),
                                topLeft = Offset(pillLeft, pillTop),
                                size = Size(pillW, pillH),
                                cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx())
                            )

                            val dotColor = if (isMoving) Color.White else CrustBrown
                            val dotY1 = pillTop + pillH * 0.38f
                            val dotY2 = pillTop + pillH * 0.64f
                            for (i in -2..2) {
                                val dotX = pillLeft + pillW / 2f + i * 7.dp.toPx()
                                drawCircle(dotColor, radius = 1.6.dp.toPx(), center = Offset(dotX, dotY1))
                                drawCircle(dotColor, radius = 1.6.dp.toPx(), center = Offset(dotX, dotY2))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom actions: Cancel & Confirm Snip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .squigglyBorder(
                                color = SquiggleBorderLight,
                                cornerRadius = 14.dp
                            ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val bmp = sourceBitmap
                            if (bmp != null) {
                                val cropped = cropBitmapNormalized(
                                    source = bmp,
                                    normLeft = normLeft,
                                    normTop = normTop,
                                    normRight = normRight,
                                    normBottom = normBottom
                                )
                                onSnipConfirmed(cropped)
                            }
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .squigglyBorder(
                                color = CrustBrown,
                                cornerRadius = 14.dp,
                                strokeWidth = 1.6.dp
                            )
                            .testTag("confirm_snip_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Snip", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Pixel-accurate bitmap crop using exact rounded coordinates matching the Canvas preview.
 */
private fun cropBitmapNormalized(
    source: Bitmap,
    normLeft: Float,
    normTop: Float,
    normRight: Float,
    normBottom: Float
): Bitmap {
    val l = min(normLeft, normRight).coerceIn(0f, 1f)
    val r = max(normLeft, normRight).coerceIn(0f, 1f)
    val t = min(normTop, normBottom).coerceIn(0f, 1f)
    val b = max(normTop, normBottom).coerceIn(0f, 1f)

    val leftPx = (l * source.width).roundToInt().coerceIn(0, source.width - 1)
    val topPx = (t * source.height).roundToInt().coerceIn(0, source.height - 1)
    val rightPx = (r * source.width).roundToInt().coerceIn(leftPx + 1, source.width)
    val bottomPx = (b * source.height).roundToInt().coerceIn(topPx + 1, source.height)

    val cropWidth = (rightPx - leftPx).coerceIn(1, source.width - leftPx)
    val cropHeight = (bottomPx - topPx).coerceIn(1, source.height - topPx)

    return try {
        Bitmap.createBitmap(source, leftPx, topPx, cropWidth, cropHeight)
    } catch (_: Exception) {
        source
    }
}

fun generateSampleScreenBitmap(): Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#FCFBF8") }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val cardPaint = Paint().apply { color = android.graphics.Color.parseColor("#F1EFEA") }
    canvas.drawRoundRect(60f, 200f, (width - 60).toFloat(), 840f, 36f, 36f, cardPaint)

    val borderPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#8D675A")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    canvas.drawRoundRect(60f, 200f, (width - 60).toFloat(), 840f, 36f, 36f, borderPaint)

    val titlePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#1E2742")
        textSize = 50f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("Field Note: Quick Capture", 100f, 320f, titlePaint)

    val bodyPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#55555F")
        textSize = 36f
        isAntiAlias = true
    }
    canvas.drawText("Tap the floating mascot anytime.", 100f, 420f, bodyPaint)
    canvas.drawText("Select any region of your screen.", 100f, 485f, bodyPaint)
    canvas.drawText("Add your context note and #tag.", 100f, 550f, bodyPaint)

    return bitmap
}
