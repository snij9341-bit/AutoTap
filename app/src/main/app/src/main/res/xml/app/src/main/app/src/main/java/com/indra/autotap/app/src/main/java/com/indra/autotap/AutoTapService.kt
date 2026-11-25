package com.indra.autotap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class AutoTapService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var busy = false
    private var lastTap = 0L

    private val gamePackages = setOf("com.dts.freefireth", "com.dts.freefiremax")

    private val loop = object : Runnable {
        override fun run() {
            scan()
            handler.postDelayed(this, 1500)
        }
    }

    override fun onServiceConnected() {
        handler.post(loop)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(loop)
        recognizer.close()
        super.onDestroy()
    }

    private fun scan() {
        val prefs = getSharedPreferences("autotap", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false) || busy) return

        val pkg = rootInActiveWindow?.packageName?.toString()
        if (pkg != null && pkg !in gamePackages) return

        busy = true
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(result: ScreenshotResult) {
                val buffer = result.hardwareBuffer
                val bmp = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                    ?.copy(Bitmap.Config.ARGB_8888, false)
                buffer.close()
                if (bmp == null) {
                    busy = false
                    return
                }
                process(bmp, prefs)
            }

            override fun onFailure(errorCode: Int) {
                busy = false
            }
        })
    }

    private fun process(bmp: Bitmap, prefs: SharedPreferences) {
        recognizer.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { text ->
                val w = bmp.width.toFloat()
                val h = bmp.height.toFloat()
                var hit: Point? = null

                outer@ for (block in text.textBlocks) for (line in block.lines) {
                    val box = line.boundingBox ?: continue
                    val t = line.text.uppercase().filter { it in 'A'..'Z' }
                    val fx = box.centerX() / w
                    val fy = box.centerY() / h

                    val match = when {
                        "PLAYAGAIN" in t && prefs.getBoolean("playagain", true) &&
                            fx > 0.55f && fy > 0.5f -> true
                        "CONTINUE" in t && prefs.getBoolean("continue", true) &&
                            fx > 0.65f && fy > 0.8f -> true
                        "LOBBY" in t && prefs.getBoolean("lobby", true) &&
                            fx > 0.65f && fy > 0.8f -> true
                        else -> false
                    }
                    if (match) {
                        hit = Point(box.centerX(), box.centerY())
                        break@outer
                    }
                }
                bmp.recycle()

                val now = System.currentTimeMillis()
                val p = hit
                if (p != null && now - lastTap > 2500) {
                    lastTap = now
                    tap(p.x.toFloat(), p.y.toFloat())
                } else {
                    busy = false
                }
            }
            .addOnFailureListener {
                bmp.recycle()
                busy = false
            }
    }

    private fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        val ok = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) { busy = false }
            override fun onCancelled(g: GestureDescription?) { busy = false }
        }, null)
        if (!ok) busy = false
    }
}
