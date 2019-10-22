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
import com.maximintegrated.algorithm_hrv.HrvAlgorithm
import com.maximintegrated.algorithm_hrv.HrvAlgorithmInitConfig
import com.maximintegrated.algorithm_hrv.HrvAlgorithmInput
import com.maximintegrated.algorithm_hrv.HrvAlgorithmOutput
import com.maximintegrated.algorithm_respiratory_rate.RespiratoryRateAlgorithm
import com.maximintegrated.algorithm_respiratory_rate.RespiratoryRateAlgorithmInitConfig
import com.maximintegrated.algorithm_respiratory_rate.RespiratoryRateAlgorithmInput
import com.maximintegrated.algorithm_respiratory_rate.RespiratoryRateAlgorithmOutput
import kotlinx.android.synthetic.main.fragment_offline_data.*

class OfflineDataFragment : Fragment() {

    private var hrvAlgorithmInitConfig: HrvAlgorithmInitConfig? = null
    private val hrvAlgorithmInput = HrvAlgorithmInput()
    private val hrvAlgorithmOutput = HrvAlgorithmOutput()

    private var respiratoryRateAlgorithmInitConfig: RespiratoryRateAlgorithmInitConfig? = null
    private val respiratoryRateAlgorithmInput = RespiratoryRateAlgorithmInput()
    private val respiratoryRateAlgorithmOutput = RespiratoryRateAlgorithmOutput()

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

        hrvAlgorithmInitConfig = HrvAlgorithmInitConfig(40f, 60, 15)
        HrvAlgorithm.init(hrvAlgorithmInitConfig)

        respiratoryRateAlgorithmInitConfig = RespiratoryRateAlgorithmInitConfig(
            RespiratoryRateAlgorithmInitConfig.SourceOptions.WRIST,
            RespiratoryRateAlgorithmInitConfig.LedCodes.GREEN,
            RespiratoryRateAlgorithmInitConfig.SamplingRateOption.Hz_25
        )

        RespiratoryRateAlgorithm.init(respiratoryRateAlgorithmInitConfig)

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
        val resultList: ArrayList<HrvOfflineChartData> = arrayListOf()

        for (data in offlineDataList) {
            hrvAlgorithmInput.ibi = data.rr
            hrvAlgorithmInput.ibiConfidence = data.rrConfidence
            hrvAlgorithmInput.isIbiValid = true

            HrvAlgorithm.run(hrvAlgorithmInput, hrvAlgorithmOutput)

            if (hrvAlgorithmOutput.isHrvCalculated) {
                resultList.add(
                    HrvOfflineChartData(
                        avnn = hrvAlgorithmOutput.timeDomainHrvMetrics.avnn,
                        sdnn = hrvAlgorithmOutput.timeDomainHrvMetrics.sdnn,
                        rmssd = hrvAlgorithmOutput.timeDomainHrvMetrics.rmssd,
                        pnn50 = hrvAlgorithmOutput.timeDomainHrvMetrics.pnn50,
                        ulf = hrvAlgorithmOutput.freqDomainHrvMetrics.ulf,
                        vlf = hrvAlgorithmOutput.freqDomainHrvMetrics.vlf,
                        lf = hrvAlgorithmOutput.freqDomainHrvMetrics.lf,
                        hf = hrvAlgorithmOutput.freqDomainHrvMetrics.hf,
                        lfOverHf = hrvAlgorithmOutput.freqDomainHrvMetrics.lfOverHf,
                        totPwr = hrvAlgorithmOutput.freqDomainHrvMetrics.totPwr
                    )
                )
            }

        }

        return resultList
    }


    private fun runRrAlgo(): List<Float> {
        val resultList: ArrayList<Float> = arrayListOf()

        for (data in offlineDataList) {
            respiratoryRateAlgorithmInput.ppg = data.green.toFloat()
            respiratoryRateAlgorithmInput.ibi = data.rr
            respiratoryRateAlgorithmInput.ibiConfidence = data.rrConfidence.toFloat()

            if (calculated == data.rr) {
                respiratoryRateAlgorithmInput.isIbiUpdateFlag = false
            } else {
                respiratoryRateAlgorithmInput.isIbiUpdateFlag = true;
                calculated = data.rr
            }

            respiratoryRateAlgorithmInput.isPpgUpdateFlag = true

            RespiratoryRateAlgorithm.run(
                respiratoryRateAlgorithmInput,
                respiratoryRateAlgorithmOutput
            )

            resultList.add(respiratoryRateAlgorithmOutput.respirationRate)
        }
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
