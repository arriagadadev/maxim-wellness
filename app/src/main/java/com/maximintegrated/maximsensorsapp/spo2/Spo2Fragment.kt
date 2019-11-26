package com.maximintegrated.maximsensorsapp.spo2

import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
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
import com.maximintegrated.maximsensorsapp.whrm.WhrmFragment
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_spo2_fragment_content.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class Spo2Fragment : Fragment(), IOnBackPressed, OnBluetoothDeviceClickListener {

    companion object {
        fun newInstance() = Spo2Fragment()
        const val STATUS_TIMEOUT = 3
    }

    private lateinit var hspViewModel: HspViewModel
    private lateinit var chartView: MultiChannelChartView

    private lateinit var viewReferenceDevice: ReferenceDeviceView
    private lateinit var polarViewModel: PolarViewModel
    private var bleScannerDialog: BleScannerDialog? = null

    private lateinit var menuItemStartMonitoring: MenuItem
    private lateinit var menuItemStopMonitoring: MenuItem
    private lateinit var menuItemLogToFile: MenuItem
    private lateinit var menuItemLogToFlash: MenuItem
    private lateinit var menuItemSettings: MenuItem
    private lateinit var menuItemEnabledScd: MenuItem

    private var measurementStartTimestamp: Long? = null
    private var dataRecorder: DataRecorder? = null
    private var startTime: String? = null

    private var rResult: Float? = null
        set(value) {
            field = value
            if (value != null) {
                rResultView.emptyValue = value.toString()
            } else {
                rResultView.emptyValue = ResultCardView.EMPTY_VALUE
            }
        }

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
            }

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

        initializeChronometer()
        setupChart()
        setupToolbar()
    }

    private fun initializeChronometer() {

        spo2Chronometer.format = "Start Time ${startTime ?: ResultCardView.EMPTY_VALUE} 00:%s"
        spo2Chronometer.setOnChronometerTickListener { cArg ->
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
            DataSetInfo(R.string.channel_red, R.color.channel_red)
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
            setTitle(R.string.spo2)
        }

        toolbar.pageTitle = requireContext().getString(R.string.spo2)
    }

    private fun startMonitoring() {
        isMonitoring = true
        menuItemEnabledScd.isEnabled = false

        dataRecorder = DataRecorder("SpO2")

        clearChart()
        clearCardViewValues()

        startTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        spo2Chronometer.base = SystemClock.elapsedRealtime()
        spo2Chronometer.start()

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

    private fun sendDefaultSettings() {
        hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "scdenable", "1"))
        hspViewModel.sendCommand(
            SetConfigurationCommand("wearablesuite", "spo2ledpdconfig", "1020")
        )
    }

    private fun sendAlgoMode() {
        if (algorithmModeContinuousRadioButton.isChecked) {
            hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode", "0"))
        } else if (algorithmModeOneShotRadioButton.isChecked) {
            hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode", "1"))
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

    private fun stopMonitoring() {
        isMonitoring = false
        menuItemEnabledScd.isEnabled = true

        dataRecorder?.close()
        dataRecorder = null

        startTime = null
        spo2Chronometer.stop()

        spo2ResultView.isMeasuring = false

        setAlgorithmModeRadioButtonsEnabled(true)

        hspViewModel.stopStreaming()
        polarViewModel.disconnect()
    }

    private fun showStopMonitoringDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
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
        val settingsDialog = Spo2SettingsFragmentDialog.newInstance()
        fragmentManager?.let { settingsDialog.show(it, "") }
    }

    private fun showArbitraryCommandDialog() {
        val arbitraryCommandDialog = ArbitraryCommandFragmentDialog.newInstance()
        arbitraryCommandDialog.setTargetFragment(this, 1337)
        fragmentManager?.let { arbitraryCommandDialog.show(it, "arbitraryCommandDialog") }
    }

    fun addStreamData(streamData: HspStreamData) {

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

        hrResultView.measurementProgress = getMeasurementProgress()
        hrResultView.result = streamData.hr
    }

    private fun getMeasurementProgress(): Int {
        return ((System.currentTimeMillis() - (measurementStartTimestamp
            ?: 0L)) * 100 / WhrmFragment.HR_MEASURING_PERIOD_IN_MILLIS).toInt()
    }

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