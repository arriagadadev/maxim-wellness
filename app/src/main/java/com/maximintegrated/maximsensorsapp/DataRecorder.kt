package com.maximintegrated.maximsensorsapp

import android.os.Environment
import android.util.Log
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DataRecorder(type: String) {

    companion object{
        val OUTPUT_DIRECTORY = File(Environment.getExternalStorageDirectory(), "MaximSensorsApp")
        private val TIMESTAMP_FORMAT = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        private val CSV_HEADER_HSP = arrayOf(
            "sample_count","sample_time","green", "green2", "ir", "red",
            "acceleration_x", "acceleration_y", "acceleration_z","op_mode",
            "hr", "hr_confidence", "rr", "rr_confidence", "activity", "r", "spo2_confidence", "spo2",
            "spo2_percentage_complete", "spo2_low_snr", "spo2_motion", "spo2_low_pi",
            "spo2_unreliable_r", "spo2_state", "scd_state", "walking_steps", "running_steps",
            "calorie", "cadence", "timestamp"
        )
    }
    private val csvWriter: CsvWriter
    private val timestamp = TIMESTAMP_FORMAT.format(Date())

    init {
        csvWriter = CsvWriter.open(
            getCsvFilePath(type),
            CSV_HEADER_HSP
        )
    }

    private fun getCsvFilePath(type: String) =
        File(OUTPUT_DIRECTORY, "MaximSensorsApp_${timestamp}_$type.csv").absolutePath


    fun record(data: HspStreamData) {
        csvWriter.write(
            data.sampleCount,
            data.sampleTime,
            data.green,
            data.green2,
            data.ir,
            data.red,
            data.accelerationX,
            data.accelerationY,
            data.accelerationZ,
            data.operationMode,
            data.hr,
            data.hrConfidence,
            data.rr,
            data.rrConfidence,
            data.activity,
            data.r,
            data.wspo2Confidence,
            data.spo2,
            data.wspo2PercentageComplete,
            data.wspo2LowSnr,
            data.wspo2Motion,
            data.wspo2LowPi,
            data.wspo2UnreliableR,
            data.wspo2State,
            data.scdState,
            data.walkSteps,
            data.runSteps,
            data.kCal,
            data.cadence,
            data.currentTimeMillis
        )
    }

    fun close() {
        csvWriter.close()
    }
}