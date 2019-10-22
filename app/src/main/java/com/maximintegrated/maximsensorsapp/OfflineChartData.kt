package com.maximintegrated.maximsensorsapp

data class OfflineChartData(
    val dataSetValues: List<Pair<Float, Float>>,
    val title: String,
    val chartType: String
)