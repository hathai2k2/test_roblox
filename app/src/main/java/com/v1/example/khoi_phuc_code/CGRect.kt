package com.v1.example.khoi_phuc_code

data class CGRect(
    val position: Int,
    val origin: CGPoint,
    val size: CGSize
) {
    companion object {
        const val FRONT = 1
        const val RIGHT = 2
        const val LEFT = 3
        const val BACK = 4
        const val TOP = 5
        const val BOTTOM = 6
    }

    override fun toString(): String {
        val posStr = when (position) {
            FRONT -> "FRONT"
            RIGHT -> "RIGHT"
            LEFT -> "LEFT"
            BACK -> "BACK"
            TOP -> "TOP"
            BOTTOM -> "BOTTOM"
            else -> "NULL"
        }
        return "CGRect(position=$posStr, origin=$origin, size=$size)"
    }
}