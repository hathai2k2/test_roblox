package com.v1.example.khoi_phuc_code

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import com.v1.example.test_ronlox.R
import com.v1.example.test_ronlox.getBitmapFromVectorDrawable
import kotlin.math.max
import kotlin.math.min

class StickerOverlay(context: Context) : android.view.View(context) {
    val stickers = mutableListOf<Sticker>()
    var selectedSticker: Sticker? = null
    var isResizing = false
    var lastTouchX = 0f
    var lastTouchY = 0f


    // Multi-touch gesture support
    private var initialDistance = 0f
    private var initialRotation = 0f
    private var initialScale = 1f
    private var lastPointerCount = 0

    // Resize gesture support
    private var resizeStartDistance = 0f
    private var resizeStartScale = 1f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Enable high-quality bitmap filtering for better scaling
        isFilterBitmap = true
        isDither = true
    }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2 * resources.displayMetrics.density
        isAntiAlias = true
    }

    private val handleSize = 40f * resources.displayMetrics.density

    // Control button icons
    private val closeIcon: Bitmap = getBitmapFromVectorDrawable(context, R.drawable.ic_close)
    private val flipIcon: Bitmap = getBitmapFromVectorDrawable(context, R.drawable.ic_flip)
    private val doneIcon: Bitmap = getBitmapFromVectorDrawable(context, R.drawable.ic_done)
    private val resizeIcon: Bitmap = getBitmapFromVectorDrawable(context, R.drawable.ic_resize)

    // Button rectangles
    private var closeButtonRect = RectF()
    private var flipButtonRect = RectF()
    private var doneButtonRect = RectF()
    private var resizeButtonRect = RectF()

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw stickers
        stickers.forEach { sticker ->
            val rectF = RectF(
                sticker.x,
                sticker.y,
                sticker.x + sticker.width * sticker.scale,
                sticker.y + sticker.height * sticker.scale
            )

            canvas.save()
            // Apply rotation
            canvas.rotate(sticker.rotation, rectF.centerX(), rectF.centerY())

            // Apply flip if needed
            if (sticker.isFlipped) {
                canvas.scale(-1f, 1f, rectF.centerX(), rectF.centerY())
            }

            // Draw sticker
            canvas.drawBitmap(sticker.bitmap, null, rectF, paint)

            // Draw border if selected (still in transformed context)
            if (sticker == selectedSticker) {
                canvas.drawRect(rectF, borderPaint)
            }

            canvas.restore()

            // Calculate button positions (outside rotation context)
            if (sticker == selectedSticker) {
                // Calculate rotated button positions
                // Top-left: Close button
                val closePoint = rotatePoint(
                    rectF.left,
                    rectF.top,
                    rectF.centerX(),
                    rectF.centerY(),
                    sticker.rotation
                )
                closeButtonRect = RectF(
                    closePoint.x - handleSize / 2,
                    closePoint.y - handleSize / 2,
                    closePoint.x + handleSize / 2,
                    closePoint.y + handleSize / 2
                )

                // Top-right: Flip button
                val flipPoint = rotatePoint(
                    rectF.right,
                    rectF.top,
                    rectF.centerX(),
                    rectF.centerY(),
                    sticker.rotation
                )
                flipButtonRect = RectF(
                    flipPoint.x - handleSize / 2,
                    flipPoint.y - handleSize / 2,
                    flipPoint.x + handleSize / 2,
                    flipPoint.y + handleSize / 2
                )

                // Bottom-left: Done button
                val donePoint = rotatePoint(
                    rectF.left,
                    rectF.bottom,
                    rectF.centerX(),
                    rectF.centerY(),
                    sticker.rotation
                )
                doneButtonRect = RectF(
                    donePoint.x - handleSize / 2,
                    donePoint.y - handleSize / 2,
                    donePoint.x + handleSize / 2,
                    donePoint.y + handleSize / 2
                )

                // Bottom-right: Resize button
                val resizePoint = rotatePoint(
                    rectF.right,
                    rectF.bottom,
                    rectF.centerX(),
                    rectF.centerY(),
                    sticker.rotation
                )
                resizeButtonRect = RectF(
                    resizePoint.x - handleSize / 2,
                    resizePoint.y - handleSize / 2,
                    resizePoint.x + handleSize / 2,
                    resizePoint.y + handleSize / 2
                )

                // Draw buttons so they stay upright
                canvas.drawBitmap(closeIcon, null, closeButtonRect, null)
                canvas.drawBitmap(flipIcon, null, flipButtonRect, null)
                canvas.drawBitmap(doneIcon, null, doneButtonRect, null)
                canvas.drawBitmap(resizeIcon, null, resizeButtonRect, null)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val xTouch = event.x
        val yTouch = event.y
        val pointerCount = event.pointerCount

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = xTouch
                lastTouchY = yTouch
                isResizing = false
                lastPointerCount = 1

                // FIRST: Check if tapping on control buttons of currently selected sticker
                selectedSticker?.let { sticker ->
                    android.util.Log.d("StickerOverlay", "Checking buttons for selected sticker")

                    // Close button (top-left) - delete sticker
                    if (closeButtonRect.contains(xTouch, yTouch)) {
                        android.util.Log.d(
                            "StickerOverlay",
                            "Close button tapped - deleting sticker"
                        )
                        stickers.remove(sticker)
                        selectedSticker = null
                        invalidate()
                        return true
                    }

                    // Flip button (top-right) - flip sticker horizontally
                    if (flipButtonRect.contains(xTouch, yTouch)) {
                        android.util.Log.d(
                            "StickerOverlay",
                            "Flip button tapped - flipping sticker"
                        )
                        sticker.isFlipped = !sticker.isFlipped
                        invalidate()
                        return true
                    }

                    // Done button (bottom-left) - deselect sticker
                    if (doneButtonRect.contains(xTouch, yTouch)) {
                        android.util.Log.d(
                            "StickerOverlay",
                            "Done button tapped - deselecting sticker"
                        )
                        selectedSticker = null
                        invalidate()
                        return true
                    }

                    // Resize button (bottom-right) - start resizing
                    if (resizeButtonRect.contains(xTouch, yTouch)) {
                        android.util.Log.d(
                            "StickerOverlay",
                            "Resize button tapped - starting resize"
                        )
                        isResizing = true

                        // Store initial state for smooth resizing
                        val centerX = sticker.x + (sticker.width * sticker.scale) / 2
                        val centerY = sticker.y + (sticker.height * sticker.scale) / 2
                        val dx = xTouch - centerX
                        val dy = yTouch - centerY
                        resizeStartDistance = kotlin.math.sqrt(dx * dx + dy * dy)
                        resizeStartScale = sticker.scale

                        invalidate()
                        return true
                    }
                }

                // SECOND: Check if tapping on a sticker (select it or keep current selection)
                val tappedSticker = findStickerAt(xTouch, yTouch)
                if (tappedSticker != null) {
                    android.util.Log.d(
                        "StickerOverlay",
                        "Sticker found - selecting/keeping selection"
                    )
                    selectedSticker = tappedSticker
                    invalidate()
                    return true
                }

                // THIRD: Không có sticker nào được touch
                // Nếu có sticker đang được chọn, deselect nó (giống như nhấn Done button)
                if (selectedSticker != null) {
                    android.util.Log.d("StickerOverlay", "Tapped outside sticker - deselecting")
                    selectedSticker = null
                    invalidate()
                    return true  // Consume event để không pass through
                }

                // Nếu body part color change enabled, pass through để body parts có thể nhận event
                if (enableBodyPartColorChange) {
                    android.util.Log.d(
                        "StickerOverlay",
                        "No sticker selected, body change enabled - passing through"
                    )
                    return false  // Pass through to views below
                }

                android.util.Log.d(
                    "StickerOverlay",
                    "No sticker selected, body change disabled - passing through"
                )
                return false  // Pass through
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Second finger down - start multi-touch gesture
                if (pointerCount == 2 && selectedSticker != null) {
                    initialDistance = calculateDistance(event)
                    initialRotation = calculateRotation(event)
                    initialScale = selectedSticker!!.scale
                    lastPointerCount = 2
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                selectedSticker?.let { sticker ->
                    // Two-finger gestures (zoom and rotate)
                    if (pointerCount == 2 && lastPointerCount == 2) {
                        // Calculate new distance and rotation
                        val currentDistance = calculateDistance(event)
                        val currentRotation = calculateRotation(event)

                        // Zoom: scale based on distance change (unlimited)
                        if (initialDistance > 0) {
                            val scaleFactor = currentDistance / initialDistance
                            sticker.scale = max(
                                0.1f,
                                initialScale * scaleFactor
                            )  // Only minimum limit for usability
                        }

                        // Rotate: update rotation based on angle change
                        val rotationDelta = currentRotation - initialRotation
                        sticker.rotation = (sticker.rotation + rotationDelta) % 360f
                        initialRotation = currentRotation

                        invalidate()
                        return true
                    }

                    // Single finger gestures (move or resize)
                    if (pointerCount == 1 && lastPointerCount == 1) {
                        if (isResizing) {
                            // Smooth resize using initial state (unlimited size)
                            val centerX = sticker.x + (sticker.width * sticker.scale) / 2
                            val centerY = sticker.y + (sticker.height * sticker.scale) / 2
                            val dx = xTouch - centerX
                            val dy = yTouch - centerY
                            val currentDistance = kotlin.math.sqrt(dx * dx + dy * dy)

                            // Calculate scale based on distance ratio from initial state
                            if (resizeStartDistance > 0) {
                                val scaleFactor = currentDistance / resizeStartDistance
                                sticker.scale =
                                    max(0.1f, resizeStartScale * scaleFactor)  // Only minimum limit
                            }

                            invalidate()
                            return true
                        }

                        // Move sticker
                        val dx = xTouch - lastTouchX
                        val dy = yTouch - lastTouchY

                        val stickerWidth = sticker.width * sticker.scale
                        val stickerHeight = sticker.height * sticker.scale

                        val newX = sticker.x + dx
                        val newY = sticker.y + dy

                        // Keep within bounds
                        sticker.x = min(max(newX, 0f), (width - stickerWidth).coerceAtLeast(0f))
                        sticker.y = min(max(newY, 0f), (height - stickerHeight).coerceAtLeast(0f))

                        lastTouchX = xTouch
                        lastTouchY = yTouch
                        invalidate()
                        return true
                    }

                    lastPointerCount = pointerCount
                    return true
                }
                return false  // No sticker selected, pass through
            }

            MotionEvent.ACTION_POINTER_UP -> {
                lastPointerCount = pointerCount - 1
                if (lastPointerCount == 1) {
                    // Reset to single touch mode
                    lastTouchX = event.getX(0)
                    lastTouchY = event.getY(0)
                }
                return selectedSticker != null
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val hadSelectedSticker = selectedSticker != null
                isResizing = false
                lastPointerCount = 0
                // Don't deselect sticker on ACTION_UP to keep it selected
                invalidate()
                return hadSelectedSticker  // Only consume if there was a sticker
            }
        }
        return false  // Default: pass through
    }

    private fun calculateDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun calculateRotation(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(1) - event.getX(0)
        val dy = event.getY(1) - event.getY(0)
        return Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    private fun findStickerAt(x: Float, y: Float): Sticker? {
        return stickers.reversed().find {
            RectF(it.x, it.y, it.x + it.width * it.scale, it.y + it.height * it.scale)
                .contains(x, y)
        }
    }

    private fun rotatePoint(x: Float, y: Float, cx: Float, cy: Float, angle: Float): PointF {
        val rad = Math.toRadians(angle.toDouble())
        val cos = Math.cos(rad)
        val sin = Math.sin(rad)
        val dx = x - cx
        val dy = y - cy
        return PointF((dx * cos - dy * sin + cx).toFloat(), (dx * sin + dy * cos + cy).toFloat())
    }
}