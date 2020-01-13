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
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.hrv.FreqDomainHrvMetrics
import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig
import com.maximintegrated.algorithms.hrv.TimeDomainHrvMetrics
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.set
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import com.maximintegrated.maximsensorsapp.view.MultiChannelChartView
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_hrv_fragment_content.*
import kotlinx.android.synthetic.main.view_multi_channel_chart.view.*
import java.text.SimpleDateFormat
import java.util.*

class HrvFragment : Fragment(), IOnBackPressed {
    companion object {
        fun newInstance() = HrvFragment()
    }

    private lateinit var hspViewModel: HspViewModel

    private lateinit var menuItemStartMonitoring: MenuItem
    private lateinit var menuItemStopMonitoring: MenuItem
    private lateinit var menuItemLogToFile: MenuItem
    private lateinit var menuItemLogToFlash: MenuItem
    private lateinit var menuItemSettings: MenuItem
    private lateinit var menuItemEnabledScd: MenuItem

    private var algorithmInitConfig: AlgorithmInitConfig? = null
    private val algorithmInput = AlgorithmInput()
    private val algorithmOutput = AlgorithmOutput()

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
            val text = (value ?: ResultCardView.EMPTY_VALUE) + " ms"
            avnnView.text = text

        }

    private var sdnn: String? = null
        set(value) {
            field = value
            val text = (value ?: ResultCardView.EMPTY_VALUE) + " ms"
            sdnnView.text = text

        }

    private var rmssd: String? = null
        set(value) {
            field = value
            val text = (value ?: ResultCardView.EMPTY_VALUE) + " ms"
            rmssdView.text = text

        }

    private var pnn50: String? = null
        set(value) {
            field = value
            val text = (value ?: ResultCardView.EMPTY_VALUE)
            pnn50View.text = text
        }


    private var ulf: String? = null
        set(value) {
            field = value
            val text = (value ?: ResultCardView.EMPTY_VALUE) + " ms²"
            ulfView.text = text

        }

    private var vlf: String? = null
        set(value) {
            field = value
            val text = (value ?: ResultCardView.EMPTY_VALUE) + " ms²"
            vlfView.text = text

        }

    private var lf: String? = null
        set(value) {
            field = value
            val text = (value ?: ResultCardView.EMPTY_VALUE) + " ms²"
            lfView.text = text

        }

    private var hf: String? = null
        set(value) {
            field = value
            val text = (value ?: ResultCardView.EMPTY_VALUE) + " ms²"
            hfView.text = text
        }

    private var lfOverHf: String? = null
        set(value) {
            field = value
            lfOverHfView.text = value ?: ResultCardView.EMPTY_VALUE

        }

    private var totPwr: String? = null
        set(value) {
            field = value
            val text = (value ?: ResultCardView.EMPTY_VALUE) + " ms²"
            totPwrView.text = text
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        algorithmInitConfig = AlgorithmInitConfig()
        algorithmInitConfig?.hrvConfig = HrvAlgorithmInitConfig(40f, 60, 15)
        algorithmInitConfig?.enableAlgorithmsFlag = MaximAlgorithms.FLAG_HRV

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
                menuItemEnabledScd = findItem(R.id.enable_scd)

                menuItemEnabledScd.isChecked = ScdSettings.scdEnabled
                menuItemEnabledScd.isEnabled = true
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.monitoring_start -> startMonitoring()
                    R.id.monitoring_stop -> showStopMonitoringDialog()
                    R.id.log_to_file -> dataLoggingToggled()
                    R.id.log_to_flash -> flashLoggingToggled()
                    R.id.enable_scd -> enableScdToggled()
                    R.id.hrm_settings -> showSettingsDialog()
                    R.id.send_arbitrary_command -> showArbitraryCommandDialog()
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }
            setTitle(R.string.hrv)
        }

        toolbar.pageTitle = requireContext().getString(R.string.hrv)

    }

    fun addStreamData(streamData: HspStreamData) {

        dataRecorder?.record(streamData)

        algorithmInput.set(streamData)

        if (streamData.rr != 0f) {
            ibiChartView.addData(streamData.rr)
        }

        MaximAlgorithms.run(algorithmInput, algorithmOutput)

        percentCompleted.measurementProgress = algorithmOutput.hrv.percentCompleted

        if (algorithmOutput.hrv.isHrvCalculated) {
            updateTimeDomainHrvMetrics(algorithmOutput.hrv.timeDomainHrvMetrics)
            updateFrequencyDomainHrvMetrics(algorithmOutput.hrv.freqDomainHrvMetrics)
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
        hspViewModel.sendCommand(
            SetConfigurationCommand(
                "wearablesuite",
                "scdenable",
                if (menuItemEnabledScd.isChecked) "1" else "0"
            )
        )
    }

    private fun sendLogToFlashCommand() {
        hspViewModel.sendCommand(
            SetConfigurationCommand(
                "flash",
                "log",
                if (menuItemLogToFlash.isChecked) "1" else "0"
            )
        )
    }

    private fun startMonitoring() {
        isMonitoring = true
        menuItemEnabledScd.isEnabled = false
        menuItemLogToFlash.isEnabled = false
        dataRecorder = DataRecorder("Hrv")

        clearChart()
        clearCardViewValues()

        startTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        hrvChronometer.base = SystemClock.elapsedRealtime()
        hrvChronometer.start()

        MaximAlgorithms.init(algorithmInitConfig)

        percentCompleted.measurementProgress = 0
        percentCompleted.isMeasuring = true
        percentCompleted.result = null
        percentCompleted.isTimeout = false

        hspViewModel.isDeviceSupported
            .observe(this) {
                sendDefaultSettings()
                sendLogToFlashCommand()
                hspViewModel.startStreaming()
            }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        menuItemEnabledScd.isEnabled = true
        menuItemLogToFlash.isEnabled = true

        dataRecorder?.close()
        dataRecorder = null

        startTime = null
        hrvChronometer.stop()

        percentCompleted.isMeasuring = false
        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV)

        hspViewModel.stopStreaming()
    }

    private fun showStopMonitoringDialog() {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Stop Monitoring")
        alertDialog.setMessage("Are you sure you want to stop monitoring ?")
            .setPositiveButton("OK") { dialog, which ->
                stopMonitoring()
                dialog.dismiss()
            }.setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun showArbitraryCommandDialog() {
        val arbitraryCommandDialog = ArbitraryCommandFragmentDialog.newInstance()
        arbitraryCommandDialog.setTargetFragment(this, 1339)
        fragmentManager?.let { arbitraryCommandDialog.show(it, "arbitraryCommandDialog") }
    }

    private fun dataLoggingToggled() {

    }

    private fun flashLoggingToggled() {
        menuItemLogToFlash.isChecked = !menuItemLogToFlash.isChecked
        menuItemLogToFile.isChecked = !menuItemLogToFlash.isChecked
    }

    private fun enableScdToggled() {
        ScdSettings.scdEnabled = !menuItemEnabledScd.isChecked
        menuItemEnabledScd.isChecked = ScdSettings.scdEnabled
    }

    private fun showSettingsDialog() {

    }

    private fun clearChart() {
        timeChartView.clearChart()
        frequencyChartView.clearChart()
        ibiChartView.clearChart()
    }

    private fun clearCardViewValues() {
        avnn = null
        sdnn = null
        rmssd = null
        pnn50 = null
        ulf = null
        vlf = null
        lf = null
        hf = null
        lfOverHf = null
        totPwr = null

    }

    override fun onBackPressed(): Boolean {
        return isMonitoring
    }

    override fun onStopMonitoring() {
        stopMonitoring()
    }
}
