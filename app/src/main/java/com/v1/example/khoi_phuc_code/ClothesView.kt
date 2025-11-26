package com.v1.example.khoi_phuc_code

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import com.v1.example.test_ronlox.R

class ClothesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val bodyView: ImageView
    private val leftHandView: ImageView
    private val rightHandView: ImageView
    private val leftLegView: ImageView
    private val rightLegView: ImageView

    init {
        orientation = VERTICAL
        inflate(context, R.layout.people, this)

        bodyView = findViewById(R.id.body)
        leftHandView = findViewById(R.id.left_hand)
        rightHandView = findViewById(R.id.right_hand)
        leftLegView = findViewById(R.id.left_leg)
        rightLegView = findViewById(R.id.right_leg)
    }

    /**
     * Load các phần chính từ Clothes
     * 0: body, 6: left leg, 12: right leg
     * 1: left hand, 2: right hand (tuỳ preset)
     */
    fun loadParts(clothes: Clothes, isLeg: Boolean = true, isArm: Boolean = true) {
        if (clothes.parts.size < 13) return

        // Thân
        bodyView.setImageBitmap(clothes.parts[0])

//        // Tay (nếu có)
        if (clothes.parts.size > 1) leftHandView.setImageBitmap(clothes.parts[1])
        if (clothes.parts.size > 2) rightHandView.setImageBitmap(clothes.parts[2])

        // Chân
        leftLegView.setImageBitmap(clothes.parts[6])
        rightLegView.setImageBitmap(clothes.parts[12])
    }

    /**
     * Thay đổi bitmap của 1 phần bất kỳ
     */
    fun setPartBitmap(index: Int, bmp: Bitmap) {
        when (index) {
            0 -> bodyView.setImageBitmap(bmp)
            1 -> leftHandView.setImageBitmap(bmp)
            2 -> rightHandView.setImageBitmap(bmp)
            6 -> leftLegView.setImageBitmap(bmp)
            12 -> rightLegView.setImageBitmap(bmp)
        }
    }
}
