package com.maximintegrated.maximsensorsapp

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bluetooth.devicelist.OnBluetoothDeviceClickListener
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.bpt.hsp.protocol.HspCommand
import com.maximintegrated.maximsensorsapp.exts.getCurrentFragment
import com.maximintegrated.maximsensorsapp.exts.replaceFragment
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber.d

class MainActivity : AppCompatActivity(), OnBluetoothDeviceClickListener {
    private lateinit var bluetoothDevice: BluetoothDevice

    private lateinit var hspViewModel: HspViewModel

    companion object {
        private const val KEY_BLUETOOTH_DEVICE = "com.maximintegrated.hsp.BLUETOOTH_DEVICE"
        const val REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 3

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        appVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
        bluetoothDevice = intent.getParcelableExtra(KEY_BLUETOOTH_DEVICE)

        hspViewModel = ViewModelProviders.of(this).get(HspViewModel::class.java)
        hspViewModel.connect(bluetoothDevice)

        hspViewModel.isDeviceSupported
            .observe(this) {
                hspViewModel.sendCommand(HspCommand.fromText("get_device_info"))
            }

        hspViewModel.commandResponse
            .observe(this) { response ->
                if (response.command.name == HspCommand.COMMAND_GET_DEVICE_INFO && response.parameters.size > 1) {
                    serverVersion.text =
                        getString(R.string.server_version, response.parameters[0].value)
                    hubVersion.text = getString(R.string.hub_version, response.parameters[1].value)
                }
            }

        d("Connected bluetooth device $bluetoothDevice")
        showMenuItems(arrayListOf("sensors"), arrayListOf("algoos"))
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_READ_EXTERNAL_STORAGE_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_READ_EXTERNAL_STORAGE_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                } else {
                    // permission denied, boo!
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
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
        val fragment = (getCurrentFragment() as? IOnBackPressed)

        if (fragment != null) {
            fragment.onBackPressed()?.let {
                if (it) {
                    showStopMonitoringDialog()
                } else {
                    super.onBackPressed()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun showStopMonitoringDialog() {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
        alertDialog.setTitle("Stop Monitoring")
        alertDialog.setMessage("Are you sure you want to stop monitoring ?")
            .setPositiveButton("OK") { dialog, which ->
                (getCurrentFragment() as? IOnBackPressed)?.onStopMonitoring()
                dialog.dismiss()
                super.onBackPressed()
            }.setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    override fun onBluetoothDeviceClicked(bluetoothDevice: BluetoothDevice) {
        val fragment = (getCurrentFragment() as? OnBluetoothDeviceClickListener)
        fragment?.onBluetoothDeviceClicked(bluetoothDevice)
    }
}
