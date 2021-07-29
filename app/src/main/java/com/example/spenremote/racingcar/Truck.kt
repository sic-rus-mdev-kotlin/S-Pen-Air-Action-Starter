package com.example.spenremote.racingcar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

class Truck {
    @JvmField
    var x = 0
    @JvmField
    var y = 0
    var width = 0
    @JvmField
    var height = 0
    private var boundary: Rect? = null
    var isLast: Boolean
    private var mBitmap: Bitmap

    constructor(bitmap: Bitmap) {
        mBitmap = bitmap
        width = bitmap.width
        height = bitmap.height
        boundary = Rect()
        isLast = false
    }

    constructor(bitmap: Bitmap, x: Int, y: Int) {
        mBitmap = bitmap
        this.x = x
        this.y = y
        width = bitmap.width
        height = bitmap.height
        boundary = Rect()
        isLast = false
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(mBitmap, x.toFloat(), y.toFloat(), null)
    }

    fun moveDown(speed: Int) {
        y += speed
    }

    fun draw(canvas: Canvas, b: Boolean) {
        canvas.drawBitmap(mBitmap, x.toFloat(), y.toFloat(), null)
    }

    fun setPosition(x: Int) {
        this.x = x
    }

    fun getBoundary(): Rect? {
        val handicap = 20
        boundary!!.left = x + handicap
        boundary!!.top = y + handicap
        boundary!!.right = x + width - handicap
        boundary!!.bottom = y + height - handicap
        return boundary
    }
}