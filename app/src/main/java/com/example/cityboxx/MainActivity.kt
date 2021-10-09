package com.example.cityboxx

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

const val CONNECTING_STATUS = 3
const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2
const val REQUEST_ENABLE_BT: Int = 24
const val PERMISSION_BLUETOOTH_CODE: Int = 43
const val PERMISSION_BLUETOOTH_CODE1: Int = 65
const val PERMISSION_BLUETOOTH_CODE2: Int = 94
const val PERMISSION_BLUETOOTH_SCAN: Int = 111


class MainActivity : AppCompatActivity() {
    private lateinit var temp: TextView
    private lateinit var hamid: TextView
    private lateinit var lightButton: Button
    private lateinit var ventButton: Button
    private lateinit var connectedButton: FloatingActionButton
    private var tempStr: String = "xx"
    private var hamidStr: String = "xx"
    private val ventBundle: Bundle = Bundle()
    private val lightBundle: Bundle = Bundle()
    private val ventSettMap = HashMap<String, String>()
    private val lightSettMap = HashMap<String, String>()
    private var ventSettList: ArrayList<String> = ArrayList<String>()
    private var lightSettList: ArrayList<String> = ArrayList<String>()
    private var connection: Boolean = false
    private var boxxBluetoothService: BoxxBluetoothService? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ventSettList.add(getString(R.string.interval_on))
        ventSettList.add(getString(R.string.interval_off))
        ventSettList.add(getString(R.string.temperature_on))
        ventSettList.add(getString(R.string.temperature_off))
        lightSettList.add(getString(R.string.interval_on))
        lightSettList.add(getString(R.string.temperature_off))

        initViews()

        val b = intent.extras
        if (b != null) {
            val nameDevice =
                b.getString("nameDeviceList")
            val addressDevice =
                b.getString("addressDeviceList")

            Log.d("DeviceName", nameDevice.toString())
            Log.d("DeviceAddress", addressDevice.toString())

            if (b.getString("cancel") != null) {
                showSnackBar("Device not selected")
            } else {
                if (nameDevice != null) {
                    val bluetoothManager =
                        applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val bluetoothAdapter = bluetoothManager.adapter
                    if (addressDevice != null) {
                        boxxBluetoothService =
                            BoxxBluetoothService(mHandler, addressDevice, bluetoothAdapter)
                        boxxBluetoothService!!.connect()
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (boxxBluetoothService != null) {
            boxxBluetoothService!!.cancelConnect()
        }
    }

    private fun initViews() {
        temp = findViewById(R.id.temp)
        hamid = findViewById(R.id.hamid)
        lightButton = findViewById(R.id.light)
        ventButton = findViewById(R.id.vent)
        connectedButton = findViewById(R.id.connectButton)
        temp.text = tempStr
        hamid.text = hamidStr

        //Button click
        val i = Intent(this, ActivitySettings::class.java)
        lightButton.setOnClickListener {
            setLightMap(lightSettList)
            initLightBundle(lightSettMap)
            i.putExtras(lightBundle)
            startActivity(i)
        }
        ventButton.setOnClickListener {
            setVentMap(ventSettList)
            initVentBundle(ventSettMap)
            i.putExtras(ventBundle)
            startActivity(i)
        }
        connectedButton.setOnClickListener {
            val bluetoothManager =
                applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            if (!connection) {
                if (bluetoothAdapter != null) {
                    if (!bluetoothAdapter.isEnabled) {
                        enableBtIntent()
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ContextCompat.checkSelfPermission(
                                    baseContext,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                )
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                Log.d("permission", "request perm")
                                ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                                    PERMISSION_BLUETOOTH_CODE
                                )
                            } else {
                                val intent = Intent(this, DeviceListView::class.java)
                                startActivity(intent)
                            }
                        } else {
                            val intent = Intent(this, DeviceListView::class.java)
                            startActivity(intent)
                        }
                    }
                } else {
                    showSnackBar("Bluetooth is not available ")
                }
            } else {
                if (boxxBluetoothService != null) {
                    boxxBluetoothService!!.cancelConnect()
                }
            }
        }
    }

    // Activity Result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == RESULT_OK) {
                    showSnackBar("Bluetooth on")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(
                                baseContext,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.d("permission", "request perm")
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                                PERMISSION_BLUETOOTH_CODE
                            )
                        } else {
                            val intent = Intent(this, DeviceListView::class.java)
                            startActivity(intent)
                        }
                    } else {
                        val intent = Intent(this, DeviceListView::class.java)
                        startActivity(intent)
                    }
                } else {
                    showSnackBar("Bluetooth on is canceled")
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
            PERMISSION_BLUETOOTH_CODE -> {
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(this, DeviceListView::class.java)
                    startActivity(intent)
                } else {
                    Log.d("Permission not result", "разрешение не получено")
                    showSnackBar("разрешение не получено")
                }
            }
            PERMISSION_BLUETOOTH_CODE1 -> {
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showSnackBar("разрешение получено")
                    enableBtIntent()
                } else {
                    Log.d("Permission not result", "разрешение не получено")
                    showSnackBar("разрешение не получено")
                }
            }
        }
    }

    // Bundle for ventilation
    private fun initVentBundle(mapSettingsVent: HashMap<String, String>) {
        ventBundle.putString("nameSett", getString(R.string.name_vent))
        ventBundle.putParcelableArrayList("SettList", mapToListBundle(mapSettingsVent))
    }

    // Bundle for
    private fun initLightBundle(mapSettLight: HashMap<String, String>) {
        lightBundle.putString("nameSett", getString(R.string.name_lamp))
        lightBundle.putParcelableArrayList("SettList", mapToListBundle(mapSettLight))
    }

    // convert list settings to map
    private fun mapToListBundle(map: HashMap<String, String>): java.util.ArrayList<out Parcelable> {
        val list: ArrayList<Bundle> = ArrayList<Bundle>()
        for ((key, value) in map) {
            list.add(Bundle().apply {
                putString(ActivitySettings().keySett, key)
                putString(ActivitySettings().value, value)
            })
        }
        return list
    }

    // update vent map
    private fun setVentMap(ventList: ArrayList<String>) {
        for (s in ventList) {
            ventSettMap[s] = "120" + "хх"
        }
    }

    // update light map
    private fun setLightMap(lightList: ArrayList<String>) {
        for (s in lightList) {
            lightSettMap[s] = "40"
        }
    }

    // SnackBarShow
    fun showSnackBar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .show()
    }

    //Request for bluetooth on
    private fun enableBtIntent() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    //handler for bluetooth message
    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                CONNECTING_STATUS -> when (msg.arg1) {
                    1 -> {
                        showSnackBar("Device connect")
                        connection = true
                        connectedButton.imageTintList =
                            ColorStateList.valueOf(resources.getColor(R.color.white))
                        connectedButton.imageTintList =
                            ColorStateList.valueOf(resources.getColor(R.color.lGreen_icon))
                    }
                    -1 -> {
                        showSnackBar("Device no connect")
                        connection = false
                    }
                    4 -> {
                        showSnackBar("Device not available")
                        connection = false
                    }
                    3 -> {
                        showSnackBar("Device disconnected")
                        connection = false
                        connectedButton.imageTintList =
                            ColorStateList.valueOf(resources.getColor(R.color.white))
                        connectedButton.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.purpule_900))
                    }
                }
                MESSAGE_READ -> {
                    val arduinoMsg: String = msg.obj.toString()// Read message from Arduino
                    Log.d("Handler message", arduinoMsg)
                    if (arduinoMsg.indexOf("temp") > -1) {
                        tempStr = arduinoMsg.substring(arduinoMsg.indexOf("::") + 2)
                        val hex = Integer.parseInt("00B0", 16)
                        val str = Char(hex)
                        temp.text = "t $tempStr C$str"
                    } else if (arduinoMsg.indexOf("hamid") > -1) {
                        hamidStr = arduinoMsg.substring(arduinoMsg.indexOf("::") + 2)
                        hamid.text = "φ $hamidStr %"
                    }
                }
            }
        }
    }
}
