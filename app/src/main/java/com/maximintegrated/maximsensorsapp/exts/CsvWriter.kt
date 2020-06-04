package com.maximintegrated.maximsensorsapp.exts

import java.io.File
import java.util.concurrent.LinkedBlockingDeque

class CsvWriter private constructor(var filePath: String) {

    companion object {
        private val POISON_PILL = Any()

        fun open(filePath: String, header: Array<String> = emptyArray()): CsvWriter {
            val csvWriter = CsvWriter(filePath)
            if (header.isNotEmpty()) {
                csvWriter.write(*header)
            }
            return csvWriter
        }

        interface CsvWriterListener {
            fun onCompleted(isSuccessful: Boolean)
        }
    }

    private val linesQueue = LinkedBlockingDeque<Any>()

    var isOpen = true
        private set

    var listener: CsvWriterListener? = null

    private var delete = false

    private var packetLossOccurred = false

    init {
        ioThread {
            val file = File(filePath)
            file.parentFile?.mkdirs()

            file.printWriter().use { out ->
                var count = 0
                var flushed = false
                while (true) {
                    val line = linesQueue.take()
                    if (line == POISON_PILL) {
                        break
                    }
                    count++

                    out.println(line)
                    if (count > 10000) {
                        out.flush()
                        flushed = true
                        count = 0
                    }

                }
                if ((count == 1 && !flushed) || delete) {
                    listener?.onCompleted(false)
                    file.delete()
                }else if(packetLossOccurred){
                    listener?.onCompleted(false)
                    val renamedFile = File(file.parentFile, File.separator + file.nameWithoutExtension + "!!!PacketLoss!!!." + file.extension)
                    file.renameTo(renamedFile)
                }else{
                    listener?.onCompleted(true)
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

    fun close(packetLossOccurred: Boolean = false) {
        this.packetLossOccurred = packetLossOccurred
        if (isOpen) {
            isOpen = false
            linesQueue.offer(POISON_PILL)
        } else {
            throw IllegalStateException("Writer is already closed!")
        }
    }

    fun delete(){
        delete = true
    }
}

