package com.v1.example.khoi_phuc_code

import android.graphics.*
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.v1.example.test_ronlox.R

class TestActivity : AppCompatActivity() {

    private lateinit var bodyView: ImageView
    private lateinit var leftHandView: ImageView
    private lateinit var rightHandView: ImageView
    private lateinit var leftLegView: ImageView
    private lateinit var rightLegView: ImageView

    private lateinit var finalImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_test)

        // 15 mảnh
        bodyView = findViewById(R.id.body)
        leftHandView = findViewById(R.id.left_hand)
        rightHandView = findViewById(R.id.right_hand)
        leftLegView = findViewById(R.id.left_leg)
        rightLegView = findViewById(R.id.right_leg)


        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.template1)
        val density = resources.displayMetrics.density
        val builder = ClothesBuilder(density)

        var clothes = builder.buildClothesFromBitmap(bitmap, "Sirt")
        clothes.parts.forEachIndexed { index, bmp ->
            Log.d("TestActivity", "Part[$index] size: ${bmp.width}x${bmp.height}")
        }

        if (clothes.parts.isNotEmpty() && clothes.parts.size >= 18) {
//            val newParts = clothes.parts.toMutableList()
//            changeBitmapColor(newParts[6], Color.RED)?.let { newParts[6] = it }
//            changeBitmapColor(newParts[12], Color.BLUE)?.let { newParts[12] = it }
//            clothes = clothes.copy(parts = newParts) // Nếu Clothes là data class
            // --- Body 0-5 ---
            val bodyViews = listOf(
                findViewById<ImageView>(R.id.body0),
                findViewById<ImageView>(R.id.body1),
                findViewById<ImageView>(R.id.body2),
                findViewById<ImageView>(R.id.body3),
                findViewById<ImageView>(R.id.body4),
                findViewById<ImageView>(R.id.body5)
            )
            bodyViews.forEachIndexed { index, imageView ->
                imageView.setImageBitmap(clothes.parts[index])
            }

            // --- Bên trái 6-11 ---
            val leftViews = listOf(
                findViewById<ImageView>(R.id.left0),
                findViewById<ImageView>(R.id.left1),
                findViewById<ImageView>(R.id.left2),
                findViewById<ImageView>(R.id.left3),
                findViewById<ImageView>(R.id.left4),
                findViewById<ImageView>(R.id.left5)
            )
            leftViews.forEachIndexed { index, imageView ->
                imageView.setImageBitmap(clothes.parts[6 + index])
            }

            // --- Bên phải 12-17 ---
            val rightViews = listOf(
                findViewById<ImageView>(R.id.right0),
                findViewById<ImageView>(R.id.right1),
                findViewById<ImageView>(R.id.right2),
                findViewById<ImageView>(R.id.right3),
                findViewById<ImageView>(R.id.right4),
                findViewById<ImageView>(R.id.right5)
            )
            rightViews.forEachIndexed { index, imageView ->
                imageView.setImageBitmap(clothes.parts[12 + index])
            }

            val mergedBitmap = mergeClothesPartsOriginal(clothes.parts, density)
            findViewById<ImageView>(R.id.finalImageView).setImageBitmap(mergedBitmap)
        }

    }

    /** Ghép tất cả các mảnh theo vị trí gốc */
    private fun mergeClothesPartsOriginal(parts: List<Bitmap>, density: Float): Bitmap {
        val allPresets = ClothesPresets.front + ClothesPresets.right + ClothesPresets.left

        // Tính kích thước canvas dựa trên preset
        var maxRight = 0
        var maxBottom = 0
        for (preset in allPresets) {
            val right = (preset.origin.x * density + preset.size.width * density).toInt()
            val bottom = (preset.origin.y * density + preset.size.height * density).toInt()
            if (right > maxRight) maxRight = right
            if (bottom > maxBottom) maxBottom = bottom
        }

        val result = Bitmap.createBitmap(maxRight, maxBottom, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // Vẽ từng mảnh theo vị trí gốc
        for ((index, preset) in allPresets.withIndex()) {
            if (index >= parts.size) break
            val bmp = parts[index]
            val left = (preset.origin.x * density).toInt()
            val top = (preset.origin.y * density).toInt()
            canvas.drawBitmap(bmp, left.toFloat(), top.toFloat(), paint)
        }

        return result
    }

    fun changeBitmapColor(src: Bitmap?, colorFilter: Int): Bitmap? {
        val bmp = src?.config?.let { Bitmap.createBitmap(src.width, src.height, it) }
        val canvas = bmp?.let { Canvas(it) }
        val paint = Paint()
        paint.colorFilter = PorterDuffColorFilter(colorFilter, PorterDuff.Mode.SRC_ATOP)
        src?.let { canvas?.drawBitmap(it, 0f, 0f, paint) }
        return bmp
    }

}
