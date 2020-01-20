package com.maximintegrated.maximsensorsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig
import com.maximintegrated.algorithms.respiratory.RespiratoryRateAlgorithmInitConfig
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

class OfflineDataFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var algorithmInitConfig: AlgorithmInitConfig? = null
    private val algorithmOutput = AlgorithmOutput()

    var calculated: Float = 0f

    private var logFile: File? = null

    private var refHrFile: File? = null

    private var oneHzFile: File? = null

    private var alignFile: File? = null

    private var offlineDataList: ArrayList<AlgorithmInput> = arrayListOf()

    companion object {
        fun newInstance(logFile: File): OfflineDataFragment {
            val fragment = OfflineDataFragment()
            fragment.logFile = logFile
            var refPath = File(
                DataRecorder.OUTPUT_DIRECTORY,
                "/REFERENCE_DEVICE/${logFile.nameWithoutExtension}_reference_device.csv"
            ).absolutePath
            var oneHzPath = File(
                DataRecorder.OUTPUT_DIRECTORY,
                "/1Hz/${logFile.nameWithoutExtension}_1Hz.csv"
            ).absolutePath
            var alignPath = File(
                DataRecorder.OUTPUT_DIRECTORY,
                "/ALIGNED/${logFile.nameWithoutExtension}_aligned.csv"
            ).absolutePath
            fragment.refHrFile = if (File(refPath).exists()) File(refPath) else null
            fragment.oneHzFile = if (File(oneHzPath).exists()) File(oneHzPath) else null
            fragment.alignFile = if (File(alignPath).exists()) File(alignPath) else null
            return fragment
        }
    }

    private var hrAlignment = HrAlignment.ALIGNMENT_NO_REF_DEVICE

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

        progressBar.visibility = View.VISIBLE
        doAsync {

            offlineDataList = readAlgorithmInputsFromFile(logFile)

            val alignedDataList = readTimeStampAndHrFromAlignedFile(alignFile)

            val resultList = runHrvAlgo()

            var hrTimestampStart = 0L

            if (alignedDataList.isNotEmpty()) {
                hrAlignment = HrAlignment.ALIGNMENT_SUCCESSFUL
                hrTimestampStart = alignedDataList[0].first
                offlineChart.put(
                    1, OfflineChartData(
                        alignedDataList.map {
                            Entry(
                                (it.first - hrTimestampStart).toFloat(),
                                it.second.toFloat()
                            )
                        },
                        "HR (bpm)",
                        "Maxim"
                    )
                )
                offlineChart.put(
                    1, OfflineChartData(
                        alignedDataList.map {
                            Entry(
                                (it.first - hrTimestampStart).toFloat(),
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

                val refDataList = readTimeStampAndHrFromReferenceFile(refHrFile)

                if (oneHzDataList.isNotEmpty()) {
                    hrTimestampStart = oneHzDataList[0].first
                }

                if (refDataList.isNotEmpty()) {
                    hrTimestampStart = min(hrTimestampStart, refDataList[0].first)
                    hrAlignment = HrAlignment.ALIGNMENT_FAIL
                } else {
                    hrAlignment = HrAlignment.ALIGNMENT_NO_REF_DEVICE
                }

                offlineChart.put(
                    1, OfflineChartData(
                        oneHzDataList.map {
                            Entry(
                                (it.first - hrTimestampStart).toFloat(),
                                it.second.toFloat()
                            )
                        },
                        "HR (bpm)",
                        "Maxim"
                    )
                )

                if (refDataList.isNotEmpty()) {
                    offlineChart.put(
                        1, OfflineChartData(
                            refDataList.map {
                                Entry(
                                    (it.first - hrTimestampStart).toFloat(),
                                    it.second.toFloat()
                                )
                            },
                            "HR (bpm)",
                            "Ref"
                        )
                    )
                    refDataList.clear()
                }

                oneHzDataList.clear()
            }

            offlineChart.put(2, OfflineChartData(
                    offlineDataList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.spo2 / 10f) },
                    "SpO2 (%)",
                    "Maxim"
                )
            )

            offlineChart.put(3, OfflineChartData(
                offlineDataList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.rr / 10f) },
                "IBI (ms)",
                "Maxim"
            ))

            offlineChart.put(4,  OfflineChartData(
                offlineDataList.mapIndexed { idx, it ->
                    Entry(
                        idx.toFloat(),
                        (it.walkSteps + it.runSteps).toFloat()
                    )
                },
                "STEPS",
                "Maxim"
            ))

            offlineChart.put(5,  OfflineChartData(
                offlineDataList.mapIndexed { idx, it ->
                    Entry(
                        idx.toFloat(), sqrt(
                            (it.accelerationX / 1000f).pow(2) + (it.accelerationY / 1000f).pow(2) +
                                    (it.accelerationZ / 1000f).pow(2)
                        )
                    )
                },
                "MOTION (g)",
                "Maxim"
            ))

            offlineChart.put(6,  OfflineChartData(
                runRrAlgo().mapIndexed { idx, it -> Entry(idx.toFloat(), it) },
                "RESPIRATION RATE \n(breath/min)",
                "Maxim"
            ))

            offlineChart.put(7,  OfflineChartData(
                resultList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.rmssd) },
                "RMSSD (ms)",
                "Maxim"
            ))

            offlineChart.put(8,  OfflineChartData(
                resultList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.sdnn) },
                "SDNN (ms)",
                "Maxim"
            ))

            offlineChart.put(9,  OfflineChartData(
                resultList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.avnn) },
                "AVNN (ms)",
                "Maxim"
            ))

            offlineChart.put(10,  OfflineChartData(
                resultList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.pnn50) },
                "PNN50 (ms)",
                "Maxim"
            ))

            offlineChart.put(11,  OfflineChartData(
                resultList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.ulf) },
                "ULF (ms²)",
                "Maxim"
            ))

            offlineChart.put(12,  OfflineChartData(
                resultList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.vlf) },
                "VLF (ms²)",
                "Maxim"
            ))

            offlineChart.put(13,  OfflineChartData(
                resultList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.lf) },
                "LF (ms²)",
                "Maxim"
            ))

            offlineChart.put(14,  OfflineChartData(
                resultList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.hf) },
                "HF (ms²)",
                "Maxim"
            ))

            offlineChart.put(15,  OfflineChartData(
                resultList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.lfOverHf) },
                "LF/HF",
                "Maxim"
            ))

            offlineChart.put(16,  OfflineChartData(
                resultList.mapIndexed { idx, it -> Entry(idx.toFloat(), it.totPwr) },
                "TOTAL POWER (ms²)",
                "Maxim"
            ))

            offlineChart.put(17,  OfflineChartData(
                runStressAlgo().mapIndexed { idx, it -> Entry(idx.toFloat(), it.toFloat()) },
                "STRESS SCORE",
                "Maxim"
            ))

            resultList.clear()

            uiThread {
                progressBar.visibility = View.GONE
                chartSpinner.setSelection(1)
            }
        }
    }

    private fun runHrvAlgo(): ArrayList<HrvOfflineChartData> {
        algorithmInitConfig?.hrvConfig = HrvAlgorithmInitConfig(40f, 60, 15)
        algorithmInitConfig?.enableAlgorithmsFlag = MaximAlgorithms.FLAG_HRV
        MaximAlgorithms.init(algorithmInitConfig)

        val resultList: ArrayList<HrvOfflineChartData> = arrayListOf()

        for (algorithmInput in offlineDataList) {
            MaximAlgorithms.run(algorithmInput, algorithmOutput)

            if (algorithmOutput.hrv.isHrvCalculated) {
                resultList.add(
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
            }

        }

        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV)

        return resultList
    }

    private fun runStressAlgo(): List<Int> {
        algorithmInitConfig?.hrvConfig = HrvAlgorithmInitConfig(40f, 90, 30)
        algorithmInitConfig?.enableAlgorithmsFlag =
            MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_STRESS
        MaximAlgorithms.init(algorithmInitConfig)

        val resultList: ArrayList<Int> = arrayListOf()

        for (algorithmInput in offlineDataList) {
            if (MaximAlgorithms.run(algorithmInput, algorithmOutput)) {
                Timber.d("Stress score = ${algorithmOutput.stress.stressScore}")
                resultList.add(algorithmOutput.stress.stressScore)
            }
        }

        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_STRESS)

        return resultList
    }


    private fun runRrAlgo(): List<Float> {
        algorithmInitConfig?.enableAlgorithmsFlag = MaximAlgorithms.FLAG_RESP
        MaximAlgorithms.init(algorithmInitConfig)

        val resultList: ArrayList<Float> = arrayListOf()

        for (algorithmInput in offlineDataList) {
            MaximAlgorithms.run(
                algorithmInput,
                algorithmOutput
            )

            resultList.add(algorithmOutput.respiratory.respirationRate)
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
        }
    }
}
