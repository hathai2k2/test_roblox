package com.v1.example.khoi_phuc_code

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.v1.example.test_ronlox.R
import kotlin.math.max

class CustomImageLayoutFixed @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val TAG = "CustomImageLayoutFixed"

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var fullBitmap: Bitmap? = null
    private var textureBitmap: Bitmap? = null
    private var currentType: String = ""

    // Cắt các phần: body, tay trái, tay phải, chân trái, chân phải
    private val parts = mutableListOf<Bitmap>()

    init {
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
    }

    fun setTexturesBitmap(bitmap: Bitmap, type: String) {
        Log.d(TAG, "setTexturesBitmap called: ${bitmap.width}x${bitmap.height}, type=$type")
        textureBitmap = resizeBitmapIfNeeded(bitmap)
        currentType = type
        post { applyBitmap(textureBitmap!!, currentType) }
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxSize: Int = 1024): Bitmap {
        val scale = max(bitmap.width, bitmap.height).toFloat() / maxSize
        return if (scale > 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width / scale).toInt(), (bitmap.height / scale).toInt(), true)
        } else bitmap
    }

    private fun applyBitmap(bitmap: Bitmap, type: String) {
        Log.d(TAG, "applyBitmap called with type=$type, bitmap=${bitmap.width}x${bitmap.height}")
        // Cắt từng phần (ví dụ tỉ lệ giả lập)
        parts.clear()
        val w = bitmap.width
        val h = bitmap.height
        val body = Bitmap.createBitmap(bitmap, w/4, h/4, w/2, h/2)
        val leftHand = Bitmap.createBitmap(bitmap, 0, h/4, w/4, h/2)
        val rightHand = Bitmap.createBitmap(bitmap, w*3/4, h/4, w/4, h/2)
        val leftLeg = Bitmap.createBitmap(bitmap, w/4, h*3/4, w/4, h/4)
        val rightLeg = Bitmap.createBitmap(bitmap, w/2, h*3/4, w/4, h/4)

        parts.addAll(listOf(body, leftHand, rightHand, leftLeg, rightLeg))

        // Tạo full bitmap dựa trên canvas view size
        if (width > 0 && height > 0) {
            fullBitmap?.recycle()
            fullBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(fullBitmap!!)
            drawParts(canvas)
            Log.d(TAG, "Full bitmap created: ${fullBitmap!!.width}x${fullBitmap!!.height}")
            invalidate()
        }
    }

    private fun drawParts(canvas: Canvas) {
        if (parts.isEmpty()) return
        val pw = width.toFloat()
        val ph = height.toFloat()

        // Vẽ body
        val body = parts[0]
        canvas.drawBitmap(Bitmap.createScaledBitmap(body, (pw*0.5).toInt(), (ph*0.5).toInt(), true), pw*0.25f, ph*0.25f, paint)

        // Vẽ tay trái
        val lh = parts[1]
        canvas.drawBitmap(Bitmap.createScaledBitmap(lh, (pw*0.25).toInt(), (ph*0.5).toInt(), true), 0f, ph*0.25f, paint)

        // Vẽ tay phải
        val rh = parts[2]
        canvas.drawBitmap(Bitmap.createScaledBitmap(rh, (pw*0.25).toInt(), (ph*0.5).toInt(), true), pw*0.75f, ph*0.25f, paint)

        // Vẽ chân trái
        val ll = parts[3]
        canvas.drawBitmap(Bitmap.createScaledBitmap(ll, (pw*0.25).toInt(), (ph*0.25).toInt(), true), pw*0.25f, ph*0.75f, paint)

        // Vẽ chân phải
        val rl = parts[4]
        canvas.drawBitmap(Bitmap.createScaledBitmap(rl, (pw*0.25).toInt(), (ph*0.25).toInt(), true), pw*0.5f, ph*0.75f, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        fullBitmap?.let { canvas.drawBitmap(it, 0f, 0f, paint) }
    }
}
