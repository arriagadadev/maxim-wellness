package com.maximintegrated.maximsensorsapp.whrm

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.children
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bluetooth.ble.BleScannerDialog
import com.maximintegrated.bluetooth.devicelist.OnBluetoothDeviceClickListener
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.bpt.polar.HeartRateMeasurement
import com.maximintegrated.bpt.polar.PolarViewModel
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import com.maximintegrated.maximsensorsapp.view.MultiChannelChartView
import com.maximintegrated.maximsensorsapp.view.ReferenceDeviceView
import kotlinx.android.synthetic.main.include_whrm_fragment_content.*
import kotlinx.android.synthetic.main.view_measurement_result.view.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class WhrmFragment : MeasurementBaseFragment(), OnBluetoothDeviceClickListener {

    companion object {
        fun newInstance() = WhrmFragment()

        val HR_MEASURING_PERIOD_IN_MILLIS = TimeUnit.SECONDS.toMillis(13)
        val TIMEOUT_INTERVAL_IN_MILLIS = TimeUnit.SECONDS.toMillis(40)
        val MIN_CYCLE_TIME_IN_MILLIS = TimeUnit.SECONDS.toMillis(60)
    }

    private lateinit var viewReferenceDevice: ReferenceDeviceView

    private lateinit var polarViewModel: PolarViewModel
    private var bleScannerDialog: BleScannerDialog? = null

    private lateinit var chartView: MultiChannelChartView
    //private var dataRecorder: DataRecorder? = null

    private var measurementStartTimestamp: Long? = null
    private var minConfidenceLevel = 0
    private var hrExpireDuration = 30
    private var lastValidHrTimestamp: Long = 0L

    private var countDownTimer: CountDownTimer? = null

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

    private val heartRateMeasurementObserver =
        androidx.lifecycle.Observer<HeartRateMeasurement> { heartRateMeasurement ->
            dataRecorder?.record(heartRateMeasurement)
            viewReferenceDevice.heartRateMeasurement = heartRateMeasurement
            Timber.d("%s", heartRateMeasurement)
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

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
        setupToolbar(getString(R.string.whrm_toolbar))
        setupChart()
        hrResultView.measuringWarningMessageView.text = ""
    }

    override fun initializeChronometer() {

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
            DataSetInfo(R.string.channel_green1, R.color.channel_green),
            DataSetInfo(R.string.channel_green2, R.color.channel_green2)
        )

        chartView.maximumEntryCount = 100
    }

    override fun sendDefaultSettings() {
        hspViewModel.sendCommand(
            SetConfigurationCommand(
                "wearablesuite",
                "scdenable",
                if (menuItemEnabledScd.isChecked) "1" else "0"
            )
        )
        hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode ", "2"))
    }

    private fun sendAlgoMode() {
        if (radioButtonNormalMode.isChecked) {
            hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode", "2"))
        } else if (radioButtonSampledMode.isChecked) {
            hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode", "3"))
        }
    }

    override fun isMonitoringChanged() {
        hrResultView.isMeasuring = isMonitoring
    }

    override fun startMonitoring() {
        isMonitoring = true
        dataRecorder = DataRecorder("Whrm")
        dataRecorder?.dataRecorderListener = this
        menuItemEnabledScd.isEnabled = false
        menuItemLogToFlash.isEnabled = false

        clearChart()

        countDownTimer?.cancel()
        if (radioButtonSampledMode.isChecked) {
            countDownTimer?.start()
        }

        hrResultView.measurementProgress = 0

        measurementStartTimestamp = null
        hrResultView.measurementProgress = getMeasurementProgress()
        hrResultView.result = null

        setAlgorithmModeRadioButtonsEnabled(false)

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
        val timeInterval = max(WhrmSettings.sampledModeTimeInterval, MIN_CYCLE_TIME_IN_MILLIS)
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeInterval, 1000) {
            override fun onFinish() {
                startMonitoring()
            }

            override fun onTick(millisUntilFinished: Long) {
            }
        }
        if (isMonitoring && radioButtonSampledMode.isChecked) {
            countDownTimer?.start()
        }
    }

    override fun stopMonitoring() {
        isMonitoring = false
        menuItemEnabledScd.isEnabled = true
        menuItemLogToFlash.isEnabled = true

        startTime = null
        whrmChronometer.stop()

        setAlgorithmModeRadioButtonsEnabled(true)
        dataRecorder?.close()
        dataRecorder = null

        hspViewModel.stopStreaming()
        polarViewModel.disconnect()
    }

    private fun setAlgorithmModeRadioButtonsEnabled(isEnabled: Boolean) {
        for (radioButton in algorithmModeRadioGroup.children) {
            radioButton.isEnabled = isEnabled
        }
    }

    override fun addStreamData(streamData: HspStreamData) {
        renderHrmModel(streamData)
        dataRecorder?.record(streamData)
        stepCount = streamData.runSteps + streamData.walkSteps

        if (streamData.rr != 0f) {
            ibi = "${streamData.rr} msec"
        }
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
            .observeForever(heartRateMeasurementObserver)

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

    override fun dataLoggingToggled() {

    }

    override fun showSettingsDialog() {
        val whrmSettingsDialog = WhrmSettingsFragmentDialog.newInstance()
        whrmSettingsDialog.setTargetFragment(this, 1560)
        fragmentManager?.let { whrmSettingsDialog.show(it, "whrmSettingsDialog") }
    }

    override fun showInfoDialog() {
        val helpDialog =
            HelpDialog.newInstance(getString(R.string.whrm_info), getString(R.string.info))
        fragmentManager?.let { helpDialog.show(it, "helpDialog") }
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

        if (radioButtonNormalMode.isChecked) {
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
        } else {
            hrResultView.result = streamData.hr
            if (streamData.hrConfidence == 100) {
                stopMonitoring()
            } else if (System.currentTimeMillis() - measurementStartTimestamp!! >= TIMEOUT_INTERVAL_IN_MILLIS) {
                hrResultView.result = null
                Toast.makeText(context, "TIME OUT", Toast.LENGTH_SHORT).show()
                stopMonitoring()
            }
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

    override fun onBluetoothDeviceClicked(bluetoothDevice: BluetoothDevice) {
        val deviceName = bluetoothDevice.name ?: ""

        when {
            deviceName.startsWith("", false) -> polarViewModel.connect(
                bluetoothDevice
            )
        }

        bleScannerDialog?.dismiss()
    }

    override fun onDetach() {
        super.onDetach()
        countDownTimer?.cancel()
        polarViewModel.heartRateMeasurement.removeObserver(heartRateMeasurementObserver)
    }
}