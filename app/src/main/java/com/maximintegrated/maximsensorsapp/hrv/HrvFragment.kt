package com.maximintegrated.maximsensorsapp.hrv

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.algorithm_hrv.*
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.maximsensorsapp.BleConnectionInfo
import com.maximintegrated.maximsensorsapp.DataRecorder
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.ResultCardView
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import com.maximintegrated.maximsensorsapp.view.MultiChannelChartView
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_hrv_fragment_content.*
import kotlinx.android.synthetic.main.view_multi_channel_chart.view.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class HrvFragment : Fragment() {
    companion object {
        fun newInstance() = HrvFragment()
    }

    private lateinit var hspViewModel: HspViewModel

    private lateinit var menuItemStartMonitoring: MenuItem
    private lateinit var menuItemStopMonitoring: MenuItem
    private lateinit var menuItemLogToFile: MenuItem
    private lateinit var menuItemLogToFlash: MenuItem
    private lateinit var menuItemSettings: MenuItem

    private var hrvAlgorithmInitConfig: HrvAlgorithmInitConfig? = null
    private val hrvAlgorithmInput = HrvAlgorithmInput()
    private val hrvAlgorithmOutput = HrvAlgorithmOutput()

    private lateinit var timeChartView: MultiChannelChartView
    private lateinit var frequencyChartView: MultiChannelChartView
    private lateinit var ibiChartView: MultiChannelChartView

    private var dataRecorder: DataRecorder? = null

    private var startTime: String? = null

    private var isMonitoring: Boolean = false
        set(value) {
            field = value
            menuItemStopMonitoring.isVisible = value
            menuItemStartMonitoring.isVisible = !value

        }

    private var avnn: String? = null
        set(value) {
            field = value
            avnnView.text = value ?: ResultCardView.EMPTY_VALUE

        }

    private var sdnn: String? = null
        set(value) {
            field = value
            sdnnView.text = value ?: ResultCardView.EMPTY_VALUE

        }

    private var rmssd: String? = null
        set(value) {
            field = value
            rmssdView.text = value ?: ResultCardView.EMPTY_VALUE

        }

    private var pnn50: String? = null
        set(value) {
            field = value
            pnn50View.text = value ?: ResultCardView.EMPTY_VALUE
        }


    private var ulf: String? = null
        set(value) {
            field = value
            ulfView.text = value ?: ResultCardView.EMPTY_VALUE

        }

    private var vlf: String? = null
        set(value) {
            field = value
            vlfView.text = value ?: ResultCardView.EMPTY_VALUE

        }

    private var lf: String? = null
        set(value) {
            field = value
            lfView.text = value ?: ResultCardView.EMPTY_VALUE

        }

    private var hf: String? = null
        set(value) {
            field = value
            hfView.text = value ?: ResultCardView.EMPTY_VALUE
        }

    private var lfOverHf: String? = null
        set(value) {
            field = value
            lfOverHfView.text = value ?: ResultCardView.EMPTY_VALUE

        }

    private var totPwr: String? = null
        set(value) {
            field = value
            totPwrView.text = value ?: ResultCardView.EMPTY_VALUE
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        hspViewModel = ViewModelProviders.of(requireActivity()).get(HspViewModel::class.java)

        hspViewModel.connectionState
            .observe(this) { (device, connectionState) ->
                toolbar.connectionInfo = if (hspViewModel.bluetoothDevice != null) {
                    BleConnectionInfo(connectionState, device.name, device.address)
                } else {
                    null
                }
            }

        hspViewModel.commandResponse
            .observe(this) { hspResponse ->
                Timber.d(hspResponse.toString())
            }

        hspViewModel.streamData
            .observe(this) { hspStreamData ->
                addStreamData(hspStreamData)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hrv, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hrvAlgorithmInitConfig = HrvAlgorithmInitConfig(40f, 60, 15)
        HrvAlgorithm.init(hrvAlgorithmInitConfig)

        timeChartView = view.findViewById(R.id.time_chart_view)
        frequencyChartView = view.findViewById(R.id.frequency_chart_view)
        ibiChartView = view.findViewById(R.id.ibi_chart_view)

        initializeChronometer()
        setupChart()
        setupToolbar()
    }

    private fun initializeChronometer() {

        hrvChronometer.format = "Start Time ${startTime ?: ResultCardView.EMPTY_VALUE} 00:%s"
        hrvChronometer.setOnChronometerTickListener { cArg ->
            val elapsedMillis = SystemClock.elapsedRealtime() - cArg.base
            if (elapsedMillis > 3600000L) {
                cArg.format = "Start Time ${startTime ?: ResultCardView.EMPTY_VALUE} 0%s"
            } else {
                cArg.format = "Start Time ${startTime ?: ResultCardView.EMPTY_VALUE} 00:%s"
            }
        }
    }


    private fun setupChart() {
        timeChartView.dataSetInfoList = listOf(
            DataSetInfo(R.string.avnn, R.color.channel_ir),
            DataSetInfo(R.string.sdnn, R.color.channel_red),
            DataSetInfo(R.string.rmssd, R.color.channel_green),
            DataSetInfo(R.string.pnn50, R.color.colorPrimaryDark)
        )

        frequencyChartView.dataSetInfoList = listOf(
            DataSetInfo(R.string.vlf, R.color.channel_red),
            DataSetInfo(R.string.lf, R.color.channel_green),
            DataSetInfo(R.string.hf, R.color.colorPrimaryDark),
            DataSetInfo(R.string.lfOverHf, R.color.colorPrimary),
            DataSetInfo(R.string.totPwr, R.color.color_secondary),
            DataSetInfo(R.string.ulf, R.color.channel_ir)
        )

        ibiChartView.dataSetInfoList = listOf(
            DataSetInfo(R.string.ibi, R.color.channel_red)
        )

        timeChartView.titleView.text = getString(R.string.time_domain_metrics)
        frequencyChartView.titleView.text = getString(R.string.frequency_domain_metrics)
        ibiChartView.titleView.text = getString(R.string.ibiRr)

        timeChartView.maximumEntryCount = 100
        frequencyChartView.maximumEntryCount = 100
        ibiChartView.maximumEntryCount = 100

        val arr = IntArray(4) { i -> 0 }
        val arr2 = IntArray(6) { i -> 0 }

        for (i in 0..20) {
            timeChartView.addData(*arr)
            frequencyChartView.addData(*arr2)
        }
    }

    private fun setupToolbar() {

        toolbar.apply {
            inflateMenu(R.menu.toolbar_menu)
            menu.apply {
                menuItemStartMonitoring = findItem(R.id.monitoring_start)
                menuItemStopMonitoring = findItem(R.id.monitoring_stop)
                menuItemLogToFile = findItem(R.id.log_to_file)
                menuItemLogToFlash = findItem(R.id.log_to_flash)
                menuItemSettings = findItem(R.id.hrm_settings)
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.monitoring_start -> startMonitoring()
                    R.id.monitoring_stop -> stopMonitoring()
                    R.id.log_to_file -> dataLoggingToggled()
                    R.id.log_to_flash -> flashLoggingToggled()
                    R.id.hrm_settings -> showSettingsDialog()
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }
            setNavigationOnClickListener {
                onBackPressed()
            }
            setTitle(R.string.hrv)
        }

        toolbar.pageTitle = requireContext().getString(R.string.hrv)

    }

    fun addStreamData(streamData: HspStreamData) {

        dataRecorder?.record(streamData)

        hrvAlgorithmInput.ibi = streamData.rr
        hrvAlgorithmInput.ibiConfidence = streamData.rrConfidence
        hrvAlgorithmInput.isIbiValid = true

        HrvAlgorithm.run(hrvAlgorithmInput, hrvAlgorithmOutput)

        percentCompleted.measurementProgress = hrvAlgorithmOutput.percentCompleted

        ibiChartView.addData(streamData.rr.toInt())

        if (hrvAlgorithmOutput.isHrvCalculated) {
            updateTimeDomainHrvMetrics(hrvAlgorithmOutput.timeDomainHrvMetrics)
            updateFrequencyDomainHrvMetrics(hrvAlgorithmOutput.freqDomainHrvMetrics)
        }
    }

    private fun updateTimeDomainHrvMetrics(timeDomainHrvMetrics: TimeDomainHrvMetrics) {
        avnn = "%.2f".format(timeDomainHrvMetrics.avnn)
        sdnn = "%.2f".format(timeDomainHrvMetrics.sdnn)
        rmssd = "%.2f".format(timeDomainHrvMetrics.rmssd)
        pnn50 = "%.2f".format(timeDomainHrvMetrics.pnn50)

        timeChartView.addData(
            timeDomainHrvMetrics.avnn.toInt(),
            timeDomainHrvMetrics.sdnn.toInt(),
            timeDomainHrvMetrics.rmssd.toInt(),
            timeDomainHrvMetrics.pnn50.toInt()
        )

    }

    private fun updateFrequencyDomainHrvMetrics(freqDomainHrvMetrics: FreqDomainHrvMetrics) {
        ulf = "%.2f".format(freqDomainHrvMetrics.ulf)
        vlf = "%.2f".format(freqDomainHrvMetrics.vlf)
        lf = "%.2f".format(freqDomainHrvMetrics.lf)
        hf = "%.2f".format(freqDomainHrvMetrics.hf)
        lfOverHf = "%.2f".format(freqDomainHrvMetrics.lfOverHf)
        totPwr = "%.2f".format(freqDomainHrvMetrics.totPwr)

        frequencyChartView.addData(
            freqDomainHrvMetrics.ulf.toInt(),
            freqDomainHrvMetrics.vlf.toInt(),
            freqDomainHrvMetrics.lf.toInt(),
            freqDomainHrvMetrics.hf.toInt(),
            freqDomainHrvMetrics.lfOverHf.toInt(),
            freqDomainHrvMetrics.totPwr.toInt()
        )
    }

    private fun sendDefaultSettings() {
        hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "scdenable", "1"))
//        hspViewModel.sendCommand(SetConfigurationCommand("scdpowersaving", " ", "1 10 5"))
    }

    private fun startMonitoring() {
        isMonitoring = true
        dataRecorder = DataRecorder("Hrv")

        clearChart()

        startTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        hrvChronometer.base = SystemClock.elapsedRealtime()
        hrvChronometer.start()


        percentCompleted.measurementProgress = 0
        percentCompleted.isMeasuring = true
        percentCompleted.result = null
        percentCompleted.isTimeout = false

        hspViewModel.isDeviceSupported
            .observe(this) {
                sendDefaultSettings()
                hspViewModel.startStreaming()
            }
    }

    private fun stopMonitoring() {
        isMonitoring = false

        dataRecorder?.close()
        dataRecorder = null

        startTime = null
        hrvChronometer.stop()

        percentCompleted.isMeasuring = false
        HrvAlgorithm.end()

        hspViewModel.stopStreaming()
    }

    private fun dataLoggingToggled() {

    }

    private fun flashLoggingToggled() {

    }

    private fun showSettingsDialog() {

    }

    private fun onBackPressed() {

    }

    fun clearChart() {
        timeChartView.clearChart()
        frequencyChartView.clearChart()
        ibiChartView.clearChart()
    }

}
