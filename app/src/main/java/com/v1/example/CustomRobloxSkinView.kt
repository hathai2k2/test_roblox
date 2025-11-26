package com.v1.example


import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

data class UVPart(
    val name: String,
    val srcRect: Rect,   // Tọa độ trên PNG 585x559
    val destRect: Rect,  // Vị trí vẽ trên Canvas
    var isVisibleOnScreen: Boolean = true
)

class CustomRobloxSkinView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var texture: Bitmap? = null
    val parts = mutableListOf<UVPart>()
    private var fullSkinBitmap: Bitmap? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        // Khởi tạo UV mapping chuẩn cho texture 585x559
        // destRect có thể điều chỉnh theo Canvas hiển thị
        val scale = 1 // hệ số scale nếu muốn phóng to
        parts.add(UVPart("FRONT", Rect(0,0,128,128), Rect(100,0,228,128)))
        parts.add(UVPart("BACK", Rect(128,0,256,128), Rect(228,0,356,128)))
        parts.add(UVPart("LEFT", Rect(256,0,384,128), Rect(0,0,128,128)))
        parts.add(UVPart("RIGHT", Rect(384,0,512,128), Rect(356,0,484,128)))
        parts.add(UVPart("UP", Rect(0,128,128,192), Rect(100,128,228,192)))
        parts.add(UVPart("BOTTOM", Rect(128,128,256,192), Rect(100,192,228,256)))
        parts.add(UVPart("LEFT_HAND", Rect(256,128,320,192), Rect(0,128,64,192)))
        parts.add(UVPart("RIGHT_HAND", Rect(320,128,384,192), Rect(484,128,548,192)))
        parts.add(UVPart("LEFT_LEG", Rect(384,128,448,256), Rect(100,256,164,384)))
        parts.add(UVPart("RIGHT_LEG", Rect(448,128,512,256), Rect(228,256,292,384)))
        // Bạn có thể thêm các phần khác nếu texture có thêm
    }

    /** Gán texture PNG gốc 585x559 */
    fun setTexture(textureBitmap: Bitmap) {
        texture = textureBitmap
        generateFullSkinBitmap() // Cập nhật bitmap tổng thể
        invalidate()
    }

    /** Set trạng thái hiển thị của từng phần */
    fun setPartVisible(partName: String, visible: Boolean) {
        parts.find { it.name == partName }?.isVisibleOnScreen = visible
        invalidate()
    }

    /** Cắt 1 phần từ texture */
    private fun getPartBitmap(part: UVPart): Bitmap? {
        val tex = texture ?: return null
        return Bitmap.createBitmap(
            tex,
            part.srcRect.left,
            part.srcRect.top,
            part.srcRect.width(),
            part.srcRect.height()
        )
    }

    /** Vẽ view */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        texture ?: return

        for (part in parts) {
            if (!part.isVisibleOnScreen) continue
            val bmp = getPartBitmap(part) ?: continue
            canvas.drawBitmap(bmp, null, part.destRect, paint)
        }
    }

    /** Xuất bitmap full skin, tất cả phần, bỏ qua isVisibleOnScreen */
    fun exportFullSkinBitmap(): Bitmap? {
        val tex = texture ?: return null
        val width = 585 // hoặc tính theo destRect max
        val height = 559
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        for (part in parts) {
            val partBmp = getPartBitmap(part) ?: continue
            canvas.drawBitmap(partBmp, null, part.destRect, paint)
        }
        return bmp
    }

    /** Tạo bitmap tổng thể để vẽ trên view */
    private fun generateFullSkinBitmap() {
        val tex = texture ?: return
        val width = 585
        val height = 559
        fullSkinBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(fullSkinBitmap!!)
        for (part in parts) {
            val bmp = getPartBitmap(part) ?: continue
            canvas.drawBitmap(bmp, null, part.destRect, paint)
        }
    }

    /** Tùy chỉnh layout nếu muốn */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 600
        val desiredHeight = 600
        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
}
