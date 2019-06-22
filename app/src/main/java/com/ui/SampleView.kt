package com.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v4.view.ViewCompat
import android.view.MotionEvent
import android.view.View
import com.touchgestures.TouchGestureDetector
import kotlin.math.cos
import kotlin.math.sin

class SampleView(context: Context) : View(context) {
    private object Camera {
        var x: Float = 0f
        var y: Float = 0f
        var zoom: Float = 1f
        var angle: Float = 0f
    }

    private val mTouchGestureDetector: TouchGestureDetector = TouchGestureDetector(context, object : TouchGestureDetector.Listener {
        override fun onEnd() {
            // Toast.makeText(context, "onEnd()", Toast.LENGTH_SHORT).show()
        }
        override fun onDoubleClick(x: Float, y: Float) {
            // Toast.makeText(context, "onDoubleClick($x, $y)", Toast.LENGTH_SHORT).show()
        }
        override fun onClick(detector: TouchGestureDetector, x: Float, y: Float) {
            // Toast.makeText(context, "onClick($x, $y)", Toast.LENGTH_SHORT).show()
        }
        override fun onLongClick(x: Float, y: Float) {
            // Toast.makeText(context, "onEnd()", Toast.LENGTH_SHORT).show()
        }


        override fun onMove(detector: TouchGestureDetector, deltaX: Float, deltaY: Float) {
            Camera.x += deltaX
            Camera.y += deltaY
        }

        override fun onScale(detector: TouchGestureDetector, focusX: Float, focusY: Float, scaleFactor: Float) {
            Camera.x = (Camera.x - focusX) * scaleFactor + focusX
            Camera.y = (Camera.y - focusY) * scaleFactor + focusY
            Camera.zoom *= scaleFactor
        }

        override fun onRotate(focusX: Float, focusY: Float, angle: Float) {
            val c: Float = cos(angle)
            val s: Float = sin(angle)

            Camera.x -= focusX
            Camera.y -= focusY

            val cx: Float = Camera.x
            val cy: Float = Camera.y

            Camera.x = c * cx - s * cy
            Camera.y = s * cx + c * cy

            Camera.x += focusX
            Camera.y += focusY

            val da: Float = Math.toDegrees(angle.toDouble()).toFloat()
            Camera.angle += da
        }
    })


    private val mPaint: Paint = Paint()

    init {
        mPaint.color = Color.parseColor("#FBC02D")
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val ret: Boolean = mTouchGestureDetector.onTouchEvent(event)

        if (ret)
            invalidate()

        return ret
    }

    override fun computeScroll() {
        if (mTouchGestureDetector.computeScroll()) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }



    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#5472d3"))

        canvas.translate(Camera.x, Camera.y)
        canvas.scale(Camera.zoom, Camera.zoom)
        canvas.rotate(Camera.angle)

        canvas.drawRect(0f, 0f, 100f, 100f, mPaint)
    }
}