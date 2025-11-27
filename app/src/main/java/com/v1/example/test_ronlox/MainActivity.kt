package com.v1.example.test_ronlox

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.v1.example.khoi_phuc_code.Clothes
import com.v1.example.khoi_phuc_code.ClothesBuilder
import com.v1.example.khoi_phuc_code.ClothesView
import com.v1.example.khoi_phuc_code.ClothesView2
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var clothesView: ClothesView2
    private lateinit var currentClothes: Clothes

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ClothesView
        clothesView = findViewById(R.id.clothesView)

        // Load sample clothes data
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.pant)
        val density = resources.displayMetrics.density
        val builder = ClothesBuilder(density)
        currentClothes = builder.buildClothesFromBitmap(bitmap, "Sirt")
        // Load the clothes parts into ClothesView
        clothesView.loadParts(currentClothes, isLeg = true, isArm = false)

        // Nút thêm sticker
        findViewById<Button>(R.id.addStickerBtn).setOnClickListener {
            val stickerBitmap = loadBitmapFromResource(this, R.drawable.ic_headphone, 150, 150)
            clothesView.addSticker(stickerBitmap, 100f, 200f, 150, 150)
            Log.d("MainActivity", "Added sticker to ClothesView")
        }

//        // Nút thay thế body part
//        findViewById<Button>(R.id.replaceBlueBtn).setOnClickListener {
//            // Replace body with a different texture
//            val replacement = BitmapFactory.decodeResource(resources, R.drawable.icon11111111)
//            clothesView.setPartBitmap(0, replacement)
//            Log.d("MainActivity", "Replaced body part")
//        }

        // Nút xóa stickers
        findViewById<Button>(R.id.undo).setOnClickListener {
            clothesView.clearStickers()
            Log.d("MainActivity", "Cleared all stickers")
        }

        // Nút lưu ảnh (chỉ body, không có head, có stickers)
        findViewById<Button>(R.id.saveImageBtn).setOnClickListener {
            if (checkPermission()) {
                // Lấy ảnh với stickers đã merge, cắt biên
                val mergedBitmap = clothesView.captureWithStickersApplied()

                // Lưu ảnh
                saveImage(mergedBitmap, "ClothesView_Merged")

                Toast.makeText(this, "Đã merge stickers và rebuild!", Toast.LENGTH_SHORT).show()
            } else {
                requestPermission()
            }
        }

        // Nút lưu template (Roblox format) - trực tiếp từ view hiện tại
        findViewById<Button>(R.id.saveTemplateBtn).setOnClickListener {
            if (checkPermission()) {
                // Tạo template Roblox từ clothes gốc (không có stickers)
                val templateBitmap = clothesView.captureAsTemplate(currentClothes,isLeg = true,isArm = false)

                // Lưu template
                saveImage(templateBitmap, "ClothesView_Template")

                Toast.makeText(this, "Đã lưu template Roblox!", Toast.LENGTH_SHORT).show()
            } else {
                requestPermission()
            }
        }

        // Check permission on start
        if (!checkPermission()) {
            requestPermission()
        }
    }

    /**
     * Rebuild Clothes từ merged bitmap (như load từ R.drawable.image)
     * Bitmap này đã có stickers merged và đã được crop
     */
    private fun rebuildClothesFromMergedBitmap(mergedBitmap: Bitmap): Clothes {
        val density = resources.displayMetrics.density
        val builder = ClothesBuilder(density)

        // Build lại clothes từ merged bitmap, giống như load ban đầu
        return builder.buildClothesFromBitmap(mergedBitmap, "MergedWithStickers")
    }


    /**
     * Tạo template Roblox với stickers đã được merge vào texture
     */
    private fun createRobloxTemplateWithStickers(): Bitmap {
        // 1. Capture từng body part với stickers merged
        val bodyWithStickers = captureBodyPartWithStickers(clothesView.findViewById(R.id.body))
        val leftLegWithStickers = captureBodyPartWithStickers(clothesView.findViewById(R.id.left_leg))
        val rightLegWithStickers = captureBodyPartWithStickers(clothesView.findViewById(R.id.right_leg))

        // 2. Tạo template Roblox với UV mapping chuẩn
        return createRobloxTemplate(bodyWithStickers, leftLegWithStickers, rightLegWithStickers)
    }

    /**
     * Chụp một body part với stickers đã merge
     */
    private fun captureBodyPartWithStickers(view: ImageView): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Vẽ view gốc
        view.draw(canvas)

        // Vẽ stickers overlay nếu có
        val stickerOverlay = clothesView.findViewById<android.view.View>(
            clothesView.childCount - 1
        ) // StickerOverlay là child cuối cùng

        // Get sticker overlay position
        val viewLocation = IntArray(2)
        view.getLocationInWindow(viewLocation)

        val overlayLocation = IntArray(2)
        stickerOverlay.getLocationInWindow(overlayLocation)

        // Translate canvas để vẽ stickers đúng vị trí
        canvas.save()
        canvas.translate(
            (overlayLocation[0] - viewLocation[0]).toFloat(),
            (overlayLocation[1] - viewLocation[1]).toFloat()
        )
        stickerOverlay.draw(canvas)
        canvas.restore()

        return bitmap
    }

    /**
     * Tạo template Roblox chuẩn 585×559 từ các body parts
     * Sử dụng UV mapping từ ClothesPresets
     */
    private fun createRobloxTemplate(bodyBitmap: Bitmap, leftLegBitmap: Bitmap, rightLegBitmap: Bitmap): Bitmap {
        val templateWidth = 585
        val templateHeight = 559

        val templateBitmap = Bitmap.createBitmap(templateWidth, templateHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(templateBitmap)

        // Background xám nhạt Roblox
        canvas.drawColor(Color.rgb(163, 162, 165))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // ============== TORSO (BODY) - ClothesPresets.front ==============
        // FRONT: (231, 74) - 128×128
        val torsoFront = Bitmap.createScaledBitmap(bodyBitmap, 128, 128, true)
        canvas.drawBitmap(torsoFront, 231f, 74f, paint)

        // RIGHT: (165, 74) - 64×128
        val torsoRight = Bitmap.createScaledBitmap(bodyBitmap, 64, 128, true)
        canvas.drawBitmap(torsoRight, 165f, 74f, paint)

        // LEFT: (361, 74) - 64×128
        val torsoLeft = Bitmap.createScaledBitmap(bodyBitmap, 64, 128, true)
        canvas.drawBitmap(torsoLeft, 361f, 74f, paint)

        // BACK: (427, 74) - 128×128
        val torsoBack = Bitmap.createScaledBitmap(bodyBitmap, 128, 128, true)
        canvas.drawBitmap(torsoBack, 427f, 74f, paint)

        // TOP: (231, 8) - 128×64
        val torsoTop = Bitmap.createScaledBitmap(bodyBitmap, 128, 64, true)
        canvas.drawBitmap(torsoTop, 231f, 8f, paint)

        // BOTTOM: (231, 204) - 128×64
        val torsoBottom = Bitmap.createScaledBitmap(bodyBitmap, 128, 64, true)
        canvas.drawBitmap(torsoBottom, 231f, 204f, paint)

        // ============== LEFT LEG - ClothesPresets.left ==============
        // FRONT: (217, 355) - 64×128
        val leftLegFront = Bitmap.createScaledBitmap(leftLegBitmap, 64, 128, true)
        canvas.drawBitmap(leftLegFront, 217f, 355f, paint)

        // RIGHT: (151, 355) - 64×128
        val leftLegRight = Bitmap.createScaledBitmap(leftLegBitmap, 64, 128, true)
        canvas.drawBitmap(leftLegRight, 151f, 355f, paint)

        // LEFT: (19, 355) - 64×128
        val leftLegLeft = Bitmap.createScaledBitmap(leftLegBitmap, 64, 128, true)
        canvas.drawBitmap(leftLegLeft, 19f, 355f, paint)

        // BACK: (85, 355) - 64×128
        val leftLegBack = Bitmap.createScaledBitmap(leftLegBitmap, 64, 128, true)
        canvas.drawBitmap(leftLegBack, 85f, 355f, paint)

        // TOP: (217, 289) - 64×64
        val leftLegTop = Bitmap.createScaledBitmap(leftLegBitmap, 64, 64, true)
        canvas.drawBitmap(leftLegTop, 217f, 289f, paint)

        // BOTTOM: (217, 485) - 64×64
        val leftLegBottom = Bitmap.createScaledBitmap(leftLegBitmap, 64, 64, true)
        canvas.drawBitmap(leftLegBottom, 217f, 485f, paint)

        // ============== RIGHT LEG - ClothesPresets.right ==============
        // FRONT: (308, 355) - 64×128
        val rightLegFront = Bitmap.createScaledBitmap(rightLegBitmap, 64, 128, true)
        canvas.drawBitmap(rightLegFront, 308f, 355f, paint)

        // RIGHT: (506, 355) - 64×128
        val rightLegRight = Bitmap.createScaledBitmap(rightLegBitmap, 64, 128, true)
        canvas.drawBitmap(rightLegRight, 506f, 355f, paint)

        // LEFT: (374, 355) - 64×128
        val rightLegLeft = Bitmap.createScaledBitmap(rightLegBitmap, 64, 128, true)
        canvas.drawBitmap(rightLegLeft, 374f, 355f, paint)

        // BACK: (440, 355) - 64×128
        val rightLegBack = Bitmap.createScaledBitmap(rightLegBitmap, 64, 128, true)
        canvas.drawBitmap(rightLegBack, 440f, 355f, paint)

        // TOP: (308, 288) - 64×64
        val rightLegTop = Bitmap.createScaledBitmap(rightLegBitmap, 64, 64, true)
        canvas.drawBitmap(rightLegTop, 308f, 288f, paint)

        // BOTTOM: (308, 485) - 64×64
        val rightLegBottom = Bitmap.createScaledBitmap(rightLegBitmap, 64, 64, true)
        canvas.drawBitmap(rightLegBottom, 308f, 485f, paint)

        return templateBitmap
    }



    fun loadBitmapFromResource(context: Context, resId: Int, width: Int, height: Int): Bitmap {
        val drawable = context.getDrawable(resId)
            ?: throw IllegalArgumentException("Resource not found: $resId")

        return if (drawable is BitmapDrawable) {
            // Nếu là bitmap drawable, lấy bitmap trực tiếp và resize
            Bitmap.createScaledBitmap(drawable.bitmap, width, height, true)
        } else {
            // Nếu là vector drawable hoặc các loại khác → vẽ lên bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ không cần WRITE_EXTERNAL_STORAGE
            true
        } else {
            val result = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            result == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImage(bitmap: Bitmap, fileName: String) {
        val timestamp = System.currentTimeMillis()
        val fullFileName = "${fileName}_$timestamp.png"

        try {
            val fos: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Sử dụng MediaStore
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ClothesView")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                // Android 9 trở xuống - Sử dụng File
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(imagesDir, "ClothesView")
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                val image = File(appDir, fullFileName)
                fos = FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(this, "Đã lưu: $fullFileName", Toast.LENGTH_LONG).show()
                Log.d("MainActivity", "Image saved: $fullFileName")
            } ?: run {
                Toast.makeText(this, "Lỗi khi lưu ảnh", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Error saving image", e)
        }
    }

}