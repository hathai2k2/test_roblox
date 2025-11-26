package com.v1.example.test_ronlox

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class StickerImageViewV2(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    var onBlueRegionClick: ((index: Int, rect: Rect) -> Unit)? = null
    private var bitmap: Bitmap? = null  // Ảnh hiện tại
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

    init {
        setBackgroundColor(Color.WHITE)
    }

    /** Set bitmap template */
    fun setBitmap(bmp: Bitmap) {
        // Đảm bảo bitmap có config ARGB_8888 để giữ màu đúng
        bitmap = if (bmp.config == Bitmap.Config.ARGB_8888) {
            bmp
        } else {
            bmp.copy(Bitmap.Config.ARGB_8888, true)
        }
        GlobalScope.launch(Dispatchers.Default) { // Hoặc Scope riêng
            val (regions, masks) = findBlueRegionsWithMasks(bitmap!!)

            // Quay lại Main Thread để cập nhật UI/Variables
            withContext(Dispatchers.Main) {
                blueRegions = regions
                blueRegionMasks.clear()
                masks.forEachIndexed { index, mask ->
                    blueRegionMasks[index] = mask
                }

                // Lưu trạng thái ban đầu vào history (cũng là copy ảnh nặng)
                historyStack.clear()
                historyStack.add(bitmap!!.copy(Bitmap.Config.ARGB_8888, true))

                // ... Log và các bước hoàn tất khác

                requestLayout()
                invalidate()
            }
        }
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
        // Lưu replacement mới
        replacementBitmaps[index] = newBitmap
        // Merge replacement mới lên bitmap
        bitmap = if (mask != null) {
            mergeImageOnRectWithMask(bitmap!!, newBitmap, rect, mask)
        } else {
            mergeImageOnRect(bitmap!!, newBitmap, rect)
        }
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
        // 1. LẤY TẤT CẢ PIXEL MỘT LẦN (Tăng tốc độ truy cập pixel)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Mảng lưu trạng thái đã thăm (visited array)
        val visited = BooleanArray(width * height)
        val regions = mutableListOf<Rect>()
        val masks = mutableListOf<Bitmap>()

        // Dùng mảng Float 3 phần tử để tái sử dụng, tránh cấp phát bộ nhớ liên tục trong vòng lặp
        val hsv = FloatArray(3)

        // 2. Tối ưu hóa hàm kiểm tra màu xanh (dùng mảng pixels)
        fun isBlue(x: Int, y: Int): Boolean {
            if (x < 0 || y < 0 || x >= width || y >= height) return false
            val index = y * width + x
            val pixel = pixels[index]

            Color.colorToHSV(pixel, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val value = hsv[2]
            // Điều kiện màu xanh (tương tự như cũ)
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
                val startIndex = yStart * width + xStart
                if (visited[startIndex] || !isBlue(xStart, yStart)) continue

                val bounds = Rect(xStart, yStart, xStart, yStart)
                val queue = ArrayDeque<Pair<Int, Int>>()
                // Lưu index 1D thay vì Pair<Int, Int> để giảm overhead đối tượng
                val bluePixelIndices = mutableListOf<Int>()

                queue.add(Pair(xStart, yStart))
                visited[startIndex] = true

                while (queue.isNotEmpty()) {
                    val (x, y) = queue.removeFirst()
                    bluePixelIndices.add(y * width + x) // Lưu index 1D

                    // Cập nhật bounds
                    bounds.left = min(bounds.left, x)
                    bounds.top = min(bounds.top, y)
                    bounds.right = max(bounds.right, x)
                    bounds.bottom = max(bounds.bottom, y)

                    for (dir in directions) {
                        val nx = x + dir[0]
                        val ny = y + dir[1]
                        val nextIndex = ny * width + nx

                        if (nx in 0 until width && ny in 0 until height &&
                            !visited[nextIndex] && isBlue(nx, ny)
                        ) {
                            visited[nextIndex] = true
                            queue.add(Pair(nx, ny))
                        }
                    }
                }

                // Tối ưu hóa: Bỏ qua các vùng quá nhỏ (nếu cần)
                // if (bounds.width() < MIN_SIZE || bounds.height() < MIN_SIZE) continue

                // 3. TẠO MASK BẰNG setPixels (Nhanh hơn drawPoint)
                val maskWidth = bounds.width()
                val maskHeight = bounds.height()

                val maskPixels = IntArray(maskWidth * maskHeight) { Color.TRANSPARENT } // Khởi tạo mảng trong suốt

                // Điền pixel màu trắng (Mask)
                bluePixelIndices.forEach { index1D ->
                    val x = index1D % width
                    val y = index1D / width

                    // Chuyển tọa độ Global về tọa độ Local của Mask
                    val localX = x - bounds.left
                    val localY = y - bounds.top

                    // Gán màu Trắng vào mảng Mask
                    val maskIndex = localY * maskWidth + localX
                    if (maskIndex in 0 until maskPixels.size) {
                        maskPixels[maskIndex] = Color.WHITE
                    }
                }

                // Tạo Bitmap và setPixels một lần
                val mask = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
                mask.setPixels(maskPixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)

                regions.add(bounds)
                masks.add(mask)
            }
        }
        return Pair(regions, masks)
    }

    /** Merge với mask để chỉ vẽ vào pixel xanh */
    private fun mergeImageOnRectWithMask(
        original: Bitmap,
        overlay: Bitmap,
        rect: Rect,
        mask: Bitmap
    ): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Scale overlay khớp với rect
        val scaledOverlay = Bitmap.createScaledBitmap(overlay, rect.width(), rect.height(), true)

        // Tạo bitmap tạm để apply mask với background TRANSPARENT
        val maskedOverlay =
            Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
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