package com.v1.example.test_ronlox

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class StickerImageView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    // Callback khi vùng xanh được click
    var onBlueRegionClick: ((index: Int, rect: Rect) -> Unit)? = null
    private var bitmap: Bitmap? = null                // Ảnh hiện tại
    private val stickers = mutableListOf<Sticker>()   // Sticker/overlay
    var blueRegions = listOf<Rect>()                 // Vùng màu xanh
        private set

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var selectedSticker: Sticker? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Bitmap thay thế cho từng vùng xanh
    private val replacementBitmaps = mutableMapOf<Int, Bitmap>()

    // Lưu mask của từng vùng xanh
    private val blueRegionMasks = mutableMapOf<Int, Bitmap>()

    // History để undo - lưu stack các trạng thái bitmap
    private val historyStack = mutableListOf<Bitmap>()
    private val maxHistorySize = 20 // Giới hạn số lần undo

    /** Set bitmap template */
    fun setBitmap(bmp: Bitmap) {
        // Đảm bảo bitmap có config ARGB_8888 để giữ màu đúng
        bitmap = if (bmp.config == Bitmap.Config.ARGB_8888) {
            bmp
        } else {
            bmp.copy(Bitmap.Config.ARGB_8888, true)
        }

        val (regions, masks) = findBlueRegionsWithMasks(bitmap!!)
        blueRegions = regions
        blueRegionMasks.clear()
        masks.forEachIndexed { index, mask ->
            blueRegionMasks[index] = mask
        }

        // Lưu trạng thái ban đầu vào history
        historyStack.clear()
        historyStack.add(bitmap!!.copy(Bitmap.Config.ARGB_8888, true))

        // Debug: log tất cả vùng xanh
        if (blueRegions.isEmpty()) {
            android.util.Log.d("StickerImageView", "Không tìm thấy vùng màu xanh nào!")
        } else {
            blueRegions.forEachIndexed { index, rect ->
                android.util.Log.d("StickerImageView", "Vùng xanh $index: $rect")
            }
        }
        requestLayout()
        invalidate()
    }

    /** Thêm sticker bình thường */
    fun addSticker(stickerBitmap: Bitmap, x: Float, y: Float, width: Int, height: Int) {
        stickers.add(Sticker(stickerBitmap, x, y, width, height))
        invalidate()
    }

    /** Thay thế vùng xanh theo index */
    fun replaceBlueRegion(index: Int, newBitmap: Bitmap) {
        if (bitmap == null || index !in blueRegions.indices) return

        // Lưu trạng thái trước khi thay đổi
        saveToHistory()

        val rect = blueRegions[index]
        val mask = blueRegionMasks[index]

        if (mask != null) {
            bitmap = mergeImageOnRectWithMask(bitmap!!, newBitmap, rect, mask)
        } else {
            bitmap = mergeImageOnRect(bitmap!!, newBitmap, rect)
        }

        replacementBitmaps[index] = newBitmap
        android.util.Log.d("StickerImageView", "Đã replace vùng xanh $index")
        invalidate()
    }

    /** Lưu trạng thái hiện tại vào history */
    private fun saveToHistory() {
        bitmap?.let { bmp ->
            val snapshot = bmp.copy(Bitmap.Config.ARGB_8888, true)
            historyStack.add(snapshot)

            // Giới hạn số lượng history
            if (historyStack.size > maxHistorySize) {
                historyStack.removeAt(0)
            }

            android.util.Log.d("StickerImageView", "History size: ${historyStack.size}")
        }
    }

    /** Undo - quay về trạng thái trước đó */
    fun undo() {
        if (historyStack.size > 1) {
            // Xóa trạng thái hiện tại
            historyStack.removeAt(historyStack.size - 1)

            // Lấy trạng thái trước đó
            val previousState = historyStack.last()
            bitmap = previousState.copy(Bitmap.Config.ARGB_8888, true)

            android.util.Log.d("StickerImageView", "Đã undo, còn ${historyStack.size} trạng thái")
            invalidate()
        } else {
            android.util.Log.d("StickerImageView", "Không thể undo thêm, đã ở trạng thái ban đầu")
        }
    }

    /** Kiểm tra có thể undo không */
    fun canUndo(): Boolean {
        return historyStack.size > 1
    }

    /** Xóa toàn bộ history và reset về ban đầu */
    fun resetToOriginal() {
        if (historyStack.isNotEmpty()) {
            val original = historyStack.first()
            bitmap = original.copy(Bitmap.Config.ARGB_8888, true)

            // Giữ lại trạng thái ban đầu
            val firstState = historyStack.first()
            historyStack.clear()
            historyStack.add(firstState)

            replacementBitmaps.clear()

            android.util.Log.d("StickerImageView", "Đã reset về trạng thái ban đầu")
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        bitmap?.let { bmp ->
            // Vẽ bitmap vừa scale theo view - KHÔNG dùng filter để giữ màu gốc
            val destRect = Rect(0, 0, width, height)
            val bitmapPaint = Paint().apply {
                isAntiAlias = false
                isFilterBitmap = false
                isDither = false
            }
            canvas.drawBitmap(bmp, null, destRect, bitmapPaint)

            // Scale để mapping touch vào vùng xanh
            val scaleX = width.toFloat() / bmp.width
            val scaleY = height.toFloat() / bmp.height

            // Vẽ overlay vùng xanh debug (tắt để không bị màu xanh nhạt)
            // Uncomment nếu muốn xem vùng xanh để debug
            /*
            val debugPaint = Paint().apply {
                color = Color.argb(80, 0, 0, 255)
                style = Paint.Style.FILL
            }
            blueRegions.forEachIndexed { index, rect ->
                val left = rect.left * scaleX
                val top = rect.top * scaleY
                val right = rect.right * scaleX
                val bottom = rect.bottom * scaleY
                canvas.drawRect(left, top, right, bottom, debugPaint)
            }
            */
        }

        // Vẽ sticker
        stickers.forEach { sticker ->
            val rectF = RectF(
                sticker.x,
                sticker.y,
                sticker.x + sticker.width * sticker.scale,
                sticker.y + sticker.height * sticker.scale
            )
            val save = canvas.save()
            canvas.rotate(sticker.rotation, rectF.centerX(), rectF.centerY())
            canvas.drawBitmap(sticker.bitmap, null, rectF, paint)
            canvas.restoreToCount(save)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val xTouch = event.x
        val yTouch = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = xTouch
                lastTouchY = yTouch

                // Check click vào sticker trước
                selectedSticker = findStickerAt(xTouch, yTouch)

                // Nếu không click sticker, check click vào vùng xanh
                if (selectedSticker == null) {
                    bitmap?.let { bmp ->
                        val scaleX = width.toFloat() / bmp.width
                        val scaleY = height.toFloat() / bmp.height

                        blueRegions.forEachIndexed { index, rect ->
                            val left = rect.left * scaleX
                            val top = rect.top * scaleY
                            val right = rect.right * scaleX
                            val bottom = rect.bottom * scaleY

                            if (xTouch in left..right && yTouch in top..bottom) {
                                // Chỉ gọi callback, KHÔNG replace ở đây
                                onBlueRegionClick?.invoke(index, rect)
                                android.util.Log.d("StickerImageView", "Click vào vùng xanh $index")
                            }
                        }
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Nếu đang kéo sticker
                selectedSticker?.let { sticker ->
                    val dx = xTouch - lastTouchX
                    val dy = yTouch - lastTouchY
                    sticker.x += dx
                    sticker.y += dy
                    lastTouchX = xTouch
                    lastTouchY = yTouch
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                selectedSticker = null
            }
        }

        return true
    }


    private fun findStickerAt(x: Float, y: Float): Sticker? {
        return stickers.reversed().find { sticker ->
            val rect = RectF(
                sticker.x,
                sticker.y,
                sticker.x + sticker.width * sticker.scale,
                sticker.y + sticker.height * sticker.scale
            )
            rect.contains(x, y)
        }
    }

    data class Sticker(
        val bitmap: Bitmap,
        var x: Float,
        var y: Float,
        val width: Int,
        val height: Int,
        var scale: Float = 1f,
        var rotation: Float = 0f
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        bitmap?.let { bmp ->
            val maxWidth = MeasureSpec.getSize(widthMeasureSpec)
            val maxHeight = MeasureSpec.getSize(heightMeasureSpec)

            val bmpWidth = bmp.width
            val bmpHeight = bmp.height

            val ratio = bmpWidth.toFloat() / bmpHeight

            var finalWidth = bmpWidth
            var finalHeight = bmpHeight

            if (finalWidth > maxWidth) {
                finalWidth = maxWidth
                finalHeight = (finalWidth / ratio).toInt()
            }
            if (finalHeight > maxHeight) {
                finalHeight = maxHeight
                finalWidth = (finalHeight * ratio).toInt()
            }

            setMeasuredDimension(finalWidth, finalHeight)
        } ?: super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /** Tìm vùng xanh VÀ tạo mask cho từng vùng */
    private fun findBlueRegionsWithMasks(bitmap: Bitmap): Pair<List<Rect>, List<Bitmap>> {
        val width = bitmap.width
        val height = bitmap.height
        val visited = Array(height) { BooleanArray(width) }
        val regions = mutableListOf<Rect>()
        val masks = mutableListOf<Bitmap>()

        fun isBlue(x: Int, y: Int): Boolean {
            val pixel = bitmap.getPixel(x, y)
            val hsv = FloatArray(3)
            Color.colorToHSV(pixel, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val value = hsv[2]
            return hue in 180f..260f && sat > 0.2f && value > 0.2f
        }

        val directions = arrayOf(
            intArrayOf(1, 0),
            intArrayOf(-1, 0),
            intArrayOf(0, 1),
            intArrayOf(0, -1)
        )

        for (yStart in 0 until height) {
            for (xStart in 0 until width) {
                if (visited[yStart][xStart] || !isBlue(xStart, yStart)) continue

                val bounds = Rect(xStart, yStart, xStart, yStart)
                val queue = ArrayDeque<Pair<Int, Int>>()
                val bluePixels = mutableListOf<Pair<Int, Int>>()

                queue.add(Pair(xStart, yStart))
                visited[yStart][xStart] = true

                while (queue.isNotEmpty()) {
                    val (x, y) = queue.removeFirst()
                    bluePixels.add(Pair(x, y))

                    bounds.left = min(bounds.left, x)
                    bounds.top = min(bounds.top, y)
                    bounds.right = max(bounds.right, x)
                    bounds.bottom = max(bounds.bottom, y)

                    for (dir in directions) {
                        val nx = x + dir[0]
                        val ny = y + dir[1]
                        if (nx in 0 until width && ny in 0 until height &&
                            !visited[ny][nx] && isBlue(nx, ny)
                        ) {
                            visited[ny][nx] = true
                            queue.add(Pair(nx, ny))
                        }
                    }
                }

                // Tạo mask cho vùng này
                val maskWidth = bounds.width()
                val maskHeight = bounds.height()
                val mask = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)

                // Vẽ mask: trắng ở pixel xanh, trong suốt ở chỗ khác
                val maskCanvas = Canvas(mask)
                maskCanvas.drawColor(Color.TRANSPARENT)

                val whitePaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }

                bluePixels.forEach { (x, y) ->
                    val localX = x - bounds.left
                    val localY = y - bounds.top
                    maskCanvas.drawPoint(localX.toFloat(), localY.toFloat(), whitePaint)
                }

                regions.add(bounds)
                masks.add(mask)
            }
        }
        return Pair(regions, masks)
    }

    /** Merge với mask để chỉ vẽ vào pixel xanh */
    private fun mergeImageOnRectWithMask(original: Bitmap, overlay: Bitmap, rect: Rect, mask: Bitmap): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Scale overlay khớp với rect
        val scaledOverlay = Bitmap.createScaledBitmap(overlay, rect.width(), rect.height(), true)

        // Tạo bitmap tạm để apply mask với background TRANSPARENT
        val maskedOverlay = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(maskedOverlay)

        // ĐẢM BẢO background trong suốt
        maskedOverlay.eraseColor(Color.TRANSPARENT)

        // Vẽ overlay
        tempCanvas.drawBitmap(scaledOverlay, 0f, 0f, null)

        // Apply mask - chỉ giữ lại phần có mask
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        tempCanvas.drawBitmap(mask, 0f, 0f, maskPaint)

        // Vẽ lên bitmap gốc - CHỈ ghi đè vùng có màu
        val finalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
        canvas.drawBitmap(maskedOverlay, rect.left.toFloat(), rect.top.toFloat(), finalPaint)

        return result
    }

    /** Merge overlay vào rect (fallback nếu không có mask) */
    private fun mergeImageOnRect(original: Bitmap, overlay: Bitmap, rect: Rect): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val scaledOverlay = Bitmap.createScaledBitmap(overlay, rect.width(), rect.height(), true)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
        canvas.drawBitmap(scaledOverlay, rect.left.toFloat(), rect.top.toFloat(), paint)
        paint.xfermode = null

        return result
    }
}