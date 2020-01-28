package com.maximintegrated.maximsensorsapp.exts

import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.bpt.hsp.HspStreamData

fun AlgorithmInput.set(data: HspStreamData) {
    sampleCount = data.sampleCount
    sampleTime = data.sampleTimeInt
    green = data.green
    green2 = data.green2
    ir = data.ir
    red = data.red
    accelerationX = data.accelerationXInt
    accelerationY = data.accelerationYInt
    accelerationZ = data.accelerationZInt
    operationMode = data.operationMode
    hr = data.hr
    hrConfidence = data.hrConfidence
    rr = data.rrInt
    rrConfidence = data.rrConfidence
    activity = data.activity
    r = data.rInt
    wspo2Confidence = data.wspo2Confidence
    spo2 = data.spo2Int
    wspo2PercentageComplete = data.wspo2PercentageComplete
    wspo2LowSnr = data.wspo2LowSnr
    wspo2Motion = data.wspo2Motion
    wspo2LowPi = data.wspo2LowPi
    wspo2UnreliableR = data.wspo2UnreliableR
    wspo2State = data.wspo2State
    scdState = data.scdState
    walkSteps = data.walkSteps
    runSteps = data.runSteps
    kCal = data.kCalInt
    totalActEnergy = data.totalActEnergyInt
    timestamp = data.currentTimeMillis
}