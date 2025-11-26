package com.v1.example.test_ronlox

import android.graphics.Bitmap
import android.graphics.Rect

data class ItemData(
    var bitmap: Bitmap,
    var blueRegions: List<Rect> = emptyList()
)
