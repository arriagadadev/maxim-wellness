package com.maximintegrated.maximsensorsapp.respiratory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.respiratory.RespiratoryRateAlgorithmInitConfig
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.set
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import com.maximintegrated.maximsensorsapp.view.FloatValueFormatter
import com.maximintegrated.maximsensorsapp.view.MultiChannelChartView
import kotlinx.android.synthetic.main.include_respiratory_fragment_content.*
import kotlinx.android.synthetic.main.view_multi_channel_chart.view.titleView
import kotlinx.android.synthetic.main.view_result_card.view.*
import java.text.DecimalFormat

class RespiratoryFragment : MeasurementBaseFragment() {

    companion object {
        fun newInstance() = RespiratoryFragment()
    }

    private lateinit var chartView: MultiChannelChartView

    private var algorithmInitConfig: AlgorithmInitConfig? = null
    private val algorithmInput = AlgorithmInput()
    private val algorithmOutput = AlgorithmOutput()

    private val decimalFormat = DecimalFormat("0.0")

    private var respiration: Float? = null
        set(value) {
            field = value
            if (value != null) {
                respirationResultView.emptyValue = decimalFormat.format(value)
            } else {
                respirationResultView.emptyValue = ResultCardView.EMPTY_VALUE
            }
        }

    private var scd: Int? = null
        set(value) {
            field = value
            if (value != null) {
                respirationResultView.scdStateTextView.text = Scd.values()[value].displayName
            } else {
                respirationResultView.scdStateTextView.text = Scd.NO_DECISION.displayName
            }
        }

    /*private var respirationConfidence: Int? = null
        set(value) {
            field = value
            if (value != null) {
                respirationResultView.confidenceProgressBar.progress = value
            } else {
                respirationResultView.confidenceProgressBar.progress = 0
            }
        }*/

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_respiratory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartView = view.findViewById(R.id.chart_view)

        algorithmInitConfig = AlgorithmInitConfig()
        algorithmInitConfig?.respConfig = RespiratoryRateAlgorithmInitConfig(
            RespiratoryRateAlgorithmInitConfig.SourceOptions.WRIST,
            RespiratoryRateAlgorithmInitConfig.LedCodes.GREEN,
            RespiratoryRateAlgorithmInitConfig.SamplingRateOption.Hz_25
        )
        algorithmInitConfig?.enableAlgorithmsFlag = MaximAlgorithms.FLAG_RESP

        setupChart()
        setupToolbar(getString(R.string.respiratory))
    }

    private fun setupChart() {
        chartView.dataSetInfoList = listOf(
            DataSetInfo(R.string.respiration_rate, R.color.channel_red)
        )

        chartView.titleView.text = getString(R.string.respiration_rate)
        chartView.maximumEntryCount = 4500
        chartView.setFormatterForYAxis(FloatValueFormatter(decimalFormat))
    }

    override fun addStreamData(streamData: HspStreamData) {

        dataRecorder?.record(streamData)

        algorithmInput.set(streamData)

        if (MaximAlgorithms.run(algorithmInput, algorithmOutput)) {
            if (algorithmOutput.respiratory.respirationRate < 0.01f) {
                algorithmOutput.respiratory.respirationRate = 0f
            }
            chartView.addData(algorithmOutput.respiratory.respirationRate)
            respiration = algorithmOutput.respiratory.respirationRate
            notificationResults[MXM_KEY] =
                "Resp. rate: ${decimalFormat.format(respiration)} breath/min"
            updateNotification()
        }

        scd = streamData.scdState
    }

    override fun getMeasurementType(): String {
        return "Respiration_Rate"
    }

    override fun startMonitoring() {
        super.startMonitoring()

        clearChart()
        clearCardViewValues()

        MaximAlgorithms.init(algorithmInitConfig)

        hspViewModel.isDeviceSupported
            .observe(this) {
                sendDefaultSettings()
                sendAlgoMode()
                sendLogToFlashCommand()
                hspViewModel.startStreaming()
            }
    }

    override fun stopMonitoring() {
        super.stopMonitoring()

        MaximAlgorithms.end(MaximAlgorithms.FLAG_RESP)

        hspViewModel.stopStreaming()
    }

    override fun dataLoggingToggled() {

    }

    override fun showSettingsDialog() {

    }

    override fun showInfoDialog() {
        val helpDialog =
            HelpDialog.newInstance(getString(R.string.resp_info), getString(R.string.info))
        fragmentManager?.let { helpDialog.show(it, "helpDialog") }
    }

    private fun clearChart() {
        chartView.clearChart()
    }

    private fun clearCardViewValues() {
        respiration = null
        scd = null
    }
}