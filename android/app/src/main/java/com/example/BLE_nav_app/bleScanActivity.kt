package com.example.BLE_nav_app

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.BLE_nav_app.databinding.ActivityBleScanBinding
import com.google.android.material.snackbar.Snackbar
import com.jiahuan.svgmapview.SVGMapView
import com.jiahuan.svgmapview.overlay.SVGMapLocationOverlay
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt


class bleScanActivity : AppCompatActivity() {
    private var btAdapter: BluetoothAdapter? = null
    private var btLeScanner: BluetoothLeScanner? = null
    private lateinit var binding: ActivityBleScanBinding
    private val beaconRssiMap = mutableMapOf<String, Int>()
    private val rssiAvgBufferMap = mutableMapOf<String, MutableList<Double>>()
    private val beaconDistanceMap = mutableMapOf<String, Double>()
    private val beaconFilteredRssiMap = mutableMapOf<String, Double>()
    private lateinit var svgMapView: SVGMapView
    private lateinit var locationOverlay: SVGMapLocationOverlay
    private lateinit var userPosition: PointF
//    private var trilaterationCounter = 0

    val beaconCoordinatesMap = mapOf(
        "Beacon1" to Pair(50.0, 230.0),
        "Beacon2" to Pair(0.0, 0.0),
        "Beacon3" to Pair(160.0, 0.0)
    )
    data class RangedBeaconData(val x: Double, val y: Double)
//    data class UserPosition(val x: Double, val y: Double)

    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    private val enableBtLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth is on!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth is off!", Toast.LENGTH_SHORT).show()
            }
        }

    private var referenceRSSI = -70.0 // estimated RSSI at 1 meter
    private val pathLossExponent = 2.5
    private val averageAt1mBuffer = mutableListOf<Double>()
    private val rssiBuffer = mutableListOf<Double>()
    private var calibrationCompleted = true

    private var deviceName: String? = null
    private var deviceRssi: Int? = null

    private val scanLeCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val localDeviceName: String?
            val localDeviceRssi: Int?
            try {
                localDeviceName = result?.device?.name
                localDeviceRssi = result?.rssi
                if (localDeviceName != null && localDeviceRssi != null) {
//                    calibrateReferenceRSSI(localDeviceName, localDeviceRssi)
                    if (calibrationCompleted) {
                        processScanResult(localDeviceName, localDeviceRssi)
                    }
                } else {
                    Log.d("errorLog", "Null device or RSSI value was scanned")
                }
            } catch (e: SecurityException) {
                // Handle SecurityException if needed
                Log.e("SecurityException", "SecurityException occurred: ${e.message}")
            }
        }
    }

    private fun processScanResult(localDeviceName: String, localDeviceRssi: Int) {
        if (localDeviceName == "Beacon1" || localDeviceName == "Beacon2" || localDeviceName == "Beacon3") {
            deviceName = localDeviceName
            deviceRssi = localDeviceRssi
            beaconRssiMap[localDeviceName] = localDeviceRssi
            Log.d("MyLog", "$beaconRssiMap scanned")
            val kf = KalmanFilter(1.0, 1.0, 0.5, referenceRSSI)
                val filteredRssi = kf.getFilteredValue(localDeviceRssi.toDouble())
                beaconFilteredRssiMap[localDeviceName] = filteredRssi
                val distance = calcDistance(filteredRssi)
                beaconDistanceMap[localDeviceName] = distance
                if (beaconDistanceMap.size >= 3) {
                    val rbd1 = RangedBeaconData(beaconCoordinatesMap["Beacon1"]!!.first, beaconCoordinatesMap["Beacon1"]!!.second)
                    val rbd2 = RangedBeaconData(beaconCoordinatesMap["Beacon2"]!!.first, beaconCoordinatesMap["Beacon2"]!!.second)
                    val rbd3 = RangedBeaconData(beaconCoordinatesMap["Beacon3"]!!.first, beaconCoordinatesMap["Beacon3"]!!.second)
                    val distance1 = beaconDistanceMap["Beacon1"] ?: 0.0
                    val distance2 = beaconDistanceMap["Beacon2"] ?: 0.0
                    val distance3 = beaconDistanceMap["Beacon3"] ?: 0.0
                    val position = trilaterationPosition(rbd1, rbd2, rbd3, distance1*100, distance2*100, distance3*100)
                    var x = position["x"] ?: 0.0
                    var y = position["y"] ?: 0.0

                    userPosition = PointF(x.toFloat(), y.toFloat())
                    updateUserPosition(userPosition)
                    Log.d("Trilateration", "User position: (${position["x"]}, ${position["y"]})")
                } else {
                    Log.d("Trilateration", "Insufficient beacons detected for trilateration")
                }

                Log.d("KalmanFilter", "$localDeviceName scanned. Filtered rssi: ${filteredRssi.toInt()}")
            }
            logAllBeaconData()
        }


    private fun startScanning() {
        if (checkPermissions()) {
            try {
                btLeScanner?.startScan(scanLeCallback)
            } catch (e: SecurityException) {
                Log.d("MyLog", "Starting scanning failed: ${e.message}")
            }
        } else {
            Log.d("MyLog", "Permissions not granted for scanning")
        }
    }

    private fun logAllBeaconData() {
        val logMessage = StringBuilder()

        beaconRssiMap.forEach { (name, rssi) ->
            logMessage.append("$name: Rssi: $rssi, ")
            beaconFilteredRssiMap[name]?.let { filteredRssi ->
                val formattedFilteredRssi = filteredRssi.roundToInt()
                logMessage.append("Filtered RSSI: $formattedFilteredRssi, ")
            }
//            beaconDistanceMap[name]?.let { distance ->
//                logMessage.append("Distance: $distance, ")
//            }
        }
        Log.d("AllBeaconData", logMessage.toString())

    }

    private fun calibrateReferenceRSSI(localDeviceName: String, localDeviceRssi: Int) {
        if (localDeviceName == "Beacon1" && !calibrationCompleted) {
            averageAt1mBuffer.add(localDeviceRssi.toDouble())
            Log.d("Rssiat1m", "Element $localDeviceRssi added, size is ${averageAt1mBuffer.size}")
            if (averageAt1mBuffer.size == 100) { // Adjust the buffer size as needed
                referenceRSSI = averageAt1mBuffer.average()
                averageAt1mBuffer.clear()
                calibrationCompleted = true
                Log.d("ReferenceRSSI", "Calibrated Reference RSSI: $referenceRSSI")
                Snackbar.make(binding.root, "Calibration completed!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun addToRssiBufferAndAverage(deviceName: String, rssi: Double): Double? {
        val deviceBuffer = rssiAvgBufferMap.getOrPut(deviceName) { mutableListOf() }
        deviceBuffer.add(rssi)
        Log.d("RssiBuffer", "$deviceName: Element $rssi added, size is ${deviceBuffer.size}")

        if (deviceBuffer.size == 10) {
            val average = deviceBuffer.average()
            deviceBuffer.clear()
            return average
        }
        return 0.0
    }

    class KalmanFilter(
        private val processNoise: Double,
        private val sensorNoise: Double,
        private val estimatedError: Double,
        private val initialValue: Double
    ) {
        private var q: Double = processNoise
        private var r: Double = sensorNoise
        private var x: Double = initialValue
        private var p: Double = estimatedError
        private var k: Double = 0.0

        fun getFilteredValue(measurement: Double): Double {
            // Prediction phase
            p += q

            // Measurement update
            k = p / (p + r)
            x += k * (measurement - x)
            p *= (1 - k)

            return x
        }
    }

    private fun calcDistance(rssi: Double): Double {
        val distance = 10.0.pow((rssi - referenceRSSI) / (-10.0 * pathLossExponent))
        Log.d("referenceRssi", "${deviceName}: Reference rssi used: $referenceRSSI")
        Snackbar.make(binding.root, "${deviceName}: Estimated distance: $distance", Snackbar.LENGTH_SHORT).show()
        return distance
    }

    fun trilaterationPosition(
        rbd1: RangedBeaconData,
        rbd2: RangedBeaconData,
        rbd3: RangedBeaconData,
        distance1: Double,
        distance2: Double,
        distance3: Double
    ): Map<String, Double> {
        val a = (-2 * rbd1.x) + (2 * rbd2.x)
        val b = (-2 * rbd1.y) + (2 * rbd2.y)
        val c = distance1.pow(2) - distance2.pow(2) - rbd1.x.pow(2) + rbd2.x.pow(2) - rbd1.y.pow(2) + rbd2.y.pow(2)
        val d = (-2 * rbd2.x) + (2 * rbd3.x)
        val e = (-2 * rbd2.y) + (2 * rbd3.y)
        val f = distance2.pow(2) - distance3.pow(2) - rbd2.x.pow(2) + rbd3.x.pow(2) - rbd2.y.pow(2) + rbd3.y.pow(2)

        val x = (c * e - f * b) / (e * a - b * d)
        val y = (c * d - a * f) / (b * d - a * e)

        return mapOf("x" to x, "y" to y)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBleScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initBt()
        preLaunchPermissions()

        // Initialize IndoorMapView
        svgMapView = binding.svgMapView
        svgMapView.loadMap(AssetsHelper.getContent(this, "MAP2.svg"))

        // Set initial position
        locationOverlay = SVGMapLocationOverlay(svgMapView)
        userPosition = PointF(200.0f, 100.0f)
        locationOverlay.setPosition(userPosition)
        svgMapView.overLays.add(locationOverlay)
        svgMapView.refresh()

        // Start scanning after calibration
        startScanning()
    }

    fun updateUserPosition(userPointF: PointF) {
        val mapWidth = 150200.0f
        val mapHeight = 42200.0f

        if (userPointF != PointF(0.0f, 0.0f)) {
            // Reduce precision to one digit after the decimal point using Locale.US
            val reducedPrecisionPoint = PointF(
                String.format(Locale.US, "%.1f", userPointF.x).toFloat(),
                String.format(Locale.US, "%.1f", userPointF.y).toFloat()
            )

            // Check if the point is within map bounds
            if (reducedPrecisionPoint.x in 0.0f..mapWidth && reducedPrecisionPoint.y in 0.0f..mapHeight) {
                locationOverlay.setPosition(reducedPrecisionPoint)
                svgMapView.refresh()
                Log.d("updateUserPosition", "User position: $reducedPrecisionPoint")
            } else {
                Log.w("updateUserPosition", "User position is out of map bounds: $reducedPrecisionPoint (map width=$mapWidth, map height=$mapHeight)")
            }
        }
    }


    override fun onResume() {
        super.onResume()
        enableBluetooth()
    }

    private fun enableBluetooth() {
        if (btAdapter?.isEnabled == false) {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            startScanning()
        }
    }

    private fun initBt() {
        val bManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = bManager.adapter
        btLeScanner = btAdapter?.bluetoothLeScanner
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

//    private fun initRcView() = with(binding) {
//        rcView.layoutManager = LinearLayoutManager(this@bleScanActivity)
//        rcAdapter = BDeviceAdapter(ArrayList())
//        rcView.adapter = rcAdapter
//    }

    private fun preLaunchPermissions() {
        if (!checkPermissions()) {
            launchCheckPermissions()
        }
    }

    private fun launchCheckPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }
}
