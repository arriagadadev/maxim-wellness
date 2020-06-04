package com.maximintegrated.maximsensorsapp

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.maximintegrated.algorithms.AlgorithmInput
import de.siegmar.fastcsv.reader.CsvReader
import de.siegmar.fastcsv.reader.CsvRow
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

fun readAlgorithmInputsFromFile(file: File?): ArrayList<AlgorithmInput> {
    val inputs: ArrayList<AlgorithmInput> = arrayListOf()
    if (file == null) {
        return inputs
    }
    val reader = CsvReader()
    reader.setContainsHeader(true)
    try {
        val parser = reader.parse(file, StandardCharsets.UTF_8)
        var row: CsvRow? = parser.nextRow()
        while (row != null) {
            val input = csvRowToAlgorithmInput(row)
            if (input != null) {
                inputs.add(input)
            }
            row = parser.nextRow()
        }
    } catch (e: Exception) {

    }
    return inputs
}

fun csvRowToAlgorithmInput(row: String): AlgorithmInput? {
    val items = row.split(",")
    if (items.size < 31) return null
    val input = AlgorithmInput()
    input.green = items[2].toFloat().toInt()
    input.green2 = items[3].toFloat().toInt()
    input.ir = items[4].toFloat().toInt()
    input.red = items[5].toFloat().toInt()
    input.accelerationX = (items[6].toFloat() * 1000f).toInt()
    input.accelerationY = (items[7].toFloat() * 1000f).toInt()
    input.accelerationZ = (items[8].toFloat() * 1000f).toInt()
    input.operationMode = items[9].toFloat().toInt()
    input.hr = items[10].toFloat().toInt()
    input.hrConfidence = items[11].toFloat().toInt()
    input.rr = (items[12].toFloat() * 10f).toInt()
    input.rrConfidence = items[13].toFloat().toInt()
    input.activity = items[14].toFloat().toInt()
    input.r = (items[15].toFloat() * 1000f).toInt()
    input.wspo2Confidence = items[16].toFloat().toInt()
    input.spo2 = (items[17].toFloat() * 10f).toInt()
    input.wspo2PercentageComplete = items[18].toFloat().toInt()
    input.wspo2LowSnr = items[19].toFloat().toInt()
    input.wspo2Motion = items[20].toFloat().toInt()
    input.wspo2LowPi = items[21].toFloat().toInt()
    input.wspo2UnreliableR = items[22].toFloat().toInt()
    input.wspo2State = items[23].toFloat().toInt()
    input.scdState = items[24].toFloat().toInt()
    input.walkSteps = items[25].toFloat().toInt()
    input.runSteps = items[26].toFloat().toInt()
    input.kCal = (items[27].toFloat() * 10f).toInt()
    input.totalActEnergy = (items[28].toFloat() * 10f).toInt()
    input.timestamp = items[30].toDouble().toLong()
    return input
}

fun csvRowToAlgorithmInput(row: CsvRow): AlgorithmInput? {
    val input = AlgorithmInput()
    if (row.fieldCount < 31) return null
    input.green = row.getField(2).toFloat().toInt()
    input.green2 = row.getField(3).toFloat().toInt()
    input.ir = row.getField(4).toFloat().toInt()
    input.red = row.getField(5).toFloat().toInt()
    input.accelerationX = (row.getField(6).toFloat() * 1000f).toInt()
    input.accelerationY = (row.getField(7).toFloat() * 1000f).toInt()
    input.accelerationZ = (row.getField(8).toFloat() * 1000f).toInt()
    input.operationMode = row.getField(9).toFloat().toInt()
    input.hr = row.getField(10).toFloat().toInt()
    input.hrConfidence = row.getField(11).toFloat().toInt()
    input.rr = (row.getField(12).toFloat() * 10f).toInt()
    input.rrConfidence = row.getField(13).toFloat().toInt()
    input.activity = row.getField(14).toFloat().toInt()
    input.r = (row.getField(15).toFloat() * 1000f).toInt()
    input.wspo2Confidence = row.getField(16).toFloat().toInt()
    input.spo2 = (row.getField(17).toFloat() * 10f).toInt()
    input.wspo2PercentageComplete = row.getField(18).toFloat().toInt()
    input.wspo2LowSnr = row.getField(19).toFloat().toInt()
    input.wspo2Motion = row.getField(20).toFloat().toInt()
    input.wspo2LowPi = row.getField(21).toFloat().toInt()
    input.wspo2UnreliableR = row.getField(22).toFloat().toInt()
    input.wspo2State = row.getField(23).toFloat().toInt()
    input.scdState = row.getField(24).toFloat().toInt()
    input.walkSteps = row.getField(25).toFloat().toInt()
    input.runSteps = row.getField(26).toFloat().toInt()
    input.kCal = (row.getField(27).toFloat() * 10f).toInt()
    input.totalActEnergy = (row.getField(28).toFloat() * 10f).toInt()
    input.timestamp = row.getField(30).toDouble().toLong()
    return input
}

fun getFormattedTime(elapsedTime: Long): String {
    val hour = TimeUnit.MILLISECONDS.toHours(elapsedTime)
    val min = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60
    val sec = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
    return String.format("%02d:%02d:%02d", hour, min, sec)
}

fun showAlertDialog(context: Context, title: String, message: String, positiveButtonText: String, action: (() -> Unit)? = null) {
    val alertDialog = AlertDialog.Builder(context)
    alertDialog.setTitle(title)
    alertDialog.setMessage(message)
        .setPositiveButton(positiveButtonText) { dialog, _ ->
            action?.let { it() }
            dialog.dismiss()
        }.setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
    alertDialog.setCancelable(true)
    alertDialog.show()
}