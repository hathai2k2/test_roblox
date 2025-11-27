package com.v1.example.test_ronlox

import android.graphics.Color
import androidx.annotation.ColorInt

data class StrokeConfig(
    val opacity: Int = 255,
    val width: Float = 7f.toDp(),
    val eraseWidth: Float = 7f.toDp(),
    @ColorInt val color: Int = Color.BLACK,
    val type: CustomDraw.BrushType = CustomDraw.BrushType.SOLID
)
