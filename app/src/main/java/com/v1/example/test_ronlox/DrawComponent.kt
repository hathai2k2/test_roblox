package com.v1.example.test_ronlox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff.Mode
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.min

class DrawComponent(
    private val drawBitmap: Bitmap,
    id: String = System.currentTimeMillis().toString()
) : Component(id) {

    private val bound = RectF()
    private val paint = Paint()
    private var config = DrawConfig()
    private val overlayPaint = Paint()
    private var overlay: Bitmap? = null

    init {
        bound.set(0f, 0f, getWidth(), getHeight())
    }

    fun getBitmap(): Bitmap {
        return drawBitmap
    }

    override fun getWidth() = drawBitmap.width.toFloat()

    override fun getHeight() = drawBitmap.height.toFloat()

    override fun getMinHeight() = min(drawBitmap.height.toFloat(), super.getMinHeight())

    override fun getMinWidth() = min(drawBitmap.width.toFloat(), super.getMinWidth())

    override fun copy(context: Context) = DrawComponent(drawBitmap.copy(drawBitmap.config!!, false), id).apply {
        this.matrix.set(this@DrawComponent.matrix)
        this.setConfig(this@DrawComponent.config)
    }

    override fun onDraw(canvas: Canvas) {
        if (overlay != null) {
            canvas.drawBitmap(overlay!!, null, bound, null)
        } else {
            canvas.drawBitmap(drawBitmap, null, bound, paint)
        }
    }

    fun setConfig(config: DrawConfig) {
        this.config = config
        paint.alpha = config.opacity
        if (config.bitmapOverlay == null) {
            overlay = null
            paint.setColorFilter(config.color?.let { PorterDuffColorFilter(config.color, Mode.SRC_IN) })
        } else {
            paint.setColorFilter(null)
            config.bitmapOverlay.let {
                overlayPaint.setXfermode(PorterDuffXfermode(Mode.SRC_ATOP))
                overlayPaint.shader = BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                overlay = Bitmap.createBitmap(drawBitmap.width, drawBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(overlay!!)
                canvas.drawBitmap(drawBitmap, null, bound, paint)
                canvas.drawRect(bound, overlayPaint)
            }
        }
    }

    fun getConfig() = config

    override fun applyCurrentConfig() {
        setConfig(config)
    }
}
