package com.example.ui.components

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal enum class CropTouchMode {
    NONE,
    MOVE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    EDGE_LEFT,
    EDGE_TOP,
    EDGE_RIGHT,
    EDGE_BOTTOM
}

@Stable
class CropOverlayState(
    sourceWidth: Int = 0,
    sourceHeight: Int = 0
) {
    var sourceBmpWidth by mutableStateOf(sourceWidth)
        private set
    var sourceBmpHeight by mutableStateOf(sourceHeight)
        private set

    var viewWidth by mutableFloatStateOf(0f)
        private set
    var viewHeight by mutableFloatStateOf(0f)
        private set

    var imageLeft by mutableFloatStateOf(0f)
        private set
    var imageTop by mutableFloatStateOf(0f)
        private set
    var imageRight by mutableFloatStateOf(0f)
        private set
    var imageBottom by mutableFloatStateOf(0f)
        private set

    var cropLeft by mutableFloatStateOf(0f)
        private set
    var cropTop by mutableFloatStateOf(0f)
        private set
    var cropRight by mutableFloatStateOf(0f)
        private set
    var cropBottom by mutableFloatStateOf(0f)
        private set

    private var isInitialized = false

    val cropWidthPx: Int
        get() = (cropRight - cropLeft).roundToInt().coerceAtLeast(0)

    val cropHeightPx: Int
        get() = (cropBottom - cropTop).roundToInt().coerceAtLeast(0)

    fun setSourceImageDimensions(width: Int, height: Int) {
        sourceBmpWidth = width
        sourceBmpHeight = height
        isInitialized = false
        updateImageBounds()
    }

    fun resetToFullBounds() {
        if (imageRight > imageLeft && imageBottom > imageTop) {
            cropLeft = imageLeft
            cropTop = imageTop
            cropRight = imageRight
            cropBottom = imageBottom
            isInitialized = true
        }
    }

    fun updateViewSize(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        if (abs(width - viewWidth) < 0.5f && abs(height - viewHeight) < 0.5f && isInitialized) return
        viewWidth = width
        viewHeight = height
        updateImageBounds()
    }

    private fun updateImageBounds() {
        val vw = viewWidth
        val vh = viewHeight
        if (vw <= 0f || vh <= 0f) return

        if (sourceBmpWidth > 0 && sourceBmpHeight > 0) {
            val scale = min(vw / sourceBmpWidth.toFloat(), vh / sourceBmpHeight.toFloat())
            val displayedW = sourceBmpWidth * scale
            val displayedH = sourceBmpHeight * scale
            val offsetX = (vw - displayedW) / 2f
            val offsetY = (vh - displayedH) / 2f
            imageLeft = offsetX
            imageTop = offsetY
            imageRight = offsetX + displayedW
            imageBottom = offsetY + displayedH
        } else {
            imageLeft = 0f
            imageTop = 0f
            imageRight = vw
            imageBottom = vh
        }

        if (!isInitialized || cropRight <= cropLeft || cropBottom <= cropTop) {
            cropLeft = imageLeft
            cropTop = imageTop
            cropRight = imageRight
            cropBottom = imageBottom
            isInitialized = true
        } else {
            cropLeft = max(cropLeft, imageLeft)
            cropTop = max(cropTop, imageTop)
            cropRight = min(cropRight, imageRight)
            cropBottom = min(cropBottom, imageBottom)
        }
    }

    internal fun detectTouchMode(x: Float, y: Float, touchTolerance: Float): CropTouchMode {
        if (hypot(x - cropLeft, y - cropTop) < touchTolerance) return CropTouchMode.TOP_LEFT
        if (hypot(x - cropRight, y - cropTop) < touchTolerance) return CropTouchMode.TOP_RIGHT
        if (hypot(x - cropLeft, y - cropBottom) < touchTolerance) return CropTouchMode.BOTTOM_LEFT
        if (hypot(x - cropRight, y - cropBottom) < touchTolerance) return CropTouchMode.BOTTOM_RIGHT

        if (abs(x - cropLeft) < touchTolerance && y in cropTop..cropBottom) return CropTouchMode.EDGE_LEFT
        if (abs(x - cropRight) < touchTolerance && y in cropTop..cropBottom) return CropTouchMode.EDGE_RIGHT
        if (abs(y - cropTop) < touchTolerance && x in cropLeft..cropRight) return CropTouchMode.EDGE_TOP
        if (abs(y - cropBottom) < touchTolerance && x in cropLeft..cropRight) return CropTouchMode.EDGE_BOTTOM

        if (x in cropLeft..cropRight && y in cropTop..cropBottom) return CropTouchMode.MOVE

        return CropTouchMode.NONE
    }

    internal fun applyDrag(mode: CropTouchMode, rawDx: Float, rawDy: Float, minCropSize: Float) {
        val hasBounds = imageRight > imageLeft && imageBottom > imageTop
        val minX = if (hasBounds) imageLeft else 0f
        val maxX = if (hasBounds) imageRight else viewWidth
        val minY = if (hasBounds) imageTop else 0f
        val maxY = if (hasBounds) imageBottom else viewHeight

        when (mode) {
            CropTouchMode.MOVE -> {
                var dx = rawDx
                var dy = rawDy
                if (cropLeft + dx < minX) dx = minX - cropLeft
                if (cropRight + dx > maxX) dx = maxX - cropRight
                if (cropTop + dy < minY) dy = minY - cropTop
                if (cropBottom + dy > maxY) dy = maxY - cropBottom
                cropLeft += dx
                cropRight += dx
                cropTop += dy
                cropBottom += dy
            }

            CropTouchMode.TOP_LEFT -> {
                cropLeft = max(minX, min(cropLeft + rawDx, cropRight - minCropSize))
                cropTop = max(minY, min(cropTop + rawDy, cropBottom - minCropSize))
            }

            CropTouchMode.TOP_RIGHT -> {
                cropRight = min(maxX, max(cropRight + rawDx, cropLeft + minCropSize))
                cropTop = max(minY, min(cropTop + rawDy, cropBottom - minCropSize))
            }

            CropTouchMode.BOTTOM_LEFT -> {
                cropLeft = max(minX, min(cropLeft + rawDx, cropRight - minCropSize))
                cropBottom = min(maxY, max(cropBottom + rawDy, cropTop + minCropSize))
            }

            CropTouchMode.BOTTOM_RIGHT -> {
                cropRight = min(maxX, max(cropRight + rawDx, cropLeft + minCropSize))
                cropBottom = min(maxY, max(cropBottom + rawDy, cropTop + minCropSize))
            }

            CropTouchMode.EDGE_LEFT -> {
                cropLeft = max(minX, min(cropLeft + rawDx, cropRight - minCropSize))
            }

            CropTouchMode.EDGE_TOP -> {
                cropTop = max(minY, min(cropTop + rawDy, cropBottom - minCropSize))
            }

            CropTouchMode.EDGE_RIGHT -> {
                cropRight = min(maxX, max(cropRight + rawDx, cropLeft + minCropSize))
            }

            CropTouchMode.EDGE_BOTTOM -> {
                cropBottom = min(maxY, max(cropBottom + rawDy, cropTop + minCropSize))
            }

            CropTouchMode.NONE -> Unit
        }
    }

    fun calculateCropRect(sourceWidth: Int, sourceHeight: Int): Rect {
        val vw = viewWidth
        val vh = viewHeight
        if (vw <= 0f || vh <= 0f || sourceWidth <= 0 || sourceHeight <= 0) {
            return Rect(0, 0, max(1, sourceWidth), max(1, sourceHeight))
        }

        val scale = min(vw / sourceWidth.toFloat(), vh / sourceHeight.toFloat())
        val displayedWidth = sourceWidth * scale
        val displayedHeight = sourceHeight * scale
        val offsetX = (vw - displayedWidth) / 2f
        val offsetY = (vh - displayedHeight) / 2f

        val validLeft = max(cropLeft, offsetX)
        val validTop = max(cropTop, offsetY)
        val validRight = min(cropRight, offsetX + displayedWidth)
        val validBottom = min(cropBottom, offsetY + displayedHeight)

        if (validRight <= validLeft || validBottom <= validTop) {
            return Rect(0, 0, sourceWidth, sourceHeight)
        }

        val left = max(0, min(sourceWidth - 1, ((validLeft - offsetX) / scale).roundToInt()))
        val top = max(0, min(sourceHeight - 1, ((validTop - offsetY) / scale).roundToInt()))
        val right = max(left + 1, min(sourceWidth, ((validRight - offsetX) / scale).roundToInt()))
        val bottom = max(top + 1, min(sourceHeight, ((validBottom - offsetY) / scale).roundToInt()))

        return Rect(left, top, right, bottom)
    }
}

@Composable
fun CropOverlayCanvas(
    state: CropOverlayState,
    modifier: Modifier = Modifier,
    onCropChanged: (widthPx: Int, heightPx: Int) -> Unit = { _, _ -> }
) {
    val density = LocalDensity.current
    val touchTolerancePx = with(density) { 36.dp.toPx() }
    val minCropSizePx = with(density) { 48.dp.toPx() }
    val borderStrokePx = with(density) { 2.5.dp.toPx() }
    val gridStrokePx = with(density) { 1.dp.toPx() }
    val handleRadiusPx = with(density) { 10.dp.toPx() }
    val handleStrokePx = with(density) { 2.dp.toPx() }

    val scrimColor = Color(0xAF000000)
    val borderColor = Color(0xFF2196F3)
    val gridColor = Color(0x78FFFFFF)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("crop_overlay_canvas")
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        LaunchedEffect(widthPx, heightPx) {
            state.updateViewSize(widthPx, heightPx)
            onCropChanged(state.cropWidthPx, state.cropHeightPx)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val mode = state.detectTouchMode(down.position.x, down.position.y, touchTolerancePx)
                        if (mode != CropTouchMode.NONE) {
                            down.consume()
                        }
                        var prev = down.position
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break
                            if (!change.pressed) {
                                break
                            }
                            if (mode != CropTouchMode.NONE) {
                                val dx = change.position.x - prev.x
                                val dy = change.position.y - prev.y
                                state.applyDrag(mode, dx, dy, minCropSizePx)
                                onCropChanged(state.cropWidthPx, state.cropHeightPx)
                                change.consume()
                            }
                            prev = change.position
                        }
                    }
                }
        ) {
            val left = state.cropLeft
            val top = state.cropTop
            val right = state.cropRight
            val bottom = state.cropBottom
            val cropW = (right - left).coerceAtLeast(0f)
            val cropH = (bottom - top).coerceAtLeast(0f)

            // 1. Scrim outside crop rectangle
            drawRect(scrimColor, Offset.Zero, Size(size.width, top.coerceAtLeast(0f)))
            drawRect(scrimColor, Offset(0f, top), Size(left.coerceAtLeast(0f), cropH))
            drawRect(scrimColor, Offset(right, top), Size((size.width - right).coerceAtLeast(0f), cropH))
            drawRect(scrimColor, Offset(0f, bottom), Size(size.width, (size.height - bottom).coerceAtLeast(0f)))

            // 2. Rule of thirds grid
            val thirdW = cropW / 3f
            val thirdH = cropH / 3f
            drawLine(gridColor, Offset(left + thirdW, top), Offset(left + thirdW, bottom), strokeWidth = gridStrokePx)
            drawLine(gridColor, Offset(left + 2 * thirdW, top), Offset(left + 2 * thirdW, bottom), strokeWidth = gridStrokePx)
            drawLine(gridColor, Offset(left, top + thirdH), Offset(right, top + thirdH), strokeWidth = gridStrokePx)
            drawLine(gridColor, Offset(left, top + 2 * thirdH), Offset(right, top + 2 * thirdH), strokeWidth = gridStrokePx)

            // 3. Crop border
            drawRect(
                color = borderColor,
                topLeft = Offset(left, top),
                size = Size(cropW, cropH),
                style = Stroke(width = borderStrokePx)
            )

            // 4. Corner and edge handles
            fun drawHandle(cx: Float, cy: Float, r: Float) {
                drawCircle(Color.White, radius = r, center = Offset(cx, cy))
                drawCircle(borderColor, radius = r, center = Offset(cx, cy), style = Stroke(width = handleStrokePx))
            }

            drawHandle(left, top, handleRadiusPx)
            drawHandle(right, top, handleRadiusPx)
            drawHandle(left, bottom, handleRadiusPx)
            drawHandle(right, bottom, handleRadiusPx)

            val edgeR = handleRadiusPx * 0.7f
            drawHandle((left + right) / 2f, top, edgeR)
            drawHandle((left + right) / 2f, bottom, edgeR)
            drawHandle(left, (top + bottom) / 2f, edgeR)
            drawHandle(right, (top + bottom) / 2f, edgeR)
        }
    }
}
