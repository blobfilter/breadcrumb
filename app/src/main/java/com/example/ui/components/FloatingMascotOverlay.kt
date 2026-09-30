package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrustBrown
import kotlin.math.roundToInt

/**
 * Draggable Floating Mascot Overlay that snaps/pins to either side (left or right edge) of the screen.
 * - Clicking the Mascot launches the Image Snip Tool.
 * - When Accessibility text selection detects highlighted text, a floating "Save Selected Text" pill
 *   appears right next to the Mascot for 1-tap capture.
 */
@Composable
fun FloatingMascotOverlay(
    visible: Boolean,
    mascotVisible: Boolean = true,
    detectedSelectedText: String?,
    onMascotClick: () -> Unit,
    onSaveSelectedTextClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    if (!mascotVisible && detectedSelectedText.isNullOrBlank()) return

    val density = LocalDensity.current
    val mascotSizeDp = 56.dp
    val edgeMarginDp = 6.dp
    val mascotSizePx = with(density) { mascotSizeDp.toPx() }
    val edgeMarginPx = with(density) { edgeMarginDp.toPx() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(320f)
        val maxHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(480f)

        val leftEdgeX = edgeMarginPx

        var isDockedLeft by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }
        var dragX by remember(maxWidthPx) { mutableFloatStateOf((maxWidthPx - mascotSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)) }
        var posY by remember(maxHeightPx) { mutableFloatStateOf(maxHeightPx * 0.38f) }

        val minYPx = with(density) { 64.dp.toPx() }
        val maxYPx = (maxHeightPx - mascotSizePx - with(density) { 88.dp.toPx() }).coerceAtLeast(minYPx)

        if (!mascotVisible) {
            // Standalone Text Selection Pill when floating mascot overlay is off
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (isDockedLeft) edgeMarginDp else 0.dp,
                        end = if (!isDockedLeft) edgeMarginDp else 0.dp
                    ),
                contentAlignment = if (isDockedLeft) Alignment.TopStart else Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier.offset {
                        IntOffset(0, posY.coerceIn(minYPx, maxYPx).roundToInt())
                    }
                ) {
                    DetectedTextSelectionPill(
                        selectedText = detectedSelectedText,
                        onClick = onSaveSelectedTextClick,
                        showMascotIcon = true
                    )
                }
            }
        } else {
            val rightEdgeX = (maxWidthPx - mascotSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)
            val targetX = if (isDragging) {
                dragX
            } else {
                if (isDockedLeft) leftEdgeX else rightEdgeX
            }

            val animatedX by animateFloatAsState(
                targetValue = targetX,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "mascot_edge_snap"
            )

            Row(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = if (isDockedLeft) animatedX.roundToInt() else (animatedX.roundToInt()).coerceAtLeast(0),
                            y = posY.coerceIn(minYPx, maxYPx).roundToInt()
                        )
                    }
                    .pointerInput(maxWidthPx, maxHeightPx) {
                        detectDragGestures(
                            onDragStart = {
                                dragX = if (isDockedLeft) leftEdgeX else rightEdgeX
                                isDragging = true
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragX = (dragX + dragAmount.x).coerceIn(leftEdgeX, rightEdgeX)
                                posY = (posY + dragAmount.y).coerceIn(minYPx, maxYPx)
                            },
                            onDragEnd = {
                                isDragging = false
                                val center = dragX + mascotSizePx / 2f
                                isDockedLeft = center < (maxWidthPx / 2f)
                                dragX = if (isDockedLeft) leftEdgeX else rightEdgeX
                            },
                            onDragCancel = {
                                isDragging = false
                                dragX = if (isDockedLeft) leftEdgeX else rightEdgeX
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mascot is toggled on: show pill next to draggable mascot
                if (!isDockedLeft) {
                    DetectedTextSelectionPill(
                        selectedText = detectedSelectedText,
                        onClick = onSaveSelectedTextClick,
                        showMascotIcon = false
                    )
                    if (!detectedSelectedText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }

                // Draggable Floating Mascot Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(mascotSizeDp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), clip = false)
                        .clickable { onMascotClick() }
                        .testTag("floating_mascot_overlay")
                ) {
                    BreadcrumbMascot(
                        modifier = Modifier.fillMaxSize(),
                        squigglyOutline = true
                    )
                }

                // If docked on left, show the detected text pill to the RIGHT of the mascot
                if (isDockedLeft) {
                    if (!detectedSelectedText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    DetectedTextSelectionPill(
                        selectedText = detectedSelectedText,
                        onClick = onSaveSelectedTextClick,
                        showMascotIcon = false
                    )
                }
            }
        }
    }
}

@Composable
private fun DetectedTextSelectionPill(
    selectedText: String?,
    onClick: (String) -> Unit,
    showMascotIcon: Boolean = false
) {
    AnimatedVisibility(
        visible = !selectedText.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val text = selectedText ?: ""
        val preview = if (text.length > 20) "${text.take(20)}…" else text
        Box(
            modifier = Modifier
                .squigglyBorder(
                    color = CrustBrown,
                    cornerRadius = 18.dp,
                    strokeWidth = 1.6.dp,
                    seed = 3.3f
                )
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                .clickable { onClick(text) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("accessibility_save_text_pill")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showMascotIcon) {
                    BreadcrumbMascot(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = "Save \"$preview\"",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            }
        }
    }
}
