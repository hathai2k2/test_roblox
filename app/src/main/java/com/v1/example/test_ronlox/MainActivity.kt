package com.v1.example.test_ronlox

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.v1.example.khoi_phuc_code.Clothes
import com.v1.example.khoi_phuc_code.ClothesBuilder
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

        // Nút bật/tắt chế độ touch để đổi màu/texture body parts
        var touchColorEnabled = false
        findViewById<Button>(R.id.replaceBlueBtn).setOnClickListener { button ->
            touchColorEnabled = !touchColorEnabled

            if (touchColorEnabled) {
                // Load texture từ icon11111111.png
                val textureBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon11111111)

                // Bật chế độ touch với texture mode
                clothesView.enableBodyPartColorChange = true

                clothesView.colorChangeIsLeg = true
                clothesView.colorChangeIsArm = true
                clothesView.useTextureMode = true  // Bật texture mode
                clothesView.textureToApply = textureBitmap

                (button as Button).text = "Touch Mode: ON"
                Toast.makeText(this, "Touch body parts to apply texture", Toast.LENGTH_SHORT).show()

                // Setup callback để log khi touch vào body part
                clothesView.onBodyPartTouched = { partName, partIndex ->
                    Log.d("MainActivity", "Touched: $partName (index: $partIndex)")
                    Toast.makeText(this, "Applied texture to: $partName", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Tắt chế độ touch
                clothesView.enableBodyPartColorChange = false
                clothesView.useTextureMode = false
                clothesView.textureToApply = null
                (button as Button).text = "Touch Mode: OFF"
                Toast.makeText(this, "Touch mode disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Nút xóa stickers và textures
        findViewById<Button>(R.id.undo).setOnClickListener {
            clothesView.clearAll(isLeg = true,isArm = false)
            Log.d("MainActivity", "Cleared all stickers and textures")
            Toast.makeText(this, "Cleared stickers & textures", Toast.LENGTH_SHORT).show()
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
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                isDither = true
            }

            canvas.drawBitmap(bitmap, 0f, 0f, paint)
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