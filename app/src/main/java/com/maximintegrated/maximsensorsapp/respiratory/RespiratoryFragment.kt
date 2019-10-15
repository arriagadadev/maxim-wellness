package com.maximintegrated.maximsensorsapp.respiratory

import android.os.Bundle
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
import com.maximintegrated.maximsensorsapp.BleConnectionInfo
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import com.maximintegrated.maximsensorsapp.view.MultiChannelChartView
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_respiratory_fragment_content.*
import kotlinx.android.synthetic.main.view_multi_channel_chart.view.*
import timber.log.Timber

class RespiratoryFragment : Fragment() {

    companion object {
        fun newInstance() = RespiratoryFragment()
    }

    private lateinit var hspViewModel: HspViewModel

    private lateinit var menuItemStartMonitoring: MenuItem
    private lateinit var menuItemStopMonitoring: MenuItem
    private lateinit var menuItemLogToFile: MenuItem
    private lateinit var menuItemLogToFlash: MenuItem
    private lateinit var menuItemSettings: MenuItem

    private lateinit var chartView: MultiChannelChartView

    var calculated: Float = 0f

    private var respiratoryRateAlgorithmInitConfig: RespiratoryRateAlgorithmInitConfig? = null
    private val respiratoryRateAlgorithmInput = RespiratoryRateAlgorithmInput()
    private val respiratoryRateAlgorithmOutput = RespiratoryRateAlgorithmOutput()

    private var isMonitoring: Boolean = false
        set(value) {
            field = value
            menuItemStopMonitoring.isVisible = value
            menuItemStartMonitoring.isVisible = !value

        }

    private var respiration: Float = 0f
        set(value) {
            field = value
            respirationResultView.emptyValue = "%.2f".format(value)
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
        return inflater.inflate(R.layout.fragment_respiratory, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartView = view.findViewById(R.id.chart_view)

        respiratoryRateAlgorithmInitConfig = RespiratoryRateAlgorithmInitConfig(
            RespiratoryRateAlgorithmInitConfig.SourceOptions.FINGER,
            RespiratoryRateAlgorithmInitConfig.LedCodes.GREEN,
            RespiratoryRateAlgorithmInitConfig.SamplingRateOption.Hz_25
        )
        RespiratoryRateAlgorithm.init(respiratoryRateAlgorithmInitConfig)

        setupChart()
        setupToolbar()
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
            setTitle(R.string.respiratory)
        }

        toolbar.pageTitle = requireContext().getString(R.string.respiratory)

    }

    fun addStreamData(streamData: HspStreamData) {
        respiratoryRateAlgorithmInput.ppg = streamData.green.toFloat()
        respiratoryRateAlgorithmInput.ibi = streamData.rr
        respiratoryRateAlgorithmInput.ibiConfidence = streamData.rrConfidence.toFloat()

        if (calculated == streamData.rr) {
            respiratoryRateAlgorithmInput.isIbiUpdateFlag = false
        } else {
            respiratoryRateAlgorithmInput.isIbiUpdateFlag = true;
            calculated = streamData.rr
        }

        respiratoryRateAlgorithmInput.isPpgUpdateFlag = true

        RespiratoryRateAlgorithm.run(respiratoryRateAlgorithmInput, respiratoryRateAlgorithmOutput)

        chartView.addData(respiratoryRateAlgorithmOutput.respirationRate.toInt())
        respiration = respiratoryRateAlgorithmOutput.respirationRate
    }

    private fun startMonitoring() {
        isMonitoring = true

        hspViewModel.isDeviceSupported
            .observe(this) {
                hspViewModel.startStreaming()
            }
    }

    private fun stopMonitoring() {
        isMonitoring = false

        RespiratoryRateAlgorithm.end()

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
        chartView.clearChart()
    }
}