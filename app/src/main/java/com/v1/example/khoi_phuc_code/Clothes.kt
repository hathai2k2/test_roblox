package com.v1.example.khoi_phuc_code

import android.graphics.Bitmap

data class Clothes(
    var parts: List<Bitmap> = emptyList(),
    var type: String = ""
)