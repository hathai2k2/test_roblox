package com.v1.example.test_ronlox

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.TypedValue
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.v1.example.khoi_phuc_code.StickerOverlay
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

fun drawHighQuality(source: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }

    canvas.drawBitmap(source, 0f, 0f, paint)

    return result
}

fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
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

fun mergeStickersIntoPart(stickerOverlay: StickerOverlay, partBitmap: Bitmap, partView: ImageView): Bitmap {
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

    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }

    stickerOverlay.stickers.forEach { st ->

        val stickerLeftInView = (overlayLocation[0] + st.x) - partLocation[0]
        val stickerTopInView = (overlayLocation[1] + st.y) - partLocation[1]

        val left = stickerLeftInView * scaleX
        val top = stickerTopInView * scaleY
        val right = left + (st.width * st.scale) * scaleX
        val bottom = top + (st.height * st.scale) * scaleY

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