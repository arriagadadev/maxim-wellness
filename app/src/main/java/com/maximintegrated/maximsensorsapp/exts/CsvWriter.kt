package com.maximintegrated.maximsensorsapp.exts

import java.io.File
import java.util.concurrent.LinkedBlockingDeque

class CsvWriter private constructor(filePath: String) {

    companion object {
        private val POISON_PILL = Any()

        fun open(filePath: String, header: Array<String> = emptyArray()): CsvWriter {
            val csvWriter = CsvWriter(filePath)
            if (header.isNotEmpty()) {
                csvWriter.write(*header)
            }
            return csvWriter
        }
    }

    private val linesQueue = LinkedBlockingDeque<Any>()

    var isOpen = true
        private set


    init {
        ioThread {
            val file = File(filePath)
            file.parentFile.mkdirs()

            file.printWriter().use { out ->
                while (true) {
                    val line = linesQueue.take()
                    if (line == POISON_PILL) {
                        break
                    }

                    out.println(line)
                }
            }
        }
    }

    fun write(vararg columns: Any) {
        if (isOpen) {
            val line = columns.joinToString(",")
            linesQueue.offer(line)
        } else {
            throw IllegalStateException("Writer is not open!")
        }
    }

    fun close() {
        if (isOpen) {
            isOpen = false
            linesQueue.offer(POISON_PILL)
        } else {
            throw IllegalStateException("Writer is already closed!")
        }
    }
}

