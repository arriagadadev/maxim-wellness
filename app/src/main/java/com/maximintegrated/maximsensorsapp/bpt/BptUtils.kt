package com.maximintegrated.maximsensorsapp.bpt

import de.siegmar.fastcsv.reader.CsvReader
import de.siegmar.fastcsv.reader.CsvRow
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

val HISTORY_FILE: File
    get() = File(
        BptViewModel.OUTPUT_DIRECTORY,
        "${File.separator}${BptSettings.currentUser}${File.separator}BPTrending_history.csv"
    )

val CALIBRATION_FILE: File
    get() = File(
        BptViewModel.OUTPUT_DIRECTORY,
        "${File.separator}${BptSettings.currentUser}${File.separator}BPTrending_calibration_data.txt"
    )



const val NUMBER_OF_REFERENCES = 3
const val NUMBER_OF_FEATURES = 36
const val PPG_TEMPLATE_LENGTH = 50
const val CAL_RESULT_LENGTH = 78 + 25
const val NUMBER_OF_CALIBRATION_DATA_IN_CALIBRATION_RESULT = 2

fun saveHistoryData(historyData: BptHistoryData) {
    if (!HISTORY_FILE.exists()) {
        HISTORY_FILE.parentFile?.mkdirs()
        HISTORY_FILE.createNewFile()
        HISTORY_FILE.appendText(BptHistoryData.CSV_HEADER_ARRAY.joinToString(",") + "\n")
    }
    HISTORY_FILE.appendText(historyData.toText())
}

fun readHistoryData(): List<BptHistoryData> {
    val list: ArrayList<BptHistoryData> = arrayListOf()
    if(!HISTORY_FILE.exists()){
        return list
    }
    val reader = CsvReader()
    reader.setContainsHeader(true)
    try {
        val parser = reader.parse(HISTORY_FILE, StandardCharsets.UTF_8)
        var row: CsvRow? = parser.nextRow()
        while (row != null) {
            val timestamp = row.getField(0).toLongOrZero()
            val sbp1 = row.getField(1).toIntOrZero()
            val dbp1 = row.getField(2).toIntOrZero()
            val hr = row.getField(3).toIntOrZero()
            val spo2 = row.getField(4).toIntOrZero()
            val pulseFlag = row.getField(5).toIntOrZero()
            val isCalibration = row.getField(6) == "Calibration"
            var sbp2 = 0
            var dbp2 = 0
            var sbp3 = 0
            var dbp3 = 0
            if(row.fieldCount > 10){
                sbp2 = row.getField(7).toIntOrZero()
                dbp2 = row.getField(8).toIntOrZero()
                sbp3 = row.getField(9).toIntOrZero()
                dbp3 = row.getField(10).toIntOrZero()
            }
            val data = BptHistoryData(timestamp, isCalibration, sbp1, dbp1, hr, spo2, pulseFlag, sbp2, dbp2, sbp3, dbp3)
            list.add(data)
            row = parser.nextRow()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun saveCalibrationData(vararg calibrations: BptCalibrationData){
    if (!CALIBRATION_FILE.exists()) {
        CALIBRATION_FILE.parentFile?.mkdirs()
        CALIBRATION_FILE.createNewFile()
    }
    for(calibration in calibrations){
        CALIBRATION_FILE.appendText(calibration.toText())
    }
}

fun String.toIntOrZero(): Int{
    return this.toIntOrNull() ?: 0
}

fun String.toFloatOrZero(): Float{
    return this.toFloatOrNull() ?: 0f
}

fun String.toLongOrZero(): Long{
    return this.toLongOrNull() ?: 0L
}