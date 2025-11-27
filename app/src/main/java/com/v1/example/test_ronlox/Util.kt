import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.RectF
import android.util.TypedValue
import kotlin.math.abs
import kotlin.math.round

//package com.v1.example.test_ronlox
//
//import android.graphics.Bitmap
//import org.opencv.core.Mat
//import org.opencv.android.Utils
//object Util {
//    // Chuyển Bitmap sang Mat
//    fun bitmapToMat(bitmap: Bitmap): Mat {
//        val mat = Mat()
//        Utils.bitmapToMat(bitmap, mat)
//        return mat
//    }
//
//    // Chuyển Mat sang Bitmap
//    fun matToBitmap(mat: Mat): Bitmap {
//        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
//        Utils.matToBitmap(mat, bitmap)
//        return bitmap
//    }
//}


fun Float.toDp(): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics
    )
}

fun Int.toDp(context: Context): Int {
    val density = context.resources.displayMetrics.density
    return (this * density).toInt()
}

fun Int.isDarkColor(): Boolean {
    val darkness =
        1 - (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255
    return if (darkness < 0.5) {
        false
    } else {
        true
    }
}

fun Int.isShowRate(): Boolean {
    return this in listOf(2, 4, 6, 10, 14)
}

fun FloatArray.top() = this[1]

fun FloatArray.left() = this[0]

fun FloatArray.bottom() = this[5]

fun FloatArray.right() = this[2]

fun FloatArray.width() = abs(right() - left())

fun FloatArray.height() = abs(bottom() - top())

fun FloatArray.toRectF(rectF: RectF) {
    rectF.set(
        Float.POSITIVE_INFINITY,
        Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY,
        Float.NEGATIVE_INFINITY
    )
    for (i in 1 until size step 2) {
        val x = round(this[i - 1] * 10) / 10f
        val y = round(this[i] * 10) / 10f
        rectF.left = if (x < rectF.left) x else rectF.left
        rectF.top = if (y < rectF.top) y else rectF.top
        rectF.right = if (x > rectF.right) x else rectF.right
        rectF.bottom = if (y > rectF.bottom) y else rectF.bottom
    }
    rectF.sort()
}

fun FloatArray.toRectF(): RectF {
    val rect = RectF()
    toRectF(rect)
    return rect
}