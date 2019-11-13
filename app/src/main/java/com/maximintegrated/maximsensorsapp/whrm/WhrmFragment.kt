package com.maximintegrated.maximsensorsapp.whrm

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import com.maximintegrated.maximsensorsapp.view.MultiChannelChartView
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_whrm_fragment_content.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WhrmFragment : Fragment(), IOnBackPressed {

    companion object {
        fun newInstance() = WhrmFragment()

        val HR_MEASURING_PERIOD_IN_MILLIS = TimeUnit.SECONDS.toMillis(13)
    }

    private lateinit var hspViewModel: HspViewModel
    private lateinit var chartView: MultiChannelChartView
    private var dataRecorder: DataRecorder? = null

    private var measurementStartTimestamp: Long? = null
    private var minConfidenceLevel = 0
    private var hrExpireDuration = 30
    private var lastValidHrTimestamp: Long = 0L

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
            hrResultView.isMeasuring = value
        }

    private var startTime: String? = null

    private var hrConfidence: Int? = null
        set(value) {
            field = value
//            hrConfidenceView.value = value?.toFloat()
        }

    private var ibi: String? = null
        set(value) {
            field = value
            ibiView.emptyValue = value ?: ResultCardView.EMPTY_VALUE
        }

    private var stepCount: Int? = null
        set(value) {
            field = value
            stepsView.emptyValue = value.toString()
        }

    private var energy: String? = null
        set(value) {
            field = value
            energyView.emptyValue = value.toString()
        }

    private var activity: String? = null
        set(value) {
            field = value
            activityView.emptyValue = value ?: ResultCardView.EMPTY_VALUE
        }

    private var scd: String? = null
        set(value) {
            field = value
            scdView.emptyValue = value ?: ResultCardView.EMPTY_VALUE
        }

    private var cadence: String? = null
        set(value) {
            field = value
            cadenceView.emptyValue = value.toString()
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
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_whrm, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chartView = view.findViewById(R.id.chart_view)

        initializeChronometer()
        setupToolbar()
        setupChart()
    }

    private fun initializeChronometer() {

        whrmChronometer.format = "Start Time ${startTime ?: ResultCardView.EMPTY_VALUE} 00:%s"
        whrmChronometer.setOnChronometerTickListener { cArg ->
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
            DataSetInfo(R.string.channel_ir, R.color.channel_ir),
            DataSetInfo(R.string.channel_green, R.color.channel_green)
        )

        chartView.maximumEntryCount = 100
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
                    R.id.monitoring_stop -> showStopMonitoringDialog()
                    R.id.log_to_file -> dataLoggingToggled()
                    R.id.log_to_flash -> flashLoggingToggled()
                    R.id.hrm_settings -> showSettingsDialog()
                    R.id.send_arbitrary_command -> showArbitraryCommandDialog()
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }
            setTitle(R.string.whrm)
        }

        toolbar.pageTitle = requireContext().getString(R.string.whrm)

    }

    private fun sendDefaultSettings() {
        hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "scdenable", "1"))
        hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode ", "2"))
//        hspViewModel.sendCommand(SetConfigurationCommand("scdpowersaving", " ", "1 10 5"))
    }

    private fun startMonitoring() {
        isMonitoring = true
        dataRecorder = DataRecorder("Whrm")

        clearChart()

        hrResultView.measurementProgress = 0

        measurementStartTimestamp = null
        hrResultView.measurementProgress = getMeasurementProgress()
        hrResultView.result = null

        startTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        whrmChronometer.base = SystemClock.elapsedRealtime()
        whrmChronometer.start()

        hspViewModel.isDeviceSupported
            .observe(this) {
                sendDefaultSettings()
                hspViewModel.startStreaming()
            }
    }

    private fun stopMonitoring() {
        isMonitoring = false

        startTime = null
        whrmChronometer.stop()

        dataRecorder?.close()
        dataRecorder = null

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
        arbitraryCommandDialog.setTargetFragment(this, 1338)
        fragmentManager?.let { arbitraryCommandDialog.show(it, "arbitraryCommandDialog") }
    }

    fun addStreamData(streamData: HspStreamData) {
        renderHrmModel(streamData)
        dataRecorder?.record(streamData)
        stepCount = streamData.runSteps + streamData.walkSteps
        ibi = "${streamData.rr} msec"
        energy = "${streamData.kCal} cal"
        activity = Activity.values()[streamData.activity].displayName
        scd = Scd.values()[streamData.scdState].displayName
        cadence = "${streamData.cadence} steps/min"
    }

    private fun dataLoggingToggled() {

    }

    private fun flashLoggingToggled() {

    }

    private fun showSettingsDialog() {

    }

    private fun clearChart() {
        chartView.clearChart()
    }

    private fun clearCardViewValues() {
        hrConfidence = null
        ibi = null
        stepCount = null
        energy = null
        activity = null
        scd = null
        cadence = null
    }

    private fun shouldShowMeasuringProgress(): Boolean {
        return (System.currentTimeMillis() - (measurementStartTimestamp
            ?: 0L)) < HR_MEASURING_PERIOD_IN_MILLIS
    }

    private fun getMeasurementProgress(): Int {
        return ((System.currentTimeMillis() - (measurementStartTimestamp
            ?: 0L)) * 100 / HR_MEASURING_PERIOD_IN_MILLIS).toInt()
    }

    private fun renderHrmModel(streamData: HspStreamData) {
        if (measurementStartTimestamp == null) {
            measurementStartTimestamp = System.currentTimeMillis()
        }

        chartView.addData(streamData.green, streamData.green2)

        val shouldShowMeasuringProgress = shouldShowMeasuringProgress()
        if (shouldShowMeasuringProgress) {
            hrConfidence = null

            hrResultView.measurementProgress = getMeasurementProgress()
            hrResultView.result = null
        } else {
            hrConfidence = streamData.hrConfidence
        }

        if (isHrConfidenceHighEnough(streamData) && !shouldShowMeasuringProgress) {
            hrResultView.result = streamData.hr

            lastValidHrTimestamp = System.currentTimeMillis()
        } else if (isHrObsolete()) {
            // show HR as empty
            hrResultView.result = null
        }
    }

    private fun isHrConfidenceHighEnough(hrmModel: HspStreamData): Boolean {
        return if (hrmModel.hr < 40 || hrmModel.hr > 240) {
            false
        } else {
            hrmModel.hrConfidence >= minConfidenceLevel
        }
    }

    private fun isHrObsolete(): Boolean {
        return (System.currentTimeMillis() - lastValidHrTimestamp) > TimeUnit.SECONDS.toMillis(
            hrExpireDuration.toLong()
        )
    }

    override fun onBackPressed(): Boolean {
        return isMonitoring
    }

    override fun onStopMonitoring() {
        stopMonitoring()
    }
}