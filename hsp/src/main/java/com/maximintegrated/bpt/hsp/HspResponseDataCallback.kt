package com.maximintegrated.bpt.hsp

import android.bluetooth.BluetoothDevice
import com.maximintegrated.bpt.hsp.HspResponseDataMerger.Companion.COMMAND_RESPONSE_END_BYTE
import com.maximintegrated.bpt.hsp.HspResponseDataMerger.Companion.COMMAND_RESPONSE_PADDING_BYTE
import com.maximintegrated.bpt.hsp.protocol.HspResponse
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data

interface HspResponseCallback {
    fun onCommandResponseReceived(device: BluetoothDevice, commandResponse: HspResponse<*>)
    fun onStreamDataReceived(device: BluetoothDevice, data: HspStreamData)
}

abstract class HspResponseDataCallback : DataReceivedCallback, HspResponseCallback {
    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        val packet = data.value ?: return

        if (packet[0] == HspResponseDataMerger.STREAM_START_BYTE) {
            onStreamDataReceived(device, HspStreamData.fromPacket(packet))
        } else {
            onCommandResponseReceived(device, data.toCommandResponse())
        }
    }

    private fun Data.toText() = value?.let {
        String(it).trim(
            COMMAND_RESPONSE_PADDING_BYTE.toChar(),
            COMMAND_RESPONSE_END_BYTE.toChar(),
            '\r',
            ' '
        )
    } ?: ""

    private fun Data.toCommandResponse() = HspResponse.fromText(toText())
}