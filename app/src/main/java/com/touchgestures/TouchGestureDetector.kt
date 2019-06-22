package com.touchgestures

import android.content.Context
import android.os.Handler
import android.util.SparseArray
import android.util.TypedValue
import android.view.MotionEvent
import android.view.VelocityTracker
import android.widget.Scroller
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.hypot
import kotlin.math.max

class TouchGestureDetector(context: Context, private val listener: Listener) {
    companion object {
        const val ANIM_MODE_MOVE = 0
        const val ANIM_MODE_SCALE = 1
        const val ANIM_MODE_ROTATE = 2

        const val BUFFER_SIZE = 6

        const val LONG_CLICK_TIME = 500L
    }

    private var focusX: Float = Float.NaN
    private var focusY: Float = Float.NaN

    var span: Float = Float.NaN

    private val mScroller: Scroller = Scroller(context)
    private var firstPointerUpTime: Long = -1L
    private var animMode: Int = -1

    private var pointerCount: Int = 0
    private var moved: Boolean = false

    private var bufferIndex: Int = 0
    private val rotateVelBuffer: FloatArray = FloatArray(BUFFER_SIZE)
    private val scaleVelBuffer: FloatArray = FloatArray(BUFFER_SIZE)
    private val moveVelXBuffer: FloatArray = FloatArray(BUFFER_SIZE)
    private val moveVelYBuffer: FloatArray = FloatArray(BUFFER_SIZE)

    private var isRotating: Boolean = false
    private var isRotBufferIdx: Int = 0

    private var anchoredFocusX: Float = Float.NaN
    private var anchoredFocusY: Float = Float.NaN

    private var rotVel: Float = Float.NaN

    private var startSpan: Float = Float.NaN
    private var scaleVel: Float = Float.NaN
    private var zoomIn: Boolean = false

    private var velX: Float = Float.NaN
    private var velY: Float = Float.NaN

    private var lastTime: Long = -1L

    private val histX = SparseArray<Float>()
    private val histY = SparseArray<Float>()

    private var wasScrolling: Boolean = false

    private var lastDownTime: Long = -1

    private var startTime: Long = -1
    private var tapCount: Int = 0
    private var tapX: Float = 0f
    private var tapY: Float = 0f
    private val maxTapRadius: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics)

    private var shouldEnd: Boolean = false

    private var velocityTracker: VelocityTracker? = null


    fun onTouchEvent(event: MotionEvent): Boolean {
        val eventTime: Long = event.eventTime
        pointerCount = event.pointerCount

        if (event.actionMasked in arrayOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP)) {
            isRotBufferIdx = 0

            if (moved && firstPointerUpTime == -1L && event.actionMasked in arrayOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP)) {
                val vx: Float = moveVelXBuffer.sum()
                val vy: Float = moveVelYBuffer.sum()
                val v: Float = hypot(vx, vy)
                val s: Float = scaleVelBuffer.sum()
                val sabs: Float = abs(s)
                val r: Float = rotateVelBuffer.sum()
                var rabs: Float = abs(r)
                if (!isRotating) rabs = 0f
                val m: Float = max(max(rabs, sabs), v)
                animMode = when (m) {
                    rabs -> {
                        rotVel = r / BUFFER_SIZE
                        if (!rotVel.isFinite()) rotVel = 0f

                        startSpan = span
                        anchoredFocusX = focusX
                        anchoredFocusY = focusY
                        ANIM_MODE_ROTATE
                    }
                    sabs -> {
                        scaleVel = getTotalVelocity(event)
                        zoomIn = s > 0
                        startSpan = span
                        anchoredFocusX = focusX
                        anchoredFocusY = focusY
                        ANIM_MODE_SCALE
                    }
                    else -> {
                        val vel: Float = getTotalVelocity(event)
                        velX = vx / v * vel
                        velY = vy / v * vel
                        ANIM_MODE_MOVE
                    }
                }


                firstPointerUpTime = eventTime
            }

            val (lastFocusX: Float, lastFocusY: Float) = focusX to focusY

            calculateFocus(event)
            calculateSpan(event)
            calculateHist(event)

            // Down
            if (event.actionMasked in arrayOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN)) {
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                }

                firstPointerUpTime = -1L
                animMode = -1
                mScroller.forceFinished(true)

                // Double tap
                if (eventTime - startTime > 400 || pointerCount > 1 || hypot(event.x - tapX, event.y - tapY) > maxTapRadius) tapCount = 0
                if (tapCount == 0) {
                    startTime = eventTime
                    tapX = event.x
                    tapY = event.y
                }
                tapCount++


                // Long click
                lastDownTime = eventTime
                if (!wasScrolling && pointerCount == 1 && tapCount == 1) {
                    val savedLastDownTime: Long = lastDownTime
                    Handler().postDelayed({
                        if (savedLastDownTime == lastDownTime && tapCount == 1 && pointerCount == 1 && hypot(focusX - tapX, focusY - tapY) < 8 /*8 pikseliä*/) {
                            listener.onLongClick(tapX, tapY)
                        }
                    }, LONG_CLICK_TIME)
                }
            }
            // Up && pointercount == 1
            else if (pointerCount == 1) {
                val d: Long = eventTime - firstPointerUpTime
                if (d < 128) {
                    when (animMode) {
                        ANIM_MODE_ROTATE -> {
                            focusX = 0f
                            mScroller.fling(0, 0, rotVel.toInt(), 0, Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
                        }
                        ANIM_MODE_SCALE -> {
                            if (scaleVel.isFinite()) {
                                mScroller.fling(startSpan.toInt(), 0, scaleVel.toInt(), 0, Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
                            }
                        }
                        ANIM_MODE_MOVE -> {
                            focusX = 0f
                            focusY = 0f
                            mScroller.fling(0, 0, velX.toInt(), velY.toInt(), Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
                        }
                    }

                    shouldEnd = true
                }

                // Double tap
                if (tapCount > 0 && eventTime - startTime > 300) {
                    tapCount = 0
                }
                else if (tapCount == 1 && hypot(event.x - tapX, event.y - tapY) < maxTapRadius) {
                    val x: Float = event.x
                    val y: Float = event.y
                    Handler().postDelayed({
                        if (tapCount == 1) {
                            listener.onClick(this, x, y)
                            tapCount = 0
                        }
                    }, 300)
                }
                if (tapCount == 2) {
                    listener.onDoubleClick(lastFocusX, lastFocusY)
                    tapCount = 0
                }


                histX.clear()
                histY.clear()

                span = Float.NaN
                bufferIndex = 0
                rotateVelBuffer.fill(0f)
                scaleVelBuffer.fill(0f)
                moveVelXBuffer.fill(0f)
                moveVelYBuffer.fill(0f)

                isRotating = false

                velocityTracker?.recycle()
                velocityTracker = null
            }

            lastTime = eventTime
            moved = false

            return true
        }

        velocityTracker?.also {
            it.addMovement(event)
            it.computeCurrentVelocity(1000)
        }

        val lastFocusX: Float = focusX
        val lastFocusY: Float = focusY
        calculateFocus(event)
        listener.onMove(this, focusX - lastFocusX, focusY - lastFocusY)


        getMoveVelocity(lastFocusX, lastFocusY, eventTime).apply {
            moveVelXBuffer[bufferIndex] = first
            moveVelYBuffer[bufferIndex] = second
        }

        if (pointerCount > 1) {
            val lastSpan: Float = span
            calculateSpan(event)
            listener.onScale(this, focusX, focusY, span / lastSpan)

            scaleVelBuffer[bufferIndex] = getScaleVelocity(lastSpan, eventTime)

            val angle: Float = getAngle(event, lastFocusX, lastFocusY)
            rotateVelBuffer[bufferIndex] = getRotationVelocity(angle, eventTime)
            if (isRotating) {
                listener.onRotate(focusX, focusY, angle)
            }

            isRotBufferIdx++
            if (!isRotating && isRotBufferIdx == BUFFER_SIZE) {
                val v: Float = hypot(moveVelXBuffer.sum(), moveVelYBuffer.sum())
                val sabs: Float = abs(scaleVelBuffer.sum())
                val rabs: Float = abs(rotateVelBuffer.sum())

                val isZero: Boolean = v == 0f || sabs == 0f || rabs == 0f
                if (isZero) {
                    isRotBufferIdx = 0
                }
                else if (rabs > sabs) {
                    isRotating = true
                }
            }
        }

        lastTime = eventTime
        bufferIndex = (bufferIndex + 1) % BUFFER_SIZE
        moved = true

        return true
    }

    fun computeScroll(): Boolean {
        if (mScroller.computeScrollOffset()) {
            when (animMode) {
                ANIM_MODE_ROTATE -> {
                    val lastFocusX: Float = focusX
                    focusX = mScroller.currX.toFloat()
                    val da: Float = (focusX - lastFocusX) / startSpan
                    if (da.isFinite()) {
                        listener.onRotate(anchoredFocusX, anchoredFocusY, da)
                    }
                }
                ANIM_MODE_SCALE -> {
                    val lastSpan: Float = span
                    span = mScroller.currX.toFloat()
                    val scaleFactor: Float = if (zoomIn) (span / lastSpan) else (lastSpan / span)
                    if (scaleFactor.isFinite()) {
                        listener.onScale(this, anchoredFocusX, anchoredFocusY, scaleFactor)
                    }
                }
                ANIM_MODE_MOVE -> {
                    val lastFocusX: Float = focusX
                    val lastFocusY: Float = focusY
                    focusX = mScroller.currX.toFloat()
                    focusY = mScroller.currY.toFloat()
                    val dx: Float = focusX - lastFocusX
                    val dy: Float = focusY - lastFocusY
                    if (dx.isFinite() && dy.isFinite()) {
                        listener.onMove(this, dx, dy)
                    }
                }
            }

            wasScrolling = true

            return true
        }

        if (shouldEnd) {
            listener.onEnd()
            shouldEnd = false
        }

        wasScrolling = false

        return false
    }

    /**
     * Stop zoom or translation animation
     */
    fun stopScrollers() {
        if (shouldEnd) {
            listener.onEnd()
            shouldEnd = false
        }

        mScroller.forceFinished(true)
    }

    private fun calculateFocus(event: MotionEvent) {
        focusX = 0f
        focusY = 0f

        var div = 0
        val upIndex: Int = if (event.actionMasked in arrayOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP)) event.actionIndex else -1
        val count: Int = pointerCount
        for (i: Int in 0 until count) {
            if (i == upIndex)
                continue

            focusX += event.getX(i)
            focusY += event.getY(i)
            div++
        }

        focusX /= div
        focusY /= div
    }

    private fun calculateSpan(event: MotionEvent) {
        var spanX = 0f
        var spanY = 0f

        var div = 0
        val upIndex: Int = if (event.actionMasked in arrayOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP)) event.actionIndex else -1
        val count: Int = pointerCount
        for (i: Int in 0 until count) {
            if (i == upIndex)
                continue

            spanX += abs(focusX - event.getX(i))
            spanY += abs(focusY - event.getY(i))
            div++
        }

        span = hypot(spanX, spanY) / div
    }

    private fun getAngle(event: MotionEvent, lastFocusX: Float, lastFocusY: Float): Float {
        var angle = 0f

        var div = 0
        val upIndex: Int = if (event.actionMasked in arrayOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP)) event.actionIndex else -1
        val count: Int = pointerCount
        for (i: Int in 0 until count) {
            if (i == upIndex)
                continue

            val id: Int = event.getPointerId(i)

            val px: Float = event.getX(i)
            val py: Float = event.getY(i)
            var ax: Float = px - focusX
            var ay: Float = py - focusY
            val ad: Float = hypot(ax, ay)
            ax /= ad
            ay /= ad
            var bx: Float = histX[id]!! - lastFocusX
            var by: Float = histY[id]!! - lastFocusY
            val bd: Float = hypot(bx, by)
            bx /= bd
            by /= bd
            angle -= asin(ax * by - ay * bx)
            div++

            histX.put(id, px)
            histY.put(id, py)
        }

        return angle / div
    }

    private fun calculateHist(event: MotionEvent) {
        val count: Int = pointerCount
        for (i: Int in 0 until count) {
            val id: Int = event.getPointerId(i)

            histX.put(id, event.getX(i))
            histY.put(id, event.getY(i))
        }
    }

    private fun getMoveVelocity(lastFocusX: Float, lastFocusY: Float, evenTime: Long): Pair<Float, Float> {
        val d: Float = (evenTime - lastTime) * 1E-3f
        return (focusX - lastFocusX) / d to (focusY - lastFocusY) / d
    }
    private fun getScaleVelocity(lastSpan: Float, evenTime: Long): Float {
        return (span - lastSpan) / (evenTime - lastTime) * 1000
    }
    private fun getRotationVelocity(angle: Float, eventTime: Long): Float {
        return angle * span / (eventTime - lastTime) * 1000
    }

    private fun getTotalVelocity(event: MotionEvent): Float {
        var x = 0f
        var y = 0f

        val count: Int = pointerCount
        velocityTracker?.apply {
            for (i: Int in 0 until count) {
                val id: Int = event.getPointerId(i)

                x += abs(getXVelocity(id))
                y += abs(getYVelocity(id))
            }
        }

        return hypot(x, y) / count
    }

    interface Listener {
        /**
         * Called when animation end.
         */
        fun onEnd()

        fun onDoubleClick(x: Float, y: Float)
        fun onClick(detector: TouchGestureDetector, x: Float, y: Float)
        fun onLongClick(x: Float, y: Float)

        fun onMove(detector: TouchGestureDetector, deltaX: Float, deltaY: Float)
        fun onScale(detector: TouchGestureDetector, focusX: Float, focusY: Float, scaleFactor: Float)
        fun onRotate(focusX: Float, focusY: Float, angle: Float)
    }
}