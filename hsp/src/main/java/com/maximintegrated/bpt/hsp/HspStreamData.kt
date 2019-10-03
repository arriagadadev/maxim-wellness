package com.maximintegrated.bpt.hsp


data class HspStreamData(
    val sampleCount: Int,
    val ir: Int,
    val red: Int,
    val green: Int,
    val accelerationX: Float,
    val accelerationY: Float,
    val accelerationZ: Float,
    val currentTimeMillis: Long = System.currentTimeMillis()
) {
    companion object {

        // get_format ppg 4 enc=bin cs=1 format={smpleCnt,8},{grnCnt,20},{irCnt,20},{redCnt,20},{accelX,14,3},{accelY,14,3},{accelZ,14,3},{hr,9},{hrconf,8},{spo2,8},{wspo2conf,8},{wspo2lowSNR,1} err=0
        fun fromPacket(packet: ByteArray) = with(BitStreamReader(packet, 8)) {
            HspStreamData(
                sampleCount = nextInt(8),
                green = nextInt(20),
                ir = nextInt(20),
                red = nextInt(20),
                accelerationX = nextSignedFloat(14, 1000),
                accelerationY = nextSignedFloat(14, 1000),
                accelerationZ = nextSignedFloat(14, 1000)
            )
        }
    }
}