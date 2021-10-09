package com.example.cityboxx

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.widget.*
import android.bluetooth.BluetoothDevice

import android.content.Intent

import android.content.BroadcastReceiver
import android.view.View
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

class DeviceListView : Activity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var listAdapterNewDev: SimpleAdapter
    private lateinit var listAdapterPairDev: SimpleAdapter
    private lateinit var listViewsNewDew: ListView
    private lateinit var listViewPairDev: ListView
    private lateinit var buttonScan: Button
    private lateinit var textViewNewDev: TextView
    private lateinit var textViewPairDev: TextView
    private lateinit var progressBarScan: ProgressBar
    private var listNewDev = mutableListOf<HashMap<String, String>>()
    private var listPairedDev = mutableListOf<HashMap<String, String>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_divice_list_view)

        // Get local bluetooth adapter
        val bluetoothManager =
            applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        //Register for broadcasts when device is discovered
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter)

        // Register for broadcasts when discovery has finished
        val filter1 = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(mReceiver, filter1)

        initViews()
        setListPairedDev()
    }

    private fun initViews() {
        progressBarScan = findViewById(R.id.scan_progress)
        listViewPairDev = findViewById(R.id.paired_devices)
        listViewsNewDew = findViewById(R.id.new_devices)
        buttonScan = findViewById(R.id.scan_button)
        textViewNewDev = findViewById(R.id.text_new_dev)
        textViewPairDev = findViewById(R.id.text_paired_dev)
        listAdapterNewDev = adapterDev(listNewDev)
        listAdapterPairDev = adapterDev(listPairedDev)
        listViewPairDev.adapter = listAdapterPairDev
        listViewsNewDew.adapter = listAdapterNewDev

        //click button scan
        buttonScan.setOnClickListener {
            listNewDev.clear()

            //Permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        baseContext,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                        PERMISSION_BLUETOOTH_SCAN
                    )
                } else {
                    // permission is granted
                    textViewNewDev.visibility = View.VISIBLE
                    doDiscovery()
                }
            } else {
                if (ContextCompat.checkSelfPermission(
                        baseContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_BLUETOOTH_CODE1
                    )
                } else {
                    // permission is granted and check next
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                        if (ContextCompat.checkSelfPermission(
                                baseContext,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                PERMISSION_BLUETOOTH_CODE2
                            )
                        } else {
                            // permission is granted
                            textViewNewDev.visibility = View.VISIBLE
                            doDiscovery()
                        }
                    } else {
                        // version < Q
                        textViewNewDev.visibility = View.VISIBLE
                        doDiscovery()
                    }
                }
            }
        }

        listViewsNewDew.setOnItemClickListener { adapterView, view, i, l ->
            itemClick(i, listNewDev)
        }

        listViewPairDev.setOnItemClickListener { adapterView, view, i, l ->
            itemClick(i, listPairedDev)
        }
    }

    // click to item listView
    private fun itemClick(i: Int, list: MutableList<HashMap<String, String>>) {
        val address = list[i]["address"]
        val name = list[i]["name"]
        if (address != null && address.isNotEmpty()) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("addressDeviceList", address)
            intent.putExtra("nameDeviceList", name)
            startActivity(intent)
        }
    }

    //initializer SimpleAdapter devices
    private fun adapterDev(list: MutableList<HashMap<String, String>>): SimpleAdapter {
        return SimpleAdapter(
            applicationContext,
            list,
            R.layout.item_device_blue,
            arrayOf("name", "address"),
            intArrayOf(R.id.name_dev, R.id.address_dev)
        )
    }

    // discovery
    private fun doDiscovery() {
        Log.d("DeviceList", "doDiscovery()")
        // Indicate scanning in the title
        // If we're already discovering, stop it
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        // Request discover from BluetoothAdapter
        bluetoothAdapter.startDiscovery()
        progressBarScan.visibility = View.VISIBLE
        listViewsNewDew.visibility = View.VISIBLE
        Log.d("doDiscovery", bluetoothAdapter.startDiscovery().toString())
        Log.d("doDiscovery", bluetoothAdapter.isDiscovering.toString())
    }

    // init paired device
    private fun setListPairedDev() {
        // Get a set of currently paired devices
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.isNotEmpty()) {
            textViewPairDev.visibility = View.VISIBLE
            for (device in pairedDevices) {
                listPairedDev.add(HashMap<String, String>().apply {
                    put("name", device.name)
                    put("address", device.address)
                })
            }
        } else {
            listViewPairDev.visibility = View.GONE
            listPairedDev.add(HashMap<String, String>().apply {
                put("name", "Не найдено")
                put("address", "")
            })
        }
    }

    // BroadcastReceiver
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // When discovery finds a device
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    // If it's already paired, skip it, because it's been listed already
                    if (device != null && device.bondState != BluetoothDevice.BOND_BONDED) {
                        var check = false
                        for (NewDev in listNewDev) {
                            if (NewDev["address"].equals(device.address)) {
                                check = true
                            }
                        }
                        if (!check) {
                            listNewDev.add(HashMap<String, String>().apply {
                                put("name", device.name)
                                put("address", device.address)
                            })
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("Bluetooth", "Discovery_Finished")
                    progressBarScan.visibility = View.GONE
                    // add setTitle string
                    if (listNewDev.size < 1) {
                        listNewDev.add(HashMap<String, String>().apply {
                            put("name", "Не найдено")
                            put("address", "")
                        })
                    }
                }
            }
        }
    }

    //Permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_BLUETOOTH_SCAN -> {
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showSnackBar("разрешение получено")
                    textViewNewDev.visibility = View.VISIBLE
                    doDiscovery()
                } else {
                    showSnackBar("разрешение не получено")
                }
            }
            PERMISSION_BLUETOOTH_CODE1 -> {
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showSnackBar("разрешение получено")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                        if (ContextCompat.checkSelfPermission(
                                baseContext,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                PERMISSION_BLUETOOTH_CODE2
                            )
                        } else {
                            // permission is granted
                            textViewNewDev.visibility = View.VISIBLE
                            doDiscovery()
                        }
                    } else {
                        // version < Q
                        textViewNewDev.visibility = View.VISIBLE
                        doDiscovery()
                    }
                } else {
                    showSnackBar("разрешение не получено")
                }
            }
            PERMISSION_BLUETOOTH_CODE2 -> {
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showSnackBar("разрешение получено")
                    Log.d("permission", "")
                    textViewNewDev.visibility = View.VISIBLE
                    doDiscovery()
                } else {
                    showSnackBar("разрешение не получено")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure we're not doing discovery anymore
        bluetoothAdapter.cancelDiscovery()

        // Unregister broadcast listeners
        unregisterReceiver(mReceiver)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("cancel", "is canceled")
        startActivity(intent)
    }

    fun showSnackBar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .show()
    }
}

