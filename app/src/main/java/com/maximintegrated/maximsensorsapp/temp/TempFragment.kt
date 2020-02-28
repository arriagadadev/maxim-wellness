package com.maximintegrated.maximsensorsapp.temp

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.HspTempStreamData
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.maximsensorsapp.DataRecorder
import com.maximintegrated.maximsensorsapp.MeasurementBaseFragment
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import kotlinx.android.synthetic.main.include_temp_fragment_content.*
import java.io.File
import java.text.DecimalFormat
import java.util.*

class TempFragment : MeasurementBaseFragment() {

    companion object {
        fun newInstance() = TempFragment()

        private const val SAMPLE_INTERVAL_START_MS = 500
        private const val SAMPLE_INTERVAL_STEP_MS = 500
        private const val SAMPLE_INTERVAL_END_MS = 10000
    }

    private var temperature: Float?
        get() = tempValueView.temperatureInCelsius
        set(value) {
            tempValueView.temperatureInCelsius = value
        }

    private val sampleIntervalInMillis: Int
        get() {
            val seconds =
                sampleIntervalDecimalFormat.parse(sampleIntervalSpinner.selectedItem.toString())
            return (seconds.toFloat() * DateUtils.SECOND_IN_MILLIS).toInt()
        }

    private val sampleIntervalDecimalFormat = DecimalFormat("0.0")

    var timestamp = ""


    private val tempStreamObserver = Observer<HspTempStreamData> { data ->
        if (!isMonitoring) return@Observer
        renderTempData(data)
    }

    private var csvWriter: CsvWriter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        hspViewModel.tempStreamData.observeForever(tempStreamObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_temp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        useDataRecorder = false
        setupToolbar(getString(R.string.temp))
        setupSampleIntervalSpinner()
        menuItemArbitraryCommand.isVisible = false
        menuItemLogToFlash.isVisible = false
        menuItemEnabledScd.isVisible = false
        menuItemSettings.isVisible = false
    }

    override fun addStreamData(streamData: HspStreamData) {

    }

    private fun renderTempData(data: HspTempStreamData) {
        csvWriter?.write(data.toCsvModel())
        temperature = data.temperature
        val temperatureInFahrenheit = celsiusToFahrenheit(data.temperature)
        notificationResults[MXM_KEY] = "Temperature: $temperature °C / $temperatureInFahrenheit °F"
        updateNotification()
        tempChartView.addTempData(data.temperature)
    }

    override fun getMeasurementType(): String {
        return getString(R.string.temp)
    }

    override fun startMonitoring() {
        super.startMonitoring()
        menuItemLogToFlash.isEnabled = true
        timestamp = DataRecorder.TIMESTAMP_FORMAT.format(Date())
        csvWriter = CsvWriter.open(getCsvFilePath(), HspTempStreamData.CSV_HEADER_ARRAY)
        tempChartView.clearData()

        hspViewModel.isDeviceSupported
            .observe(this) {
                hspViewModel.sendCommand(SetConfigurationCommand("blepower", "0"))
                sendLogToFlashCommand()
                hspViewModel.startTempStreaming(sampleIntervalInMillis)
            }
    }

    override fun stopMonitoring() {
        hspViewModel.tempStreamData.removeObserver(tempStreamObserver)
        super.stopMonitoring()
        csvWriter?.close()
        menuItemLogToFlash.isEnabled = false

        hspViewModel.stopStreaming()

    }

    override fun isMonitoringChanged() {
        super.isMonitoringChanged()
        sampleIntervalSpinner.isEnabled = !isMonitoring
    }

    override fun dataLoggingToggled() {

    }

    override fun showSettingsDialog() {

    }

    override fun showInfoDialog() {

    }

    private fun setupSampleIntervalSpinner() {
        val spinnerArray =
            (SAMPLE_INTERVAL_START_MS..SAMPLE_INTERVAL_END_MS step SAMPLE_INTERVAL_STEP_MS).map {
                sampleIntervalDecimalFormat.format(it.toFloat() / DateUtils.SECOND_IN_MILLIS)
            }.toList()

        sampleIntervalSpinner.adapter = ArrayAdapter<String>(
            requireContext(), R.layout.numeric_spinner_item, spinnerArray
        ).apply {
            setDropDownViewResource(R.layout.numeric_spinner_dropdown_item)
        }
    }

    private fun getCsvFilePath() = File(
        DataRecorder.OUTPUT_DIRECTORY,
        "/TEMP/MaximSensorsApp_${timestamp}_${getMeasurementType()}.csv"
    ).absolutePath

    override fun onDetach() {
        super.onDetach()
        hspViewModel.tempStreamData.removeObserver(tempStreamObserver)
    }
}