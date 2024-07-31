package com.example.metalstars

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.cos
import kotlin.math.sin

class PhoneOrientation(context: Context) {
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private var quaternion = FloatArray(4)

    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravity, 0, gravity.size)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.size)
                }
            }

            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                updateQuaternion()
            }
        }
    }

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(sensorListener, accelerometer)
        sensorManager.unregisterListener(sensorListener, magnetometer)
    }

    // rotation quaternion relative to magnetic north
    fun getQuaternion(): FloatArray {
        return quaternion
    }

    private fun updateQuaternion() {
        val orientationMatrix = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientationMatrix)

        // Calculating quaternion components
        val c1 = cos(orientationMatrix[0] / 2)  // yaw (azimuth)
        val c2 = cos(orientationMatrix[1] / 2)  // pitch
        val c3 = cos(orientationMatrix[2] / 2)  // roll
        val s1 = sin(orientationMatrix[0] / 2)
        val s2 = sin(orientationMatrix[1] / 2)
        val s3 = sin(orientationMatrix[2] / 2)

        quaternion[0] = s1 * s2 * c3 + c1 * c2 * s3 // x
        quaternion[1] = s1 * c2 * c3 + c1 * s2 * s3 // y
        quaternion[2] = c1 * s2 * c3 - s1 * c2 * s3 // z
        quaternion[3] = c1 * c2 * c3 - s1 * s2 * s3 // w
    }
}