package com.maximintegrated.maximsensorsapp.spo2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.BleConnectionInfo
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import com.maximintegrated.maximsensorsapp.view.MultiChannelChartView
import kotlinx.android.synthetic.main.fragment_spo2.*
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_spo2_fragment_content.*
import timber.log.Timber
import kotlin.math.roundToInt


class Spo2Fragment : Fragment() {

    companion object {
        fun newInstance() = Spo2Fragment()
    }

    private lateinit var hspViewModel: HspViewModel
    private lateinit var chartView: MultiChannelChartView

    private lateinit var menuItemStartMonitoring: MenuItem
    private lateinit var menuItemStopMonitoring: MenuItem
    private lateinit var menuItemLogToFile: MenuItem
    private lateinit var menuItemLogToFlash: MenuItem
    private lateinit var menuItemSettings: MenuItem

    private var isMonitoring: Boolean = false
        set(value) {
            field = value
            menuItemStopMonitoring.isVisible = value
            menuItemStartMonitoring.isVisible = !value

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
                Timber.d(hspStreamData.toString())
            }


        hspViewModel.commandResponse
            .observe(this) { hspResponse ->
                Timber.d(hspResponse.toString())
            }
    }

    private fun setupChart() {
        chartView.dataSetInfoList = listOf(
            DataSetInfo(R.string.channel_ir, R.color.channel_ir),
            DataSetInfo(R.string.channel_red, R.color.channel_red),
            DataSetInfo(R.string.channel_green, R.color.channel_green)
        )

        chartView.maximumEntryCount = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_spo2, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartView = view.findViewById(R.id.chart_view)

        setupToolbar()
        setupChart()
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
            setTitle(R.string.spo2)
        }

        toolbar.pageTitle = requireContext().getString(R.string.spo2)

    }

    private fun startMonitoring() {
        isMonitoring = true

        spo2ResultView.isMeasuring = true
        spo2ResultView.result = null
        spo2ResultView.isTimeout = false

        hspViewModel.isDeviceSupported
            .observe(this) {
                hspViewModel.startStreaming()
            }
    }

    private fun stopMonitoring() {
        isMonitoring = false

        hspViewModel.stopStreaming()
    }

    private fun dataLoggingToggled() {

    }

    private fun flashLoggingToggled() {

    }

    private fun showSettingsDialog() {
        val settingsDialog = Spo2SettingsFragmentDialog.newInstance()
        fragmentManager?.let { settingsDialog.show(it, "") }
    }

    private fun onBackPressed() {

    }

    fun addStreamData(streamData: HspStreamData) {
        chartView.addData(streamData.ir, streamData.red, streamData.green)

//        motionView.emptyValue = model.motionMessage
        spo2ResultView.measurementProgress = streamData.wspo2PercentageComplete
        spo2ResultView.result = streamData.spo2.roundToInt()

//        if (algorithmModeContinuousRadioButton.isChecked) {
//            spo2ResultView.result = model.spo2.roundToInt()
//            confidenceView.value = model.spo2Confidence.toFloat()
//        } else if (model.spo2PercentageCompleted == 100) {
//            spo2ResultView.result = model.spo2.roundToInt()
//            confidenceView.value = model.spo2Confidence.toFloat()
//            stopMonitoring()
//        }
//
//        if (model.isTimeout) {
//            spo2ResultView.isTimeout = true
//            stopMonitoring()
//        }
    }

    fun clearChart() {
        chartView.clearChart()
    }
}