package com.maximintegrated.bpt.hsp

import android.bluetooth.BluetoothGattCharacteristic

fun BluetoothGattCharacteristic?.hasProperty(property: Int): Boolean {
    return ((this != null) && (properties and property) > 0)
}

val BluetoothGattCharacteristic?.hasReadProperty
    get() = hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

val BluetoothGattCharacteristic?.hasWriteProperty
    get() = hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

val BluetoothGattCharacteristic?.hasNotifyProperty
    get() = hasProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

val BluetoothGattCharacteristic?.hasIndicateProperty
    get() = hasProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)
