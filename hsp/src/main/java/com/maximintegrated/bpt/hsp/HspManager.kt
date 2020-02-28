package com.maximintegrated.bpt.hsp

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.maximintegrated.bpt.hsp.protocol.HspCommand
import com.maximintegrated.bpt.hsp.protocol.HspResponse
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.MtuRequest
import no.nordicsemi.android.ble.data.Data
import timber.log.Timber
import java.util.*

class HspManager(context: Context) : BleManager<HspManagerCallbacks>(context) {
    companion object {
        private val UUID_SERVICE = UUID.fromString("00001523-1212-efde-1523-785feabcd123")
        private val UUID_COMMAND_CHARACTERISTIC =
            UUID.fromString("00001027-1212-efde-1523-785feabcd123")
        private val UUID_RESPONSE_CHARACTERISTIC =
            UUID.fromString("00001011-1212-efde-1523-785feabcd123")

        const val COMMAND_SEPARATOR = '\n'
    }

    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var responseCharacteristic: BluetoothGattCharacteristic? = null

    private val responseDataCallback = object : HspResponseDataCallback() {
        override fun onCommandResponseReceived(
            device: BluetoothDevice,
            commandResponse: HspResponse<*>
        ) {
            mCallbacks.onCommandResponseReceived(device, commandResponse)
        }

        override fun onStreamDataReceived(device: BluetoothDevice, packet: ByteArray) {
            mCallbacks.onStreamDataReceived(device, packet)
        }
    }


    private val gattCallback = object : BleManagerGattCallback() {
        override fun onDeviceDisconnected() {
            commandCharacteristic = null
            responseCharacteristic = null
        }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(UUID_SERVICE)
            service?.let {
                commandCharacteristic = it.getCharacteristic(UUID_COMMAND_CHARACTERISTIC)
                responseCharacteristic = it.getCharacteristic(UUID_RESPONSE_CHARACTERISTIC)
            }

            return commandCharacteristic.hasWriteProperty && responseCharacteristic.hasNotifyProperty
        }

        override fun initialize() {
            super.initialize()
            enableResponseCharacteristicNotifications()

        }
    }

    override fun getGattCallback() = gattCallback

    fun enableResponseCharacteristicNotifications() {
        if (isConnected) {
            setNotificationCallback(responseCharacteristic)
                .merge(HspResponseDataMerger())
                .with(responseDataCallback)
            enableNotifications(responseCharacteristic)
                .done { device ->
                    Timber.i(
                        "Enabled response notifications (Device: %s)",
                        device
                    )
                }
                .fail { device, status ->
                    Timber.e(
                        "Failed to enable response notifications (Device: %s, Status: %d)",
                        device,
                        status
                    )
                }
                .enqueue()
        }
    }

    public override fun requestMtu(mtu: Int): MtuRequest {
        return super.requestMtu(mtu)
    }

    fun disableResponseCharacteristicNotifications() {
        if (isConnected) {
            disableIndications(responseCharacteristic)
                .done { device ->
                    Timber.i(
                        "Disabled response notifications (Device: %s)",
                        device
                    )
                }
                .enqueue()
        }
    }

    fun sendCommand(command: HspCommand) {
        if (isConnected) {
            val commandStr = command.toText()
            val paddedCommand = if (commandStr.endsWith(COMMAND_SEPARATOR)) {
                commandStr
            } else {
                commandStr + COMMAND_SEPARATOR
            }

            writeCharacteristic(commandCharacteristic, Data.from(paddedCommand))
                .split(HspCommandDataSplitter())
                .enqueue()
        }
    }
}