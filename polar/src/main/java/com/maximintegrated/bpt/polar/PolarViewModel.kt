package com.maximintegrated.bpt.polar

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import timber.log.Timber

class PolarViewModel(application: Application) : AndroidViewModel(application),
    PolarManagerCallbacks {

    var bluetoothDevice: BluetoothDevice? = null
        private set

    private val connectionStateMutable = MutableLiveData<Pair<BluetoothDevice, Int>>()
    val connectionState: LiveData<Pair<BluetoothDevice, Int>> get() = connectionStateMutable

    private val batteryLevelMutable = MutableLiveData<Int>()
    val batteryLevel: LiveData<Int> get() = batteryLevelMutable

    private val heartRateMeasurementMutable =
        MutableLiveData<HeartRateMeasurement>()
    val heartRateMeasurement: LiveData<HeartRateMeasurement>
        get() = heartRateMeasurementMutable

    private val isDeviceSupportedMutable = MutableLiveData<Boolean>()
    val isDeviceSupported: LiveData<Boolean> get() = isDeviceSupportedMutable

    private val polarManager = PolarManager(application)

    val isConnected: Boolean
        get() = connectionState.value?.second == BluetoothAdapter.STATE_CONNECTED


    init {
        polarManager.setGattCallbacks(this)
    }

    override fun onCleared() {
        if (polarManager.isConnected) {
            disconnect()
        }
    }

    fun connect(device: BluetoothDevice) {
        if (bluetoothDevice == null) {
            bluetoothDevice = device
            polarManager
                .connect(device)
                .enqueue()
        }
    }

    fun disconnect() {
        bluetoothDevice = null
        polarManager
            .disconnect()
            .enqueue()
    }

    /**
     * Reconnect to previously connected device
     */
    fun reconnect() {
        polarManager
            .disconnect()
            .enqueue()

        bluetoothDevice?.let {
            polarManager
                .connect(it)
                .retry(3, 100)
                .useAutoConnect(false)
                .enqueue()
        }
    }

    fun readBatteryLevel() {
        polarManager.readBatteryLevelCharacteristic()
    }

    fun enableBatteryLevelNotifications() {
        polarManager.enableBatteryLevelCharacteristicNotifications()
    }

    fun disableBatteryLevelNotifications() {
        polarManager.disableBatteryLevelCharacteristicNotifications()
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
        connectionStateMutable.value = device to BluetoothProfile.STATE_DISCONNECTING
        Timber.i("Disconnecting from device %s", device)
    }

    override fun onDeviceDisconnected(device: BluetoothDevice) {
        connectionStateMutable.value = device to BluetoothProfile.STATE_DISCONNECTED
        Timber.i("Disconnected from device %s", device)
    }

    override fun onDeviceConnecting(device: BluetoothDevice) {
        connectionStateMutable.value = device to BluetoothProfile.STATE_CONNECTING
        Timber.i("Connecting to device %s", device)
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        connectionStateMutable.value = device to BluetoothProfile.STATE_CONNECTED
        Timber.i("Connected to device %s", device)
    }

    override fun onDeviceNotSupported(device: BluetoothDevice) {
        isDeviceSupportedMutable.value = false
        Timber.e("Unsupported device %s", device)
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        isDeviceSupportedMutable.value = true
        Timber.i("Device %s is ready", device)
    }

    override fun onBondingRequired(device: BluetoothDevice) {
        Timber.d("Bonding to device %s is required", device)
    }

    override fun onBondingFailed(device: BluetoothDevice) {
        Timber.e("Bonding to device %s failed", device)
    }

    override fun onBonded(device: BluetoothDevice) {
        Timber.d("Bonded to device %s", device)
    }

    override fun onServicesDiscovered(device: BluetoothDevice, optionalServicesFound: Boolean) {
        Timber.i("Services of device %s discovered", device)
    }

    override fun onLinkLossOccurred(device: BluetoothDevice) {
        // should be handled by onDeviceDisconnected
        Timber.e("Link to device %s is lost", device)
    }

    override fun onError(device: BluetoothDevice, message: String, errorCode: Int) {
        Timber.e(
            "An error occurred (Device: %s, ErrorCode: %d, Message: %s)",
            device,
            errorCode,
            message
        )
    }

    override fun onHeartRateMeasurementReceived(
        device: BluetoothDevice,
        heartRate: Int,
        contactDetected: Boolean?,
        energyExpanded: Int?,
        rrIntervals: MutableList<Int>?
    ) {
        heartRateMeasurementMutable.value =
            HeartRateMeasurement(heartRate, contactDetected, energyExpanded, rrIntervals)
    }

    override fun onBatteryLevelChanged(device: BluetoothDevice, batteryLevel: Int) {
        batteryLevelMutable.value = batteryLevel
    }
}