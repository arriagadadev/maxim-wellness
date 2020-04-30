package com.maximintegrated.maximsensorsapp.bpt

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

class BptCalibrationData(
    timestampStr: String = SimpleDateFormat(
        "yyyyMMddHHmmss",
        Locale.US
    ).format(Date())
) {
    var timestamp = timestampStr
    var date = timestampStr.substring(0, 8).toIntOrZero()
    var time = timestampStr.substring(8).toIntOrZero()
    var sysBp = IntArray(NUMBER_OF_REFERENCES) { 0 }
    var diaBp = IntArray(NUMBER_OF_REFERENCES) { 0 }
    var feat = FloatArray(NUMBER_OF_FEATURES) { 0F }
    var ppgTemplate = FloatArray(PPG_TEMPLATE_LENGTH) { 0f }
    var tSys = 0
    var tDia = 0
    var tHr = 0
    var eSys = 0
    var eDia = 0
    var r = 0f
    var spo2 = 0f
    var medication = 0
    var nonResting = 0

    companion object {

        fun parseCalibrationDataFromString(line: String): BptCalibrationData {
            val data = BptCalibrationData()
            val array = line.split(" ")
            var index = 0
            with(data) {
                date = array[index++].toIntOrZero()
                time = array[index++].toIntOrZero()
                for (i in sysBp.indices) {
                    sysBp[i] = array[index++].toIntOrZero()
                }
                for (i in diaBp.indices) {
                    diaBp[i] = array[index++].toIntOrZero()
                }
                for (i in feat.indices) {
                    feat[i] = array[index++].toFloatOrZero()
                }
                for (i in ppgTemplate.indices) {
                    ppgTemplate[i] = array[index++].toFloatOrZero()
                }
                tSys = array[index++].toIntOrZero()
                tDia = array[index++].toIntOrZero()
                tHr = array[index++].toIntOrZero()
                eSys = array[index++].toIntOrZero()
                eDia = array[index++].toIntOrZero()
                r = array[index++].toFloatOrZero()
                spo2 = array[index++].toFloatOrZero()
                medication = array[index++].toIntOrZero()
                nonResting = array[index].toIntOrZero()
            }
            return data
        }

        fun parseCalibrationDataFromCommandResponse(value: ByteArray): Array<BptCalibrationData> {
            val buffer = ByteBuffer.wrap(value)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            val array =
                Array(NUMBER_OF_CALIBRATION_DATA_IN_CALIBRATION_RESULT) { BptCalibrationData() }

            for (i in array.indices) {
                with(array[i]) {
                    date = buffer.int
                    time = buffer.int
                    for (j in sysBp.indices) {
                        sysBp[j] = buffer.int
                    }
                    for (j in diaBp.indices) {
                        diaBp[j] = buffer.int
                    }
                    for (j in feat.indices) {
                        feat[j] = buffer.float
                    }
                    for (j in ppgTemplate.indices) {
                        ppgTemplate[j] = buffer.float
                    }
                    tSys = buffer.int
                    tDia = buffer.int
                    tHr = buffer.int
                    eSys = buffer.int
                    eDia = buffer.int
                    r = buffer.float
                    spo2 = buffer.float
                    medication = buffer.int
                    nonResting = buffer.int
                }
            }
            return array
        }
    }

    fun toArray(): ByteArray {
        val buffer = ByteBuffer.allocate(CAL_RESULT_LENGTH * 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        with(buffer) {
            putInt(date)
            putInt(time)
            for (value in sysBp) {
                putInt(value)
            }
            for (value in diaBp) {
                putInt(value)
            }
            for (value in feat) {
                putFloat(value)
            }
            for (value in ppgTemplate) {
                putFloat(value)
            }
            putInt(tSys)
            putInt(tDia)
            putInt(tHr)
            putInt(eSys)
            putInt(eDia)
            putFloat(r)
            putFloat(spo2)
            putInt(medication)
            putInt(nonResting)
        }
        return buffer.array()
    }

    override fun toString(): String {
        return "BptCalibrationData(timestamp='$timestamp', date=$date, time=$time, sysBp=${sysBp.contentToString()}, diaBp=${diaBp.contentToString()}, feat=${feat.contentToString()}, ppgTemplate=${ppgTemplate.contentToString()}, tSys=$tSys, tDia=$tDia, tHr=$tHr, eSys=$eSys, eDia=$eDia, r=$r, spo2=$spo2, medication=$medication, nonResting=$nonResting)"
    }

    fun toText(): String {
        return "$date $time ${sysBp.joinToString(" ")} ${diaBp.joinToString(" ")} ${feat.joinToString(
            " "
        )} ${ppgTemplate.joinToString(" ")} $tSys $tDia $tHr $eSys $eDia $r $spo2 $medication $nonResting\n"
    }
}