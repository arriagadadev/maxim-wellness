package com.maximintegrated.maximsensorsapp.bpt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bpt.hsp.HspBptStreamData
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.bpt.BptStatus.*
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_bpt_measurement_fragment_content.*
import kotlinx.android.synthetic.main.view_multi_channel_chart.view.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class BptMeasurementFragment : Fragment(), IOnBackPressed {
    companion object {
        fun newInstance(): BptMeasurementFragment {
            return BptMeasurementFragment()
        }

        const val NUMBER_OF_DATA_TO_DISCARD = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_bpt_measurement, container, false)

    private lateinit var hspViewModel: HspViewModel
    private lateinit var bptViewModel: BptViewModel

    lateinit var menuItemStartMonitoring: MenuItem
    lateinit var menuItemStopMonitoring: MenuItem

    private var csvWriter: CsvWriter? = null
    private val irMovingAverage = MovingAverage(3)
    private lateinit var statusArray: Array<String>

    private var bptSuccess = false

    private var dataCount = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusArray = resources.getStringArray(R.array.bpt_algorithm_out_status)

        hspViewModel = ViewModelProviders.of(requireActivity()).get(HspViewModel::class.java)

        bptViewModel = ViewModelProviders.of(requireActivity()).get(BptViewModel::class.java)

        hspViewModel.connectionState
            .observe(this) { (device, connectionState) ->
                toolbar.connectionInfo = if (hspViewModel.bluetoothDevice != null) {
                    BleConnectionInfo(connectionState, device.name, device.address)
                } else {
                    null
                }
            }

        bptViewModel.stopTimer()

        toolbar.apply {
            inflateMenu(R.menu.bpt_menu)
            menu.apply {
                menuItemStartMonitoring = findItem(R.id.monitoring_start)
                menuItemStopMonitoring = findItem(R.id.monitoring_stop)
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.monitoring_start -> startMonitoring()
                    R.id.monitoring_stop -> showStopMonitoringDialog()
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }
            title = getString(R.string.bp_trending)
            pageTitle = getString(R.string.bp_trending)
        }

        bptViewModel.isMonitoring.observe(this) {
            menuItemStopMonitoring.isVisible = it
            menuItemStartMonitoring.isVisible = !it
        }

        bptViewModel.elapsedTime.observe(this) {
            if (bptViewModel.startTime == "") {
                toolbar.subtitle = getFormattedTime(it)
            } else {
                toolbar.subtitle = "Start Time: ${bptViewModel.startTime} - ${getFormattedTime(it)}"
            }
        }

        setupChart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }

    override fun onDetach() {
        super.onDetach()
        hspViewModel.bptStreamData.removeObserver(dataStreamObserver)
    }

    private fun setupChart() {
        chartView.titleView.isVisible = false
        chartView.dataSetInfoList = listOf(
            DataSetInfo(R.string.ppg_signal, R.color.channel_red)
        )
        chartView.maximumEntryCount = 600
        chartView.line_chart_view.axisLeft.isEnabled = false
    }

    private fun initCsvWriter() {
        val timestamp = HspBptStreamData.TIMESTAMP_FORMAT.format(Date())
        val file = File(
            BptViewModel.OUTPUT_DIRECTORY,
            "${File.separator}${BptSettings.currentUser}${File.separator}BPTrending_${timestamp}_estimation.csv"
        )
        csvWriter = CsvWriter.open(file.absolutePath, HspBptStreamData.CSV_HEADER_ARRAY)
    }

    private val dataStreamObserver = Observer<HspBptStreamData> { data ->
        if (bptViewModel.isMonitoring.value == false) return@Observer
        addStreamData(data)
    }

    private fun startMonitoring() {
        if (bptViewModel.isMonitoring.value!!) return
        bptSuccess = false
        val calResult = getCalibrationDataInHexString()
        if (calResult == "") {
            Toast.makeText(
                requireContext(),
                R.string.calibration_required_warning,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        chartView.clearChart()
        bptViewModel.startMeasurement()
        initCsvWriter()
        hspViewModel.streamType = HspViewModel.StreamType.BPT
        hspViewModel.bptStreamData.observeForever(dataStreamObserver)
        hspViewModel.setBptDateTime()
        hspViewModel.setSpO2Coefficients(
            bptViewModel.spO2Coefficients[0],
            bptViewModel.spO2Coefficients[1],
            bptViewModel.spO2Coefficients[2]
        )
        hspViewModel.setCalibrationResult(getCalibrationDataInHexString())
        hspViewModel.startBptEstimationStreaming()
    }

    private fun stopMonitoring() {
        if (!bptViewModel.isMonitoring.value!!) return
        hspViewModel.stopStreaming()
        bptViewModel.stopMeasurement()
        hspViewModel.bptStreamData.removeObserver(dataStreamObserver)
        csvWriter?.close()
        dataCount = 0
        csvWriter = null
    }

    private fun addStreamData(data: HspBptStreamData) {
        dataCount++
        if (dataCount <= NUMBER_OF_DATA_TO_DISCARD) {
            return
        }
        csvWriter?.write(data.toCsvModel())
        irMovingAverage.add(data.irCnt)
        chartView.addData(irMovingAverage.average())
        progressBar.progress = data.progress
        progressTextView.text = getString(R.string.percent_format, data.progress.toString())
        hrTextView.text = data.hr.toString()
        spo2TextView.text = data.spo2.toString()
        signalTextView.text = statusArray[data.status]
        if (data.hrAboveResting == 1) {
            showWarningMessage(
                getString(R.string.fast_hr),
                getString(R.string.hr_above_resting_message),
                false
            )
        }
        onStatusChanged(data)
    }

    override fun onBackPressed(): Boolean {
        return bptViewModel.isMonitoring.value ?: false
    }

    override fun onStopMonitoring() {
        stopMonitoring()
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

    private fun getCalibrationDataInHexString(): String {
        if (!CALIBRATION_FILE.exists()) {
            return ""
        }
        val lines = CALIBRATION_FILE.bufferedReader().readLines()
        if (lines.size < 2) {
            return ""
        }
        val calibrations = lines.takeLast(2)
            .map { BptCalibrationData.parseCalibrationDataFromString(it).toArray() }
        val buffer = ByteBuffer.allocate(CAL_RESULT_LENGTH * 4 * 2)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(calibrations[0])
        buffer.put(calibrations[1])
        return buffer.array().toHexString()
    }

    private fun onStatusChanged(data: HspBptStreamData) {
        val status = BptStatus.fromInt(data.status)
        if (status != CAL_SEGMENT_DONE) {
            signalTextView.text = statusArray[data.status]
        }
        when (status) {
            SUCCESS -> {
                warningLayout.isVisible = false
                calibrationChartCardView.isVisible = true
                if (!bptSuccess) {
                    sbpTextView.text = data.sbp.toString()
                    dbpTextView.text = data.dbp.toString()
                    saveHistoryData(data.toHistoryModel(false))
                    bptSuccess = true
                }
            }
            FAILURE -> stopMonitoring()
            INIT_SUBJECT_FAILURE -> showWarningMessage(
                getString(R.string.subject_failure),
                getString(R.string.subject_error_message),
                true
            )
            INIT_CAL_REF_BP_TRENDING_ERROR -> showWarningMessage(
                getString(R.string.trending_error),
                getString(R.string.trending_error_message),
                true
            )
            INIT_CAL_REF_BP_INCONSISTENCY_ERROR_1 -> showWarningMessage(
                getString(R.string.inconsistency_error),
                getString(R.string.inconsistency_error_1),
                true
            )
            INIT_CAL_REF_BP_INCONSISTENCY_ERROR_2 -> showWarningMessage(
                getString(R.string.inconsistency_error),
                getString(R.string.inconsistency_error_2),
                true
            )
            INIT_CAL_REF_BP_INCONSISTENCY_ERROR_3 -> showWarningMessage(
                getString(R.string.inconsistency_error),
                getString(R.string.inconsistency_error_3),
                true
            )
            INIT_MIN_PULSE_PRESSURE_ERROR -> showWarningMessage(
                getString(R.string.pulse_pressure_error),
                getString(R.string.pulse_pressure_error_message),
                true
            )
            INIT_CAL_REF_BP_ERRONEOUS_REF -> showWarningMessage(
                getString(R.string.missing_reference_error),
                getString(R.string.missing_reference_error_message),
                true
            )
            HR_TOO_HIGH_IN_CAL -> showWarningMessage(
                getString(R.string.fast_hr),
                getString(R.string.fast_hr_message),
                true
            )
            HR_TOO_HIGH_IN_EST -> showWarningMessage(
                getString(R.string.fast_hr),
                getString(R.string.fast_hr_message),
                false
            )
            PI_OUT_OF_RANGE -> showWarningMessage(
                getString(R.string.pi_out_of_range),
                getString(R.string.pi_out_of_range_message),
                true
            )
            ESTIMATION_ERROR -> showWarningMessage(
                getString(R.string.estimation_error),
                getString(R.string.estimation_error_message),
                true
            )
            TOO_MANY_CAL_POINT_ERROR -> showWarningMessage(
                getString(R.string.too_many_cal),
                getString(R.string.too_many_cal_message),
                true
            )
            else -> {

            }
        }
    }

    private fun showWarningMessage(title: String, message: String, abort: Boolean) {
        if (!warningLayout.isVisible || abort) {
            bpWarningTitleTextView.text = title
            bpWarningMessageTextView.text = message
            bpWarningSkipButton.isVisible = !abort
            warningLayout.isVisible = true
            calibrationChartCardView.isInvisible = true
        }
        if (abort) {
            stopMonitoring()
        }
    }
}