package com.maximintegrated.maximsensorsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig
import com.maximintegrated.algorithms.respiratory.RespiratoryRateAlgorithmInitConfig
import kotlinx.android.synthetic.main.fragment_offline_data.*

class OfflineDataFragment : Fragment() {

    private var algorithmInitConfig: AlgorithmInitConfig? = null
    private val algorithmInput = AlgorithmInput()
    private val algorithmOutput = AlgorithmOutput()

    var calculated: Float = 0f

    private val offlineDataList: ArrayList<OfflineDataModel>
        get() = arguments?.getParcelableArrayList(KEY_DATA_MODEL_LIST) ?: arrayListOf()

    companion object {
        private const val KEY_DATA_MODEL_LIST = "DataModelList"
        fun newInstance(offlineDataList: ArrayList<OfflineDataModel>): OfflineDataFragment {
            val fragment = OfflineDataFragment()
            fragment.arguments = Bundle().apply {
                putParcelableArrayList(KEY_DATA_MODEL_LIST, offlineDataList)
            }

            return fragment
        }
    }

    private val adapter: OfflineDataAdapter by lazy { OfflineDataAdapter() }

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
        algorithmInitConfig?.hrvConfig = HrvAlgorithmInitConfig(40f, 60, 15)
        algorithmInitConfig?.respConfig = RespiratoryRateAlgorithmInitConfig(
            RespiratoryRateAlgorithmInitConfig.SourceOptions.WRIST,
            RespiratoryRateAlgorithmInitConfig.LedCodes.GREEN,
            RespiratoryRateAlgorithmInitConfig.SamplingRateOption.Hz_25
        )

        initRecyclerView()

        val resultList = runHrvAlgo()


        val list: List<OfflineChartData> = arrayListOf(
            OfflineChartData(
                offlineDataList.mapIndexed { idx, it -> Pair(it.hr, idx.toFloat()) },
                "HR",
                ""
            ),
            OfflineChartData(
                offlineDataList.mapIndexed { idx, it -> Pair(it.spo2, idx.toFloat()) },
                "SpO2",
                ""
            ),
            OfflineChartData(
                offlineDataList.mapIndexed { idx, it -> Pair(it.rr, idx.toFloat()) },
                "IBI",
                ""
            ),
            OfflineChartData(offlineDataList.mapIndexed { idx, it ->
                Pair(
                    it.steps,
                    idx.toFloat()
                )
            }, "STEPS", ""),
            OfflineChartData(offlineDataList.mapIndexed { idx, it ->
                Pair(
                    it.motion,
                    idx.toFloat()
                )
            }, "MOTION", ""),

            OfflineChartData(runRrAlgo().mapIndexed { idx, it ->
                Pair(it, idx.toFloat())
            }, "RESPIRATION RATE", ""),

            OfflineChartData(resultList.mapIndexed { idx, it ->
                Pair(it.rmssd, idx.toFloat())
            }, "RMSSD", ""),

            OfflineChartData(resultList.mapIndexed { idx, it ->
                Pair(it.sdnn, idx.toFloat())
            }, "SDNN", ""),

            OfflineChartData(resultList.mapIndexed { idx, it ->
                Pair(it.avnn, idx.toFloat())
            }, "AVNN", ""),

            OfflineChartData(resultList.mapIndexed { idx, it ->
                Pair(it.pnn50, idx.toFloat())
            }, "PNN50", ""),

            OfflineChartData(resultList.mapIndexed { idx, it ->
                Pair(it.ulf, idx.toFloat())
            }, "ULF", ""),

            OfflineChartData(resultList.mapIndexed { idx, it ->
                Pair(it.vlf, idx.toFloat())
            }, "VLF", ""),

            OfflineChartData(resultList.mapIndexed { idx, it ->
                Pair(it.lf, idx.toFloat())
            }, "LF", ""),

            OfflineChartData(resultList.mapIndexed { idx, it ->
                Pair(it.hf, idx.toFloat())
            }, "HF", ""),

            OfflineChartData(resultList.mapIndexed { idx, it ->
                Pair(it.lfOverHf, idx.toFloat())
            }, "LFOVERHF", ""),

            OfflineChartData(resultList.mapIndexed { idx, it ->
                Pair(it.totPwr, idx.toFloat())
            }, "TOTPWR", "")


        )

        offlineDataList.clear()


        adapter.dataSetList = list
        adapter.notifyDataSetChanged()
    }

    private fun runHrvAlgo(): List<HrvOfflineChartData> {
        algorithmInitConfig?.enableAlgorithmsFlag = MaximAlgorithms.FLAG_HRV
        MaximAlgorithms.init(algorithmInitConfig)

        val resultList: ArrayList<HrvOfflineChartData> = arrayListOf()

        for (data in offlineDataList) {
            algorithmInput.rr = (data.rr * 1000f).toInt()
            algorithmInput.rrConfidence = data.rrConfidence

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


    private fun runRrAlgo(): List<Float> {
        algorithmInitConfig?.enableAlgorithmsFlag = MaximAlgorithms.FLAG_RESP
        MaximAlgorithms.init(algorithmInitConfig)

        val resultList: ArrayList<Float> = arrayListOf()

        for (data in offlineDataList) {
            algorithmInput.green = data.green.toInt()
            algorithmInput.rr = (data.rr * 1000f).toInt()
            algorithmInput.rrConfidence = data.rrConfidence

            MaximAlgorithms.run(
                algorithmInput,
                algorithmOutput
            )

            resultList.add(algorithmOutput.respiratory.respirationRate)
        }

        MaximAlgorithms.end(MaximAlgorithms.FLAG_RESP)

        return resultList
    }

    private fun initRecyclerView() {
        chartRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(chartRecyclerView)
        chartRecyclerView.adapter = adapter
    }
}
