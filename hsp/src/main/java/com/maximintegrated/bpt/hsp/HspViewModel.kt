package com.maximintegrated.bpt.hsp

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.maximintegrated.bpt.hsp.protocol.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class HspViewModel(application: Application) : AndroidViewModel(application),
    HspManagerCallbacks {

    enum class DeviceModel{
        UNDEFINED,
        ME11A,
        ME11B,
        ME11C,
        ME11D
    }

    enum class StreamType{
        PPG,
        ECG,
        TEMP,
        BPT
    }

    var bluetoothDevice: BluetoothDevice? = null
        private set

    private val connectionStateMutable = MutableLiveData<Pair<BluetoothDevice, Int>>()
    val connectionState: LiveData<Pair<BluetoothDevice, Int>> get() = connectionStateMutable

    private val commandResponseMutable = MutableLiveData<HspResponse<*>>()
    val commandResponse: LiveData<HspResponse<*>> get() = commandResponseMutable

    private val streamDataMutable = MutableLiveData<HspStreamData>()
    val streamData: LiveData<HspStreamData> get() = streamDataMutable

    private val tempStreamDataMutable = MutableLiveData<HspTempStreamData>()
    val tempStreamData: LiveData<HspTempStreamData> get() = tempStreamDataMutable

    private val ecgStreamDataMutable = MutableLiveData<Array<HspEcgStreamData>>()
    val ecgStreamData: LiveData<Array<HspEcgStreamData>> get() = ecgStreamDataMutable

    private val bptStreamDataMutable = MutableLiveData<HspBptStreamData>()
    val bptStreamData: LiveData<HspBptStreamData> get() = bptStreamDataMutable

    private val isDeviceSupportedMutable = MutableLiveData<Boolean>()
    val isDeviceSupported: LiveData<Boolean> get() = isDeviceSupportedMutable

    private val hspManager = HspManager(application)

    private val sharedPreferences =
        application.getSharedPreferences("ScdSettings", Context.MODE_PRIVATE)

    val isConnected: Boolean
        get() = connectionState.value?.second == BluetoothAdapter.STATE_CONNECTED

    var deviceModel = DeviceModel.UNDEFINED

    var streamType = StreamType.PPG

    init {
        hspManager.setGattCallbacks(this)
    }

    override fun onCleared() {
        if (hspManager.isConnected) {
            stopStreaming()
            disconnect()
        }
    }

    fun connect(device: BluetoothDevice) {
        if (bluetoothDevice == null) {
            bluetoothDevice = device
            hspManager
                .connect(device).enqueue()
            hspManager.requestMtu(100).enqueue()
        }
    }

    fun disconnect() {
        bluetoothDevice = null
        hspManager
            .disconnect()
            .enqueue()
    }

    /**
     * Reconnect to previously connected device
     */
    fun reconnect() {
        hspManager
            .disconnect()
            .enqueue()

        bluetoothDevice?.let {
            hspManager
                .connect(it)
                .retry(3, 100)
                .useAutoConnect(false)
                .enqueue()
        }
    }

    fun sendCommand(command: HspCommand) {
        hspManager.sendCommand(command)
    }

    fun startStreaming() {

        val isScdEnabled = sharedPreferences.getBoolean("scdEnabled", false)

        if (isScdEnabled) {
            sendCommand(SetConfigurationCommand("scdpowersaving", " ", "1 10 5"))
        }

        sendCommand(SetConfigurationCommand("stream", "bin"))
        sendCommand(ReadCommand("ppg", 9))
    }

    fun startTempStreaming(sampleIntervalInMillis: Int) {
        sendCommand(SetConfigurationCommand("stream", "bin"))
        sendCommand(SetConfigurationCommand("temp", "sr", sampleIntervalInMillis.toString()))
        sendCommand(ReadCommand("temp", 0))
    }

    fun startEcgStreaming() {
        sendCommand(SetConfigurationCommand("stream", "bin"))
        sendCommand(ReadCommand("ecg", 2))
    }

    fun startBptCalibrationStreaming() {
        sendCommand(ReadCommand("bpt", 0))
    }

    fun startBptEstimationStreaming() {
        sendCommand(ReadCommand("bpt", 1))
    }

    fun setBptDateTime(){
        val timestamp = SimpleDateFormat("yyMMdd hhmmss").format(Date())
        sendCommand(SetConfigurationCommand("bpt", "date_time", timestamp))
    }

    fun setSysBp(sbp: Int){
        setSysBp(sbp, sbp, sbp)
    }

    fun setSysBp(sbp0: Int, sbp1: Int, sbp2: Int){
        sendCommand(SetConfigurationCommand("bpt","sys_bp", "$sbp0 $sbp1 $sbp2"))
    }

    fun setDiaBp(dbp: Int){
        setDiaBp(dbp, dbp, dbp)
    }

    fun setDiaBp(dbp0: Int, dbp1: Int, dbp2: Int){
        sendCommand(SetConfigurationCommand("bpt","dia_bp", "$dbp0 $dbp1 $dbp2"))
    }

    fun setSpO2Coefficients(a: Float, b: Float, c: Float){
        val constant = 1e5f
        val strA = "%08X".format((a * constant).toInt())
        val strB = "%08X".format((b * constant).toInt())
        val strC = "%08X".format((c * constant).toInt())
        sendCommand(SetConfigurationCommand("bpt","spo2_coefs", "$strA $strB $strC"))
    }

    fun setCalibrationResult(calibrationResultsInHexString: String){
        sendCommand(SetConfigurationCommand("bpt", "cal_result", calibrationResultsInHexString))
    }

    fun getBptCalibrationResults(){
        sendCommand(GetConfigurationCommand("bpt", "cal_result"))
    }

    fun stopStreaming() {
        sendCommand(StopCommand())
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

    override fun onCommandResponseReceived(
        device: BluetoothDevice,
        commandResponse: HspResponse<*>
    ) {
        commandResponseMutable.value = commandResponse
    }

    override fun onStreamDataReceived(device: BluetoothDevice, packet: ByteArray) {
        when(streamType){
            StreamType.PPG -> streamDataMutable.value = HspStreamData.fromPacket(packet)
            StreamType.ECG -> ecgStreamDataMutable.value = HspEcgStreamData.fromPacket(packet)
            StreamType.TEMP -> tempStreamDataMutable.value = HspTempStreamData.fromPacket(packet)
            StreamType.BPT -> bptStreamDataMutable.value = HspBptStreamData.fromPacket(packet)
        }
    }
}