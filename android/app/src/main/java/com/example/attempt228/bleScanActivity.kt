package com.example.attempt228

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
import com.example.attempt228.databinding.ActivityBleScanBinding
import kotlin.math.pow

class bleScanActivity : AppCompatActivity() {
    private var btAdapter: BluetoothAdapter? = null
    private var btLeScanner: BluetoothLeScanner? = null
    private lateinit var binding: ActivityBleScanBinding
    private lateinit var rcAdapter: BDeviceAdapter
    private val ble1mArr = mutableListOf<Int>()
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}

    private val enableBtLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if (it.resultCode == Activity.RESULT_OK){
                Toast.makeText(this, "Bluetooth is on!", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "Bluetooth is off!", Toast.LENGTH_SHORT).show()
            }
        }

    private var referenceRSSI = -70 // estimated RSSI at 1 meter
    private val pathLossExponent = 2.4

    private val scanLeCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val deviceName: String?
            val deviceRssi: Int?
            try {
                deviceName = result?.device?.name
                deviceRssi = result?.rssi
                if (deviceName == "Redmi Note 8 Pro" && deviceRssi != null){
                    ble1mArr.add(deviceRssi)
                    calcAvgRSSIAt1m()
                    calcDistance(deviceRssi)
                    rcAdapter.addDevice(BDevice(deviceName, deviceRssi))
                    Log.d("MyLog", "$deviceName scanned. Rssi: $deviceRssi")
                } else {
                    Log.d("MyLog", "Null device was scanned")
                }
            } catch (e: SecurityException) {
                // Handle SecurityException if needed
            }
        }
    }

    private fun calcAvgRSSIAt1m() {
        if (ble1mArr.size >= 100) {
            referenceRSSI = ble1mArr.average().toInt()
            Log.d("1mRssi", "Estimated 1-meter RSSI: $referenceRSSI")
            ble1mArr.clear()
        }
    }

    private fun calcDistance(rssi: Int): Double {
        val distance = 10.0.pow((rssi - referenceRSSI) / (-10.0 * pathLossExponent))
        Log.d("referenceRssi", "Reference rssi used: $referenceRSSI")
        Log.d("distanceRssi", "Estimated distance $distance")
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
        val indoorMapView = findViewById<IndoorMapView>(R.id.indoorMapView)

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
        }catch (e: SecurityException){
            Log.d("MyLog", "Starting scanning failed")
        }
    }

    private fun initBt(){
        val bManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = bManager.adapter
        btLeScanner = btAdapter?.bluetoothLeScanner
    }

    private fun enableBt(){
        if (btAdapter?.isEnabled == false){
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private fun getScanSettings(): ScanSettings{
        val scanBuilder = ScanSettings.Builder()
        scanBuilder
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0L)
        return scanBuilder.build()
    }

    private fun initRcView() = with(binding){
        rcView.layoutManager = LinearLayoutManager(this@bleScanActivity)
        rcAdapter = BDeviceAdapter(ArrayList())
        rcView.adapter = rcAdapter
    }

    private fun preLaunchPermissions(){
        if (!checkPerm()){
            launchCheckPermissions()
        }
    }

    private fun launchCheckPermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }else{
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun checkPerm(): Boolean{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        }else{
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}
