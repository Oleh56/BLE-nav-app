package com.example.BLE_nav_app

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
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
import kotlin.math.pow

class bleScanActivity : AppCompatActivity() {
    private var btAdapter: BluetoothAdapter? = null
    private var btLeScanner: BluetoothLeScanner? = null
    private lateinit var binding: ActivityBleScanBinding
    private lateinit var rcAdapter: BDeviceAdapter
    private val beacon1Arr = mutableListOf<Int>()
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
    private val pathLossExponent = 2.0
    private val averageAt1mBuffer = mutableListOf<Double>()
    private val rssiBuffer = mutableListOf<Double>()
    private var calibrationCompleted = false

    private val scanLeCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val deviceName: String?
            val deviceRssi: Int?
            try {
                deviceName = result?.device?.name
                deviceRssi = result?.rssi
                if (deviceName == "Redmi Note 8 Pro" && deviceRssi != null) {
                    rcAdapter.addDevice(BDevice(deviceName, deviceRssi))
                    calculateAt1m(deviceRssi.toDouble())
                    val avgRssi = addToRssiBufferAndAverage(deviceRssi.toDouble())
                    if (calibrationCompleted && avgRssi != null) {
                        val filteredRssi = KalmanFilter(avgRssi)
                        val distance = calcDistance(filteredRssi)
                        Log.d("MyLog", "$deviceName scanned. Rssi: $deviceRssi")
                        Log.d("KalmanFilter", "$deviceName scanned. Filtered rssi: ${filteredRssi.toInt()}")
                    } else {
                        Log.d("MyLog", "Not enough data to calculate average RSSI or calibration not completed")
                    }
                } else {
                    Log.d("MyLog", "Null device was scanned")
                }
            } catch (e: SecurityException) {
                // Handle SecurityException if needed
            }
        }
    }

    private fun addToRssiBufferAndAverage(rssi: Double): Double? {
        rssiBuffer.add(rssi)
        Log.d("RssiBuffer", "Element $rssi added, size is ${rssiBuffer.size}")

        if (rssiBuffer.size == 10) {
            val average = rssiBuffer.average()
            rssiBuffer.clear()
            return average
        }

        return null
    }

    private fun calculateAt1m(rssi: Double) {
        if (!calibrationCompleted) {
            averageAt1mBuffer.add(rssi)
            Log.d("Rssiat1m", "Element $rssi added, size is ${averageAt1mBuffer.size}")
            if (averageAt1mBuffer.size == 50) {
                val rssiAt1m = averageAt1mBuffer.average()
                referenceRSSI = rssiAt1m
                averageAt1mBuffer.clear()
                calibrationCompleted = true
            }
        }
    }

    private fun KalmanFilter(rssi: Double): Double {
        val Q = 0.00001 // process noise covariance constant
        val R = 0.001 // measurement noise covariance constant
        var P = 1.0 // estimate error covariance constant
        val X = if (calibrationCompleted) {
            referenceRSSI
        } else {
            rssi
        }
        val K = (P + Q) / (P + Q + R)
        P = R * ((P + Q) / (P + Q + R))
        val filteredRssi = X + K * (rssi - X)
        return filteredRssi
    }

    private fun calcDistance(rssi: Double) {
        val distance = 10.0.pow((rssi - referenceRSSI) / (-10.0 * pathLossExponent))
        Log.d("referenceRssi", "Reference rssi used: $referenceRSSI")
        Log.d("distanceRssi", "Estimated distance $distance")
        Snackbar.make(binding.root, "Estimated distance: $distance", Snackbar.LENGTH_SHORT).show()
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
            Pair(450f, 540f),
        )
        indoorMapView.setBeaconPositions(beaconPositions)

        val userPosition = Pair(1000f, 200f)
        indoorMapView.setUserPosition(userPosition)
    }

    override fun onResume() {
        super.onResume()
        enableBt()
        try {
            btLeScanner?.startScan(null, getScanSettings(), scanLeCallback)
            Log.d("MyLog", "Scanned Successful")
        } catch (e: SecurityException) {
            Log.d("MyLog", "Starting scanning failed")
        }
    }

    private fun initBt() {
        val bManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = bManager.adapter
        btLeScanner = btAdapter?.bluetoothLeScanner
    }

    private fun enableBt() {
        if (btAdapter?.isEnabled == false) {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private fun getScanSettings(): ScanSettings {
        val scanBuilder = ScanSettings.Builder()
        scanBuilder
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0L)
        return scanBuilder.build()
    }

    private fun initRcView() = with(binding) {
        rcView.layoutManager = LinearLayoutManager(this@bleScanActivity)
        rcAdapter = BDeviceAdapter(ArrayList())
        rcView.adapter = rcAdapter
    }

    private fun preLaunchPermissions() {
        if (!checkPerm()) {
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

    private fun checkPerm(): Boolean {
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
}