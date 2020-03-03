package com.maximintegrated.maximsensorsapp.ecg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.maximintegrated.bpt.hsp.*
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.bpt.hsp.protocol.SetRegisterCommand
import com.maximintegrated.maximsensorsapp.DataRecorder
import com.maximintegrated.maximsensorsapp.MeasurementBaseFragment
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import kotlinx.android.synthetic.main.include_ecg_fragment_content.*
import java.io.File
import java.util.*

class EcgFragment : MeasurementBaseFragment() {

    companion object {
        fun newInstance() = EcgFragment()

        private const val RTOR_AVERAGE_WINDOW_SIZE = 5
        private const val EMPTY_VALUE = "--"
    }

    private var currentRtoR: Float? = null
        set(value) {
            field = value
            if (value != null) {
                currentRtorValueView.text = value.toString()
            } else {
                currentRtorValueView.text = EMPTY_VALUE
            }
        }

    private var averageRtoR: Float? = null
        set(value) {
            field = value
            if (value != null) {
                averageRtorValueView.text = value.toString()
            } else {
                averageRtorValueView.text = EMPTY_VALUE
            }
        }

    private val rtorMovingAverage = MovingAverage(RTOR_AVERAGE_WINDOW_SIZE)

    var timestamp = ""


    private val ecgStreamObserver = Observer<Array<HspEcgStreamData>> { data ->
        if (!isMonitoring) return@Observer
        renderEcgData(data)
    }

    private var csvWriter: CsvWriter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        hspViewModel.ecgStreamData.observeForever(ecgStreamObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ecg, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        useDataRecorder = false
        setupToolbar(getString(R.string.ecg))
        menuItemArbitraryCommand.isVisible = false
        menuItemLogToFlash.isVisible = false
        menuItemEnabledScd.isVisible = false
        menuItemSettings.isVisible = false
    }

    override fun addStreamData(streamData: HspStreamData) {

    }

    private fun renderEcgData(data: Array<HspEcgStreamData>) {
        for (d in data) {
            d.filteredEcg = ecgChartView.applyFilterAndAddData(d.ecgMv)
            if(d.currentRToRBpm != 0){
                val rtor = d.currentRToRBpm.toFloat()
                currentRtoR = rtor
                rtorMovingAverage.add(rtor)
                averageRtoR = rtorMovingAverage.average
            }
            csvWriter?.write(d.toCsvModel())
        }
        notificationResults[MXM_KEY] = "ECG: ${data.last().ecgMv}"
        updateNotification()
    }

    override fun getMeasurementType(): String {
        return getString(R.string.temp)
    }

    override fun startMonitoring() {
        super.startMonitoring()
        hspViewModel.streamType = HspViewModel.StreamType.ECG
        menuItemLogToFlash.isEnabled = true
        timestamp = DataRecorder.TIMESTAMP_FORMAT.format(Date())
        csvWriter = CsvWriter.open(getCsvFilePath(), HspEcgStreamData.CSV_HEADER_ARRAY)

        averageRtoR = null
        currentRtoR = null
        rtorMovingAverage.reset()
        ecgChartView.clearData()

        hspViewModel.isDeviceSupported
            .observe(this) {
                hspViewModel.sendCommand(SetConfigurationCommand("blepower", "0"))
                sendLogToFlashCommand()
                sendDefaultRegisterValues()
                HspEcgStreamData.ECG_GAIN = EcgRegisterMap.getDefaultEcgGain()
                hspViewModel.startEcgStreaming()
            }
    }

    private fun sendDefaultRegisterValues() {
        EcgRegisterMap.Defaults.forEach { (address, value) ->
            hspViewModel.sendCommand(SetRegisterCommand("ecg", address, value))
        }
    }

    override fun stopMonitoring() {
        hspViewModel.ecgStreamData.removeObserver(ecgStreamObserver)
        super.stopMonitoring()
        csvWriter?.close()
        csvWriter = null
        menuItemLogToFlash.isEnabled = false

        hspViewModel.stopStreaming()

    }

    override fun isMonitoringChanged() {
        super.isMonitoringChanged()
        samplingRateMessage.isVisible = isMonitoring
    }

    override fun dataLoggingToggled() {

    }

    override fun showSettingsDialog() {

    }

    override fun showInfoDialog() {

    }

    private fun getCsvFilePath() = File(
        DataRecorder.OUTPUT_DIRECTORY,
        "/ECG/MaximSensorsApp_${timestamp}_${getMeasurementType()}.csv"
    ).absolutePath

    override fun onDetach() {
        super.onDetach()
        hspViewModel.ecgStreamData.removeObserver(ecgStreamObserver)
        csvWriter?.close()
        csvWriter = null
    }
}