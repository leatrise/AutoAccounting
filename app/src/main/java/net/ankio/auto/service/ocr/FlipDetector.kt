package net.ankio.auto.service.ocr

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import net.ankio.auto.storage.Logger

/**
 * 设备翻转检测器：检测设备从朝下翻转到朝上时触发回调。
 */
class FlipDetector(
    private val manager: SensorManager,
    private val debounceMs: Long = 400L,
    private val onFlipChange: () -> Unit,
) : SensorEventListener {

    private val sensor: Sensor? = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private enum class Face { UP, DOWN, UNKNOWN }

    private var lastFace = Face.UNKNOWN
    private var lastTime = 0L

    fun start(): Boolean = sensor?.let {
        manager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        true
    } ?: false.also { Logger.w("No gravity/accelerometer sensor") }

    fun stop() = manager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val z = event.values[2]
        val now = SystemClock.uptimeMillis()

        val face = when {
            z > 7f -> Face.UP
            z < -7f -> Face.DOWN
            else -> Face.UNKNOWN
        }

        if (face == Face.UNKNOWN || face == lastFace || now - lastTime < debounceMs) return

        Logger.d("Device orientation changed: $lastFace -> $face (z=$z)")

        if (face == Face.UP && lastFace == Face.DOWN) {
            Logger.d("Device flipped from face-down to face-up, triggering OCR")
            onFlipChange()
        }

        lastFace = face
        lastTime = now
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
