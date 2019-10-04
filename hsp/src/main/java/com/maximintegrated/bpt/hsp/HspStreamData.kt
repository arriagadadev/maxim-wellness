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
    val currentTimeMillis: Long = System.currentTimeMillis()
) {
    companion object {

        // get_format ppg 4 enc=bin cs=1 format={smpleCnt,8},{grnCnt,20},{irCnt,20},{redCnt,20},{accelX,14,3},{accelY,14,3},{accelZ,14,3},{hr,9},{hrconf,8},{spo2,8},{wspo2conf,8},{wspo2lowSNR,1} err=0
        fun fromPacket(packet: ByteArray) = with(BitStreamReader(packet, 8)) {
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
                rr = nextSignedFloat(14, 10),
                rrConfidence = nextInt(8),
                activity = nextInt(4),
                r = nextSignedFloat(12, 10),
                wspo2Confidence = nextInt(8),
                spo2 = nextFloat(11, 10),
                wspo2PercentageComplete = nextInt(8),
                wspo2LowSnr = nextInt(1),
                wspo2Motion = nextInt(1),
                wspo2LowPi = nextInt(1),
                wspo2UnreliableR = nextInt(1),
                wspo2State = nextInt(4),
                scdState = nextInt(4)
            )
        }
    }
}