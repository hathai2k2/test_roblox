package com.v1.example.test_ronlox

import android.graphics.Bitmap
import androidx.annotation.ColorInt

data class DrawConfig(
    val scaleFactor: Float = 1f,
    @ColorInt val color: Int? = null,
    val opacity: Int = 255,
    val angle: Float = 0f,
    val bitmapOverlay: Bitmap? = null
)
