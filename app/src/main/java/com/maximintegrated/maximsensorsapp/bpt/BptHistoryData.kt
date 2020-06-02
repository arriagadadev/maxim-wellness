package com.maximintegrated.maximsensorsapp.bpt

import com.maximintegrated.bpt.hsp.HspBptStreamData
import java.util.*
import java.util.concurrent.TimeUnit

data class BptHistoryData(
    val timestamp: Long,
    val isCalibration: Boolean = false,
    val sbp1: Int,
    val dbp1: Int,
    val hr: Int,
    val spo2: Int,
    val pulseFlag: Int,
    var sbp2: Int = 0,
    var dbp2: Int = 0,
    var sbp3: Int = 0,
    var dbp3: Int = 0
) {
    companion object {
        val CSV_HEADER_ARRAY = arrayOf(
            "Timestamp", "SysBp1", "DiaBp1", "HR", "SpO2", "Pulse Flag", "Calibration/Measurement", "SysBp2", "DiaBp2", "SysBp3", "DiaBp3"
        )
    }

    fun toText(): String {
        return "$timestamp,$sbp1,$dbp1,$hr,$spo2,$pulseFlag,${if (isCalibration) "Calibration" else "Measurement"},$sbp2,$dbp2,$sbp3,$dbp3\n"
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

fun BptHistoryData.isExpired(): Boolean{
    val today = Date()
    val timeDiffMs = today.time - timestamp
    val timeDiffDays = TimeUnit.MILLISECONDS.toDays(timeDiffMs)
    return timeDiffDays > 7
}