package com.v1.example.test_ronlox

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.*
import kotlin.math.max
import kotlin.math.min

class StickerImageViewOptimized2(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    var onBlueRegionClick: ((index: Int, rect: Rect) -> Unit)? = null

    private var bitmap: Bitmap? = null
    private val stickers = mutableListOf<Sticker>()

    var blueRegions = listOf<Rect>()
        private set
    private val blueRegionMasks = mutableMapOf<Int, Bitmap>()
    private val replacementBitmaps = mutableMapOf<Int, Bitmap>()
    private val historyStack = mutableListOf<Bitmap>()
    private val maxHistorySize = 20

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var selectedSticker: Sticker? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Max dimension khi xử lý (resize để nhanh hơn)
    private val maxProcessSize = 800

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    /** Set Bitmap template */
    fun setBitmap(bmp: Bitmap) {
        // Lưu bitmap gốc
        bitmap = if (bmp.config == Bitmap.Config.ARGB_8888) bmp else bmp.copy(Bitmap.Config.ARGB_8888, true)

        // Xử lý tìm vùng xanh ngoài Main Thread
        CoroutineScope(Dispatchers.Default).launch {
            // Resize bitmap để xử lý nhanh
            val scale = min(1f, maxProcessSize.toFloat() / max(bitmap!!.width, bitmap!!.height))
            val processBmp = Bitmap.createScaledBitmap(
                bitmap!!,
                (bitmap!!.width * scale).toInt(),
                (bitmap!!.height * scale).toInt(),
                true
            )

            // Tìm vùng xanh trên bitmap nhỏ
            val (regions, masks) = findBlueRegionsWithMasks(processBmp)

            // Scale lại các vùng xanh & mask về bitmap gốc
            val scaleX = bitmap!!.width.toFloat() / processBmp.width
            val scaleY = bitmap!!.height.toFloat() / processBmp.height
            val regionsScaled = regions.map { r ->
                Rect(
                    (r.left * scaleX).toInt(),
                    (r.top * scaleY).toInt(),
                    (r.right * scaleX).toInt(),
                    (r.bottom * scaleY).toInt()
                )
            }
            val masksScaled = masks.mapIndexed { index, mask ->
                val r = regionsScaled[index]
                Bitmap.createScaledBitmap(mask, r.width(), r.height(), true)
            }

            withContext(Dispatchers.Main) {
                blueRegions = regionsScaled
                blueRegionMasks.clear()
                masksScaled.forEachIndexed { index, mask ->
                    blueRegionMasks[index] = mask
                }

                historyStack.clear()
                historyStack.add(bitmap!!.copy(Bitmap.Config.ARGB_8888, true))

                requestLayout()
                invalidate()
            }
        }
    }

    /** Thêm sticker */
    fun addSticker(stickerBitmap: Bitmap, x: Float, y: Float, width: Int, height: Int) {
        stickers.add(Sticker(stickerBitmap, x, y, width, height))
        invalidate()
    }

    /** Thay thế vùng xanh */
    fun replaceBlueRegion(index: Int, newBitmap: Bitmap) {
        if (bitmap == null || index !in blueRegions.indices) return
        saveToHistory()
        val rect = blueRegions[index]
        val mask = blueRegionMasks[index]
        replacementBitmaps[index] = newBitmap

        bitmap = if (mask != null) mergeImageOnRectWithMask(bitmap!!, newBitmap, rect, mask)
        else mergeImageOnRect(bitmap!!, newBitmap, rect)

        invalidate()
    }

    private fun saveToHistory() {
        bitmap?.let {
            historyStack.add(it.copy(Bitmap.Config.ARGB_8888, true))
            if (historyStack.size > maxHistorySize) historyStack.removeAt(0)
        }
    }

    fun resetToOriginal() {
        if (historyStack.isNotEmpty()) {
            val original = historyStack.first()
            bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
            historyStack.clear()
            historyStack.add(original)
            replacementBitmaps.clear()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let {
            val destRect = Rect(0, 0, width, height)
            val bmpPaint = Paint().apply { isFilterBitmap = false; isDither = false }
            canvas.drawBitmap(it, null, destRect, bmpPaint)
        }

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
                selectedSticker = findStickerAt(xTouch, yTouch)

                if (selectedSticker == null) {
                    bitmap?.let { bmp ->
                        val scaleX = width.toFloat() / bmp.width
                        val scaleY = height.toFloat() / bmp.height
                        blueRegions.forEachIndexed { index, rect ->
                            if (xTouch in rect.left * scaleX..rect.right * scaleX &&
                                yTouch in rect.top * scaleY..rect.bottom * scaleY
                            ) onBlueRegionClick?.invoke(index, rect)
                        }
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
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
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> selectedSticker = null
        }
        return true
    }

    private fun findStickerAt(x: Float, y: Float): Sticker? {
        return stickers.reversed().find {
            RectF(it.x, it.y, it.x + it.width * it.scale, it.y + it.height * it.scale).contains(x, y)
        }
    }

    data class Sticker(
        val bitmap: Bitmap,
        var x: Float,
        var y: Float,
        val width: Int,
        val height: Int,
        var scale: Float = 1f,
        var rotation: Float = 0f,
        var isSelected: Boolean = false
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        bitmap?.let { bmp ->
            val maxW = MeasureSpec.getSize(widthMeasureSpec)
            val maxH = MeasureSpec.getSize(heightMeasureSpec)
            val ratio = bmp.width.toFloat() / bmp.height
            var finalW = bmp.width
            var finalH = bmp.height

            if (finalW > maxW) { finalW = maxW; finalH = (finalW / ratio).toInt() }
            if (finalH > maxH) { finalH = maxH; finalW = (finalH * ratio).toInt() }

            setMeasuredDimension(finalW, finalH)
        } ?: super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /** Tìm vùng xanh & tạo mask bằng array (tương tự version trước) */
    private fun findBlueRegionsWithMasks(bitmap: Bitmap): Pair<List<Rect>, List<Bitmap>> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val visited = BooleanArray(width * height)
        val regions = mutableListOf<Rect>()
        val masks = mutableListOf<Bitmap>()
        val hsv = FloatArray(3)
        val directions = arrayOf(intArrayOf(1,0), intArrayOf(-1,0), intArrayOf(0,1), intArrayOf(0,-1))

        fun isBlue(x: Int, y: Int): Boolean {
            val px = pixels[y * width + x]
            Color.colorToHSV(px, hsv)
            val (h,s,v) = hsv
            return h in 180f..260f && s>0.2f && v>0.2f
        }

        for (yStart in 0 until height) for (xStart in 0 until width) {
            val idx = yStart * width + xStart
            if (visited[idx] || !isBlue(xStart, yStart)) continue

            val bounds = Rect(xStart, yStart, xStart, yStart)
            val queue = ArrayDeque<Pair<Int,Int>>()
            val blueIdx = mutableListOf<Int>()

            queue.add(Pair(xStart,yStart))
            visited[idx] = true

            while(queue.isNotEmpty()) {
                val (x,y) = queue.removeFirst()
                blueIdx.add(y*width + x)
                bounds.left = min(bounds.left,x); bounds.top = min(bounds.top,y)
                bounds.right = max(bounds.right,x); bounds.bottom = max(bounds.bottom,y)

                for(dir in directions){
                    val nx = x+dir[0]; val ny = y+dir[1]
                    if(nx in 0 until width && ny in 0 until height){
                        val nidx = ny*width+nx
                        if(!visited[nidx] && isBlue(nx,ny)){
                            visited[nidx]=true
                            queue.add(Pair(nx,ny))
                        }
                    }
                }
            }

            val maskW = bounds.width()
            val maskH = bounds.height()
            val maskPixels = IntArray(maskW*maskH){Color.TRANSPARENT}
            for(i in blueIdx){
                val x = i%width; val y=i/width
                val lx = x-bounds.left; val ly=y-bounds.top
                val mi = ly*maskW+lx
                if(mi in maskPixels.indices) maskPixels[mi]=Color.WHITE
            }
            val maskBmp = Bitmap.createBitmap(maskW,maskH,Bitmap.Config.ARGB_8888)
            maskBmp.setPixels(maskPixels,0,maskW,0,0,maskW,maskH)
            regions.add(bounds); masks.add(maskBmp)
        }
        return Pair(regions,masks)
    }

    /** Merge overlay với mask */
    private fun mergeImageOnRectWithMask(original: Bitmap, overlay: Bitmap, rect: Rect, mask: Bitmap): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888,true)
        val canvas = Canvas(result)
        val scaledOverlay = Bitmap.createScaledBitmap(overlay,rect.width(),rect.height(),true)
        val maskedOverlay = Bitmap.createBitmap(rect.width(),rect.height(),Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(maskedOverlay)
        maskedOverlay.eraseColor(Color.TRANSPARENT)
        tempCanvas.drawBitmap(scaledOverlay,0f,0f,null)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode=PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        tempCanvas.drawBitmap(mask,0f,0f,maskPaint)
        val finalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode=PorterDuffXfermode(PorterDuff.Mode.SRC_OVER) }
        canvas.drawBitmap(maskedOverlay,rect.left.toFloat(),rect.top.toFloat(),finalPaint)
        return result
    }

    private fun mergeImageOnRect(original: Bitmap, overlay: Bitmap, rect: Rect): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888,true)
        val canvas = Canvas(result)
        val scaledOverlay = Bitmap.createScaledBitmap(overlay,rect.width(),rect.height(),true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode=PorterDuffXfermode(PorterDuff.Mode.SRC) }
        canvas.drawBitmap(scaledOverlay,rect.left.toFloat(),rect.top.toFloat(),paint)
        paint.xfermode=null
        return result
    }
}
