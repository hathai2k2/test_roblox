package com.v1.example.khoi_phuc_code

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.v1.example.test_ronlox.R

class Test2Activity : AppCompatActivity() {
    private lateinit var clothesView: ClothesView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test2)
        clothesView = findViewById(R.id.clothesView)
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.tshirt)
        val density = resources.displayMetrics.density
        val builder = ClothesBuilder(density)

        val clothes = builder.buildClothesFromBitmap(bitmap, "Sirt")

        // Load các phần vào CustomView
        clothesView.loadParts(clothes, isArm = true)

        // Nếu muốn override 1 phần
        // clothesView.setPartBitmap(6, myNewBitmapForLeftLeg)

    }
}