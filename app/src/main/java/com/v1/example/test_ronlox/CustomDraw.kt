package com.v1.example.test_ronlox

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import toDp
import kotlin.math.max
import kotlin.math.min

class CustomDraw(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val path = Path()
    private val erasePath = Path()
    private var paint: Paint = Paint()
    private var bitmapPaint: Paint = Paint()
    private val spacing = 10f.toDp()
    private val erasePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        color = Color.TRANSPARENT
        strokeWidth = 7f.toDp()
    }
    var isErasing = false
        set(value) {
            if (isErasing && value.not()) {
                saveBitmap()
            }
            field = value
        }
    private var currentBitmap: Bitmap? = null
    private val bitmapBound = RectF()
    private var strokeConfig = StrokeConfig()

    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var maxY = Float.MIN_VALUE

    enum class BrushType {
        SOLID, DASHED, PLATINUM
    }

    init {
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        setStrokeConfig(strokeConfig)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        currentBitmap?.let { canvas.drawBitmap(it, null, bitmapBound, bitmapPaint) }
        canvas.drawPath(path, paint)
        canvas.drawPath(erasePath, erasePaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmapBound.set(0f, 0f, w.toFloat(), h.toFloat())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val path = if (isErasing) erasePath else this.path
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                if (isErasing.not()) getNewLocation(x, y)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                saveBitmap()
            }
        }
        return true
    }

    fun setStrokeConfig(config: StrokeConfig) {
        strokeConfig = config
        paint.strokeWidth = config.width
        setBrushStyle(config.type)
        paint.color = config.color
        paint.alpha = config.opacity
        if (config.eraseWidth != erasePaint.strokeWidth) {
            saveBitmap()
            erasePaint.strokeWidth = config.eraseWidth
        }
        invalidate()
    }



    private fun createTextureFromImage(selectedColor: Int): Bitmap {
        val bitmapSize = 100
        val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val red = Color.red(selectedColor)
        val green = Color.green(selectedColor)
        val blue = Color.blue(selectedColor)
        val squareSize = 1
        for (i in 0 until bitmapSize step squareSize) {
            for (j in 0 until bitmapSize step squareSize) {
                val randomFactor = (Math.random() * 0.4 + 0.8).toFloat()
                val newRed = (red * randomFactor).toInt().coerceIn(0, 255)
                val newGreen = (green * randomFactor).toInt().coerceIn(0, 255)
                val newBlue = (blue * randomFactor).toInt().coerceIn(0, 255)
                paint.color = Color.rgb(newRed, newGreen, newBlue)
                canvas.drawRect(
                    i.toFloat(),
                    j.toFloat(),
                    (i + squareSize).toFloat(),
                    (j + squareSize).toFloat(),
                    paint
                )
            }
        }
        return bitmap
    }

    private fun setBrushStyle(type: BrushType) {
        paint.xfermode = null
        when (type) {
            BrushType.SOLID -> {
                paint.pathEffect = null
                paint.shader = null
            }

            BrushType.DASHED -> {
                paint.pathEffect =
                    DashPathEffect(floatArrayOf(10f.toDp(), 5f.toDp(), 10f.toDp(), 10f.toDp()), 0f)
                paint.shader = null
            }

            BrushType.PLATINUM -> {
                paint.pathEffect = null
                val bitmap = createTextureFromImage(strokeConfig.color)
                val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                paint.shader = shader
            }
        }
    }

    private fun getBitmap(x1: Int, y1: Int, x2: Int, y2: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this@CustomDraw.draw(canvas)
        return Bitmap.createBitmap(bitmap, x1, y1, x2 - x1, y2 - y1)
    }

    private fun saveBitmap() {
        if (measuredWidth <= 0 || measuredHeight <= 0) return
        val newBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        this@CustomDraw.draw(canvas)
        currentBitmap = newBitmap
        path.reset()
        erasePath.reset()
    }

    private fun getNewLocation(x: Float, y: Float) {
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
    }
}

