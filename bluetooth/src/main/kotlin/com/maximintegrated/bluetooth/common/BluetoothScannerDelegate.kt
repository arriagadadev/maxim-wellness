package com.maximintegrated.bluetooth.common

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.maximintegrated.bluetooth.R
import com.maximintegrated.bluetooth.devicelist.BluetoothDeviceListController
import com.maximintegrated.bluetooth.livedata.AdapterState
import com.maximintegrated.bluetooth.livedata.DiscoveryState
import com.maximintegrated.bluetooth.util.enableBluetooth
import com.maximintegrated.bluetooth.view.ErrorModel

class BluetoothScannerDelegate(
    lifecycleOwner: LifecycleOwner,
    viewModel: BaseBluetoothViewModel,
    private val deviceNamePrefix: String? = null
) {
    private val deviceListController = BluetoothDeviceListController()

    private val bluetoothOffErrorModel = ErrorModel(
        R.drawable.ic_bluetooth_off,
        R.string.bluetooth_off_error_message,
        R.string.bluetooth_off_error_action
    )

    private val locationPermissionErrorModel = ErrorModel(
        R.drawable.ic_location,
        R.string.bluetooth_location_permission_message,
        R.string.bluetooth_location_permission_action
    )

    private var isBluetoothOn = false
        set(value) {
            field = value
            showBluetoothOffErrorIfNeeded()
            scanStateChangeHandler?.invoke()
        }

    var isDiscovering
        get() = deviceListController.isDiscovering
        private set(value) {
            deviceListController.isDiscovering = value
            scanStateChangeHandler?.invoke()
        }

    val isShowingLocationPermissionMessage
        get() = deviceListController.errorModel == locationPermissionErrorModel

    val isScanStartVisible
        get() = !isShowingLocationPermissionMessage && isBluetoothOn && !isDiscovering

    val isScanStopVisible
        get() = !isShowingLocationPermissionMessage && isBluetoothOn && isDiscovering

    var deviceClickListener
        get() = deviceListController.deviceClickListener
        set(value) {
            deviceListController.deviceClickListener = value
        }

    val deviceListAdapter: RecyclerView.Adapter<*> get() = deviceListController.adapter

    var scanStateChangeHandler: (() -> Unit)? = null

    init {
        viewModel.adapterState
            .observe(lifecycleOwner, Observer {
                isBluetoothOn = (it == AdapterState.ON)
            })

        viewModel.discoveryState
            .observe(lifecycleOwner, Observer {
                isDiscovering = (it == DiscoveryState.DISCOVERING)
            })

        viewModel.pairedDevices
            .observe(lifecycleOwner, Observer {
                deviceListController.pairedDevices = if (deviceNamePrefix == null) {
                    it
                } else {
                    it.filter { device ->
                        val deviceName = device.name ?: ""
                        deviceName.startsWith(deviceNamePrefix, true)
                    }
                }
            })

        viewModel.availableDevices
            .observe(lifecycleOwner, Observer {
                deviceListController.availableDevices = if (deviceNamePrefix == null) {
                    it
                } else {
                    it.filter { device ->
                        val deviceName = device.name ?: ""
                        deviceName.startsWith(deviceNamePrefix, true)
                    }
                }
            })
    }

    fun showLocationPermissionMessage(actionClickListener: View.OnClickListener) {
        deviceListController.errorButtonClickListener = actionClickListener
        deviceListController.errorModel = locationPermissionErrorModel
        scanStateChangeHandler?.invoke()
    }

    fun hideLocationPermissionMessage() {
        deviceListController.errorButtonClickListener = null
        deviceListController.errorModel = null
        scanStateChangeHandler?.invoke()
        showBluetoothOffErrorIfNeeded()
    }

    private fun showBluetoothOffErrorIfNeeded() {
        // ignore if location permission message is shown
        if (isShowingLocationPermissionMessage) return

        if (isBluetoothOn) {
            deviceListController.errorButtonClickListener = null
            deviceListController.errorModel = null
        } else {
            deviceListController.errorButtonClickListener =
                View.OnClickListener { enableBluetooth() }
            deviceListController.errorModel = bluetoothOffErrorModel
        }
    }

}