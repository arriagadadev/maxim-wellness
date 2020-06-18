package com.maximintegrated.maximsensorsapp.bpt

import android.os.Bundle
import android.view.LayoutInflater
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
import com.maximintegrated.bpt.hsp.protocol.HspCommand
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.bpt.BptStatus.*
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import com.maximintegrated.maximsensorsapp.view.DataSetInfo
import kotlinx.android.synthetic.main.fragment_bpt_calibration.*
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_bpt_calibration_fragment_content.*
import kotlinx.android.synthetic.main.include_bpt_calibration_warning_fragment_content.*
import kotlinx.android.synthetic.main.view_multi_channel_chart.view.*
import kotlinx.android.synthetic.main.view_old_protocol_calibration_card.view.*
import java.io.File
import java.util.*

class BptCalibrationFragment : Fragment(), IOnBackPressed {
    companion object {
        fun newInstance(): BptCalibrationFragment {
            return BptCalibrationFragment()
        }

        const val NUMBER_OF_DATA_TO_DISCARD = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_bpt_calibration, container, false)

    private lateinit var hspViewModel: HspViewModel
    private lateinit var bptViewModel: BptViewModel

    private var csvWriter: CsvWriter? = null
    private val irMovingAverage = MovingAverage(3)
    private lateinit var statusArray: Array<String>

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

        hspViewModel.commandResponse.observe(this, Observer {
            if (it.command.name == HspCommand.COMMAND_GET_CFG && it.command.parameters[0].value == "bpt") {
                if (it.command.parameters.size >= 2 && it.command.parameters[1].value == "cal_result") {
                    val array = it.parameters[0].valueAsByteArray
                    val calibrations =
                        BptCalibrationData.parseCalibrationDataFromCommandResponse(array)
                    saveCalibrationData(*calibrations)
                    Toast.makeText(
                        requireContext(),
                        requireContext().getString(R.string.calibration_completed),
                        Toast.LENGTH_SHORT
                    ).show()
                    bptViewModel.onCalibrationReceived()
                    stopMonitoring()
                }
            }
        })

        toolbar.apply {
            inflateMenu(R.menu.bpt_menu)
            menu.findItem(R.id.monitoring_start).isVisible = false
            menu.findItem(R.id.monitoring_start).isEnabled = false
            title = getString(R.string.bp_trending)
            pageTitle = getString(R.string.bp_trending)
        }

        bptViewModel.elapsedTime.observe(this) {
            calibrationCircleProgressView.setValue(it / 1000f)
            calibrationCircleProgressView.setText(getFormattedTime(it))
            if (bptViewModel.startTime == "") {
                toolbar.subtitle = getFormattedTime(it)
            } else {
                toolbar.subtitle = "Start Time: ${bptViewModel.startTime} - ${getFormattedTime(it)}"
            }
        }

        bptViewModel.calibrationStates.observe(this) {
            if (it == null) return@observe
            val index = it.first
            val status = it.second
            if (index == 0) {
                calibrationCardView.status = status
            }
        }

        setupCalibrationView(calibrationCardView)
        setupChart()

        restartTimerButton.setOnClickListener {
            bptViewModel.restartTimer()
        }

        goToCalibrationButton.setOnClickListener {
            bptViewModel.stopTimer()
            calibrationWarningLayout.isVisible = false
            calibrationLayout.isVisible = true
        }

        bptViewModel.startTimer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }

    override fun onDetach() {
        super.onDetach()
        hspViewModel.bptStreamData.removeObserver(dataStreamObserver)
    }

    private fun setupCalibrationView(view: OldProtocolCalibrationView) {
        view.calibrationButton.setOnClickListener {
            if (view.calibrationButton.text == getString(R.string.start)) {
                if (view.sbp1 == 0 || view.dbp1 == 0 || view.sbp2 == 0 || view.dbp2 == 0 || view.sbp3 == 0 || view.dbp3 == 0) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.ref_bp_required_fields_warning),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    startMonitoring(
                        view.tag.toString().toInt(),
                        view.sbp1,
                        view.dbp1,
                        view.sbp2,
                        view.dbp2,
                        view.sbp3,
                        view.dbp3
                    )
                }
            } else if (view.calibrationButton.text == getString(R.string.stop)) {
                stopMonitoring(view.tag.toString().toInt())
            } else if (view.calibrationButton.text == getString(R.string.done)) {
                requireActivity().onBackPressed()
            }
        }
        view.repeatButton.setOnClickListener {
            if (view.sbp1 == 0 || view.dbp1 == 0 || view.sbp2 == 0 || view.dbp2 == 0 || view.sbp3 == 0 || view.dbp3 == 0) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.ref_bp_required_fields_warning),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startMonitoring(
                    view.tag.toString().toInt(),
                    view.sbp1,
                    view.dbp1,
                    view.sbp2,
                    view.dbp2,
                    view.sbp3,
                    view.dbp3
                )
            }
        }
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
            "${File.separator}${BptSettings.currentUser}${File.separator}BPTrending_${timestamp}_calibration.csv"
        )
        csvWriter = CsvWriter.open(file.absolutePath, HspBptStreamData.CSV_HEADER_ARRAY)
    }

    private val dataStreamObserver = Observer<HspBptStreamData> { data ->
        if (bptViewModel.isMonitoring.value == false || bptViewModel.isWaitingForCalibrationResults()) return@Observer
        addStreamData(data)
    }

    private fun startMonitoring(
        index: Int,
        sbp1: Int,
        dbp1: Int,
        sbp2: Int,
        dbp2: Int,
        sbp3: Int,
        dbp3: Int
    ) {
        if (bptViewModel.isMonitoring.value!!) return
        chartView.clearChart()
        bptViewModel.startDataCollection(index)
        initCsvWriter()
        hspViewModel.streamType = HspViewModel.StreamType.BPT
        hspViewModel.bptStreamData.observeForever(dataStreamObserver)
        hspViewModel.setBptDateTime()
        hspViewModel.setSysBp(sbp1, sbp2, sbp3)
        hspViewModel.setDiaBp(dbp1, dbp2, dbp3)
        hspViewModel.setSpO2Coefficients(
            bptViewModel.spO2Coefficients[0],
            bptViewModel.spO2Coefficients[1],
            bptViewModel.spO2Coefficients[2]
        )
        hspViewModel.startBptCalibrationStreaming()
    }

    private fun stopMonitoring(index: Int = 0) {
        if (!bptViewModel.isMonitoring.value!!) return
        hspViewModel.stopStreaming()
        hspViewModel.bptStreamData.removeObserver(dataStreamObserver)
        bptViewModel.stopDataCollection(index)
        csvWriter?.close()
        csvWriter = null
        dataCount = 0
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

    private fun onStatusChanged(data: HspBptStreamData) {
        val status = BptStatus.fromInt(data.status)
        if (status != CAL_SEGMENT_DONE) {
            signalTextView.text = statusArray[data.status]
        }
        when (status) {
            SUCCESS -> {
                warningLayout.isVisible = false
                calibrationChartCardView.isVisible = true
                hspViewModel.stopStreaming()
                hspViewModel.getBptCalibrationResults()
                bptViewModel.onCalibrationResultsRequested()
                data.sbp = calibrationCardView.sbp1
                data.dbp = calibrationCardView.dbp1
                val historyData = data.toHistoryModel(true)
                historyData.sbp2 = calibrationCardView.sbp2
                historyData.dbp2 = calibrationCardView.dbp2
                historyData.sbp3 = calibrationCardView.sbp3
                historyData.dbp3 = calibrationCardView.dbp3
                saveHistoryData(historyData)
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
            INIT_CAL_REF_BP_INCONSISTENCY_ERROR_1 -> {
                showWarningMessage(
                    getString(R.string.inconsistency_error),
                    getString(R.string.inconsistency_error_1),
                    true
                )
                calibrationCardView.discardRefMeasurement(1)
            }
            INIT_CAL_REF_BP_INCONSISTENCY_ERROR_2 -> {
                showWarningMessage(
                    getString(R.string.inconsistency_error),
                    getString(R.string.inconsistency_error_2),
                    true
                )
                calibrationCardView.discardRefMeasurement(2)
            }
            INIT_CAL_REF_BP_INCONSISTENCY_ERROR_3 -> {
                showWarningMessage(
                    getString(R.string.inconsistency_error),
                    getString(R.string.inconsistency_error_3),
                    true
                )
                calibrationCardView.discardRefMeasurement(3)
            }
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