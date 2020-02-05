package com.maximintegrated.maximsensorsapp

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.bluetooth.devicelist.OnBluetoothDeviceClickListener
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.bpt.hsp.protocol.HspCommand
import com.maximintegrated.bpt.hsp.protocol.Status
import com.maximintegrated.maximsensorsapp.exts.getCurrentFragment
import com.maximintegrated.maximsensorsapp.exts.replaceFragment
import com.maximintegrated.maximsensorsapp.service.ForegroundService
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber.d

class MainActivity : AppCompatActivity(), OnBluetoothDeviceClickListener {
    private lateinit var bluetoothDevice: BluetoothDevice

    private lateinit var hspViewModel: HspViewModel

    private var param1: ByteArray? = null

    private var param2: ByteArray? = null

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
                    val version = response.parameters[1].value
                    hspViewModel.deviceModel = when (version.split(".")[0].toIntOrNull() ?: 0) {
                        in 10..19 -> HspViewModel.DeviceModel.ME11A
                        in 20..29 -> HspViewModel.DeviceModel.ME11B
                        in 30..39 -> HspViewModel.DeviceModel.ME11C
                        in 40..49 -> HspViewModel.DeviceModel.ME11D
                        else -> HspViewModel.DeviceModel.UNDEFINED
                    }
                    showMenuItems(arrayListOf("sensors"), arrayListOf("algoos"))
                    serverVersion.text =
                        getString(R.string.server_version, response.parameters[0].value)
                    hubVersion.text = getString(R.string.hub_version, version)
                    hspViewModel.sendCommand(HspCommand.fromText("get_cfg sh_dhparams"))
                } else if (response.command.name == HspCommand.COMMAND_GET_CFG && response.parameters.isNotEmpty()) {
                    if (response.command.parameters[0].value == "sh_dhparams") {
                        val auth =
                            MaximAlgorithms.getAuthInitials(response.parameters[0].valueAsByteArray)
                        hspViewModel.sendCommand(HspCommand.fromText("set_cfg sh_dhlpublic ${auth.toHexString()}"))
                    } else if (response.command.parameters[0].value == "sh_dhrpublic") {
                        param1 = response.parameters[0].valueAsByteArray
                        hspViewModel.sendCommand(HspCommand.fromText("get_cfg sh_auth"))
                    } else if (response.command.parameters[0].value == "sh_auth") {
                        param2 = response.parameters[0].valueAsByteArray
                        if (param1 != null && param2 != null) {
                            MaximAlgorithms.authenticate(param1, param2)
                        }
                    }
                    if (response.status != Status.SUCCESS) {
                        showAuthenticationFailMessage()
                    }
                } else if (response.command.name == HspCommand.COMMAND_SET_CFG) {
                    if (response.command.parameters[0].value == "sh_dhlpublic") {
                        hspViewModel.sendCommand(HspCommand.fromText("get_cfg sh_dhrpublic"))
                    }
                }
            }

        d("Connected bluetooth device $bluetoothDevice")
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

    private fun showAuthenticationFailMessage() {
        Toast.makeText(this, "Authentication failed!", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        val fragment = (getCurrentFragment() as? IOnBackPressed)

        if (fragment != null) {
            fragment.onBackPressed().let {
                if (it) {
                    showStopMonitoringDialog()
                } else {
                    super.onBackPressed()
                }
            }
        } else {
            if (getCurrentFragment() == null || getCurrentFragment() as? MainFragment != null) {
                startActivity(
                    Intent(this, ScannerActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
            } else {
                super.onBackPressed()
            }
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

    override fun onStop() {
        super.onStop()
        val intent = Intent(this, ForegroundService::class.java)
        stopService(intent)
    }
}
