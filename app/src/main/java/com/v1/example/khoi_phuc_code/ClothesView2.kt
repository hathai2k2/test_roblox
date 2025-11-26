package com.v1.example.khoi_phuc_code

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.withRotation
import com.v1.example.test_ronlox.R
import kotlin.math.max
import kotlin.math.min

class ClothesView2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Body parts ImageViews from people.xml
    private val headView: ImageView
    private val bodyView: ImageView
    private val leftHandView: ImageView
    private val rightHandView: ImageView
    private val leftLegView: ImageView
    private val rightLegView: ImageView

    // Overlay for stickers
    private val stickerOverlay: StickerOverlay

    // Store original clothes data (with pre-cut parts from ClothesBuilder)
    private var currentClothes: Clothes? = null

    init {
        // Inflate people.xml layout
        inflate(context, R.layout.people, this)

        // Find all body part views
        headView = findViewById(R.id.head)
        bodyView = findViewById(R.id.body)
        leftHandView = findViewById(R.id.left_hand)
        rightHandView = findViewById(R.id.right_hand)
        leftLegView = findViewById(R.id.left_leg)
        rightLegView = findViewById(R.id.right_leg)

        // Add transparent sticker overlay on top
        stickerOverlay = StickerOverlay(context)
        addView(stickerOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /**
     * Load các phần chính từ Clothes
     * 0: body, 6: left leg, 12: right leg
     * 1: left hand, 2: right hand (tuỳ preset)
     */
    fun loadParts(clothes: Clothes, isLeg: Boolean = true, isArm: Boolean = true) {
        if (clothes.parts.isEmpty()) return

        // Store original clothes for template export
        this.currentClothes = clothes

        // Load head (uses body texture)


        // Load body
        bodyView.setImageBitmap(clothes.parts[0])

        // Load arms if enabled
        if (isArm) {
            if (clothes.parts.size > 1) leftHandView.setImageBitmap(clothes.parts[1])
            if (clothes.parts.size > 2) rightHandView.setImageBitmap(clothes.parts[2])
        }

        // Load legs if enabled
        if (isLeg) {
            if (clothes.parts.size > 6) leftLegView.setImageBitmap(clothes.parts[6])
            if (clothes.parts.size > 12) rightLegView.setImageBitmap(clothes.parts[12])
        }
    }

    /**
     * Thay đổi bitmap của 1 phần bất kỳ
     */
    fun setPartBitmap(index: Int, bmp: Bitmap) {
        when (index) {
            0 -> {

                bodyView.setImageBitmap(bmp)
            }
            1 -> leftHandView.setImageBitmap(bmp)
            2 -> rightHandView.setImageBitmap(bmp)
            6 -> leftLegView.setImageBitmap(bmp)
            12 -> rightLegView.setImageBitmap(bmp)
        }
    }

    /**
     * Thêm sticker
     */
    fun addSticker(stickerBitmap: Bitmap, x: Float, y: Float, width: Int, height: Int) {
        stickerOverlay.stickers.add(Sticker(stickerBitmap, x, y, width, height))
        stickerOverlay.invalidate()
    }

    /**
     * Xóa tất cả stickers
     */
    fun clearStickers() {
        stickerOverlay.stickers.clear()
        stickerOverlay.invalidate()
    }

    /**
     * Lưu ảnh toàn bộ view (bao gồm stickers)
     */
    fun captureViewAsBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    /**
     * Lưu ảnh toàn bộ (bao gồm head, body, arms, legs và stickers)
     */
    fun captureBodyOnly(): Bitmap {
        // Chụp toàn bộ view bao gồm cả head và stickers
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    /**
     * Lấy ảnh với stickers được merge vào body parts, cắt phần dính biên
     * Trả về bitmap có thể dùng lại như R.drawable.image ban đầu
     */
    fun captureWithStickersApplied(): Bitmap {
        // 1. Tạo bitmap từ toàn bộ view (bao gồm stickers)
        val fullBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(fullBitmap)
        draw(canvas)

        // 2. Tìm vùng có nội dung (crop phần transparent dư thừa)
        val bounds = findContentBounds(fullBitmap)

        // 3. Crop bitmap theo bounds
        return if (bounds != null) {
            Bitmap.createBitmap(
                fullBitmap,
                bounds.left,
                bounds.top,
                bounds.width(),
                bounds.height()
            )
        } else {
            fullBitmap
        }
    }

    /**
     * Tìm vùng có nội dung trong bitmap (bỏ qua transparent)
     */
    private fun findContentBounds(bitmap: Bitmap): Rect? {
        val width = bitmap.width
        val height = bitmap.height

        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0

        var hasContent = false

        // Scan toàn bộ bitmap để tìm pixel không transparent
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)

                // Nếu pixel không trong suốt (có nội dung)
                if (alpha > 0) {
                    hasContent = true
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        return if (hasContent) {
            Rect(minX, minY, maxX + 1, maxY + 1)
        } else {
            null
        }
    }

    /**
     * Lấy Clothes object mới với stickers đã được apply vào từng part
     * Có thể dùng để rebuild lại như ban đầu
     */
    fun getClothesWithStickers(): Clothes {
        val parts = mutableListOf<Bitmap>()

        // Chụp từng body part riêng biệt với stickers
        // Index 0: Body
        bodyView.isDrawingCacheEnabled = true
        val bodyBitmap = captureViewWithStickers(bodyView)
        bodyView.isDrawingCacheEnabled = false

        // Index 1: Left arm
        val leftArmBitmap = captureViewWithStickers(leftHandView)

        // Index 2: Right arm
        val rightArmBitmap = captureViewWithStickers(rightHandView)

        // Index 3-5: Placeholder
        for (i in 3..5) {
            parts.add(bodyBitmap.copy(Bitmap.Config.ARGB_8888, true))
        }

        // Index 6: Left leg
        val leftLegBitmap = captureViewWithStickers(leftLegView)

        // Index 7-11: Placeholder
        for (i in 7..11) {
            parts.add(bodyBitmap.copy(Bitmap.Config.ARGB_8888, true))
        }

        // Index 12: Right leg
        val rightLegBitmap = captureViewWithStickers(rightLegView)

        // Build list theo đúng thứ tự
        val finalParts = mutableListOf<Bitmap>()
        finalParts.add(bodyBitmap)      // 0
        finalParts.add(leftArmBitmap)   // 1
        finalParts.add(rightArmBitmap)  // 2
        for (i in 3..5) finalParts.add(bodyBitmap.copy(Bitmap.Config.ARGB_8888, true))
        finalParts.add(leftLegBitmap)   // 6
        for (i in 7..11) finalParts.add(bodyBitmap.copy(Bitmap.Config.ARGB_8888, true))
        finalParts.add(rightLegBitmap)  // 12

        return Clothes(parts = finalParts, type = "merged")
    }

    /**
     * Chụp một ImageView riêng lẻ với stickers overlap
     */
    private fun captureViewWithStickers(view: ImageView): Bitmap {
        // Lấy vị trí của view trong parent
        val location = IntArray(2)
        view.getLocationInWindow(location)

        val viewBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(viewBitmap)

        // Vẽ view gốc
        view.draw(canvas)

        // Vẽ stickers nếu overlap với view này
        val viewRect = Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)

        stickerOverlay.stickers.forEach { sticker ->
            val stickerRect = RectF(
                sticker.x,
                sticker.y,
                sticker.x + sticker.width * sticker.scale,
                sticker.y + sticker.height * sticker.scale
            )

            // Check overlap
            if (Rect(
                    stickerRect.left.toInt(),
                    stickerRect.top.toInt(),
                    stickerRect.right.toInt(),
                    stickerRect.bottom.toInt()
                ).intersect(viewRect)
            ) {
                // Vẽ sticker vào canvas, adjust position
                val offsetX = stickerRect.left - location[0]
                val offsetY = stickerRect.top - location[1]

                val scaledSticker = Bitmap.createScaledBitmap(
                    sticker.bitmap,
                    (sticker.width * sticker.scale).toInt(),
                    (sticker.height * sticker.scale).toInt(),
                    true
                )
                canvas.drawBitmap(scaledSticker, offsetX, offsetY, null)
            }
        }

        return viewBitmap
    }

    /**
     * Lưu template format chuẩn Roblox Classic Clothing (585×559 PNG)
     * Với stickers chỉ merge vào các part đang hiển thị (FRONT faces)
     */
    fun captureAsTemplate(clothesParam: Clothes? = null, isLeg: Boolean = false, isArm: Boolean = false): Bitmap {
        val clothes = clothesParam ?: currentClothes
        if (clothes == null || clothes.parts.size < 18) {
            return Bitmap.createBitmap(585, 559, Bitmap.Config.ARGB_8888)
        }

        // Kích thước chuẩn Roblox Classic Clothing Template
        val templateWidth = 585
        val templateHeight = 559

        // Tạo bitmap template
        val templateBitmap = Bitmap.createBitmap(templateWidth, templateHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(templateBitmap)

        // Background màu xám nhạt chuẩn Roblox
        canvas.drawColor(Color.rgb(163, 162, 165))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Chỉ merge stickers vào part FRONT (parts[0]) của body
        val bodyWithStickers = mergeStickersIntoPart(clothes.parts[0], bodyView)

        // ============== TORSO (BODY) - front (0-5) ==============
        // Index 0: FRONT (231, 74) - 128×128 - CÓ STICKERS
        canvas.drawBitmap(bodyWithStickers, null, RectF(231f, 74f, 359f, 202f), paint)

        // Index 1-5: Các faces khác KHÔNG có stickers, dùng parts gốc
        canvas.drawBitmap(clothes.parts[1], null, RectF(165f, 74f, 229f, 202f), paint)
        canvas.drawBitmap(clothes.parts[2], null, RectF(361f, 74f, 425f, 202f), paint)
        canvas.drawBitmap(clothes.parts[3], null, RectF(427f, 74f, 555f, 202f), paint)
        canvas.drawBitmap(clothes.parts[4], null, RectF(231f, 8f, 359f, 72f), paint)
        canvas.drawBitmap(clothes.parts[5], null, RectF(231f, 204f, 359f, 268f), paint)

        // ============== RIGHT LEG - right (6-11) ==============
        // Chỉ merge stickers nếu this.isLegLoaded = true
        // Right leg FRONT là parts[6]
        val rightLegWithStickers = if (isLeg && clothes.parts.size > 12) {
            mergeStickersIntoPart(clothes.parts[12], rightLegView)
        } else if (clothes.parts.size > 12) {
            clothes.parts[12]
        } else {
            clothes.parts[6]
        }

        // Index 6: FRONT - CÓ STICKERS nếu loaded
        canvas.drawBitmap(rightLegWithStickers, null, RectF(308f, 355f, 372f, 483f), paint)

        // Index 7-11: Các faces khác KHÔNG có stickers
        canvas.drawBitmap(clothes.parts[7], null, RectF(506f, 355f, 570f, 483f), paint)
        canvas.drawBitmap(clothes.parts[8], null, RectF(374f, 355f, 438f, 483f), paint)
        canvas.drawBitmap(clothes.parts[9], null, RectF(440f, 355f, 504f, 483f), paint)
        canvas.drawBitmap(clothes.parts[10], null, RectF(308f, 288f, 372f, 352f), paint)
        canvas.drawBitmap(clothes.parts[11], null, RectF(308f, 485f, 372f, 549f), paint)

        // ============== LEFT LEG - left (12-17) ==============
        // Chỉ merge stickers nếu this.isLegLoaded = true
        // Left leg FRONT là parts[12]
        val leftLegWithStickers = if (isLeg && clothes.parts.size > 6) {
            mergeStickersIntoPart(clothes.parts[6], leftLegView)
        } else if (clothes.parts.size > 6) {
            clothes.parts[6]
        } else {
            clothes.parts[0] // fallback to body
        }

        // Index 12: FRONT - CÓ STICKERS nếu loaded
        canvas.drawBitmap(leftLegWithStickers, null, RectF(217f, 355f, 281f, 483f), paint)

        // Index 13-17: Các faces khác KHÔNG có stickers
        canvas.drawBitmap(clothes.parts[13], null, RectF(151f, 355f, 215f, 483f), paint)
        canvas.drawBitmap(clothes.parts[14], null, RectF(19f, 355f, 83f, 483f), paint)
        canvas.drawBitmap(clothes.parts[15], null, RectF(85f, 355f, 149f, 483f), paint)
        canvas.drawBitmap(clothes.parts[16], null, RectF(217f, 289f, 281f, 353f), paint)
        canvas.drawBitmap(clothes.parts[17], null, RectF(217f, 485f, 281f, 549f), paint)

        return templateBitmap
    }

    /**
     * Merge stickers vào một part cụ thể
     * Phần sticker nằm TRONG part bounds → vẽ sticker
     * Phần sticker nằm NGOÀI part bounds → fill màu nền (background)
     */
    private fun mergeStickersIntoPart(partBitmap: Bitmap, partView: ImageView): Bitmap {
        // Tạo bitmap kết quả với kích thước của part gốc
        val resultBitmap = partBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background color (màu xám nhạt Roblox)
        val backgroundColor = Color.rgb(163, 162, 165)
        val bgPaint = Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }

        // Lấy vị trí của partView trong window
        val viewLocation = IntArray(2)
        partView.getLocationInWindow(viewLocation)
        val viewRect = RectF(
            viewLocation[0].toFloat(),
            viewLocation[1].toFloat(),
            (viewLocation[0] + partView.width).toFloat(),
            (viewLocation[1] + partView.height).toFloat()
        )

        // Lấy vị trí overlay trong window
        val overlayLocation = IntArray(2)
        stickerOverlay.getLocationInWindow(overlayLocation)

        // Scale factor từ view → part bitmap
        val scaleX = partBitmap.width.toFloat() / partView.width
        val scaleY = partBitmap.height.toFloat() / partView.height

        // Vẽ stickers overlap với partView
        stickerOverlay.stickers.forEach { sticker ->
            val stickerRect = RectF(
                overlayLocation[0] + sticker.x,
                overlayLocation[1] + sticker.y,
                overlayLocation[0] + sticker.x + sticker.width * sticker.scale,
                overlayLocation[1] + sticker.y + sticker.height * sticker.scale
            )

            // Check nếu sticker overlap với view
            if (RectF.intersects(viewRect, stickerRect)) {
                // Tính intersection (phần overlap)
                val intersection = RectF(viewRect)
                intersection.intersect(stickerRect)

                // Convert về local coordinates của view (0-viewWidth, 0-viewHeight)
                val localLeft = intersection.left - viewRect.left
                val localTop = intersection.top - viewRect.top
                val localRight = intersection.right - viewRect.left
                val localBottom = intersection.bottom - viewRect.top

                // Scale về kích thước part bitmap gốc
                val partLeft = localLeft * scaleX
                val partTop = localTop * scaleY
                val partRight = localRight * scaleX
                val partBottom = localBottom * scaleY

                // Tính phần nào của sticker cần vẽ (source rect)
                val stickerWidth = stickerRect.width()
                val stickerHeight = stickerRect.height()
                val srcLeft = ((intersection.left - stickerRect.left) / stickerWidth) * sticker.bitmap.width
                val srcTop = ((intersection.top - stickerRect.top) / stickerHeight) * sticker.bitmap.height
                val srcRight = ((intersection.right - stickerRect.left) / stickerWidth) * sticker.bitmap.width
                val srcBottom = ((intersection.bottom - stickerRect.top) / stickerHeight) * sticker.bitmap.height

                // Vẽ TOÀN BỘ sticker trước (bao gồm cả phần ngoài bounds)
                val stickerLocalLeft = (stickerRect.left - viewRect.left) * scaleX
                val stickerLocalTop = (stickerRect.top - viewRect.top) * scaleY
                val stickerLocalRight = (stickerRect.right - viewRect.left) * scaleX
                val stickerLocalBottom = (stickerRect.bottom - viewRect.top) * scaleY

                val fullStickerRect = RectF(stickerLocalLeft, stickerLocalTop, stickerLocalRight, stickerLocalBottom)

                // Vẽ sticker đầy đủ
                canvas.drawBitmap(sticker.bitmap, null, fullStickerRect, paint)

                // Phần sticker nằm NGOÀI bounds → fill màu nền
                // Top (phần trên viewRect)
                if (stickerLocalTop < 0) {
                    canvas.drawRect(
                        stickerLocalLeft.coerceAtLeast(0f),
                        stickerLocalTop,
                        stickerLocalRight.coerceAtMost(partBitmap.width.toFloat()),
                        0f,
                        bgPaint
                    )
                }

                // Bottom (phần dưới viewRect)
                if (stickerLocalBottom > partBitmap.height) {
                    canvas.drawRect(
                        stickerLocalLeft.coerceAtLeast(0f),
                        partBitmap.height.toFloat(),
                        stickerLocalRight.coerceAtMost(partBitmap.width.toFloat()),
                        stickerLocalBottom,
                        bgPaint
                    )
                }

                // Left (phần bên trái viewRect)
                if (stickerLocalLeft < 0) {
                    canvas.drawRect(
                        stickerLocalLeft,
                        stickerLocalTop.coerceAtLeast(0f),
                        0f,
                        stickerLocalBottom.coerceAtMost(partBitmap.height.toFloat()),
                        bgPaint
                    )
                }

                // Right (phần bên phải viewRect)
                if (stickerLocalRight > partBitmap.width) {
                    canvas.drawRect(
                        partBitmap.width.toFloat(),
                        stickerLocalTop.coerceAtLeast(0f),
                        stickerLocalRight,
                        stickerLocalBottom.coerceAtMost(partBitmap.height.toFloat()),
                        bgPaint
                    )
                }
            }
        }

        return resultBitmap
    }

    /**
     * Capture body part với stickers đã merge và crop theo bounds của view
     */
    private fun captureBodyPartWithStickersCropped(view: ImageView): Bitmap {
        // Tạo bitmap với kích thước của view
        val partBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val partCanvas = Canvas(partBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Vẽ body part gốc
        view.draw(partCanvas)

        // 2. Lấy vị trí view trong cửa sổ
        val viewLocation = IntArray(2)
        view.getLocationInWindow(viewLocation)
        val viewRect = RectF(
            viewLocation[0].toFloat(),
            viewLocation[1].toFloat(),
            (viewLocation[0] + view.width).toFloat(),
            (viewLocation[1] + view.height).toFloat()
        )

        // 3. Vẽ stickers nhưng CHỈ phần overlap với view (auto crop)
        stickerOverlay.stickers.forEach { sticker ->
            val stickerRect = RectF(
                sticker.x,
                sticker.y,
                sticker.x + sticker.width * sticker.scale,
                sticker.y + sticker.height * sticker.scale
            )

            // Lấy vị trí sticker overlay trong cửa sổ
            val overlayLocation = IntArray(2)
            stickerOverlay.getLocationInWindow(overlayLocation)

            // Convert sticker position to window coordinates
            val stickerInWindow = RectF(
                overlayLocation[0] + stickerRect.left,
                overlayLocation[1] + stickerRect.top,
                overlayLocation[0] + stickerRect.right,
                overlayLocation[1] + stickerRect.bottom
            )

            // Check nếu sticker overlap với view
            if (RectF.intersects(viewRect, stickerInWindow)) {
                // Tính intersection (phần overlap)
                val intersection = RectF(viewRect)
                intersection.intersect(stickerInWindow)

                // Convert về local coordinates của view
                val localLeft = intersection.left - viewRect.left
                val localTop = intersection.top - viewRect.top
                val localRight = intersection.right - viewRect.left
                val localBottom = intersection.bottom - viewRect.top

                // Tính phần nào của sticker bitmap cần vẽ
                val srcLeft = ((intersection.left - stickerInWindow.left) / (stickerInWindow.width())) * sticker.bitmap.width
                val srcTop = ((intersection.top - stickerInWindow.top) / (stickerInWindow.height())) * sticker.bitmap.height
                val srcRight = ((intersection.right - stickerInWindow.left) / (stickerInWindow.width())) * sticker.bitmap.width
                val srcBottom = ((intersection.bottom - stickerInWindow.top) / (stickerInWindow.height())) * sticker.bitmap.height

                // Vẽ chỉ phần cropped của sticker
                val srcRect = Rect(
                    srcLeft.toInt(),
                    srcTop.toInt(),
                    srcRight.toInt(),
                    srcBottom.toInt()
                )
                val dstRect = RectF(localLeft, localTop, localRight, localBottom)

                partCanvas.drawBitmap(sticker.bitmap, srcRect, dstRect, paint)
            }
        }

        return partBitmap
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // Inner class for sticker overlay
    private inner class StickerOverlay(context: Context) : android.view.View(context) {
        val stickers = mutableListOf<Sticker>()
        var selectedSticker: Sticker? = null
        var isResizing = false
        var lastTouchX = 0f
        var lastTouchY = 0f

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2 * resources.displayMetrics.density
            isAntiAlias = true
        }

        private val handleSize = 40f * resources.displayMetrics.density
        private val resizeIcon: Bitmap = getBitmapFromVectorDrawable(context, R.drawable.ic_close)
        private var resizeHandleRect = RectF()

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

                canvas.withRotation(sticker.rotation, rectF.centerX(), rectF.centerY()) {
                    // Draw sticker
                    canvas.drawBitmap(sticker.bitmap, null, rectF, paint)

                    // Draw border if selected
                    if (sticker == selectedSticker) {
                        canvas.drawRect(rectF, borderPaint)

                        // Draw resize handle
                        val handleRect = RectF(
                            rectF.right - handleSize,
                            rectF.bottom - handleSize,
                            rectF.right,
                            rectF.bottom
                        )
                        canvas.drawBitmap(resizeIcon, null, handleRect, null)
                    }
                }
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val xTouch = event.x
            val yTouch = event.y

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = xTouch
                    lastTouchY = yTouch
                    selectedSticker = findStickerAt(xTouch, yTouch)
                    isResizing = false

                    selectedSticker?.let { sticker ->
                        val rectF = RectF(
                            sticker.x,
                            sticker.y,
                            sticker.x + sticker.width * sticker.scale,
                            sticker.y + sticker.height * sticker.scale
                        )

                        val rotatedPoint = rotatePoint(
                            rectF.right,
                            rectF.bottom,
                            rectF.centerX(),
                            rectF.centerY(),
                            sticker.rotation
                        )

                        resizeHandleRect = RectF(
                            rotatedPoint.x - handleSize,
                            rotatedPoint.y - handleSize,
                            rotatedPoint.x,
                            rotatedPoint.y
                        )

                        if (resizeHandleRect.contains(xTouch, yTouch)) {
                            isResizing = true
                            invalidate()
                            return true
                        }
                    }
                    invalidate()
                }

                MotionEvent.ACTION_MOVE -> {
                    selectedSticker?.let { sticker ->
                        if (isResizing) {
                            val dx = xTouch - sticker.x
                            sticker.scale = max(0.2f, dx / sticker.width)
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
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (selectedSticker == null) {
                        invalidate()
                    }
                    isResizing = false
                }
            }
            return true
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

    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
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
}

