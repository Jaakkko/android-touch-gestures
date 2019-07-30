# android-touch-gestures
![alt text](https://raw.githubusercontent.com/Jaakkko/android-touch-gestures/master/screenshots/zoom.gif)
![alt text](https://raw.githubusercontent.com/Jaakkko/android-touch-gestures/master/screenshots/rotation.gif)
![alt text](https://raw.githubusercontent.com/Jaakkko/android-touch-gestures/master/screenshots/translate.gif)

## Template
```kotlin
class SampleView : View {
  
  ...
  
  private val mTouchGestureDetector: TouchGestureDetector = TouchGestureDetector(context, object : TouchGestureDetector.Listener {
        override fun onEnd() {}
        override fun onDoubleClick(x: Float, y: Float) {}
        override fun onClick(detector: TouchGestureDetector, x: Float, y: Float) {}
        override fun onLongClick(x: Float, y: Float) {}

        override fun onMove(detector: TouchGestureDetector, deltaX: Float, deltaY: Float) {}
        override fun onScale(detector: TouchGestureDetector, focusX: Float, focusY: Float, scaleFactor: Float) {}
        override fun onRotate(focusX: Float, focusY: Float, angle: Float) {}
  })
  
  ...
  
  override fun onTouchEvent(event: MotionEvent): Boolean {
    val r: Boolean = mTouchGestureDetector.onTouchEvent(event)
    if (r)
      invalidate()

    return r
  }

  override fun computeScroll() {
    if (mTouchGestureDetector.computeScroll()) {
      ViewCompat.postInvalidateOnAnimation(this)
    }
  }
}
```

Full example inside <a href="./app">app</a> directory.
