package com.example.attempt228


import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attempt228.databinding.ActivityBleScanBinding


class bleScanActivity : AppCompatActivity() {
    private var btAdapter: BluetoothAdapter? = null
    private var btLeScanner: BluetoothLeScanner? = null
    private lateinit var binding: ActivityBleScanBinding
    private lateinit var rcAdapter: BDeviceAdapter

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

    private val scanLeCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val deviceName: String?
            val deviceRssi: Int?
            try {
                deviceName = result?.device?.name
                deviceRssi = result?.rssi
                if (deviceName != null && deviceRssi != null){
                    rcAdapter.addDevice(BDevice(
                        deviceName,
                        deviceRssi
                    ))
                    Log.d("MyLog", "$deviceName scanned. Rssi: $deviceRssi")
                }else{
                    Log.d("MyLog", "Null device was scanned")
                }
            }catch (e: SecurityException){}
        }
    }

    /*private val bReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND){
                val deviceName =
                    intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                val deviceRssi =
                    intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0)
                val deviceSet = mutableSetOf<BDevice>()
                deviceSet.addAll(rcAdapter.currentList)
                deviceSet.add(
                    BDevice(
                        deviceName!!,
                        deviceRssi.toInt()
                    )
                )
                rcAdapter.submitList(deviceSet.toList())
                Log.d("MyLog", "Device name: $deviceName Rssi: $deviceRssi")
            }
        }
    }*/

    /*private fun intentFilters(){
        this.registerReceiver(bReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBleScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initRcView()
        initBt()
        preLaunchPermissions()
        /*intentFilters()
        try {
            btAdapter?.startDiscovery()
        }catch (e: SecurityException){
            Log.d("MyLog", e.message!!)
        }*/
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