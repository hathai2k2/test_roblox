package com.v1.example.khoi_phuc_code

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
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

    // Touch-to-change-color configuration
    var enableBodyPartColorChange = false
    var colorChangeIsLeg = true
    var colorChangeIsArm = true
    var colorToApply = Color.BLUE

    // Touch-to-change-texture configuration
    var useTextureMode = false  // false = color mode, true = texture mode
    var textureToApply: Bitmap? = null

    // Callback khi touch vào body part
    var onBodyPartTouched: ((partName: String, partIndex: Int) -> Unit)? = null

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

        // Setup touch listeners for body parts
        setupBodyPartTouchListeners()
    }

    /**
     * Setup touch listeners cho các body parts
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupBodyPartTouchListeners() {
        // Body touch listener
        bodyView.setOnTouchListener { _, event ->
            android.util.Log.d("ClothesView2", "Body touched! Action: ${event.action}, Enabled: $enableBodyPartColorChange")
            if (enableBodyPartColorChange && event.action == MotionEvent.ACTION_DOWN) {
                android.util.Log.d("ClothesView2", "Body touch detected - calling callback")
                onBodyPartTouched?.invoke("body", 0)
                applyColorOrTexture(0)
                true
            } else false
        }

        // Left arm touch listener
        leftHandView.setOnTouchListener { _, event ->
            android.util.Log.d("ClothesView2", "Left hand touched! Action: ${event.action}, Enabled: $enableBodyPartColorChange, IsArm: $colorChangeIsArm")
            if (enableBodyPartColorChange && colorChangeIsArm && event.action == MotionEvent.ACTION_DOWN) {
                android.util.Log.d("ClothesView2", "Left hand touch detected - calling callback")
                onBodyPartTouched?.invoke("left_hand", 1)
                applyColorOrTexture(1)
                true
            } else false
        }

        // Right arm touch listener
        rightHandView.setOnTouchListener { _, event ->
            android.util.Log.d("ClothesView2", "Right hand touched! Action: ${event.action}, Enabled: $enableBodyPartColorChange, IsArm: $colorChangeIsArm")
            if (enableBodyPartColorChange && colorChangeIsArm && event.action == MotionEvent.ACTION_DOWN) {
                android.util.Log.d("ClothesView2", "Right hand touch detected - calling callback")
                onBodyPartTouched?.invoke("right_hand", 2)
                applyColorOrTexture(2)
                true
            } else false
        }

        // Left leg touch listener
        leftLegView.setOnTouchListener { _, event ->
            android.util.Log.d("ClothesView2", "Left leg touched! Action: ${event.action}, Enabled: $enableBodyPartColorChange, IsLeg: $colorChangeIsLeg")
            if (enableBodyPartColorChange && colorChangeIsLeg && event.action == MotionEvent.ACTION_DOWN) {
                android.util.Log.d("ClothesView2", "Left leg touch detected - calling callback")
                onBodyPartTouched?.invoke("left_leg", 6)
                applyColorOrTexture(6)
                true
            } else false
        }

        // Right leg touch listener
        rightLegView.setOnTouchListener { _, event ->
            android.util.Log.d("ClothesView2", "Right leg touched! Action: ${event.action}, Enabled: $enableBodyPartColorChange, IsLeg: $colorChangeIsLeg")
            if (enableBodyPartColorChange && colorChangeIsLeg && event.action == MotionEvent.ACTION_DOWN) {
                android.util.Log.d("ClothesView2", "Right leg touch detected - calling callback")
                onBodyPartTouched?.invoke("right_leg", 12)
                applyColorOrTexture(12)
                true
            } else false
        }
    }

    /**
     * Áp dụng màu hoặc texture tùy theo mode
     */
    private fun applyColorOrTexture(partIndex: Int) {
        if (useTextureMode && textureToApply != null) {
            setPartTextureOverlay(partIndex, textureToApply!!)
        } else {
            setPartColorOverlay(partIndex, colorToApply)
        }
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
            if (clothes.parts.size > 6) rightHandView.setImageBitmap(clothes.parts[6])
            if (clothes.parts.size > 12) leftHandView.setImageBitmap(clothes.parts[12])
        }

        // Load legs if enabled
        if (isLeg) {
            if (clothes.parts.size > 6) leftLegView.setImageBitmap(clothes.parts[6])
            if (clothes.parts.size > 12) rightLegView.setImageBitmap(clothes.parts[12])
        }
    }

    /**
     * Bật/tắt chế độ touch để đổi màu body parts
     */
    fun enableTouchToChangeColor(
        enabled: Boolean,
        color: Int = Color.BLUE,
        isLeg: Boolean = true,
        isArm: Boolean = true
    ) {
        enableBodyPartColorChange = enabled
        colorToApply = color
        colorChangeIsLeg = isLeg
        colorChangeIsArm = isArm
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
     * Thay đổi màu/texture của một phần mà không ảnh hưởng đến background trong suốt
     * Chỉ áp dụng màu lên vùng có nội dung, giữ nguyên alpha channel
     */
    fun setPartColorOverlay(index: Int, color: Int) {
        val view = when (index) {
            0 -> bodyView
            1 -> leftHandView
            2 -> rightHandView
            6 -> leftLegView
            12 -> rightLegView
            else -> return
        }

        // Lấy bitmap hiện tại
        val currentBitmap = (view.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            ?: return

        // Tạo bitmap mới với overlay màu, giữ nguyên alpha
        val overlayBitmap = applyColorOverlay(currentBitmap, color)
        view.setImageBitmap(overlayBitmap)
    }

    /**
     * Thay đổi texture của một phần mà không ảnh hưởng đến background trong suốt
     * Chỉ áp dụng texture lên vùng có nội dung (không trong suốt)
     */
    fun setPartTextureOverlay(index: Int, textureBitmap: Bitmap) {
        val view = when (index) {
            0 -> bodyView
            1 -> leftHandView
            2 -> rightHandView
            6 -> leftLegView
            12 -> rightLegView
            else -> return
        }

        // Lấy bitmap hiện tại làm mask
        val currentBitmap = (view.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            ?: return

        // Áp dụng texture chỉ lên vùng không trong suốt
        val resultBitmap = applyTextureWithMask(currentBitmap, textureBitmap)
        view.setImageBitmap(resultBitmap)
    }

    /**
     * Áp dụng màu overlay lên bitmap, giữ nguyên alpha channel
     */
    private fun applyColorOverlay(source: Bitmap, color: Int): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Vẽ bitmap gốc
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Áp dụng color overlay chỉ lên vùng có nội dung
        paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return result
    }

    /**
     * Áp dụng texture lên bitmap, sử dụng alpha channel của source làm mask
     */
    private fun applyTextureWithMask(mask: Bitmap, texture: Bitmap): Bitmap {
        val width = mask.width
        val height = mask.height

        // Scale texture để match với kích thước của mask
        val scaledTexture = Bitmap.createScaledBitmap(texture, width, height, true)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Vẽ texture trước
        canvas.drawBitmap(scaledTexture, 0f, 0f, null)

        // Sử dụng mask để giữ chỉ vùng không trong suốt
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(mask, 0f, 0f, maskPaint)

        return result
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
        stickerOverlay.selectedSticker = null
        stickerOverlay.invalidate()
    }

    /**
     * Xóa tất cả textures đã apply và khôi phục về parts gốc
     */
    fun clearTextures(isArm: Boolean=false,isLeg: Boolean=false) {
        currentClothes?.let { clothes ->
            // Restore body to original
            if (clothes.parts.isNotEmpty()) {
                bodyView.setImageBitmap(clothes.parts[0])
            }

            if (isArm){
                // Restore left hand to original
                if (clothes.parts.size > 12) {
                    leftHandView.setImageBitmap(clothes.parts[12])
                }
                // Restore right hand to original
                if (clothes.parts.size > 6) {
                    rightHandView.setImageBitmap(clothes.parts[6])
                }
            }

            if (isLeg){
                // Restore left leg to original
                if (clothes.parts.size > 12) {
                    leftLegView.setImageBitmap(clothes.parts[12])
                }

                // Restore right leg to original
                if (clothes.parts.size > 6) {
                    rightLegView.setImageBitmap(clothes.parts[6])
                }
            }

            android.util.Log.d("ClothesView2", "Cleared all textures, restored original parts")
        }
    }

    /**
     * Xóa cả stickers và textures (reset hoàn toàn)
     */
    fun clearAll(isArm: Boolean=false,isLeg: Boolean=false) {
        clearStickers()
        clearTextures(isArm,isLeg)
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
     * Lưu template format chuẩn Roblox Classic Clothing (585×559 PNG)
     * Với stickers chỉ merge vào các part đang hiển thị (FRONT faces)
     *
     * **CẢI TIẾN MỚI:**
     * - Capture được texture/color đã apply thông qua setPartColorOverlay() hoặc setPartTextureOverlay()
     * - Lấy bitmap hiện tại từ ImageView thay vì dùng parts gốc từ Clothes
     * - Support cả parts có texture và không có texture
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
        canvas.drawColor(Color.TRANSPARENT)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Lấy bitmap hiện tại từ bodyView (có thể có texture đã apply)
        val currentBodyBitmap = getCurrentBitmapFromView(bodyView, clothes.parts[0])

        // Merge stickers vào part FRONT của body với texture (nếu có)
        val bodyWithStickers = mergeStickersIntoPart(currentBodyBitmap, bodyView)

        // ============== TORSO (BODY) - front (0-5) ==============
        // Index 0: FRONT (231, 74) - 128×128 - CÓ STICKERS và TEXTURE (nếu có)

        canvas.drawBitmap(bodyWithStickers, null, RectF(231f, 74f, 359f, 202f), paint)

        // Index 1-5: Các faces khác KHÔNG có stickers, dùng parts gốc
        canvas.drawBitmap(  clothes.parts[1], null, RectF(165f, 74f, 229f, 202f), paint)
        canvas.drawBitmap(clothes.parts[2], null, RectF(361f, 74f, 425f, 202f), paint)
        canvas.drawBitmap(clothes.parts[3], null, RectF(427f, 74f, 555f, 202f), paint)
        canvas.drawBitmap(clothes.parts[4], null, RectF(231f, 8f, 359f, 72f), paint)
        canvas.drawBitmap(clothes.parts[5], null, RectF(231f, 204f, 359f, 268f), paint)

        // ============== RIGHT LEG - right (6-11) ==============
        // Chỉ merge stickers nếu this.isLegLoaded = true
        // Right leg FRONT là parts[6]
        // Lấy bitmap hiện tại từ view (có thể có texture đã apply)
        val rightHandWithStickers = if (isArm && clothes.parts.size > 6) {
            val currentRightHandBitmap = getCurrentBitmapFromView(rightHandView, clothes.parts[6])
            mergeStickersIntoPart(currentRightHandBitmap, rightHandView)
        } else if (clothes.parts.size > 6) {
            getCurrentBitmapFromView(rightHandView, clothes.parts[6])
        } else {
            clothes.parts[0]
        }

        val rightLegWithStickers = if (isLeg && clothes.parts.size > 6) {
            val currentRightLegBitmap = getCurrentBitmapFromView(rightLegView, clothes.parts[6])
            mergeStickersIntoPart(currentRightLegBitmap, rightLegView)
        } else if (clothes.parts.size > 6) {
            getCurrentBitmapFromView(rightLegView, clothes.parts[6])
        } else {
            clothes.parts[6]
        }

        // Index 6: FRONT - CÓ STICKERS nếu loaded
        canvas.drawBitmap(if(isArm)rightHandWithStickers else rightLegWithStickers, null, RectF(308f, 355f, 372f, 483f), paint)

        // Index 7-11: Các faces khác KHÔNG có stickers
        canvas.drawBitmap(clothes.parts[7], null, RectF(506f, 355f, 570f, 483f), paint)
        canvas.drawBitmap(clothes.parts[8], null, RectF(374f, 355f, 438f, 483f), paint)
        canvas.drawBitmap(clothes.parts[9], null, RectF(440f, 355f, 504f, 483f), paint)
        canvas.drawBitmap(clothes.parts[10], null, RectF(308f, 288f, 372f, 352f), paint)
        canvas.drawBitmap(clothes.parts[11], null, RectF(308f, 485f, 372f, 549f), paint)

        // ============== LEFT LEG - left (12-17) ==============
        // Chỉ merge stickers nếu this.isLegLoaded = true
        // Left leg FRONT là parts[12]
        // Lấy bitmap hiện tại từ view (có thể có texture đã apply)
        val leftHandWithStickers = if (isArm && clothes.parts.size > 12) {
            val currentLeftHandBitmap = getCurrentBitmapFromView(leftHandView, clothes.parts[12])
            mergeStickersIntoPart(currentLeftHandBitmap, leftHandView)
        } else if (clothes.parts.size > 12) {
            getCurrentBitmapFromView(leftHandView, clothes.parts[12])
        } else {
            clothes.parts[0]
        }

        val leftLegWithStickers = if (isLeg && clothes.parts.size > 12) {
            val currentLeftLegBitmap = getCurrentBitmapFromView(leftLegView, clothes.parts[12])
            mergeStickersIntoPart(currentLeftLegBitmap, leftLegView)
        } else if (clothes.parts.size > 12) {
            getCurrentBitmapFromView(leftLegView, clothes.parts[12])
        } else {
            clothes.parts[0] // fallback to body
        }

        // Index 12: FRONT - CÓ STICKERS nếu loaded
        canvas.drawBitmap(if(isArm)leftHandWithStickers else leftLegWithStickers, null, RectF(217f, 355f, 281f, 483f), paint)

        // Index 13-17: Các faces khác KHÔNG có stickers
        canvas.drawBitmap(clothes.parts[13], null, RectF(151f, 355f, 215f, 483f), paint)
        canvas.drawBitmap(clothes.parts[14], null, RectF(19f, 355f, 83f, 483f), paint)
        canvas.drawBitmap(clothes.parts[15], null, RectF(85f, 355f, 149f, 483f), paint)
        canvas.drawBitmap(clothes.parts[16], null, RectF(217f, 289f, 281f, 353f), paint)
        canvas.drawBitmap(clothes.parts[17], null, RectF(217f, 485f, 281f, 549f), paint)

        return templateBitmap
    }

    /**
     * Lấy bitmap hiện tại từ ImageView (bao gồm cả texture/color đã apply)
     * Nếu ImageView có drawable, trả về bitmap từ drawable đó
     * Nếu không, fallback về original part bitmap
     */
    private fun getCurrentBitmapFromView(view: ImageView, fallbackBitmap: Bitmap): Bitmap {
        val drawable = view.drawable
        return if (drawable is android.graphics.drawable.BitmapDrawable) {
            drawable.bitmap
        } else {
            fallbackBitmap
        }
    }

    /**
     * Merge stickers vào một part cụ thể
     * Phần sticker nằm TRONG part bounds → vẽ sticker
     * Phần sticker nằm NGOÀI part bounds → fill màu nền (background)
     */
    private fun mergeStickersIntoPart(partBitmap: Bitmap, partView: ImageView): Bitmap {
        val w = partBitmap.width
        val h = partBitmap.height

        // 1) Copy part để làm base cuối
        val resultBitmap = partBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val finalCanvas = Canvas(resultBitmap)

        // ----- SCALE VIEW → BITMAP -----
        val partLocation = IntArray(2)
        partView.getLocationInWindow(partLocation)

        val scaleX = w.toFloat() / partView.width
        val scaleY = h.toFloat() / partView.height

        val overlayLocation = IntArray(2)
        stickerOverlay.getLocationInWindow(overlayLocation)

        // 2) Tạo STICKER LAYER (full bitmap)
        val stickerLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val stickerCanvas = Canvas(stickerLayer)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        stickerOverlay.stickers.forEach { st ->

            val stickerLeftInView = (overlayLocation[0] + st.x) - partLocation[0]
            val stickerTopInView  = (overlayLocation[1] + st.y) - partLocation[1]

            val left   = stickerLeftInView * scaleX
            val top    = stickerTopInView * scaleY
            val right  = left + (st.width * st.scale) * scaleX
            val bottom = top  + (st.height * st.scale) * scaleY

            val dst = RectF(left, top, right, bottom)

            stickerCanvas.save()
            stickerCanvas.rotate(st.rotation, dst.centerX(), dst.centerY())

            // Apply flip if needed
            if (st.isFlipped) {
                stickerCanvas.scale(-1f, 1f, dst.centerX(), dst.centerY())
            }

            stickerCanvas.drawBitmap(st.bitmap, null, dst, p)
            stickerCanvas.restore()
        }

        // 3) MASK = chính partBitmap (alpha)
        val mask = partBitmap

        // 4) Lấy INTERSECTION bằng SRC_IN
        val stickerMasked = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(stickerMasked)

        // Vẽ mask trước
        maskCanvas.drawBitmap(mask, 0f, 0f, null)

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }

        // Chỉ giữ phần sticker trùng với mask
        maskCanvas.drawBitmap(stickerLayer, 0f, 0f, maskPaint)
        maskPaint.xfermode = null

        // 5) GHÉP vào partBase
        finalCanvas.drawBitmap(stickerMasked, 0f, 0f, null)

        return resultBitmap
    }


    // Inner class for sticker overlay
    private inner class StickerOverlay(context: Context) : android.view.View(context) {
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

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
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
                    android.util.Log.d("StickerOverlay", "Touch at ($xTouch, $yTouch), Body change enabled: $enableBodyPartColorChange")
                    lastTouchX = xTouch
                    lastTouchY = yTouch
                    isResizing = false
                    lastPointerCount = 1

                    // FIRST: Check if tapping on control buttons of currently selected sticker
                    selectedSticker?.let { sticker ->
                        android.util.Log.d("StickerOverlay", "Checking buttons for selected sticker")

                        // Close button (top-left) - delete sticker
                        if (closeButtonRect.contains(xTouch, yTouch)) {
                            android.util.Log.d("StickerOverlay", "Close button tapped - deleting sticker")
                            stickers.remove(sticker)
                            selectedSticker = null
                            invalidate()
                            return true
                        }

                        // Flip button (top-right) - flip sticker horizontally
                        if (flipButtonRect.contains(xTouch, yTouch)) {
                            android.util.Log.d("StickerOverlay", "Flip button tapped - flipping sticker")
                            sticker.isFlipped = !sticker.isFlipped
                            invalidate()
                            return true
                        }

                        // Done button (bottom-left) - deselect sticker
                        if (doneButtonRect.contains(xTouch, yTouch)) {
                            android.util.Log.d("StickerOverlay", "Done button tapped - deselecting sticker")
                            selectedSticker = null
                            invalidate()
                            return true
                        }

                        // Resize button (bottom-right) - start resizing
                        if (resizeButtonRect.contains(xTouch, yTouch)) {
                            android.util.Log.d("StickerOverlay", "Resize button tapped - starting resize")
                            isResizing = true
                            invalidate()
                            return true
                        }
                    }

                    // SECOND: Check if tapping on a sticker (select it or keep current selection)
                    val tappedSticker = findStickerAt(xTouch, yTouch)
                    if (tappedSticker != null) {
                        android.util.Log.d("StickerOverlay", "Sticker found - selecting/keeping selection")
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
                        android.util.Log.d("StickerOverlay", "No sticker selected, body change enabled - passing through")
                        return false  // Pass through to views below
                    }

                    android.util.Log.d("StickerOverlay", "No sticker selected, body change disabled - passing through")
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

                            // Zoom: scale based on distance change
                            if (initialDistance > 0) {
                                val scaleFactor = currentDistance / initialDistance
                                sticker.scale = max(0.2f, min(3.0f, initialScale * scaleFactor))
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
                                // Resize from bottom-right corner
                                val centerX = sticker.x + (sticker.width * sticker.scale) / 2
                                val centerY = sticker.y + (sticker.height * sticker.scale) / 2
                                val dx = xTouch - centerX
                                val dy = yTouch - centerY
                                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                val baseDistance = kotlin.math.sqrt(
                                    (sticker.width * sticker.width + sticker.height * sticker.height).toFloat()
                                ) / 2
                                sticker.scale = max(0.2f, min(3.0f, distance / baseDistance))
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
        var rotation: Float = 0f,
        var isFlipped: Boolean = false
    )
}

