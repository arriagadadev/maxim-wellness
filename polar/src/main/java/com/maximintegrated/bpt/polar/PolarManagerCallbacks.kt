package com.maximintegrated.bpt.polar

import no.nordicsemi.android.ble.BleManagerCallbacks
import no.nordicsemi.android.ble.common.profile.battery.BatteryLevelCallback
import no.nordicsemi.android.ble.common.profile.hr.HeartRateMeasurementCallback

interface PolarManagerCallbacks : BleManagerCallbacks, HeartRateMeasurementCallback,
    BatteryLevelCallback