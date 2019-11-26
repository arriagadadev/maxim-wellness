package com.maximintegrated.maximsensorsapp.whrm

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bluetooth.ble.BleScannerDialog
import com.maximintegrated.bluetooth.devicelist.OnBluetoothDeviceClickListener
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.bpt.polar.PolarViewModel
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import com.maximintegrated.maximsensorsapp.view.MultiChannelChartView
import com.maximintegrated.maximsensorsapp.view.ReferenceDeviceView
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_whrm_fragment_content.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WhrmFragment : Fragment(), IOnBackPressed, OnBluetoothDeviceClickListener {

    companion object {
        fun newInstance() = WhrmFragment()

        val HR_MEASURING_PERIOD_IN_MILLIS = TimeUnit.SECONDS.toMillis(13)
    }

    private lateinit var viewReferenceDevice: ReferenceDeviceView

    private lateinit var hspViewModel: HspViewModel
    private lateinit var polarViewModel: PolarViewModel
    private var bleScannerDialog: BleScannerDialog? = null

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
    private lateinit var menuItemEnabledScd: MenuItem

    private var countDownTimer: CountDownTimer? = null

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

        radioButtonSampledMode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                setupTimer()
            }
        }

        viewReferenceDevice = referenceDeviceView

        setupReferenceDeviceView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_whrm, container, false)
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
            setTitle(R.string.whrm)
        }

        toolbar.pageTitle = requireContext().getString(R.string.whrm)
    }

    private fun sendDefaultSettings() {
        hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "scdenable", "1"))
        hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode ", "2"))
    }

    private fun sendAlgoMode() {
        if (radioButtonNormalMode.isChecked) {
            hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode", "2"))
        } else if (radioButtonSampledMode.isChecked) {
            hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode", "3"))
        }
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
        dataRecorder = DataRecorder("Whrm")
        menuItemEnabledScd.isEnabled = false

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
                sendAlgoMode()
                sendLogToFlashCommand()
                hspViewModel.startStreaming()
            }
    }


    fun setupTimer() {
        val timeInterval = WhrmSettings.sampledModeTimeInterval
        countDownTimer = object : CountDownTimer(timeInterval, 1000) {
            override fun onFinish() {
                startMonitoring()
            }

            override fun onTick(millisUntilFinished: Long) {
            }
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        menuItemEnabledScd.isEnabled = true

        startTime = null
        whrmChronometer.stop()

        countDownTimer?.cancel()

        dataRecorder?.close()
        dataRecorder = null

        hspViewModel.stopStreaming()
        polarViewModel.disconnect()
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

    private fun setupReferenceDeviceView() {
        polarViewModel = ViewModelProviders.of(requireActivity()).get(PolarViewModel::class.java)

        polarViewModel.connectionState
            .observe(this) { (device, connectionState) ->
                viewReferenceDevice.bleConnectionInfo =
                    if (polarViewModel.bluetoothDevice != null) {
                        BleConnectionInfo(connectionState, device.name, device.address)
                    } else {
                        null
                    }
            }

        polarViewModel.heartRateMeasurement
            .observe(this) { heartRateMeasurement ->
                dataRecorder?.record(heartRateMeasurement)
                viewReferenceDevice.heartRateMeasurement = heartRateMeasurement
                Timber.d("%s", heartRateMeasurement)
            }

        polarViewModel.isDeviceSupported
            .observe(this) {
                polarViewModel.readBatteryLevel()
            }

        viewReferenceDevice.onSearchButtonClick {
            showBleScannerDialog(R.string.polar_devices, "")
        }

        viewReferenceDevice.onConnectButtonClick {
            polarViewModel.reconnect()
        }

        viewReferenceDevice.onDisconnectClick {
            polarViewModel.disconnect()
        }

        viewReferenceDevice.onChangeDeviceClick {
            polarViewModel.disconnect()
            viewReferenceDevice.bleConnectionInfo = null
            showBleScannerDialog(R.string.polar_devices, "")
        }
    }

    private fun showBleScannerDialog(@StringRes titleRes: Int, deviceNamePrefix: String?) {
        bleScannerDialog = BleScannerDialog.newInstance(getString(titleRes), deviceNamePrefix)
        bleScannerDialog?.setTargetFragment(this, 1437)
        fragmentManager?.let { bleScannerDialog?.show(it, "BleScannerDialog") }
    }

    private fun dataLoggingToggled() {

    }

    private fun flashLoggingToggled() {
        menuItemLogToFlash.isChecked = !menuItemLogToFlash.isChecked
    }

    private fun enableScdToggled() {
        ScdSettings.scdEnabled = !menuItemEnabledScd.isChecked
        menuItemEnabledScd.isChecked = ScdSettings.scdEnabled
    }

    private fun showSettingsDialog() {
        val whrmSettingsDialog = WhrmSettingsFragmentDialog.newInstance()
        whrmSettingsDialog.setTargetFragment(this, 1560)
        fragmentManager?.let { whrmSettingsDialog.show(it, "whrmSettingsDialog") }
    }

    private fun showArbitraryCommandDialog() {
        val arbitraryCommandDialog = ArbitraryCommandFragmentDialog.newInstance()
        arbitraryCommandDialog.setTargetFragment(this, 1338)
        fragmentManager?.let { arbitraryCommandDialog.show(it, "arbitraryCommandDialog") }
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
            //TODO this logic should be based on isReliableHrCalculated flag. ME11 will be updated.
            if (radioButtonSampledMode.isChecked) {
                stopMonitoring()
                countDownTimer?.start()
            }
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

    override fun onBluetoothDeviceClicked(bluetoothDevice: BluetoothDevice) {
        val deviceName = bluetoothDevice.name ?: ""

        when {
            deviceName.startsWith("", false) -> polarViewModel.connect(
                bluetoothDevice
            )
        }

        bleScannerDialog?.dismiss()
    }
}