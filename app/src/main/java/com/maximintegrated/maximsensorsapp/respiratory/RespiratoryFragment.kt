package com.maximintegrated.maximsensorsapp.respiratory

import android.os.Bundle
import android.os.SystemClock
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
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.set
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import com.maximintegrated.maximsensorsapp.view.FloatValueFormatter
import com.maximintegrated.maximsensorsapp.view.MultiChannelChartView
import kotlinx.android.synthetic.main.include_respiratory_fragment_content.*
import kotlinx.android.synthetic.main.view_multi_channel_chart.view.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class RespiratoryFragment : MeasurementBaseFragment() {

    companion object {
        fun newInstance() = RespiratoryFragment()
    }

    private var startTime: String? = null

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

        initializeChronometer()
        setupChart()
        setupToolbar(getString(R.string.respiratory))
    }

    override fun initializeChronometer() {

        respirationChronometer.format =
            "Start Time ${startTime ?: ResultCardView.EMPTY_VALUE} 00:%s"
        respirationChronometer.setOnChronometerTickListener { cArg ->
            val elapsedMillis = SystemClock.elapsedRealtime() - cArg.base
            if (elapsedMillis > 3600000L) {
                cArg.format = "Start Time ${startTime ?: ResultCardView.EMPTY_VALUE} 0%s"
            } else {
                cArg.format = "Start Time ${startTime ?: ResultCardView.EMPTY_VALUE} 00:%s"
            }
        }
    }

    private fun setupChart() {
        chartView.dataSetInfoList = listOf(
            DataSetInfo(R.string.channel_red, R.color.channel_red)
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
        }
    }

    override fun sendDefaultSettings() {
        hspViewModel.sendCommand(
            SetConfigurationCommand(
                "wearablesuite",
                "scdenable",
                if (menuItemEnabledScd.isChecked) "1" else "0"
            )
        )
    }

    override fun startMonitoring() {
        super.startMonitoring()
        isMonitoring = true
        menuItemEnabledScd.isEnabled = true
        menuItemLogToFlash.isEnabled = true
        dataRecorder = DataRecorder("Respiration_Rate")

        clearChart()
        clearCardViewValues()

        startTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        respirationChronometer.base = SystemClock.elapsedRealtime()
        respirationChronometer.start()

        MaximAlgorithms.init(algorithmInitConfig)

        hspViewModel.isDeviceSupported
            .observe(this) {
                sendDefaultSettings()
                sendLogToFlashCommand()
                hspViewModel.startStreaming()
            }
    }

    override fun stopMonitoring() {
        super.stopMonitoring()
        isMonitoring = false
        menuItemEnabledScd.isEnabled = false
        menuItemLogToFlash.isEnabled = false

        dataRecorder?.close()
        dataRecorder = null

        startTime = null
        respirationChronometer.stop()

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
    }
}