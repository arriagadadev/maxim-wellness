package com.maximintegrated.maximsensorsapp

import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val ACCURACY_THRESHOLD = 70

fun align(alignedFilePath: String, maxim1HzFilePath: String, refFilePath: String) {
    val maximPairs = readTimeStampAndHrFrom1HzFile(File(maxim1HzFilePath))
    val refPairs = readTimeStampAndHrFromReferenceFile(File(refFilePath))
    if (maximPairs.isEmpty() || refPairs.isEmpty()) {
        return
    }
    val alignedTriples = getAlignedTriples(maximPairs, refPairs)
    if (alignedTriples.isEmpty()) {
        return
    }

    val csvWriterAligned = CsvWriter.open(
        alignedFilePath,
        arrayOf("timestamp", "maxim_hr", "ref_hr")
    )

    for (triple in alignedTriples) {
        csvWriterAligned.write(triple.first, triple.second, triple.third)
    }

    try {
        csvWriterAligned.close()
    } catch (e: Exception) {
        Timber.tag(DataRecorder.javaClass.simpleName).e(e.message.toString())
    }
}

private fun getAlignedTriples(
    pair1: List<Pair<Long, Int>>,
    pair2: List<Pair<Long, Int>>
): ArrayList<Triple<Long, Int, Int>> {
    val alignedTriples: ArrayList<Triple<Long, Int, Int>> = arrayListOf()
    val startTime1 = pair1.first().first
    val startTime2 = pair2.first().first
    val endTime1 = pair1.last().first
    val endTime2 = pair2.last().first

    val croppedPair1 = pair1.filter {
        it.first >= max(startTime1, startTime2) && it.first <= min(
            endTime1,
            endTime2
        )
    }
    val croppedPair2 = pair2.filter {
        it.first >= max(startTime1, startTime2) && it.first <= min(
            endTime1,
            endTime2
        )
    }

    if (croppedPair1.size <= 30 || croppedPair2.size <= 30) {
        return alignedTriples
    }

    var maximumHrAccuracy = 0
    var bestTimeDifference = 0

    var alignedPair1: List<Pair<Long, Int>>
    var alignedPair2: List<Pair<Long, Int>>

    for (timeDiff in -30..30) {
        if (timeDiff < 0) {
            alignedPair2 = croppedPair2.subList(-timeDiff, croppedPair2.size)
            alignedPair1 = croppedPair1.subList(0, croppedPair1.size - (-timeDiff))
        } else {
            alignedPair1 = croppedPair1.subList(timeDiff, croppedPair1.size)
            alignedPair2 = croppedPair2
        }

        val commonLength = min(alignedPair1.size, alignedPair2.size)
        alignedPair1 = alignedPair1.subList(0, commonLength)
        alignedPair2 = alignedPair2.subList(0, commonLength)

        val accuracy = calculate5bpmHrAccuracy(alignedPair1, alignedPair2)

        if (accuracy > maximumHrAccuracy) {
            maximumHrAccuracy = accuracy
            bestTimeDifference = timeDiff
        }
    }

    if(maximumHrAccuracy < ACCURACY_THRESHOLD){
        return alignedTriples
    }

    if (bestTimeDifference < 0) {
        alignedPair2 = croppedPair2.subList(-bestTimeDifference, croppedPair2.size)
        alignedPair1 = croppedPair1.subList(0, croppedPair1.size + bestTimeDifference)
    } else {
        alignedPair1 = croppedPair1.subList(bestTimeDifference, croppedPair1.size)
        alignedPair2 = croppedPair2
    }

    val commonLength = min(alignedPair1.size, alignedPair2.size)
    alignedPair1 = alignedPair1.subList(0, commonLength)
    alignedPair2 = alignedPair2.subList(0, commonLength)

    val timeOffset = max(0, bestTimeDifference)

    for (i in 0 until commonLength) {
        alignedTriples.add(
            Triple(
                alignedPair1[i].first + timeOffset,
                alignedPair1[i].second,
                alignedPair2[i].second
            )
        )
    }

    return alignedTriples
}

private fun calculate5bpmHrAccuracy(
    list1: List<Pair<Long, Int>>,
    list2: List<Pair<Long, Int>>
): Int {
    val absError: ArrayList<Int> = arrayListOf()

    for (i in 30 until list1.size) {
        absError.add(abs(list1[i].second - list2[i].second))
    }

    if (absError.isEmpty()) return 0

    val absErrorIndices = absError.filter { it <= 5 }

    return 100 * absErrorIndices.size / absError.size
}


fun readTimeStampAndHrFrom1HzFile(file: File?): ArrayList<Pair<Long, Int>> {
    val pairs: ArrayList<Pair<Long, Int>> = arrayListOf()
    if (file == null) {
        return pairs
    }
    val rows = file.bufferedReader().readLines().drop(1)
    for (row in rows) {
        val items = row.split(",")
        if (items.size < 2) continue
        pairs.add(Pair(items[0].toLong(), items[1].toInt()))
    }
    return pairs
}

fun readTimeStampAndHrFromReferenceFile(file: File?): ArrayList<Pair<Long, Int>> {
    val pairs: ArrayList<Pair<Long, Int>> = arrayListOf()
    if (file == null) {
        return pairs
    }
    val rows = file.bufferedReader().readLines().drop(1)
    for (row in rows) {
        val items = row.split(",")
        if (items.size < 2) continue
        pairs.add(Pair(items[0].toLong(), items[1].toInt()))
    }
    return pairs
}

fun readTimeStampAndHrFromAlignedFile(file: File?): ArrayList<Triple<Long, Int, Int>> {
    val triples: ArrayList<Triple<Long, Int, Int>> = arrayListOf()
    if (file == null) {
        return triples
    }
    val rows = file.bufferedReader().readLines().drop(1)
    for (row in rows) {
        val items = row.split(",")
        if (items.size < 3) continue
        triples.add(Triple(items[0].toLong(), items[1].toInt(), items[2].toInt()))
    }
    return triples
}