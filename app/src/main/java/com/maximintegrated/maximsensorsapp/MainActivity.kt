package com.maximintegrated.maximsensorsapp

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.exts.getCurrentFragment
import com.maximintegrated.maximsensorsapp.exts.replaceFragment
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber.d

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothDevice: BluetoothDevice

    private lateinit var hspViewModel: HspViewModel

    companion object {
        private const val KEY_BLUETOOTH_DEVICE = "com.maximintegrated.hsp.BLUETOOTH_DEVICE"

        fun start(context: Context, bluetoothDevice: BluetoothDevice) {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(KEY_BLUETOOTH_DEVICE, bluetoothDevice)
                })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appVersion.text = "3.3.3"
        bluetoothDevice = intent.getParcelableExtra(KEY_BLUETOOTH_DEVICE)

        hspViewModel = ViewModelProviders.of(this).get(HspViewModel::class.java)
        hspViewModel.connect(bluetoothDevice)

        d("Connected bluetooth device $bluetoothDevice")
        showMenuItems(arrayListOf("sensors"), arrayListOf("algoos"))
    }

    private fun showMenuItems(deviceSensors: List<String>, firmwareAlgorithms: List<String>) {
        progressBar.isVisible = false
        replaceFragment(
            MainFragment.newInstance(
                deviceSensors.toTypedArray(),
                firmwareAlgorithms.toTypedArray()
            )
        )
    }

    override fun onBackPressed() {

        when (getCurrentFragment()) {
            is MainFragment -> Log.d("Main", "Main")
            else -> hspViewModel.stopStreaming()
        }

        super.onBackPressed()
    }
}
