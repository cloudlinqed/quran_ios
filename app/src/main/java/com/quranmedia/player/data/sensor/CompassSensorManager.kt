package com.quranmedia.player.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class CompassData(
    val azimuth: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM,
    val isAvailable: Boolean = true
)

@Singleton
class CompassSensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _compassData = MutableStateFlow(CompassData())
    val compassData: StateFlow<CompassData> = _compassData.asStateFlow()

    private var useRotationVector = true
    private var lastAzimuth = 0f

    // Fallback sensor data
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    fun start() {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationSensor != null) {
            useRotationVector = true
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            // Fallback to accelerometer + magnetic field
            useRotationVector = false
            val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            if (accel != null && magnetic != null) {
                sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_UI)
            } else {
                _compassData.value = CompassData(isAvailable = false)
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        gravity = null
        geomagnetic = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val azimuth: Float

        if (useRotationVector && event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        } else {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> gravity = event.values.clone()
                Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values.clone()
            }
            val g = gravity ?: return
            val m = geomagnetic ?: return

            val rotationMatrix = FloatArray(9)
            if (!SensorManager.getRotationMatrix(rotationMatrix, null, g, m)) return
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        }

        val smoothed = lowPassFilter(azimuth, lastAzimuth)
        lastAzimuth = smoothed
        val normalized = (smoothed + 360f) % 360f

        _compassData.value = _compassData.value.copy(azimuth = normalized)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        _compassData.value = _compassData.value.copy(accuracy = accuracy)
    }

    /**
     * Low-pass filter with circular interpolation to handle 359->0 wrap.
     */
    private fun lowPassFilter(newValue: Float, oldValue: Float, alpha: Float = 0.15f): Float {
        var delta = newValue - oldValue
        // Circular wrap handling
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return oldValue + alpha * delta
    }
}
