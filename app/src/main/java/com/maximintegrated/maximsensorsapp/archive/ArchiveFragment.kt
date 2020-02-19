package com.maximintegrated.maximsensorsapp.archive

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.sleep.SleepAlgorithmInitConfig
import com.maximintegrated.algorithms.sleep.SleepUserInfo
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import com.maximintegrated.maximsensorsapp.exts.addFragment
import kotlinx.android.synthetic.main.fragment_archive.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Comparator


class ArchiveFragment : RecyclerViewClickListener, Fragment(),
    CsvWriter.Companion.CsvWriterListener {

    var algorithmInitConfig = AlgorithmInitConfig()

    var algoResult = false

    companion object {
        private val OUTPUT_DIRECTORY =
            File(Environment.getExternalStorageDirectory(), "MaximSleepQa")

        val SLEEP_TIMESTAMP_FORMAT = SimpleDateFormat("yyyy/MM/dd/HH/mm/ss", Locale.US)

        private val CSV_HEADER_SLEEP = arrayOf(
            "Timestamp",
            "wake_decision_status",
            "latency",
            "wake_decision",
            "phase_output_status",
            "phase_output",
            "hr",
            "ibi",
            "spo2",
            "acc_magnitude"
        )

        fun newInstance() = ArchiveFragment()
    }

    private val adapter: FileListAdapter by lazy { FileListAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputDirectory = File(DataRecorder.OUTPUT_DIRECTORY, "RAW")
        if (!inputDirectory.exists()) {
            inputDirectory.mkdirs()
        }
        val directory = File(inputDirectory.absolutePath)
        val files = directory.listFiles().toList().filter { !it.name.contains("1Hz") }

        initRecyclerView()

        adapter.fileList = files.sortedWith(Comparator<File> { file1, file2 ->
            when {
                file1.lastModified() > file2.lastModified() -> -1
                file1.lastModified() < file2.lastModified() -> 1
                else -> 0
            }
        }).toMutableList()

        adapter.notifyDataSetChanged()
    }

    private fun initRecyclerView() {
        fileRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        fileRecyclerView.adapter = adapter
    }

    private fun handleListItemClick(file: File) {
        requireActivity().addFragment(OfflineDataFragment.newInstance(file))
    }

    override fun onRowClicked(file: File) {
        handleListItemClick(file)
    }

    override fun onDeleteClicked(file: File) {
        showDeleteDialog(file)
    }

    override fun onShareClicked(file: File) {

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "vnd.android.cursor.dir/email"
            setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "MaximSensorsApp Csv File")
            putExtra(Intent.EXTRA_TEXT, "File Name: ${file.name}")
            val uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().packageName + ".fileprovider",
                file
            )
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        startActivity(intent)
    }

    override fun onSleepClicked(file: File) {
        progressBar.visibility = View.VISIBLE
        doAsync {
            algoResult = runSleepAlgo(file)
        }
    }

    private fun showDeleteDialog(file: File) {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Delete File")
        alertDialog.setMessage("Are you sure you want to delete this file ?")
            .setPositiveButton("Delete") { dialog, which ->
                val deleted = file.delete()
                if (deleted) {
                    adapter.fileList.remove(file)
                    adapter.notifyDataSetChanged()
                }
                dialog.dismiss()
            }.setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun runSleepAlgo(file: File): Boolean {
        val outputDirectory = OUTPUT_DIRECTORY

        val oneHzFile = File(
            DataRecorder.OUTPUT_DIRECTORY,
            "/1Hz/${file.nameWithoutExtension}_1Hz.csv"
        )

        var hrSum = 0
        var hrCount = 0
        var restingHr = 0f

        var isHeaderObtained = false

        if(oneHzFile.exists()){
            oneHzFile.useLines { seq ->
                seq.forEach {
                    if (!isHeaderObtained) {
                        isHeaderObtained = true
                        return@forEach
                    }
                    val items = it.split(",")
                    if (items.size < 2) return@forEach
                    val hr = items[1].toFloatOrNull()?.toInt()
                    if (hr != null && hr != 0) {
                        hrSum += hr
                        hrCount++
                    }
                }
            }
        }else if(file.exists()){
            file.useLines { seq ->
                seq.forEach {
                    if (!isHeaderObtained) {
                        isHeaderObtained = true
                        return@forEach
                    }
                    val items = it.split(",")
                    val hr = items[10].toFloatOrNull()?.toInt()
                    if (hr != null && hr != 0) {
                        hrSum += hr
                        hrCount++
                    }
                }
            }
        }


        if (hrCount != 0) {
            restingHr = hrSum * 1f / hrCount
        }
        val userInfo = SleepUserInfo(20, 70, SleepUserInfo.Gender.MALE, restingHr)

        algorithmInitConfig.sleepConfig = SleepAlgorithmInitConfig(
            SleepAlgorithmInitConfig.DetectableSleepDuration.MINIMUM_30_MIN,
            userInfo,
            restingHr != 0f,
            true,
            true,
            true
        )
        algorithmInitConfig.enableAlgorithmsFlag = MaximAlgorithms.FLAG_SLEEP

        MaximAlgorithms.init(algorithmInitConfig)

        var sleepFound = false

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val outputFile = File(outputDirectory, file.name)
        val csvWriter = CsvWriter.open(outputFile.absolutePath)
        csvWriter.listener = this
        val output = AlgorithmOutput()
        var count = 0

        isHeaderObtained = false
        file.useLines { seq ->
            seq.forEach {
                if (!isHeaderObtained) {
                    isHeaderObtained = true
                    return@forEach
                }
                val input = csvRowToAlgorithmInput(it)
                if (input != null) {
                    val status = MaximAlgorithms.run(input, output)
                    //TODO: check the sample code for this
                    if (status) {
                        if (output.sleep.outputDataArrayLength > 0) {
                            count++
                            if (output.sleep.output.sleepWakeDetentionLatency >= 0) {
                                sleepFound = true
                            } else {
                                output.sleep.output.sleepWakeDetentionLatency = -1
                            }
                            csvWriter.write(
                                SLEEP_TIMESTAMP_FORMAT.format(Date(output.sleep.dateInfo)),
                                output.sleep.output.sleepWakeDecisionStatus,
                                output.sleep.output.sleepWakeDetentionLatency,
                                output.sleep.output.sleepWakeDecision,
                                output.sleep.output.sleepPhaseOutputStatus,
                                output.sleep.output.sleepPhaseOutput,
                                output.sleep.output.hr,
                                output.sleep.output.ibi,
                                0,
                                output.sleep.output.accMag
                            )
                        }
                    }
                }
            }
        }
        MaximAlgorithms.end(MaximAlgorithms.FLAG_SLEEP)

        csvWriter.close()

        if (!sleepFound && outputFile.exists()) {
            outputFile.delete()
        }

        Timber.tag("Archive Fragment").d("SleepQaAlgo run success result: $sleepFound")

        return sleepFound
    }

    private fun showSleepAlgoResultDialog(success: Boolean) {
        val message: String

        if (success) {
            message = "Algorithm finished successfully"
        } else {
            message = "Algorithm did not found a sleep state in the given data"
        }
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Sleep Algorithm Result")
        alertDialog.setMessage(message)
            .setPositiveButton("OK") { dialog, which ->
                dialog.dismiss()
            }

        alertDialog.show()
    }

    override fun onCompleted(isSuccessful: Boolean) {
        doAsync {
            uiThread {
                progressBar.visibility = View.GONE
                showSleepAlgoResultDialog(algoResult)
            }
        }
    }
}