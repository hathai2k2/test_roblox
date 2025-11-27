package com.v1.example.test_ronlox


import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import toDp
import toRectF
import kotlin.math.atan2
import kotlin.math.sqrt

abstract class Component(
    var id: String = System.currentTimeMillis().toString(),
    var isVisible: Boolean = true,
    var isClicked: Boolean = false,
) {
    val matrix = Matrix()
    var needDrawBound = true
    private val matrixValues = FloatArray(9)

    abstract fun getWidth(): Float
    abstract fun getHeight(): Float
    open fun getMinWidth() = 100f.toDp()
    open fun getMinHeight() = 100f.toDp()

    open fun getMinScaleFactor() = 0.25f
    open fun getMaxScaleFactor() = 4f

    // Tạo ra instance mới của Component copy giá trị sang, không được giữ reference đến bất kỳ thành phần nào của Component cũ
    abstract fun copy(context: Context): Component
    protected abstract fun onDraw(canvas: Canvas)

    fun copyWithoutId(context: Context) = copy(context).apply { this.id = System.currentTimeMillis().toString() }

    open fun applyCurrentConfig() {}

    open fun onTouchEvent(event: MotionEvent?, parentView: View): Boolean = false

    open fun draw(canvas: Canvas) {
        canvas.save()
        canvas.concat(matrix)
        onDraw(canvas)
        canvas.restore()
    }

    open fun getBoundPoint(): FloatArray {
        return floatArrayOf(
            0f, 0f, // top left
            getWidth(), 0f, // top right
            getWidth(), getHeight(), // bottom right
            0f, getHeight() // bottom left
        )
    }

    fun getMappedPoint(dst: FloatArray, src: FloatArray) {
        dst.fill(0f)
        matrix.mapPoints(dst, src)
    }

    fun getMappedCenterPoint(dst: PointF) {
        val centerX = getWidth() / 2f
        val centerY = getHeight() / 2f
        val dstArray = FloatArray(2)
        val srcArray = floatArrayOf(centerX, centerY)
        matrix.mapPoints(dstArray, srcArray)
        dst.set(dstArray[0], dstArray[1])
    }

    fun contain(x: Float, y: Float): Boolean {
        val tempMatrix = Matrix()
        val rect = getNoRotationRect(tempMatrix)
        val newPoint = FloatArray(2)
        tempMatrix.mapPoints(newPoint, floatArrayOf(x, y))
        return rect.contains(newPoint[0], newPoint[1])
    }

    fun getNoRotationRect(matrix: Matrix): RectF {
        matrix.postRotate(-getAngle())
        val mappedPoints = FloatArray(8)
        getMappedPoint(mappedPoints, getBoundPoint())
        val noRotationPoints = FloatArray(8)
        matrix.mapPoints(noRotationPoints, mappedPoints)
        return noRotationPoints.toRectF()
    }

    fun getNoRotationRect(): RectF {
        return getNoRotationRect(Matrix())
    }

    fun getAngle(): Float {
        return Math.toDegrees(
            -(atan2(
                getMatrixValue(matrix, Matrix.MSKEW_X).toDouble(),
                getMatrixValue(matrix, Matrix.MSCALE_X).toDouble()
            ))
        ).toFloat()
    }

    fun getScaleFactor(): Float {
        val scaleX = getMatrixValue(matrix, Matrix.MSCALE_X)
        val skewY = getMatrixValue(matrix, Matrix.MSKEW_Y)
        return sqrt(scaleX * scaleX + skewY * skewY)
    }

    fun reset() {
        matrix.reset()
    }

    private fun getMatrixValue(matrix: Matrix, index: Int): Float {
        matrix.getValues(matrixValues)
        return matrixValues[index]
    }
}
