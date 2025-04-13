package com.example.metalstars

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import kotlin.math.cos
import kotlin.math.sin

class PhoneOrientation(context: Context) {
    private val sensorManager: SensorManager = context.getSystemService()!!
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
                Sensor.TYPE_ACCELEROMETER -> gravity.copyFrom(event.values)
                Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic.copyFrom(event.values)
            }

            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                updateQuaternion()
            }
        }
    }

    fun start() {
        registerSensor(accelerometer)
        registerSensor(magnetometer)
    }

    fun stop() {
        sensorManager.unregisterListener(sensorListener)
    }

    fun getQuaternion(): FloatArray = quaternion.clone()

    private fun registerSensor(sensor: Sensor?) {
        sensor?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }
    }

    private fun updateQuaternion() {
        val orientationMatrix = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientationMatrix)

        val (c1, c2, c3) = orientationMatrix.map { cos(it / 2) }
        val (s1, s2, s3) = orientationMatrix.map { sin(it / 2) }

        quaternion[0] = s1 * s2 * c3 + c1 * c2 * s3 // x
        quaternion[1] = s1 * c2 * c3 + c1 * s2 * s3 // y
        quaternion[2] = c1 * s2 * c3 - s1 * c2 * s3 // z
        quaternion[3] = c1 * c2 * c3 - s1 * s2 * s3 // w
    }

    private fun FloatArray.copyFrom(source: FloatArray) {
        System.arraycopy(source, 0, this, 0, this.size)
    }
}