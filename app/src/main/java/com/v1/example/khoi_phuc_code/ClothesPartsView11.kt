package com.v1.example.khoi_phuc_code

import android.content.Context

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ClothesPartsView1 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var clothes: Clothes? = null
    private val partPositions = mutableMapOf<Int, RectF>()

    private val spacing = 4f
    private var cellSize = 50f

    init {
        setBackgroundColor(0xFFEEEEEE.toInt())
    }

    fun setClothes(clothes: Clothes) {
        this.clothes = clothes
        calculateLayout()
        invalidate()
    }

    private fun calculateLayout() {
        partPositions.clear()

        clothes?.let { c ->
            if (c.type == "T-Shirt") {
                val size = c.parts[0].width.toFloat()
                val x = (width - size) / 2f
                val y = (height - size) / 2f
                partPositions[0] = RectF(x, y, x + size, y + size)
            } else {
                // Tính cellSize dựa trên kích thước view
                cellSize = (width / 16f).coerceAtMost(height / 12f) - spacing

                val startX = (width - cellSize * 16 - spacing * 15) / 2f
                val startY = 50f

                layoutSkinParts(startX, startY)
            }
        }
    }

    private fun layoutSkinParts(startX: Float, startY: Float) {
        // HEAD - Top row (8x8 grid position)
        // Top of head
        addPart(4, 0, 2, 2, startX, startY, 4) // TOP

        // Head main (row 1-2)
        addPart(0, 2, 2, 2, startX, startY, 2) // RIGHT
        addPart(2, 2, 2, 2, startX, startY, 0) // FRONT
        addPart(4, 2, 2, 2, startX, startY, 1) // LEFT
        addPart(6, 2, 2, 2, startX, startY, 3) // BACK

        // Bottom of head
        addPart(4, 4, 2, 2, startX, startY, 5) // BOTTOM

        // BODY - Middle section (8x12 grid)
        val bodyY = startY + cellSize * 6 + spacing * 6

        // Top of body
        addPart(4, 0, 2, 2, startX, bodyY, 10) // TOP

        // Body main
        addPart(0, 2, 2, 3, startX, bodyY, 8) // RIGHT
        addPart(2, 2, 2, 3, startX, bodyY, 6) // FRONT
        addPart(4, 2, 2, 3, startX, bodyY, 7) // LEFT
        addPart(6, 2, 2, 3, startX, bodyY, 9) // BACK

        // Bottom of body
        addPart(4, 5, 2, 2, startX, bodyY, 11) // BOTTOM

        // ARMS AND LEGS - Bottom section
        val limbY = startY + cellSize * 14 + spacing * 14

        // RIGHT ARM (4 parts horizontal)
        addPart(8, 0, 1, 1, startX, limbY, 14) // TOP
        addPart(8, 1, 1, 1, startX, limbY, 12) // FRONT
        addPart(9, 1, 1, 1, startX, limbY, 13) // LEFT
        addPart(10, 1, 1, 1, startX, limbY, 15) // BACK
        addPart(11, 1, 1, 1, startX, limbY, 16) // RIGHT
        addPart(8, 2, 1, 1, startX, limbY, 17) // BOTTOM

        // LEFT ARM (4 parts horizontal)
        addPart(0, 0, 1, 1, startX, limbY, 14) // TOP (mirror)
        addPart(3, 1, 1, 1, startX, limbY, 12) // FRONT
        addPart(2, 1, 1, 1, startX, limbY, 13) // LEFT
        addPart(1, 1, 1, 1, startX, limbY, 15) // BACK
        addPart(0, 1, 1, 1, startX, limbY, 16) // RIGHT
        addPart(3, 2, 1, 1, startX, limbY, 17) // BOTTOM
    }

    private fun addPart(
        gridX: Int,
        gridY: Int,
        gridW: Int,
        gridH: Int,
        startX: Float,
        startY: Float,
        partIndex: Int
    ) {
        val x = startX + gridX * (cellSize + spacing)
        val y = startY + gridY * (cellSize + spacing)
        val w = gridW * cellSize + (gridW - 1) * spacing
        val h = gridH * cellSize + (gridH - 1) * spacing

        partPositions[partIndex] = RectF(x, y, x + w, y + h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateLayout()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        clothes?.let { c ->
            partPositions.forEach { (partIndex, rect) ->
                if (partIndex < c.parts.size) {
                    val bitmap = c.parts[partIndex]
                    canvas.drawBitmap(bitmap, null, rect, paint)

                    // Vẽ border
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    paint.color = 0xFFCCCCCC.toInt()
                    canvas.drawRect(rect, paint)
                    paint.style = Paint.Style.FILL
                }
            }
        }
    }
}
