package com.maximintegrated.maximsensorsapp

import com.maximintegrated.algorithms.AlgorithmInput
import java.io.File

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

fun readAlgorithmInputsFromFile(file: File?): ArrayList<AlgorithmInput> {
    val inputs: ArrayList<AlgorithmInput> = arrayListOf()
    if(file == null){
        return inputs
    }
    val rows = file.readLines().drop(1)
    for (row in rows) {
        val items = row.split(",")
        if (items.size < 31) continue
        val input = AlgorithmInput()
        input.green = items[2].toInt()
        input.green2 = items[3].toInt()
        input.ir = items[4].toInt()
        input.red = items[5].toInt()
        input.accelerationX = (items[6].toFloat() * 1000f).toInt()
        input.accelerationY = (items[7].toFloat() * 1000f).toInt()
        input.accelerationZ = (items[8].toFloat() * 1000f).toInt()
        input.operationMode = items[9].toInt()
        input.hr = items[10].toInt()
        input.hrConfidence = items[11].toInt()
        input.rr = (items[12].toFloat() * 10f).toInt()
        input.rrConfidence = items[13].toInt()
        input.activity = items[14].toInt()
        input.r = (items[15].toFloat() * 1000f).toInt()
        input.wspo2Confidence = items[16].toInt()
        input.spo2 = (items[17].toFloat() * 10f).toInt()
        input.wspo2PercentageComplete = items[18].toInt()
        input.wspo2LowSnr = items[19].toInt()
        input.wspo2Motion = items[20].toInt()
        input.wspo2LowPi = items[21].toInt()
        input.wspo2UnreliableR = items[22].toInt()
        input.wspo2State = items[23].toInt()
        input.scdState = items[24].toInt()
        input.walkSteps = items[25].toInt()
        input.runSteps = items[26].toInt()
        input.kCal = (items[27].toFloat() * 10f).toInt()
        input.cadence = items[28].toInt()
        input.timestamp = items[30].toLong()
        inputs.add(input)
    }
    return inputs
}