package com.maximintegrated.maximsensorsapp

import android.os.Environment
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.polar.HeartRateMeasurement
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DataRecorder(var type: String) {

    companion object {
        val OUTPUT_DIRECTORY = File(Environment.getExternalStorageDirectory(), "MaximSensorsApp")
        val TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.US)
        private val CSV_HEADER_HSP = arrayOf(
            "sample_count",
            "sample_time",
            "green",
            "green2",
            "ir",
            "red",
            "acceleration_x",
            "acceleration_y",
            "acceleration_z",
            "op_mode",
            "hr",
            "hr_confidence",
            "rr",
            "rr_confidence",
            "activity",
            "r",
            "spo2_confidence",
            "spo2",
            "spo2_percentage_complete",
            "spo2_low_snr",
            "spo2_motion",
            "spo2_low_pi",
            "spo2_unreliable_r",
            "spo2_state",
            "scd_state",
            "walking_steps",
            "running_steps",
            "calorie",
            "cadence",
            "timestamp",
            "timestamp_milis"
        )

        private val CSV_HEADER_HSP_1Hz = arrayOf(
            "timestamp",
            "hr"
        )

        private val CSV_HEADER_REFERENCE_DEVICE =
            arrayOf("timestamp", "heart_rate", "contact_detected")
    }

    private val csvWriter: CsvWriter
    private val csvWriter1Hz: CsvWriter
    private var count = 1

    private val referenceDevice: CsvWriter

    private val timestamp = TIMESTAMP_FORMAT.format(Date())

    private var oneHzFileIsFinished = false
    private var referenceFileIsFinished = false

    var dataRecorderListener: DataRecorderListener? = null

    init {
        csvWriter = CsvWriter.open(
            getCsvFilePath(type),
            CSV_HEADER_HSP
        )

        csvWriter1Hz = CsvWriter.open(
            getCsvFilePath1Hz(type),
            CSV_HEADER_HSP_1Hz
        )

        referenceDevice = CsvWriter.open(
            getCsvFilePathReferenceDevice(type),
            CSV_HEADER_REFERENCE_DEVICE
        )
    }

    private var oneHzListener = object : CsvWriter.Companion.CsvWriterListener {
        override fun onCompleted(isSuccessful: Boolean) {
            if(!isSuccessful) return
            oneHzFileIsFinished = true
            if (referenceFileIsFinished) {
                dataRecorderListener?.onFilesAreReadyForAlignment(
                    getCsvFilePathAligned(type),
                    csvWriter1Hz.filePath,
                    referenceDevice.filePath
                )
                oneHzFileIsFinished = false
                referenceFileIsFinished = false
            }
        }
    }

    private var referenceListener = object : CsvWriter.Companion.CsvWriterListener {
        override fun onCompleted(isSuccessful: Boolean) {
            if(!isSuccessful) return
            referenceFileIsFinished = true
            if (oneHzFileIsFinished) {
                dataRecorderListener?.onFilesAreReadyForAlignment(
                    getCsvFilePathAligned(type),
                    csvWriter1Hz.filePath,
                    referenceDevice.filePath
                )
                oneHzFileIsFinished = false
                referenceFileIsFinished = false
            }
        }
    }

    private fun getCsvFilePath(type: String) =
        File(OUTPUT_DIRECTORY, "/RAW/MaximSensorsApp_${timestamp}_$type.csv").absolutePath

    private fun getCsvFilePath1Hz(type: String) =
        File(OUTPUT_DIRECTORY, "/1Hz/MaximSensorsApp_${timestamp}_${type}_1Hz.csv").absolutePath

    private fun getCsvFilePathReferenceDevice(type: String) =
        File(
            OUTPUT_DIRECTORY,
            "/REFERENCE_DEVICE/MaximSensorsApp_${timestamp}_${type}_reference_device.csv"
        ).absolutePath

    private fun getCsvFilePathAligned(type: String) =
        File(
            OUTPUT_DIRECTORY,
            "/ALIGNED/MaximSensorsApp_${timestamp}_${type}_aligned.csv"
        ).absolutePath

    fun record(data: HspStreamData) {
        csvWriter.write(
            data.sampleCount,
            TIMESTAMP_FORMAT.format(Date(data.sampleTime.toLong())),
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
            TIMESTAMP_FORMAT.format(Date(data.currentTimeMillis)),
            data.currentTimeMillis
        )

        if (count % 25 == 0) {
            csvWriter1Hz.write(
                data.currentTimeMillis,
                data.hr
            )
            count = 0
        }

        count++
    }

    fun record(data: HeartRateMeasurement) {
        referenceDevice.write(
            data.currentTimeMillis,
            data.heartRate,
            if (data.contactDetected == true) 1 else 0
        )
    }

    fun close() {
        csvWriter1Hz.listener = oneHzListener
        referenceDevice.listener = referenceListener
        try {
            csvWriter.close()
            csvWriter1Hz.close()
            referenceDevice.close()
        } catch (e: Exception) {
            Timber.tag(DataRecorder.javaClass.simpleName).e(e.message.toString())
        }
    }

    interface DataRecorderListener {
        fun onFilesAreReadyForAlignment(
            alignedFilePath: String,
            maxim1HzFilePath: String,
            refFilePath: String
        )
    }
}