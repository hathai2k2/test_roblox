package com.v1.example.khoi_phuc_code

import android.graphics.Bitmap
import android.util.Log

class ClothesBuilder(private val density: Float) {

    fun buildClothesFromBitmap(bitmap: Bitmap, type: String): Clothes {
        val parts = mutableListOf<Bitmap>()
        val density = density

        if (type == "Shirt") {
            // Chỉ lấy 1 bitmap đại diện cho thân
            parts.add(bitmap)
            Log.d("ClothesBuilder", "Shirt body size: ${bitmap.width}x${bitmap.height}")
        } else {
            // Chia nhỏ theo preset front + right + left
            for (preset in ClothesPresets.front + ClothesPresets.right + ClothesPresets.left) {
                val x = (preset.origin.x * density).toInt().coerceAtMost(bitmap.width)
                val y = (preset.origin.y * density).toInt().coerceAtMost(bitmap.height)
                val w = (preset.size.width * density).toInt().coerceAtMost(bitmap.width - x)
                val h = (preset.size.height * density).toInt().coerceAtMost(bitmap.height - y)

                if (w > 0 && h > 0) {
                    val bmp = Bitmap.createBitmap(bitmap, x, y, w, h)
                    parts.add(bmp)
                    Log.d("ClothesBuilder", "$type part at ${preset.origin.x},${preset.origin.y} size ${bmp.width}x${bmp.height}")
                } else {
                    Log.w("ClothesBuilder", "Skipped invalid $type part at $x,$y size $w x $h")
                }
            }
        }

        return Clothes(parts, type)
    }


}