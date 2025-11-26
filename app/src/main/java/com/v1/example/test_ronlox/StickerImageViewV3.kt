//package com.v1.example.test_ronlox
//
//import android.content.Context
//import android.graphics.*
//import android.util.AttributeSet
//import android.view.MotionEvent
//import android.view.View
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import org.opencv.android.Utils
//import org.opencv.core.Core
//import org.opencv.core.CvType
//import org.opencv.core.Mat
//import org.opencv.core.MatOfPoint
//import org.opencv.core.Scalar
//import org.opencv.imgproc.Imgproc
//
//class StickerImageViewV3(context: Context, attrs: AttributeSet?) : View(context, attrs) {
//    var onBlueRegionClick: ((index: Int, rect: Rect) -> Unit)? = null
//    private var bitmap: Bitmap? = null  // Ảnh hiện tại
//    private val stickers = mutableListOf<Sticker>()   // Sticker/overlay
//    var blueRegions = listOf<Rect>()                 // Vùng màu xanh
//        private set
//
//    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
//    private var selectedSticker: Sticker? = null
//    private var lastTouchX = 0f
//    private var lastTouchY = 0f
//
//    // Bitmap thay thế cho từng vùng xanh
//    private val replacementBitmaps = mutableMapOf<Int, Bitmap>()
//
//    // Lưu mask của từng vùng xanh
//    private val blueRegionMasks = mutableMapOf<Int, Bitmap>()
//
//    // History để undo - lưu stack các trạng thái bitmap
//    private val historyStack = mutableListOf<Bitmap>()
//    private val maxHistorySize = 20 // Giới hạn số lần undo
//
//    init {
//        setBackgroundColor(Color.WHITE)
//    }
//
//    /** Set bitmap template */
//    fun setBitmap(bmp: Bitmap) {
//        // Đảm bảo bitmap có config ARGB_8888 để giữ màu đúng
//        bitmap = if (bmp.config == Bitmap.Config.ARGB_8888) {
//            bmp
//        } else {
//            bmp.copy(Bitmap.Config.ARGB_8888, true)
//        }
//        GlobalScope.launch(Dispatchers.Default) { // Hoặc Scope riêng
//            val (regions, masks) = findBlueRegionsWithMasks(bitmap!!)
//
//            // Quay lại Main Thread để cập nhật UI/Variables
//            withContext(Dispatchers.Main) {
//                blueRegions = regions
//                blueRegionMasks.clear()
//                masks.forEachIndexed { index, mask ->
//                    blueRegionMasks[index] = mask
//                }
//
//                // Lưu trạng thái ban đầu vào history (cũng là copy ảnh nặng)
//                historyStack.clear()
//                historyStack.add(bitmap!!.copy(Bitmap.Config.ARGB_8888, true))
//
//                // ... Log và các bước hoàn tất khác
//
//                requestLayout()
//                invalidate()
//            }
//        }
//    }
//
//    /** Thêm sticker bình thường */
//    fun addSticker(stickerBitmap: Bitmap, x: Float, y: Float, width: Int, height: Int) {
//        stickers.add(Sticker(stickerBitmap, x, y, width, height))
//        invalidate()
//    }
//    /** Thay thế vùng xanh theo index */
//    fun replaceBlueRegion(index: Int, newBitmap: Bitmap) {
//        if (bitmap == null || index !in blueRegions.indices) return
//        // Lưu trạng thái trước khi thay đổi
//        saveToHistory()
//
//        val rect = blueRegions[index]
//        val mask = blueRegionMasks[index]
//        // Lưu replacement mới
//        replacementBitmaps[index] = newBitmap
//        // Merge replacement mới lên bitmap
//        bitmap = if (mask != null) {
//            mergeImageOnRectWithMask(bitmap!!, newBitmap, rect, mask)
//        } else {
//            mergeImageOnRect(bitmap!!, newBitmap, rect)
//        }
//        invalidate()
//    }
//
//    /** Lưu trạng thái hiện tại vào history */
//    private fun saveToHistory() {
//        bitmap?.let { bmp ->
//            val snapshot = bmp.copy(Bitmap.Config.ARGB_8888, true)
//            historyStack.add(snapshot)
//            // Giới hạn số lượng history
//            if (historyStack.size > maxHistorySize) {
//                historyStack.removeAt(0)
//            }
//            android.util.Log.d("StickerImageView", "History size: ${historyStack.size}")
//        }
//    }
//    /** Xóa toàn bộ history và reset về ban đầu */
//    fun resetToOriginal() {
//        if (historyStack.isNotEmpty()) {
//            val original = historyStack.first()
//            bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
//
//            // Giữ lại trạng thái ban đầu
//            val firstState = historyStack.first()
//            historyStack.clear()
//            historyStack.add(firstState)
//
//            replacementBitmaps.clear()
//
//            android.util.Log.d("StickerImageView", "Đã reset về trạng thái ban đầu")
//            invalidate()
//        }
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        bitmap?.let { bmp ->
//            // Vẽ bitmap vừa scale theo view - KHÔNG dùng filter để giữ màu gốc
//            val destRect = Rect(0, 0, width, height)
//            val bitmapPaint = Paint().apply {
//                isAntiAlias = false
//                isFilterBitmap = false
//                isDither = false
//            }
//            canvas.drawBitmap(bmp, null, destRect, bitmapPaint)
//        }
//        // Vẽ sticker
//        stickers.forEach { sticker ->
//            val rectF = RectF(
//                sticker.x,
//                sticker.y,
//                sticker.x + sticker.width * sticker.scale,
//                sticker.y + sticker.height * sticker.scale
//            )
//            val save = canvas.save()
//            canvas.rotate(sticker.rotation, rectF.centerX(), rectF.centerY())
//            canvas.drawBitmap(sticker.bitmap, null, rectF, paint)
//            canvas.restoreToCount(save)
//        }
//    }
//
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        val xTouch = event.x
//        val yTouch = event.y
//        when (event.actionMasked) {
//            MotionEvent.ACTION_DOWN -> {
//                lastTouchX = xTouch
//                lastTouchY = yTouch
//
//                // Check click vào sticker trước
//                selectedSticker = findStickerAt(xTouch, yTouch)
//
//                // Nếu không click sticker, check click vào vùng xanh
//                if (selectedSticker == null) {
//                    bitmap?.let { bmp ->
//                        val scaleX = width.toFloat() / bmp.width
//                        val scaleY = height.toFloat() / bmp.height
//
//                        blueRegions.forEachIndexed { index, rect ->
//                            val left = rect.left * scaleX
//                            val top = rect.top * scaleY
//                            val right = rect.right * scaleX
//                            val bottom = rect.bottom * scaleY
//
//                            if (xTouch in left..right && yTouch in top..bottom) {
//                                // Chỉ gọi callback, KHÔNG replace ở đây
//                                onBlueRegionClick?.invoke(index, rect)
//                                android.util.Log.d("StickerImageView", "Click vào vùng xanh $index")
//                            }
//                        }
//                    }
//                }
//            }
//
//            MotionEvent.ACTION_MOVE -> {
//                // Nếu đang kéo sticker
//                selectedSticker?.let { sticker ->
//                    val dx = xTouch - lastTouchX
//                    val dy = yTouch - lastTouchY
//                    sticker.x += dx
//                    sticker.y += dy
//                    lastTouchX = xTouch
//                    lastTouchY = yTouch
//                    invalidate()
//                }
//            }
//
//            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                selectedSticker = null
//            }
//        }
//
//        return true
//    }
//
//    private fun findStickerAt(x: Float, y: Float): Sticker? {
//        return stickers.reversed().find { sticker ->
//            val rect = RectF(
//                sticker.x,
//                sticker.y,
//                sticker.x + sticker.width * sticker.scale,
//                sticker.y + sticker.height * sticker.scale
//            )
//            rect.contains(x, y)
//        }
//    }
//    data class Sticker(
//        val bitmap: Bitmap,
//        var x: Float,
//        var y: Float,
//        val width: Int,
//        val height: Int,
//        var scale: Float = 1f,
//        var rotation: Float = 0f
//    )
//
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        bitmap?.let { bmp ->
//            val maxWidth = MeasureSpec.getSize(widthMeasureSpec)
//            val maxHeight = MeasureSpec.getSize(heightMeasureSpec)
//
//            val bmpWidth = bmp.width
//            val bmpHeight = bmp.height
//
//            val ratio = bmpWidth.toFloat() / bmpHeight
//
//            var finalWidth = bmpWidth
//            var finalHeight = bmpHeight
//
//            if (finalWidth > maxWidth) {
//                finalWidth = maxWidth
//                finalHeight = (finalWidth / ratio).toInt()
//            }
//            if (finalHeight > maxHeight) {
//                finalHeight = maxHeight
//                finalWidth = (finalHeight * ratio).toInt()
//            }
//
//            setMeasuredDimension(finalWidth, finalHeight)
//        } ?: super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//    }
//
//    /** Tìm vùng xanh VÀ tạo mask cho từng vùng */
//    /** Tìm vùng xanh và tạo mask bằng OpenCV, trả về android.graphics.Rect */
//    private fun findBlueRegionsWithMasks(bitmap: Bitmap): Pair<List<Rect>, List<Bitmap>> {
//        val mat = Mat()
//        Utils.bitmapToMat(bitmap, mat)
//
//        // Chuyển sang HSV
//        val hsvMat = Mat()
//        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_RGB2HSV)
//
//        // Giới hạn màu xanh (HSV)
//        val lowerBlue = Scalar(100.0, 50.0, 50.0)
//        val upperBlue = Scalar(140.0, 255.0, 255.0)
//        val mask = Mat()
//        Core.inRange(hsvMat, lowerBlue, upperBlue, mask)
//
//        // Tìm contours
//        val contours = mutableListOf<MatOfPoint>()
//        val hierarchy = Mat()
//        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
//
//        val regions = mutableListOf<Rect>()
//        val masks = mutableListOf<Bitmap>()
//
//        for (cnt in contours) {
//            // Lấy bounding rect của contour, convert sang android.graphics.Rect
//            val rectCv = Imgproc.boundingRect(cnt)
//            val rect = Rect(rectCv.x, rectCv.y, rectCv.x + rectCv.width, rectCv.y + rectCv.height)
//            regions.add(rect)
//
//            // Tạo mask Bitmap cùng kích thước rect
//            val maskMat = Mat.zeros(rectCv.height, rectCv.width, CvType.CV_8UC1)
//
//            // Shift contour về góc (0,0) của mask
//            val pointsShifted = cnt.toArray().map { pt -> org.opencv.core.Point(pt.x - rectCv.x, pt.y - rectCv.y) }.toTypedArray()
//            val cntShifted = MatOfPoint()
//            cntShifted.fromArray(*pointsShifted)
//
//            Imgproc.drawContours(maskMat, listOf(cntShifted), 0, Scalar(255.0), -1)
//
//            val maskBitmap = Bitmap.createBitmap(rectCv.width, rectCv.height, Bitmap.Config.ARGB_8888)
//            Utils.matToBitmap(maskMat, maskBitmap)
//            masks.add(maskBitmap)
//        }
//
//        mat.release()
//        hsvMat.release()
//        mask.release()
//        hierarchy.release()
//
//        return Pair(regions, masks)
//    }
//
//
//
//    /** Merge với mask để chỉ vẽ vào pixel xanh */
//    private fun mergeImageOnRectWithMask(
//        original: Bitmap,
//        overlay: Bitmap,
//        rect: Rect,
//        mask: Bitmap
//    ): Bitmap {
//        val result = original.copy(Bitmap.Config.ARGB_8888, true)
//        val canvas = Canvas(result)
//
//        // Scale overlay khớp với rect
//        val scaledOverlay = Bitmap.createScaledBitmap(overlay, rect.width(), rect.height(), true)
//
//        // Tạo bitmap tạm để apply mask với background TRANSPARENT
//        val maskedOverlay =
//            Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
//        val tempCanvas = Canvas(maskedOverlay)
//
//        // ĐẢM BẢO background trong suốt
//        maskedOverlay.eraseColor(Color.TRANSPARENT)
//
//        // Vẽ overlay
//        tempCanvas.drawBitmap(scaledOverlay, 0f, 0f, null)
//
//        // Apply mask - chỉ giữ lại phần có mask
//        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
//        }
//        tempCanvas.drawBitmap(mask, 0f, 0f, maskPaint)
//
//        // Vẽ lên bitmap gốc - CHỈ ghi đè vùng có màu
//        val finalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
//        }
//        canvas.drawBitmap(maskedOverlay, rect.left.toFloat(), rect.top.toFloat(), finalPaint)
//        return result
//    }
//
//    private fun mergeImageOnRect(original: Bitmap, overlay: Bitmap, rect: Rect): Bitmap {
//        val result = original.copy(Bitmap.Config.ARGB_8888, true)
//        val canvas = Canvas(result)
//        val scaledOverlay = Bitmap.createScaledBitmap(overlay, rect.width(), rect.height(), true)
//
//        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
//        }
//        canvas.drawBitmap(scaledOverlay, rect.left.toFloat(), rect.top.toFloat(), paint)
//        paint.xfermode = null
//        return result
//    }
//}