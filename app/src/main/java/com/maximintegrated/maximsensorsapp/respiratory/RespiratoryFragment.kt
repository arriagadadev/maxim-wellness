package com.maximintegrated.maximsensorsapp.respiratory

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.algorithm_respiratory_rate.RespiratoryRateAlgorithm
import com.maximintegrated.algorithm_respiratory_rate.RespiratoryRateAlgorithmInitConfig
import com.maximintegrated.algorithm_respiratory_rate.RespiratoryRateAlgorithmInput
import com.maximintegrated.algorithm_respiratory_rate.RespiratoryRateAlgorithmOutput
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import com.maximintegrated.maximsensorsapp.view.MultiChannelChartView
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_respiratory_fragment_content.*
import kotlinx.android.synthetic.main.view_multi_channel_chart.view.*
import java.text.SimpleDateFormat
import java.util.*

class RespiratoryFragment : Fragment(), IOnBackPressed {

    companion object {
        fun newInstance() = RespiratoryFragment()
    }

    private lateinit var hspViewModel: HspViewModel

    private lateinit var menuItemStartMonitoring: MenuItem
    private lateinit var menuItemStopMonitoring: MenuItem
    private lateinit var menuItemLogToFile: MenuItem
    private lateinit var menuItemLogToFlash: MenuItem
    private lateinit var menuItemSettings: MenuItem
    private lateinit var menuItemEnabledScd: MenuItem

    private var dataRecorder: DataRecorder? = null

    private var startTime: String? = null

    private lateinit var chartView: MultiChannelChartView

    private var respiratoryRateAlgorithmInitConfig: RespiratoryRateAlgorithmInitConfig? = null
    private val respiratoryRateAlgorithmInput = RespiratoryRateAlgorithmInput()
    private val respiratoryRateAlgorithmOutput = RespiratoryRateAlgorithmOutput()

    private var isMonitoring: Boolean = false
        set(value) {
            field = value
            menuItemStopMonitoring.isVisible = value
            menuItemStartMonitoring.isVisible = !value

        }

    private var respiration: Float? = null
        set(value) {
            field = value
            if (value != null) {
                respirationResultView.emptyValue = "%.2f".format(value)
            } else {
                respirationResultView.emptyValue = ResultCardView.EMPTY_VALUE
            }
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
        return inflater.inflate(R.layout.fragment_respiratory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartView = view.findViewById(R.id.chart_view)

        respiratoryRateAlgorithmInitConfig = RespiratoryRateAlgorithmInitConfig(
            RespiratoryRateAlgorithmInitConfig.SourceOptions.WRIST,
            RespiratoryRateAlgorithmInitConfig.LedCodes.GREEN,
            RespiratoryRateAlgorithmInitConfig.SamplingRateOption.Hz_25
        )
        RespiratoryRateAlgorithm.init(respiratoryRateAlgorithmInitConfig)

        initializeChronometer()
        setupChart()
        setupToolbar()
    }

    private fun initializeChronometer() {

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
            setTitle(R.string.respiratory)
        }

        toolbar.pageTitle = requireContext().getString(R.string.respiratory)

    }

    fun addStreamData(streamData: HspStreamData) {

        dataRecorder?.record(streamData)

        respiratoryRateAlgorithmInput.ppg = streamData.green.toFloat()
        respiratoryRateAlgorithmInput.ibi = streamData.rr
        respiratoryRateAlgorithmInput.ibiConfidence = streamData.rrConfidence.toFloat()

        respiratoryRateAlgorithmInput.isIbiUpdateFlag = streamData.rr != 0f

        respiratoryRateAlgorithmInput.isPpgUpdateFlag = true

        RespiratoryRateAlgorithm.run(respiratoryRateAlgorithmInput, respiratoryRateAlgorithmOutput)

        chartView.addData(respiratoryRateAlgorithmOutput.respirationRate.toInt())
        respiration = respiratoryRateAlgorithmOutput.respirationRate
    }

    private fun sendDefaultSettings() {
        hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "scdenable", "1"))
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
        dataRecorder = DataRecorder("Respiration_Rate")
        menuItemEnabledScd.isEnabled = true

        clearChart()
        clearCardViewValues()

        startTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        respirationChronometer.base = SystemClock.elapsedRealtime()
        respirationChronometer.start()

        hspViewModel.isDeviceSupported
            .observe(this) {
                sendDefaultSettings()
                sendLogToFlashCommand()
                hspViewModel.startStreaming()
            }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        menuItemEnabledScd.isEnabled = false

        dataRecorder?.close()
        dataRecorder = null

        startTime = null
        respirationChronometer.stop()

        RespiratoryRateAlgorithm.end()

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
        arbitraryCommandDialog.setTargetFragment(this, 1340)
        fragmentManager?.let { arbitraryCommandDialog.show(it, "arbitraryCommandDialog") }
    }

    private fun dataLoggingToggled() {

    }

    private fun flashLoggingToggled() {

    }

    private fun enableScdToggled() {
        ScdSettings.scdEnabled = !menuItemEnabledScd.isChecked
        menuItemEnabledScd.isChecked = ScdSettings.scdEnabled
    }

    private fun showSettingsDialog() {

    }

    private fun clearChart() {
        chartView.clearChart()
    }

    private fun clearCardViewValues() {
        respiration = null
    }

    override fun onBackPressed(): Boolean {
        return isMonitoring
    }

    override fun onStopMonitoring() {
        stopMonitoring()
    }
}