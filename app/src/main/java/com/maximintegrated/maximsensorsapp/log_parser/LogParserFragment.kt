package com.maximintegrated.maximsensorsapp.log_parser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.maximsensorsapp.DataRecorder
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import com.obsez.android.lib.filechooser.ChooserDialog
import kotlinx.android.synthetic.main.fragment_log_parser.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LogParserFragment : Fragment() {

    companion object {
        fun newInstance() = LogParserFragment()
        private const val LOG_FILE_HEADER = "mxim"
    }

    private var logFile: File? = null
        set(value) {
            field = value
            logFileTextView.text = logFile?.path ?: ""
            savedFile = null
        }

    private var savedFile: File? = null
        set(value) {
            field = value
            savedTextView.text = value?.path ?: ""
            if (value != null) {
                Toast.makeText(
                    context,
                    getString(R.string.file_saved_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_log_parser, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        importButton.setOnClickListener {
            showFilesToImport()
        }
        parseButton.setOnClickListener {
            logFile?.let { file -> parse(file) }
        }
    }

    private fun showFilesToImport() {
        ChooserDialog(requireContext())
            .withStartFile(DataRecorder.OUTPUT_DIRECTORY.absolutePath)
            .withChosenListener { _, dirFile ->
                run {
                    logFile = dirFile
                }
            }
            .build()
            .show()
    }

    private fun parse(file: File) {
        if (file.extension != "maximlog") {
            Toast.makeText(context, getString(R.string.file_format_no_match), Toast.LENGTH_SHORT)
                .show()
            return
        }
        val fileName = file.nameWithoutExtension
        val outFile =
            File(DataRecorder.OUTPUT_DIRECTORY, "/RAW/MaximSensorsApp_${fileName}_Flash.csv")
        /*if (outFile.exists()) {
            Toast.makeText(context, getString(R.string.file_already_exists), Toast.LENGTH_SHORT)
                .show()
            return
        }*/
        val csvWriter = CsvWriter.open(outFile.absolutePath, HspStreamData.CSV_HEADER_HSP)
        progressBar.visibility = View.VISIBLE
        doAsync {
            try {
                val inputStream = FileInputStream(file)
                val header = ByteArray(LOG_FILE_HEADER.length)
                Timber.d("MELIK: header = ${String(header)}")
                val byteCount = inputStream.read(header)
                if (byteCount != LOG_FILE_HEADER.length || String(header) != LOG_FILE_HEADER) {
                    csvWriter.close()
                    uiThread {
                        Toast.makeText(
                            context,
                            getString(R.string.not_maxim_log_file),
                            Toast.LENGTH_SHORT
                        ).show()
                        progressBar.visibility = View.GONE
                        savedFile = null
                    }
                } else {
                    val logVersion = inputStream.read()
                    if (logVersion > 0) {
                        val metaDataLength = inputStream.read()
                        val metaData = ByteArray(metaDataLength)
                        inputStream.read(metaData)
                    }
                    val formatSectionLengthBytes = ByteArray(4)
                    inputStream.read(formatSectionLengthBytes)
                    val formatSectionLength = byteArrayToInt(formatSectionLengthBytes)
                    //Timber.d("MELIK: formatSectionLength = $formatSectionLength}")
                    val formatSectionBytes = ByteArray(formatSectionLength + 1)
                    inputStream.read(formatSectionBytes)
                    //Timber.d("MELIK: formatSectionBytes = ${String(formatSectionBytes)}")
                    val rawData = ByteArray(HspStreamData.NUMBER_OF_BYTES_IN_PACKET)
                    var isFormatCorrect = true
                    while (inputStream.read(rawData) == HspStreamData.NUMBER_OF_BYTES_IN_PACKET) {
                        if (rawData[0] != (0xAA).toByte()) {
                            isFormatCorrect = false
                            break
                        }
                        val hspData = HspStreamData.fromPacket(rawData)
                        Timber.d("hspRawData = ${rawData.contentToString()}  sampleTime before fix: ${hspData.sampleTime}")
                        fixCurrentTimeMillis(hspData)
                        csvWriter.write(hspData.toCsvModel())
                    }
                    if (!isFormatCorrect) {
                        csvWriter.delete()
                        csvWriter.close()
                        uiThread {
                            progressBar.visibility = View.GONE
                            savedFile = null
                            Toast.makeText(
                                context,
                                getString(R.string.wrong_format),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        csvWriter.close()
                        uiThread {
                            progressBar.visibility = View.GONE
                            savedFile = outFile
                        }
                    }
                }
            } catch (e: Exception) {
                csvWriter.close()
                uiThread {
                    progressBar.visibility = View.GONE
                    savedFile = null
                    Toast.makeText(
                        context,
                        getString(R.string.error_occured),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }
    }

    private fun byteArrayToInt(array: ByteArray): Int {
        val buffer = ByteBuffer.wrap(array)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return buffer.int
    }

    private var prevSecond = -1
    private var counter = 0

    private fun fixCurrentTimeMillis(data: HspStreamData) {
        val sampleTime = data.sampleTime
        data.currentTimeMillis = sampleTime.toLong() * 1000
        if (sampleTime != prevSecond) {
            prevSecond = sampleTime
            counter = 0
        } else {
            counter++
            data.currentTimeMillis += 40 * counter
        }
        //Timber.d("sample Time = ${data.sampleTime.toLong()}  current = ${data.currentTimeMillis}")
    }
}