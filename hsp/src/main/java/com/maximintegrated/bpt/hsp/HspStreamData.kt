package com.maximintegrated.bpt.hsp


data class HspStreamData(
    val sampleCount: Int,
    val sampleTime: Float,
    val green: Int,
    val green2: Int,
    val ir: Int,
    val red: Int,
    val accelerationX: Float,
    val accelerationY: Float,
    val accelerationZ: Float,
    val operationMode: Int,
    val hr: Int,
    val hrConfidence: Int,
    val rr: Float,
    val rrConfidence: Int,
    val activity: Int,
    val r: Float,
    val wspo2Confidence: Int,
    val spo2: Float,
    val wspo2PercentageComplete: Int,
    val wspo2LowSnr: Int,
    val wspo2Motion: Int,
    val wspo2LowPi: Int,
    val wspo2UnreliableR: Int,
    val wspo2State: Int,
    val scdState: Int,
    val walkSteps: Int,
    val runSteps: Int,
    val kCal: Int,
    val cadence: Int,
    val currentTimeMillis: Long = System.currentTimeMillis()
) {

    var sampleTimeInt = sampleTime.toInt()
    var accelerationXInt = (accelerationX * 1000f).toInt()
    var accelerationYInt = (accelerationY * 1000f).toInt()
    var accelerationZInt = (accelerationZ * 1000f).toInt()
    var rrInt = (rr * 10f).toInt()
    var rInt = (r * 1000f).toInt()
    var spo2Int = (spo2 * 10f).toInt()

    companion object {

        // get_format ppg 9 enc=bin cs=1 format={smpleCnt,8},{smpleTime,32},{grnCnt,20},{grn2Cnt,20},{irCnt,20},{redCnt,20},{accelX,14,3},
        //{accelY,14,3},{accelZ,14,3},{opMode,4},{hr,12},{hrconf,8},{rr,14,1},{rrconf,8},{activity,4},{r,12,3},{wspo2conf,8},{spo2,11,1},{wspo2percentcomplete,8},
        //{wspo2lowSNR,1},{wspo2motion,1},{wspo2lowpi,1},{wspo2unreliableR,1},{wspo2state,4},{scdstate,4},
        //{wSteps,24},{rSteps,24},{kCal,24},{cadence,24}

        fun fromPacket(packet: ByteArray): HspStreamData {
            return with(BitStreamReader(packet, 8)) {
                HspStreamData(
                    sampleCount = nextInt(8),
                    sampleTime = nextFloat(32, 1),
                    green = nextInt(20),
                    green2 = nextInt(20),
                    ir = nextInt(20),
                    red = nextInt(20),
                    accelerationX = nextSignedFloat(14, 1000),
                    accelerationY = nextSignedFloat(14, 1000),
                    accelerationZ = nextSignedFloat(14, 1000),
                    operationMode = nextInt(4),
                    hr = nextInt(12),
                    hrConfidence = nextInt(8),
                    rr = nextFloat(14, 10),
                    rrConfidence = nextInt(8),
                    activity = nextInt(4),
                    r = nextSignedFloat(12, 1000),
                    wspo2Confidence = nextInt(8),
                    spo2 = nextFloat(11, 10),
                    wspo2PercentageComplete = nextInt(8),
                    wspo2LowSnr = nextInt(1),
                    wspo2Motion = nextInt(1),
                    wspo2LowPi = nextInt(1),
                    wspo2UnreliableR = nextInt(1),
                    wspo2State = nextInt(4),
                    scdState = nextInt(4),
                    walkSteps = nextInt(24),
                    runSteps = nextInt(24),
                    kCal = nextInt(24),
                    cadence = nextInt(24)
                )
            }
        }
    }
}