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
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.sleep.SleepAlgorithmInitConfig
import com.maximintegrated.algorithms.sleep.SleepUserInfo
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import com.maximintegrated.maximsensorsapp.exts.addFragment
import com.maximintegrated.maximsensorsapp.sleep.database.entity.Sleep
import com.maximintegrated.maximsensorsapp.sleep.utils.postProcessing
import de.siegmar.fastcsv.reader.CsvReader
import de.siegmar.fastcsv.reader.CsvRow
import kotlinx.android.synthetic.main.fragment_archive.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Comparator


class ArchiveFragment : RecyclerViewClickListener, Fragment(),
    CsvWriter.Companion.CsvWriterListener {

    var algorithmInitConfig = AlgorithmInitConfig()

    private var algorithmResult = false

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
            "acc_magnitude",
            "phase_output_processed"
        )

        fun newInstance() = ArchiveFragment()
    }

    private val adapter: FileListAdapter by lazy { FileListAdapter(this) }
    private val algorithmOutput = AlgorithmOutput()
    private var sleepList: ArrayList<Sleep> = arrayListOf()

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
            algorithmResult = runSleepAlgorithm(file)
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

    private fun runSleepAlgorithm(file: File): Boolean {
        sleepList.clear()

        val oneHzFile = File(
            DataRecorder.OUTPUT_DIRECTORY,
            "${File.separator}1Hz${File.separator}${file.nameWithoutExtension}_1Hz.csv"
        )

        var sleepFound = false

        if (!OUTPUT_DIRECTORY.exists()) {
            OUTPUT_DIRECTORY.mkdirs()
        }

        val outputFile = File(OUTPUT_DIRECTORY, file.name)

        var hrSum = 0
        var hrCount = 0
        var restingHr = 0f

        if (oneHzFile.exists()) {
            val reader = CsvReader()
            reader.setContainsHeader(true)
            try {
                var parser = reader.parse(oneHzFile, StandardCharsets.UTF_8)
                var row: CsvRow? = parser.nextRow()
                while (row != null) {
                    if (row.fieldCount < 2) {
                        row = parser.nextRow()
                        continue
                    }
                    val hr = row.getField(1).toFloatOrNull()?.toInt()
                    if (hr != null && hr != 0) {
                        hrSum += hr
                        hrCount++
                    }
                    row = parser.nextRow()
                }

                if (hrCount != 0) {
                    restingHr = hrSum * 1f / hrCount
                }
                initSleepAlgorithm(restingHr)

                parser = reader.parse(file, StandardCharsets.UTF_8)
                row = parser.nextRow()
                while (row != null) {
                    val input = csvRowToAlgorithmInput(row)
                    sleepFound = runSleepAlgorithm(input, sleepFound)
                    row = parser.nextRow()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (file.exists()) {

            val inputs = readAlgorithmInputsFromFile(file)

            restingHr = inputs.filter { it.hr != 0 }.map { it.hr }.average().toFloat()

            initSleepAlgorithm(restingHr)

            for (input in inputs) {
                sleepFound = runSleepAlgorithm(input, sleepFound)
            }
            inputs.clear()
        }

        MaximAlgorithms.end(MaximAlgorithms.FLAG_SLEEP)

        if(sleepList.isNotEmpty() && sleepFound){
            postProcessing(sleepList)
            val csvWriter = CsvWriter.open(outputFile.absolutePath)
            csvWriter.listener = this

            for(sleep in sleepList){
                csvWriter.write(
                    SLEEP_TIMESTAMP_FORMAT.format(sleep.date),
                    sleep.isSleep,
                    sleep.latency,
                    sleep.sleepWakeOutput,
                    sleep.sleepPhasesReady,
                    sleep.sleepPhasesOutput,
                    sleep.hr,
                    sleep.ibi,
                    0,
                    sleep.accMag,
                    sleep.sleepPhasesOutputProcessed
                )
            }
            csvWriter.close()
        }else{
            onCompleted(false)
        }

        return sleepFound
    }

    private fun initSleepAlgorithm(restingHr: Float) {
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
    }

    private fun runSleepAlgorithm(input: AlgorithmInput?, sleepFound: Boolean): Boolean {
        var sleepState = sleepFound
        if (input != null) {
            val status = MaximAlgorithms.run(input, algorithmOutput)
            //TODO: check the sample code for this
            if (status) {
                with(algorithmOutput.sleep) {
                    if (outputDataArrayLength > 0) {
                        if (output.sleepWakeDetentionLatency >= 0) {
                            sleepState = true
                        } else {
                            output.sleepWakeDetentionLatency = -1
                        }
                        val sleep = Sleep(
                            id = 0,
                            sourceId = 0,
                            date = Date(dateInfo),
                            isSleep = output.sleepWakeDecisionStatus,
                            latency = output.sleepWakeDetentionLatency,
                            sleepWakeOutput = output.sleepWakeDecision,
                            sleepPhasesReady = output.sleepPhaseOutputStatus,
                            sleepPhasesOutput = output.sleepPhaseOutput,
                            hr = output.hr.toDouble(),
                            ibi = output.ibi.toDouble(),
                            spo2 = 0,
                            accMag = output.accMag.toDouble(),
                            sleepPhasesOutputProcessed = 0
                        )
                        sleepList.add(sleep)
                    }
                }

            }
        }
        return sleepState
    }

    private fun showSleepAlgorithmResultDialog(success: Boolean) {
        val message =  if (success) {
            getString(R.string.algorithm_finished_successfully)
        } else {
            getString(R.string.algorithm_not_found_sleep_state)
        }
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        alertDialog.setTitle(getString(R.string.sleep_algorithm_result))
        alertDialog.setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                dialog.dismiss()
            }

        alertDialog.show()
    }

    override fun onCompleted(isSuccessful: Boolean) {
        doAsync {
            uiThread {
                progressBar.visibility = View.GONE
                showSleepAlgorithmResultDialog(algorithmResult)
            }
        }
    }
}