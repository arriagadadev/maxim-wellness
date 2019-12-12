package com.maximintegrated.maximsensorsapp

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }