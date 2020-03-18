package com.maximintegrated.maximsensorsapp.spo2

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.maximintegrated.maximsensorsapp.whrm.WhrmFragment
import kotlinx.android.synthetic.main.include_spo2_fragment_content.*
import timber.log.Timber
import kotlin.math.roundToInt


class Spo2Fragment : MeasurementBaseFragment(), OnBluetoothDeviceClickListener {

    companion object {
        fun newInstance() = Spo2Fragment()
        const val STATUS_TIMEOUT = 3
    }

    private lateinit var chartView: MultiChannelChartView

    private lateinit var viewReferenceDevice: ReferenceDeviceView
    private lateinit var polarViewModel: PolarViewModel
    private var bleScannerDialog: BleScannerDialog? = null

    private var measurementStartTimestamp: Long? = null

    private var rResult: Float? = null
        set(value) {
            field = value
            if (value != null) {
                rResultView.emptyValue = value.toString()
            } else {
                rResultView.emptyValue = ResultCardView.EMPTY_VALUE
            }
        }

    private val heartRateMeasurementObserver =
        androidx.lifecycle.Observer<HeartRateMeasurement> { heartRateMeasurement ->
            dataRecorder?.record(heartRateMeasurement)
            viewReferenceDevice.heartRateMeasurement = heartRateMeasurement
            notificationResults[REF_KEY] = "REF HR: ${heartRateMeasurement.heartRate}bpm"
            updateNotification()
            Timber.d("%s", heartRateMeasurement)
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewReferenceDevice = referenceDeviceView

        setupReferenceDeviceView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_spo2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartView = view.findViewById(R.id.chart_view)

        algorithmModeOneShotRadioButton.isChecked = true

        setupChart()
        setupToolbar(getString(R.string.spo2))
    }

    private fun setupChart() {
        chartView.dataSetInfoList = listOf(
            DataSetInfo(R.string.channel_ir, R.color.channel_ir),
            DataSetInfo(R.string.channel_red, R.color.channel_red)
        )

        chartView.maximumEntryCount = 100
    }

    override fun getMeasurementType(): String {
        return "SpO2"
    }

    override fun startMonitoring() {
        super.startMonitoring()
        menuItemEnabledScd.isEnabled = false
        menuItemLogToFlash.isEnabled = false

        clearChart()
        clearCardViewValues()

        measurementStartTimestamp = null
        hrResultView.measurementProgress = 0
        hrResultView.result = null

        spo2ResultView.isMeasuring = true
        spo2ResultView.result = null
        spo2ResultView.isTimeout = false

        setAlgorithmModeRadioButtonsEnabled(false)

        hspViewModel.isDeviceSupported
            .observe(this) {
                sendDefaultSettings()
                sendAlgoMode()
                sendLogToFlashCommand()
                hspViewModel.startStreaming()
            }
    }

    override fun sendDefaultSettings() {
        super.sendDefaultSettings()
        hspViewModel.sendCommand(
            SetConfigurationCommand("wearablesuite", "spo2ledpdconfig", "1020")
        )
    }

    override fun sendAlgoMode() {
        if (algorithmModeContinuousRadioButton.isChecked) {
            hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode", "0"))
        } else if (algorithmModeOneShotRadioButton.isChecked) {
            hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode", "1"))
        }
    }

    override fun stopMonitoring() {
        super.stopMonitoring()
        menuItemEnabledScd.isEnabled = true
        menuItemLogToFlash.isEnabled = true

        spo2ResultView.isMeasuring = false

        setAlgorithmModeRadioButtonsEnabled(true)

        hspViewModel.stopStreaming()
        polarViewModel.disconnect()
    }

    private fun setupReferenceDeviceView() {
        polarViewModel = ViewModelProviders.of(requireActivity()).get(PolarViewModel::class.java)

        polarViewModel.connectionState
            .observe(this) { (device, connectionState) ->
                if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                    notificationResults.remove(REF_KEY)
                    updateNotification()
                }
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
        /*val settingsDialog = Spo2SettingsFragmentDialog.newInstance()
        fragmentManager?.let { settingsDialog.show(it, "") }*/
    }

    override fun showInfoDialog() {
        val helpDialog =
            HelpDialog.newInstance(getString(R.string.spo2_info), getString(R.string.info))
        fragmentManager?.let { helpDialog.show(it, "helpDialog") }
    }

    override fun addStreamData(streamData: HspStreamData) {
        notificationResults[MXM_KEY] = "Maxim SpO2: ${streamData.spo2}%"
        updateNotification()
        dataRecorder?.record(streamData)

        renderSpo2Model(streamData)
        renderHrmModel(streamData)

        when (streamData.wspo2LowSnr) {
            1 -> lowSnr.background.setTint(Color.RED)
            else -> lowSnr.background.setTint(Color.GREEN)
        }

        when (streamData.wspo2Motion) {
            1 -> motion.background.setTint(Color.RED)
            else -> motion.background.setTint(Color.GREEN)
        }

        when (streamData.wspo2LowPi) {
            1 -> lowPi.background.setTint(Color.RED)
            else -> lowPi.background.setTint(Color.GREEN)
        }

        when (streamData.wspo2UnreliableR) {
            1 -> unreliableR.background.setTint(Color.RED)
            else -> unreliableR.background.setTint(Color.GREEN)
        }

        rResult = streamData.r
    }

    private fun renderSpo2Model(model: HspStreamData) {
        chartView.addData(model.ir, model.red)

        spo2ResultView.measurementProgress = model.wspo2PercentageComplete

        if (algorithmModeContinuousRadioButton.isChecked) {
            spo2ResultView.result = model.spo2.roundToInt()
        } else if (model.wspo2PercentageComplete == 100) {
            spo2ResultView.result = model.spo2.roundToInt()
            stopMonitoring()
        }

        if (model.wspo2State == STATUS_TIMEOUT) {
            spo2ResultView.isTimeout = true
            stopMonitoring()
        }
    }

    private fun renderHrmModel(streamData: HspStreamData) {
        if (measurementStartTimestamp == null) {
            measurementStartTimestamp = System.currentTimeMillis()
        }

        hrResultView.measurementProgress = 0 //getMeasurementProgress()
        hrResultView.result = streamData.hr
    }

    /*private fun getMeasurementProgress(): Int {
        return ((System.currentTimeMillis() - (measurementStartTimestamp
            ?: 0L)) * 100 / WhrmFragment.HR_MEASURING_PERIOD_IN_MILLIS).toInt()
    }*/

    private fun clearChart() {
        chartView.clearChart()
    }

    private fun clearCardViewValues() {
        rResult = null
    }

    private fun setAlgorithmModeRadioButtonsEnabled(isEnabled: Boolean) {
        for (radioButton in algorithmModeRadioGroup.children) {
            radioButton.isEnabled = isEnabled
        }
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
        polarViewModel.heartRateMeasurement.removeObserver(heartRateMeasurementObserver)
    }
}