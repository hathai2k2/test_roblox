package com.v1.example.test_ronlox

import android.graphics.Bitmap
import org.opencv.core.Mat
import org.opencv.android.Utils
object Util {
    // Chuyển Bitmap sang Mat
    fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    // Chuyển Mat sang Bitmap
    fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
}