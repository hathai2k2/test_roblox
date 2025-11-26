package com.v1.example.khoi_phuc_code

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class util {
    fun Context.loadTextureAndBuildClothes(texturePath: String, type: String): Clothes? {
        return try {
            val bitmap = assets.open(texturePath).use {
                android.graphics.BitmapFactory.decodeStream(it)
            }
            val density = resources.displayMetrics.density
            ClothesBuilder(density).buildClothesFromBitmap(bitmap, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun mergeClothesParts(parts: List<Bitmap>, density: Float): Bitmap {
        // 1. Xác định kích thước hình gốc (dựa theo preset)
        // Giả sử dùng front + right + left như ClothesPresets
        val allPresets = ClothesPresets.front + ClothesPresets.right + ClothesPresets.left

        // Tìm kích thước max
        var maxX = 0
        var maxY = 0
        for (preset in allPresets) {
            val right = ((preset.origin.x + preset.size.width) * density).toInt()
            val bottom = ((preset.origin.y + preset.size.height) * density).toInt()
            if (right > maxX) maxX = right
            if (bottom > maxY) maxY = bottom
        }

        // 2. Tạo bitmap kết quả
        val result = Bitmap.createBitmap(maxX, maxY, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // 3. Ghép từng mảnh lên canvas
        for (i in parts.indices) {
            val preset = allPresets[i]
            val left = (preset.origin.x * density).toInt()
            val top = (preset.origin.y * density).toInt()

            // Nếu muốn đổi màu, ví dụ đổi sang xanh nhạt:
            val colored = parts[i].copy(Bitmap.Config.ARGB_8888, true)
            val colorCanvas = Canvas(colored)
            val colorPaint = Paint()
            colorPaint.colorFilter = android.graphics.PorterDuffColorFilter(
                Color.parseColor("#88FF0000"), // Màu đỏ nhạt ví dụ
                android.graphics.PorterDuff.Mode.SRC_ATOP
            )
            colorCanvas.drawBitmap(colored, 0f, 0f, colorPaint)

            canvas.drawBitmap(colored, left.toFloat(), top.toFloat(), paint)
        }

        return result
    }
}