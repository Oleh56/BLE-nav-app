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
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.BLE_nav_app.databinding.ActivityBleScanBinding
import com.google.android.material.snackbar.Snackbar
import java.text.DecimalFormat
import kotlin.math.pow

class bleScanActivity : AppCompatActivity() {
    private var btAdapter: BluetoothAdapter? = null
    private var btLeScanner: BluetoothLeScanner? = null
    private lateinit var binding: ActivityBleScanBinding
    private lateinit var rcAdapter: BDeviceAdapter
    private val beaconRssiMap = mutableMapOf<String, Int>()
    private val rssiAvgBufferMap = mutableMapOf<String, MutableList<Double>>()
    private val beaconDistanceMap = mutableMapOf<String, Double>()
    private val beaconFilteredRssiMap = mutableMapOf<String, Double>()
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
    private val pathLossExponent = 1.0
    private val averageAt1mBuffer = mutableListOf<Double>()
    private val rssiBuffer = mutableListOf<Double>()
    private var calibrationCompleted = false

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
                    calibrateReferenceRSSI(localDeviceName, localDeviceRssi)
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
        if (localDeviceName == "Beacon1" || localDeviceName == "Beacon2" || localDeviceName == "Redmi Note 8 Pro") {
            deviceName = localDeviceName
            deviceRssi = localDeviceRssi
            rcAdapter.addDevice(BDevice(localDeviceName, localDeviceRssi))
            beaconRssiMap[localDeviceName] = localDeviceRssi
            Log.d("MyLog", "$localDeviceName scanned. Rssi: $localDeviceRssi")
            val avgRssi = addToRssiBufferAndAverage(localDeviceName, localDeviceRssi.toDouble())
            val kf = KalmanFilter(0.40, 1.0, 0.1, referenceRSSI)
            if (avgRssi != null) {
                val filteredRssi = kf.getFilteredValue(avgRssi.toDouble())
                beaconFilteredRssiMap[localDeviceName] = filteredRssi
                val distance = calcDistance(filteredRssi)
                beaconDistanceMap[localDeviceName] = distance
                Log.d("KalmanFilter", "$localDeviceName scanned. Filtered rssi: ${filteredRssi.toInt()}")
            } else {
                Log.d("errorLog", "avgRssi is null")
            }
            logAllBeaconData()
        } else {
            Log.d("errorLog", "Non-beacon device was scanned")
        }
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
        val decimalFormat = DecimalFormat("#.##")

        beaconRssiMap.forEach { (name, rssi) ->
            logMessage.append("$name: Rssi: $rssi, ")
            beaconFilteredRssiMap[name]?.let { filteredRssi ->
                val formattedFilteredRssi = decimalFormat.format(filteredRssi)
                logMessage.append("Filtered RSSI: $formattedFilteredRssi, ")
            }
            beaconDistanceMap[name]?.let { distance ->
                val formattedDistance = decimalFormat.format(distance)
                logMessage.append("Distance: $formattedDistance, ")
            }
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

        return null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBleScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initRcView()
        initBt()
        preLaunchPermissions()

        // Initialize IndoorMapView
        val indoorMapView = binding.indoorMapView

        val mappedBeacon1Width = indoorMapView.mappedBeaconX
        val mappedBeacon1Height = indoorMapView.mappedBeaconY

        val beaconPositions = listOf(
            Pair(mappedBeacon1Height, mappedBeacon1Width),
            Pair(450f, 540f), // Adjust as needed
        )
        indoorMapView.setBeaconPositions(beaconPositions)

        val userPosition = Pair(1000f, 200f) // Adjust as needed
        indoorMapView.setUserPosition(userPosition)

        // Start scanning after calibration
        startScanning()
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

    private fun initRcView() = with(binding) {
        rcView.layoutManager = LinearLayoutManager(this@bleScanActivity)
        rcAdapter = BDeviceAdapter(ArrayList())
        rcView.adapter = rcAdapter
    }

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
