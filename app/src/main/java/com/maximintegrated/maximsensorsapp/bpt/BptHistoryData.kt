package com.maximintegrated.maximsensorsapp.bpt

import com.maximintegrated.bpt.hsp.HspBptStreamData

data class BptHistoryData(
    val timestamp: Long,
    val isCalibration: Boolean = false,
    val sbp: Int,
    val dbp: Int,
    val hr: Int,
    val spo2: Int,
    val pulseFlag: Int
) {
    companion object {
        val CSV_HEADER_ARRAY = arrayOf(
            "Timestamp", "SysBp", "DiaBp", "HR", "SpO2", "Pulse Flag", "Calibration/Measurement"
        )
    }

    fun toText(): String {
        return "$timestamp,$sbp,$dbp,$hr,$spo2,$pulseFlag,${if (isCalibration) "Calibration" else "Measurement"}\n"
    }
}

fun HspBptStreamData.toHistoryModel(isCalibration: Boolean): BptHistoryData {
    return BptHistoryData(
        this.timestamp,
        isCalibration,
        this.sbp,
        this.dbp,
        this.hr,
        this.spo2,
        this.pulseFlag
    )
}