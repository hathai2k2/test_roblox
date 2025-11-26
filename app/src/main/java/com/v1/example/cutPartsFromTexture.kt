package com.v1.example

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect

fun cutPartsFromTexture(texture: Bitmap): Map<String, Bitmap> {
    val width = texture.width
    val height = texture.height
    val visited = Array(height) { BooleanArray(width) }

    val parts = mutableMapOf<String, Bitmap>()
    var partIndex = 0

    fun floodFill(x: Int, y: Int, bounds: Rect) {
        val stack = mutableListOf<Pair<Int, Int>>()
        stack.add(x to y)
        while (stack.isNotEmpty()) {
            val (cx, cy) = stack.removeAt(stack.size - 1)
            if (cx !in 0 until width || cy !in 0 until height) continue
            if (visited[cy][cx]) continue
            if (Color.alpha(texture.getPixel(cx, cy)) == 0) continue

            visited[cy][cx] = true
            bounds.left = minOf(bounds.left, cx)
            bounds.top = minOf(bounds.top, cy)
            bounds.right = maxOf(bounds.right, cx)
            bounds.bottom = maxOf(bounds.bottom, cy)

            // Kiểm tra 4 hướng
            stack.add((cx + 1) to cy)
            stack.add((cx - 1) to cy)
            stack.add(cx to (cy + 1))
            stack.add(cx to (cy - 1))
        }
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (!visited[y][x] && Color.alpha(texture.getPixel(x, y)) != 0) {
                val bounds = Rect(x, y, x, y)
                floodFill(x, y, bounds)

                // Cắt bitmap của phần này
                val partBitmap = Bitmap.createBitmap(
                    texture,
                    bounds.left,
                    bounds.top,
                    bounds.width() + 1,
                    bounds.height() + 1
                )
                parts["PART_$partIndex"] = partBitmap
                partIndex++
            }
        }
    }

    return parts
}
