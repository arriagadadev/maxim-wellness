package com.maximintegrated.maximsensorsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig
import com.maximintegrated.algorithms.respiratory.RespiratoryRateAlgorithmInitConfig
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import kotlinx.android.synthetic.main.fragment_offline_data.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.io.File
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

enum class HrAlignment {
    ALIGNMENT_FAIL,
    ALIGNMENT_NO_REF_DEVICE,
    ALIGNMENT_SUCCESSFUL
}

class OfflineDataFragment : Fragment(), AdapterView.OnItemSelectedListener,
    CsvWriter.Companion.CsvWriterListener {

    private var algorithmInitConfig: AlgorithmInitConfig? = null
    private val algorithmOutput = AlgorithmOutput()

    var calculated: Float = 0f

    private var logFile: File? = null

    private var refHrFile: File? = null

    private var oneHzFile: File? = null

    private var alignFile: File? = null

    private var offlineDataList: ArrayList<AlgorithmInput> = arrayListOf()

    private var respResults: List<Pair<Long, Float>> = arrayListOf()

    private var hrvResults: List<Pair<Long, HrvOfflineChartData>> = arrayListOf()

    private var stressResults: List<Pair<Long, Int>> = arrayListOf()

    companion object {
        const val RESP_INDEX = 6
        const val RMSSD_INDEX = 7
        const val SDNN_INDEX = 8
        const val AVNN_INDEX = 9
        const val PNN50_INDEX = 10
        const val ULF_INDEX = 11
        const val VLF_INDEX = 12
        const val LF_INDEX = 13
        const val HF_INDEX = 14
        const val LF_HF_INDEX = 15
        const val TOTPWR_INDEX = 16
        const val STRESS_INDEX = 17

        fun newInstance(logFile: File): OfflineDataFragment {
            val fragment = OfflineDataFragment()
            fragment.logFile = logFile
            val refPath = File(
                DataRecorder.OUTPUT_DIRECTORY,
                "${File.separator}REFERENCE_DEVICE${File.separator}${logFile.nameWithoutExtension}_reference_device.csv"
            ).absolutePath
            val oneHzPath = File(
                DataRecorder.OUTPUT_DIRECTORY,
                "${File.separator}1Hz${File.separator}${logFile.nameWithoutExtension}_1Hz.csv"
            ).absolutePath
            val alignPath = File(
                DataRecorder.OUTPUT_DIRECTORY,
                "${File.separator}ALIGNED${File.separator}${logFile.nameWithoutExtension}_aligned.csv"
            ).absolutePath
            fragment.refHrFile = if (File(refPath).exists()) File(refPath) else null
            fragment.oneHzFile = if (File(oneHzPath).exists()) File(oneHzPath) else null
            fragment.alignFile = if (File(alignPath).exists()) File(alignPath) else null
            return fragment
        }
    }

    private var hrAlignment = HrAlignment.ALIGNMENT_NO_REF_DEVICE

    private var csvWriter: CsvWriter? = null
    //private val adapter: OfflineDataAdapter by lazy { OfflineDataAdapter() }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_offline_data, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.clear()

        algorithmInitConfig = AlgorithmInitConfig()
        algorithmInitConfig?.respConfig = RespiratoryRateAlgorithmInitConfig(
            RespiratoryRateAlgorithmInitConfig.SourceOptions.WRIST,
            RespiratoryRateAlgorithmInitConfig.LedCodes.GREEN,
            RespiratoryRateAlgorithmInitConfig.SamplingRateOption.Hz_25
        )

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.chart_titles,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            chartSpinner.adapter = adapter
        }

        chartSpinner.onItemSelectedListener = this

        csvExportButton.setOnClickListener {
            val index = chartSpinner.selectedItemPosition
            var file: File?
            when (index) {
                RESP_INDEX -> {
                    file = getOutputFile("resp_out")
                    when {
                        file.exists() -> {
                            Toast.makeText(requireContext(), getString(R.string.file_already_exists), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        respResults.isEmpty() -> {
                            Toast.makeText(requireContext(), getString(R.string.no_data_found), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        else -> {
                            csvWriter = CsvWriter.open(file.absolutePath)
                            csvWriter?.listener = this@OfflineDataFragment
                        }
                    }
                }
                in RMSSD_INDEX..TOTPWR_INDEX -> {
                    file = getOutputFile("hrv_out")
                    when {
                        file.exists() -> {
                            Toast.makeText(requireContext(), getString(R.string.file_already_exists), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        hrvResults.isEmpty() -> {
                            Toast.makeText(requireContext(), getString(R.string.no_data_found), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        else -> {
                            csvWriter = CsvWriter.open(file.absolutePath)
                            csvWriter?.listener = this@OfflineDataFragment
                        }
                    }
                }
                STRESS_INDEX -> {
                    file = getOutputFile("stress_out")
                    when {
                        file.exists() -> {
                            Toast.makeText(requireContext(), getString(R.string.file_already_exists), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        stressResults.isEmpty() -> {
                            Toast.makeText(requireContext(), getString(R.string.no_data_found), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        else -> {
                            csvWriter = CsvWriter.open(file.absolutePath)
                            csvWriter?.listener = this@OfflineDataFragment
                        }
                    }

                }
            }
            progressBar.visibility = View.VISIBLE
            doAsync {
                when (index) {
                    RESP_INDEX -> {
                        csvWriter?.write("timestamp", "Respiration Rate")
                        respResults.forEach {
                            csvWriter?.write(it.first, "%.2f".format(it.second))
                        }
                        csvWriter?.close()
                    }
                    in RMSSD_INDEX..TOTPWR_INDEX -> {
                        csvWriter?.write("timestamp", "AVNN", "SDNN", "RMSSD", "PNN50",
                            "ULF", "VLF", "LF", "HF", "LF/HF", "TOT_PWR")
                        hrvResults.forEach {
                            csvWriter?.write(it.first, it.second.avnn, it.second.sdnn,
                                it.second.rmssd, it.second.pnn50, it.second.ulf, it.second.vlf,
                                it.second.lf, it.second.hf, it.second.lfOverHf, it.second.totPwr)
                        }
                        csvWriter?.close()
                    }
                    STRESS_INDEX -> {
                        csvWriter?.write("timestamp", "Stress Score")
                        stressResults.forEach {
                            csvWriter?.write(it.first, it.second)
                        }
                        csvWriter?.close()
                    }
                }
                uiThread {
                    progressBar.visibility = View.GONE
                }
            }
        }

        progressBar.visibility = View.VISIBLE
        doAsync {

            offlineDataList = readAlgorithmInputsFromFile(logFile)

            val alignedDataList = readTimeStampAndHrFromAlignedFile(alignFile)

            hrvResults = runHrvAlgo()

            respResults = runRrAlgo()

            stressResults = runStressAlgo()

            //var hrTimestampStart = 0L

            if (alignedDataList.isNotEmpty()) {
                hrAlignment = HrAlignment.ALIGNMENT_SUCCESSFUL
                //hrTimestampStart = alignedDataList[0].first
                offlineChart.put(
                    1, OfflineChartData(
                        alignedDataList.mapIndexed { idx, it ->
                            Entry(
                                idx.toFloat(),
                                it.second.toFloat()
                            )
                        },
                        "HR (bpm)",
                        "Maxim"
                    )
                )
                offlineChart.put(
                    1, OfflineChartData(
                        alignedDataList.mapIndexed { idx, it ->
                            Entry(
                                idx.toFloat(),
                                it.third.toFloat()
                            )
                        },
                        "HR (bpm)",
                        "Ref"
                    )
                )
                alignedDataList.clear()
            } else {
                val oneHzDataList = readTimeStampAndHrFrom1HzFile(oneHzFile)

                if(oneHzDataList.isEmpty()){
                    offlineChart.put(
                        1, OfflineChartData(
                            offlineDataList.filterIndexed { index, it ->  index % 25 == 0}
                                .mapIndexed { idx, it -> Entry(idx.toFloat(), it.hr.toFloat()) },
                            "HR (bpm)",
                            "Maxim"
                        )
                    )
                }else{
                    val refDataList = readTimeStampAndHrFromReferenceFile(refHrFile)
                    offlineChart.put(
                        1, OfflineChartData(
                            oneHzDataList.mapIndexed { idx, it ->
                                Entry(
                                    idx.toFloat(),
                                    it.second.toFloat()
                                )
                            },
                            "HR (bpm)",
                            "Maxim"
                        )
                    )
                    if (refDataList.isNotEmpty()) {
                        //hrTimestampStart = min(hrTimestampStart, refDataList[0].first)
                        hrAlignment = HrAlignment.ALIGNMENT_FAIL
                        offlineChart.put(
                            1, OfflineChartData(
                                refDataList.mapIndexed { idx, it ->
                                    Entry(
                                        idx.toFloat(),
                                        it.second.toFloat()
                                    )
                                },
                                "HR (bpm)",
                                "Ref"
                            )
                        )
                        refDataList.clear()
                    } else {
                        hrAlignment = HrAlignment.ALIGNMENT_NO_REF_DEVICE
                    }
                    oneHzDataList.clear()
                }
            }

            offlineChart.put(
                2, OfflineChartData(
                    offlineDataList.filterIndexed { index, it ->  index % 25 == 0}
                        .mapIndexed { idx, it -> Entry(idx.toFloat(), it.spo2 / 10f) },
                    "SpO2 (%)",
                    "Maxim"
                )
            )

            offlineChart.put(
                3, OfflineChartData(
                    offlineDataList.filterIndexed { index, it ->  it.rr != 0}
                        .mapIndexed { idx, it -> Entry(idx.toFloat(), it.rr / 10f) },
                    "IBI (ms)",
                    "Maxim"
                )
            )

            offlineChart.put(
                4, OfflineChartData(
                    offlineDataList.filterIndexed { index, it ->  index % 25 == 0}
                        .mapIndexed { idx, it ->
                        Entry(
                            idx.toFloat(),
                            (it.walkSteps + it.runSteps).toFloat()
                        )
                    },
                    "STEPS",
                    "Maxim"
                )
            )

            offlineChart.put(
                5, OfflineChartData(
                    offlineDataList.filterIndexed { index, it ->  index % 25 == 0}
                        .mapIndexed { idx, it ->
                        Entry(
                            idx.toFloat(), sqrt(
                                (it.accelerationX / 1000f).pow(2) + (it.accelerationY / 1000f).pow(2) +
                                        (it.accelerationZ / 1000f).pow(2)
                            )
                        )
                    },
                    "MOTION (g)",
                    "Maxim"
                )
            )

            offlineDataList.clear()

            offlineChart.put(
                RESP_INDEX, OfflineChartData(
                    respResults.mapIndexed { idx, it -> Entry(idx.toFloat(), it.second) },
                    "RESPIRATION RATE \n(breath/min)",
                    "Maxim"
                )
            )

            offlineChart.put(
                RMSSD_INDEX, OfflineChartData(
                    hrvResults.mapIndexed { idx, it -> Entry(idx.toFloat(), it.second.rmssd) },
                    "RMSSD (ms)",
                    "Maxim"
                )
            )

            offlineChart.put(
                SDNN_INDEX, OfflineChartData(
                    hrvResults.mapIndexed { idx, it -> Entry(idx.toFloat(), it.second.sdnn) },
                    "SDNN (ms)",
                    "Maxim"
                )
            )

            offlineChart.put(
                AVNN_INDEX, OfflineChartData(
                    hrvResults.mapIndexed { idx, it -> Entry(idx.toFloat(), it.second.avnn) },
                    "AVNN (ms)",
                    "Maxim"
                )
            )

            offlineChart.put(
                PNN50_INDEX, OfflineChartData(
                    hrvResults.mapIndexed { idx, it -> Entry(idx.toFloat(), it.second.pnn50) },
                    "PNN50 (ms)",
                    "Maxim"
                )
            )

            offlineChart.put(
                ULF_INDEX, OfflineChartData(
                    hrvResults.mapIndexed { idx, it -> Entry(idx.toFloat(), it.second.ulf) },
                    "ULF (ms²)",
                    "Maxim"
                )
            )

            offlineChart.put(
                VLF_INDEX, OfflineChartData(
                    hrvResults.mapIndexed { idx, it -> Entry(idx.toFloat(), it.second.vlf) },
                    "VLF (ms²)",
                    "Maxim"
                )
            )

            offlineChart.put(
                LF_INDEX, OfflineChartData(
                    hrvResults.mapIndexed { idx, it -> Entry(idx.toFloat(), it.second.lf) },
                    "LF (ms²)",
                    "Maxim"
                )
            )

            offlineChart.put(
                HF_INDEX, OfflineChartData(
                    hrvResults.mapIndexed { idx, it -> Entry(idx.toFloat(), it.second.hf) },
                    "HF (ms²)",
                    "Maxim"
                )
            )

            offlineChart.put(
                LF_HF_INDEX, OfflineChartData(
                    hrvResults.mapIndexed { idx, it -> Entry(idx.toFloat(), it.second.lfOverHf) },
                    "LF/HF",
                    "Maxim"
                )
            )

            offlineChart.put(
                TOTPWR_INDEX, OfflineChartData(
                    hrvResults.mapIndexed { idx, it -> Entry(idx.toFloat(), it.second.totPwr) },
                    "TOTAL POWER (ms²)",
                    "Maxim"
                )
            )

            offlineChart.put(
                STRESS_INDEX, OfflineChartData(
                    stressResults.mapIndexed { idx, it ->
                        Entry(
                            idx.toFloat(),
                            it.second.toFloat()
                        )
                    },
                    "STRESS SCORE",
                    "Maxim"
                )
            )

            //resultList.clear()

            uiThread {
                progressBar.visibility = View.GONE
                chartSpinner.setSelection(1)
            }
        }
    }

    private fun runHrvAlgo(): ArrayList<Pair<Long, HrvOfflineChartData>> {
        algorithmInitConfig?.hrvConfig = HrvAlgorithmInitConfig(40f, 60, 15)
        algorithmInitConfig?.enableAlgorithmsFlag = MaximAlgorithms.FLAG_HRV
        MaximAlgorithms.init(algorithmInitConfig)

        val resultList: ArrayList<Pair<Long, HrvOfflineChartData>> = arrayListOf()

        for (algorithmInput in offlineDataList) {
            MaximAlgorithms.run(algorithmInput, algorithmOutput)

            if (algorithmOutput.hrv.isHrvCalculated) {
                resultList.add(
                    Pair(
                        algorithmInput.timestamp,
                        HrvOfflineChartData(
                            avnn = algorithmOutput.hrv.timeDomainHrvMetrics.avnn,
                            sdnn = algorithmOutput.hrv.timeDomainHrvMetrics.sdnn,
                            rmssd = algorithmOutput.hrv.timeDomainHrvMetrics.rmssd,
                            pnn50 = algorithmOutput.hrv.timeDomainHrvMetrics.pnn50,
                            ulf = algorithmOutput.hrv.freqDomainHrvMetrics.ulf,
                            vlf = algorithmOutput.hrv.freqDomainHrvMetrics.vlf,
                            lf = algorithmOutput.hrv.freqDomainHrvMetrics.lf,
                            hf = algorithmOutput.hrv.freqDomainHrvMetrics.hf,
                            lfOverHf = algorithmOutput.hrv.freqDomainHrvMetrics.lfOverHf,
                            totPwr = algorithmOutput.hrv.freqDomainHrvMetrics.totPwr
                        )
                    )
                )
            }

        }

        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV)

        return resultList
    }

    private fun runStressAlgo(): List<Pair<Long, Int>> {
        algorithmInitConfig?.hrvConfig = HrvAlgorithmInitConfig(40f, 90, 30)
        algorithmInitConfig?.enableAlgorithmsFlag =
            MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_STRESS
        MaximAlgorithms.init(algorithmInitConfig)

        val resultList: ArrayList<Pair<Long, Int>> = arrayListOf()

        for (algorithmInput in offlineDataList) {
            if (MaximAlgorithms.run(algorithmInput, algorithmOutput)) {
                Timber.d("Stress score = ${algorithmOutput.stress.stressScore}")
                resultList.add(Pair(algorithmInput.timestamp, algorithmOutput.stress.stressScore))
            }
        }

        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_STRESS)

        return resultList
    }


    private fun runRrAlgo(): List<Pair<Long, Float>> {
        algorithmInitConfig?.enableAlgorithmsFlag = MaximAlgorithms.FLAG_RESP
        MaximAlgorithms.init(algorithmInitConfig)

        val resultList: ArrayList<Pair<Long, Float>> = arrayListOf()

        for ((index, algorithmInput) in offlineDataList.withIndex()) {
            MaximAlgorithms.run(
                algorithmInput,
                algorithmOutput
            )
            if(index % 25 == 0){
                resultList.add(
                    Pair(
                        algorithmInput.timestamp,
                        algorithmOutput.respiratory.respirationRate
                    )
                )
            }
        }

        MaximAlgorithms.end(MaximAlgorithms.FLAG_RESP)

        return resultList
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (position != 0) {
            offlineChart.display(position)
            if (position != 1) { //HR
                warningMessageView.visibility = View.GONE
            } else {
                when (hrAlignment) {
                    HrAlignment.ALIGNMENT_SUCCESSFUL -> {
                        warningMessageView.text = getString(R.string.alignment_successful)
                        warningMessageView.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_check,
                            0,
                            0,
                            0
                        )
                        warningMessageView.visibility = View.VISIBLE
                    }
                    HrAlignment.ALIGNMENT_FAIL -> {
                        warningMessageView.text = getString(R.string.alignment_fail)
                        warningMessageView.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_warning,
                            0,
                            0,
                            0
                        )
                        warningMessageView.visibility = View.VISIBLE
                    }
                    else -> warningMessageView.visibility = View.GONE
                }
            }

            if (position in RESP_INDEX..STRESS_INDEX) {
                csvExportButton.visibility = View.VISIBLE
            } else {
                csvExportButton.visibility = View.GONE
            }
        } else {
            csvExportButton.visibility = View.GONE
        }
    }

    private fun getOutputFile(suffix: String): File {
        return File(
            DataRecorder.OUTPUT_DIRECTORY,
            "${File.separator}OUTPUT${File.separator}${logFile?.nameWithoutExtension}_$suffix.csv"
        )
    }

    override fun onCompleted(isSuccessful: Boolean) {
        doAsync {
            uiThread {
                Toast.makeText(
                    requireContext(),
                    "Saved to ${csvWriter?.filePath}",
                    Toast.LENGTH_LONG
                ).show()
                csvWriter = null
            }
        }
    }
}
