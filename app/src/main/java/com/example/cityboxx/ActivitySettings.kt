package com.example.cityboxx

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import java.util.ArrayList

class ActivitySettings() : AppCompatActivity() {
    private lateinit var nameSett: String
    var keySett: String = "keySett"
    var value: String = "value"
    private var settingsList = mutableListOf<HashMap<String, String>>()
    private lateinit var nameTextView: TextView
    private lateinit var settings: ListView
    private lateinit var settingsAdapter: SimpleAdapter
    private var def: String = "default"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_settings)
        val b = intent.extras
        if (b != null) {
            nameSett = b.getString("nameSett", def)
            val bb = b.getParcelableArrayList<Bundle>("SettList")
            if (bb != null) {
                for (a in bb) {
                    settingsList.add(HashMap<String, String>().apply {
                        put(keySett, a.getString(keySett, def))
                        put(value, a.getString(value, "ff"))
                    })
                }
            }
        } else {
            nameSett = def
            settingsList.addAll(listOf(HashMap<String, String>().apply {
                put(keySett, def)
                put(value, "xx")
            }))
        }
        initViews()
        nameTextView.text = nameSett
    }

    fun initViews() {
        nameTextView = findViewById(R.id.name_group)
        settings = findViewById(R.id.list_settings)
        settingsAdapter = SimpleAdapter(
            applicationContext,
            settingsList,
            R.layout.item_settings,
            arrayOf(keySett, value),
            intArrayOf(R.id.name_setting, R.id.value_setting)
        )
        settings.adapter = settingsAdapter
    }
}