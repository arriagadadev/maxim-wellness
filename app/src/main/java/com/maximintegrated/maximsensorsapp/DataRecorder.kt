package com.maximintegrated.maximsensorsapp

import android.os.Environment
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.polar.HeartRateMeasurement
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import timber.log.Timber
import java.io.File
import java.util.*

class DataRecorder(var type: String) {

    companion object {
        val OUTPUT_DIRECTORY = File(Environment.getExternalStorageDirectory(), "MaximSensorsApp")

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

    val timestamp = HspStreamData.TIMESTAMP_FORMAT.format(Date())

    private var oneHzFileIsFinished = false
    private var referenceFileIsFinished = false

    var dataRecorderListener: DataRecorderListener? = null

    init {
        csvWriter = CsvWriter.open(
            getCsvFilePath(type),
            HspStreamData.CSV_HEADER_HSP
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
            if (!isSuccessful) return
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
            if (!isSuccessful) return
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
        File(OUTPUT_DIRECTORY, "${File.separator}RAW${File.separator}MaximSensorsApp_${timestamp}_$type.csv").absolutePath

    private fun getCsvFilePath1Hz(type: String) =
        File(OUTPUT_DIRECTORY, "${File.separator}1Hz${File.separator}MaximSensorsApp_${timestamp}_${type}_1Hz.csv").absolutePath

    private fun getCsvFilePathReferenceDevice(type: String) =
        File(
            OUTPUT_DIRECTORY,
            "${File.separator}REFERENCE_DEVICE${File.separator}MaximSensorsApp_${timestamp}_${type}_reference_device.csv"
        ).absolutePath

    private fun getCsvFilePathAligned(type: String) =
        File(
            OUTPUT_DIRECTORY,
            "${File.separator}ALIGNED${File.separator}MaximSensorsApp_${timestamp}_${type}_aligned.csv"
        ).absolutePath

    fun record(data: HspStreamData) {
        csvWriter.write(data.toCsvModel())

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
            Timber.tag(DataRecorder::class.java.simpleName).e(e.message.toString())
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