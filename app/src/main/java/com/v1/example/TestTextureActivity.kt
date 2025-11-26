package com.v1.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.v1.example.test_ronlox.R

class TestTextureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_texture)

        // Load texture PNG gốc
        val textureBitmap = BitmapFactory.decodeResource(resources, R.drawable.texture)
        Log.d("SkinTest", "Original PNG size: width=${textureBitmap.width}, height=${textureBitmap.height}")
        findViewById<ImageView>(R.id.imgOriginal).setImageBitmap(textureBitmap)

        // Cắt từng phần từ texture
        val parts = cutPartsFromTexture(textureBitmap)
        Log.d("SkinTest", "Found ${parts.size} parts")

        // Gán phần vào ImageView
        val partMap = mapOf(
            "FRONT" to R.id.imgFront,
            "BACK" to R.id.imgBack,
            "LEFT" to R.id.imgLeft,
            "RIGHT" to R.id.imgRight,
            "UP" to R.id.imgUp,
            "BOTTOM" to R.id.imgBottom,
            "LEFT_HAND" to R.id.imgLeftHand,
            "RIGHT_HAND" to R.id.imgRightHand,
            "LEFT_LEG" to R.id.imgLeftLeg,
            "RIGHT_LEG" to R.id.imgRightLeg
        )

        // Dùng heuristic: kích thước width/height hoặc vị trí để map part tự động
        val sortedParts = parts.values.sortedByDescending { it.width * it.height } // lớn -> nhỏ

        var i = 0
        for ((name, id) in partMap) {
            if (i >= sortedParts.size) break
            findViewById<ImageView>(id).setImageBitmap(sortedParts[i])
            Log.d("SkinTest", "Assign $name -> part_${i} size=${sortedParts[i].width}x${sortedParts[i].height}")
            i++
        }
    }

    /**
     * Cắt từng khối không trong suốt từ bitmap
     */
    private fun cutPartsFromTexture(texture: Bitmap): Map<String, Bitmap> {
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

                    val partBitmap = Bitmap.createBitmap(
                        texture,
                        bounds.left,
                        bounds.top,
                        bounds.width() + 1,
                        bounds.height() + 1
                    )
                    parts["part_$partIndex"] = partBitmap
                    partIndex++
                }
            }
        }

        return parts
    }
}
