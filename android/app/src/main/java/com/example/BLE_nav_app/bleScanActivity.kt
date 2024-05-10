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
    private val beacon2Arr = mutableListOf<Int>()
    private val beacon3Arr = mutableListOf<Int>()
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

    private val scanLeCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val deviceName: String?
            val deviceRssi: Int?
            try {
                deviceName = result?.device?.name
                deviceRssi = result?.rssi
                if (
                    (deviceName == "Beacon1" ||
                    deviceName == "Beacon2") &&
                    deviceRssi != null
                    ) {
                    rcAdapter.addDevice(BDevice(deviceName, deviceRssi))
                    val kf = KalmanFilter(0.065, 1.4, 1.0, referenceRSSI)
                    val filteredRssi = kf.getFilteredValue(deviceRssi.toDouble())
                    val distance = calcDistance(filteredRssi)
                    Log.d("MyLog", "$deviceName scanned. Rssi: $deviceRssi")
                    Log.d("KalmanFilter", "$deviceName scanned. Filtered rssi: ${filteredRssi.toInt()}")
                } else {
                    Log.d("errorLog", "Null device was scanned")
                }
            } catch (e: SecurityException) {
                // Handle SecurityException if needed
            }
        }
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