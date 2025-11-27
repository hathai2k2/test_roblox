package com.v1.example.khoi_phuc_code

import android.graphics.Bitmap

data class Sticker(
    val bitmap: Bitmap,
    var x: Float,
    var y: Float,
    val width: Int,
    val height: Int,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var isFlipped: Boolean = false
)
