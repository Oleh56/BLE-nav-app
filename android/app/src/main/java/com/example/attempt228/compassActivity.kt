package com.example.attempt228

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.attempt228.databinding.ActivityCompassBinding
import kotlin.math.roundToInt

class compassActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityCompassBinding
    private lateinit var sensorManager: SensorManager
    private var sensorAccelerometer: Sensor? = null
    private var sensorMagneticField: Sensor? = null

    private val floatGravity = FloatArray(3)
    private val floatGeoMagnetic = FloatArray(3)
    private val floatOrientation = FloatArray(3)
    private val floatRotationMatrix = FloatArray(9)
    private val ALPHA = 0.1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)


        val sensorEventListenerAccelerometer = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {

                floatGravity[0] = ALPHA * event.values[0] + (1 - ALPHA) * floatGravity[0]
                floatGravity[1] = ALPHA * event.values[1] + (1 - ALPHA) * floatGravity[1]
                floatGravity[2] = ALPHA * event.values[2] + (1 - ALPHA) * floatGravity[2]

                SensorManager.getRotationMatrix(
                    floatRotationMatrix,
                    null,
                    floatGravity,
                    floatGeoMagnetic
                )
                SensorManager.getOrientation(floatRotationMatrix, floatOrientation)

                binding.compassNeedle.rotation = (-(floatOrientation[0] * 180 / Math.PI)).toFloat()
            }

            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

            }
        }

        val sensorEventListenerMagneticField = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                floatGeoMagnetic[0] = ALPHA * event.values[0] + (1 - ALPHA) * floatGeoMagnetic[0]
                floatGeoMagnetic[1] = ALPHA * event.values[1] + (1 - ALPHA) * floatGeoMagnetic[1]
                floatGeoMagnetic[2] = ALPHA * event.values[2] + (1 - ALPHA) * floatGeoMagnetic[2]

                SensorManager.getRotationMatrix(floatRotationMatrix, null, floatGravity, floatGeoMagnetic)
                SensorManager.getOrientation(floatRotationMatrix, floatOrientation)

                binding.compassNeedle.rotation = (-(floatOrientation[0] * 180 / Math.PI)).toFloat()
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

            }
        }

        sensorManager.registerListener(
            sensorEventListenerAccelerometer,
            sensorAccelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        sensorManager.registerListener(
            sensorEventListenerMagneticField,
            sensorMagneticField,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onStop() {
        super.onStop()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Implement your logic for handling the rotation based on sensor changes
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Handle accuracy changes if needed
    }
}
