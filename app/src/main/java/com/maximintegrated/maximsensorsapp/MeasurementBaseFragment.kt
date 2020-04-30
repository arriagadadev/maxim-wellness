package com.maximintegrated.maximsensorsapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.google.android.material.snackbar.Snackbar
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import com.maximintegrated.maximsensorsapp.exts.ioThread
import com.maximintegrated.maximsensorsapp.service.ForegroundService
import kotlinx.android.synthetic.main.include_app_bar.*
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

abstract class MeasurementBaseFragment : Fragment(), IOnBackPressed,
    DataRecorder.DataRecorderListener {

    companion object {
        const val MXM_KEY = "MXM"
        const val REF_KEY = "REF"
    }

    var dataRecorder: DataRecorder? = null
    var useDataRecorder = true

    lateinit var hspViewModel: HspViewModel

    lateinit var menuItemStartMonitoring: MenuItem
    lateinit var menuItemStopMonitoring: MenuItem
    lateinit var menuItemLogToFile: MenuItem
    lateinit var menuItemLogToFlash: MenuItem
    lateinit var menuItemSettings: MenuItem
    lateinit var menuItemEnabledScd: MenuItem
    lateinit var menuItemArbitraryCommand: MenuItem
    lateinit var readFromFile: MenuItem

    var notificationResults: HashMap<String, String> = hashMapOf() // MXM, REF --> KEYS

    var isMonitoring: Boolean = false
        set(value) {
            field = value
            menuItemStopMonitoring.isVisible = value
            menuItemStartMonitoring.isVisible = !value
            isMonitoringChanged()
        }
    var expectingSampleCount: Int = 0

    private var startTime: String? = null
    private var startElapsedTime = 0L

    private var handler = Handler()

    fun setupToolbar(title: String) {

        toolbar.apply {
            inflateMenu(R.menu.toolbar_menu)
            menu.apply {
                menuItemStartMonitoring = findItem(R.id.monitoring_start)
                menuItemStopMonitoring = findItem(R.id.monitoring_stop)
                menuItemLogToFile = findItem(R.id.log_to_file)
                menuItemLogToFlash = findItem(R.id.log_to_flash)
                menuItemSettings = findItem(R.id.hrm_settings)
                menuItemEnabledScd = findItem(R.id.enable_scd)
                menuItemArbitraryCommand = findItem(R.id.send_arbitrary_command)
                readFromFile = findItem(R.id.readFromFileButton)

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
                    R.id.info_menu_item -> showInfoDialog()
                    R.id.send_arbitrary_command -> showArbitraryCommandDialog()
                    R.id.add_annotation -> showAnnotationDialog()
                    R.id.readFromFileButton -> runFromFile()
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }
            setTitle(title)
            pageTitle = title
        }
    }

    open fun startMonitoring() {
        hspViewModel.streamType = HspViewModel.StreamType.PPG
        if(useDataRecorder){
            dataRecorder = DataRecorder(getMeasurementType())
            dataRecorder?.dataRecorderListener = this
        }
        isMonitoring = true
        expectingSampleCount = 0
        dataCount = 0
        errorCount = 0

        startTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        startElapsedTime = SystemClock.elapsedRealtime()
        updateChronometer()
        handler.postDelayed(tickRunnable, 1000)

        startService()
    }

    open fun stopMonitoring() {
        isMonitoring = false
        expectingSampleCount = 0
        startTime = null
        handler.removeCallbacks(tickRunnable)
        dataRecorder?.close()
        dataRecorder = null
        stopService()
        errorWriter?.close()
        errorWriter = null
    }

    open fun isMonitoringChanged() {

    }

    open fun sendDefaultSettings() {
        hspViewModel.sendCommand(
            SetConfigurationCommand(
                "wearablesuite",
                "scdenable",
                if (menuItemEnabledScd.isChecked) "1" else "0"
            )
        )
        hspViewModel.sendCommand(SetConfigurationCommand("blepower", "0"))
    }

    open fun sendAlgoMode() {
        hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "algomode", "0"))
    }

    open fun sendLogToFlashCommand() {
        hspViewModel.sendCommand(
            SetConfigurationCommand(
                "flash",
                "log",
                if (menuItemLogToFlash.isChecked) "1" else "0"
            )
        )
    }

    open fun showStopMonitoringDialog() {
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

    abstract fun dataLoggingToggled()

    open fun flashLoggingToggled() {
        menuItemLogToFlash.isChecked = !menuItemLogToFlash.isChecked
        menuItemLogToFile.isChecked = !menuItemLogToFlash.isChecked
    }

    open fun enableScdToggled() {
        ScdSettings.scdEnabled = !menuItemEnabledScd.isChecked
        menuItemEnabledScd.isChecked = ScdSettings.scdEnabled
    }

    abstract fun showSettingsDialog()

    abstract fun showInfoDialog()

    open fun showArbitraryCommandDialog() {
        val arbitraryCommandDialog = ArbitraryCommandFragmentDialog.newInstance()
        arbitraryCommandDialog.setTargetFragment(this, 1338)
        fragmentManager?.let { arbitraryCommandDialog.show(it, "arbitraryCommandDialog") }
    }

    abstract fun addStreamData(streamData: HspStreamData)

    override fun onBackPressed(): Boolean {
        return isMonitoring
    }

    override fun onStopMonitoring() {
        hspViewModel.streamData.removeObserver(dataStreamObserver)
        stopMonitoring()
    }

    private var shouldShowDataLossError = false
        set(value) {
            field = value
            if (value) {
                if (view == null) {
                    return
                }
                showSnackbar(
                    view!!,
                    getString(R.string.packet_lost_message),
                    Snackbar.LENGTH_INDEFINITE
                )
            }
        }

    private var previousData: HspStreamData? = null
    private var dataCount = 0L
    private var errorCount = 0L

    private val dataStreamObserver = Observer<HspStreamData> { data ->
        //Timber.d("MELIK: $data")
        if (!isMonitoring) return@Observer
        dataCount++
        if (expectingSampleCount != data.sampleCount) {
            errorCount++
            if ((data.sampleCount - expectingSampleCount > 1) || (errorCount * 100f / dataCount > 2f)) {
                shouldShowDataLossError = true
            }
            if (previousData != null) {
                val sampleCount = expectingSampleCount
                val sampleTime = ((data.sampleTime + previousData!!.sampleTime) / 2)
                val green = (data.green + previousData!!.green) / 2
                val green2 = (data.green2 + previousData!!.green2) / 2
                val ir = (data.ir + previousData!!.ir) / 2
                val red = (data.red + previousData!!.red) / 2
                val accX = (data.accelerationX + previousData!!.accelerationX) / 2
                val accY = (data.accelerationY + previousData!!.accelerationY) / 2
                val accZ = (data.accelerationZ + previousData!!.accelerationZ) / 2
                val opMode = previousData!!.operationMode
                val hr = (data.hr + previousData!!.hr) / 2
                val hrConfidence = (data.hrConfidence + previousData!!.hrConfidence) / 2
                val rr = previousData!!.rr
                val rrConfidence = previousData!!.rrConfidence
                val activity = previousData!!.activity
                val r = (data.r + previousData!!.r) / 2
                val spo2Confidence = (data.wspo2Confidence + previousData!!.wspo2Confidence) / 2
                val spo2 = (data.spo2 + previousData!!.spo2) / 2
                val spo2PercentageComplete =
                    (data.wspo2PercentageComplete + previousData!!.wspo2PercentageComplete) / 2
                val spo2LowSnr = previousData!!.wspo2LowSnr
                val spo2Motion = previousData!!.wspo2Motion
                val spo2LowPi = previousData!!.wspo2LowPi
                val spo2UnreliableR = previousData!!.wspo2UnreliableR
                val spo2State = previousData!!.wspo2State
                val scdState = previousData!!.scdState
                val walk = (data.walkSteps + previousData!!.walkSteps) / 2
                val run = (data.runSteps + previousData!!.runSteps) / 2
                val kcal = (data.kCal + previousData!!.kCal) / 2
                val totalActEnergy = (data.totalActEnergy + previousData!!.totalActEnergy) / 2
                val timestamp = (data.currentTimeMillis + previousData!!.currentTimeMillis) / 2

                val backupData = HspStreamData(
                    sampleCount, sampleTime, green, green2, ir,
                    red, accX, accY, accZ, opMode, hr, hrConfidence, rr, rrConfidence, activity, r,
                    spo2Confidence, spo2, spo2PercentageComplete, spo2LowSnr, spo2Motion, spo2LowPi,
                    spo2UnreliableR, spo2State, scdState, walk, run, kcal, totalActEnergy, timestamp
                )
                dataRecorder?.record(backupData)
            }
            Timber.d("Error: expectingSampleCount = $expectingSampleCount  receivedSampleCount = ${data.sampleCount}")
            saveError(expectingSampleCount.toString(), data.sampleCount.toString())
            expectingSampleCount = data.sampleCount
        }
        addStreamData(data)
        previousData = data
        expectingSampleCount++
        if (expectingSampleCount == 256) {
            expectingSampleCount = 0
        }
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

        hspViewModel.streamData.observeForever(dataStreamObserver)
    }

    override fun onDetach() {
        super.onDetach()
        annotationWriter?.close()
        hspViewModel.streamData.removeObserver(dataStreamObserver)
        stopService()
    }

    override fun onFilesAreReadyForAlignment(
        alignedFilePath: String,
        maxim1HzFilePath: String,
        refFilePath: String
    ) {
        ioThread {
            align(alignedFilePath, maxim1HzFilePath, refFilePath)
        }
    }

    private var serviceActive = false

    private fun startService() {
        val intent = Intent(requireActivity(), ForegroundService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
        serviceActive = true
    }

    private fun stopService() {
        val intent = Intent(requireActivity(), ForegroundService::class.java)
        activity?.stopService(intent)
        serviceActive = false
    }

    private fun getNotificationText(): String {
        var text = ""
        if (notificationResults[MXM_KEY] != null) {
            text += notificationResults[MXM_KEY]
        }
        if (notificationResults[REF_KEY] != null) {
            text += "  ${notificationResults[REF_KEY]}"
        }
        return text
    }

    fun updateNotification() {
        if (serviceActive) {
            val intent = Intent(requireActivity(), ForegroundService::class.java)
            intent.putExtra(ForegroundService.NOTIFICATION_MESSAGE_KEY, getNotificationText())
            ContextCompat.startForegroundService(requireContext(), intent)
        }
    }

    private val timestamp = HspStreamData.TIMESTAMP_FORMAT.format(Date())
    private var annotationWriter: CsvWriter? = null
    private var errorWriter: CsvWriter? = null

    private fun getCsvFilePathAnnotation(): String {
        val name = getMeasurementType()
        return File(
            DataRecorder.OUTPUT_DIRECTORY,
            "/ANNOTATIONS/MaximSensorsApp_${timestamp}_${name}_annotation.csv"
        ).absolutePath
    }

    open fun getMeasurementType(): String {
        return this.javaClass.simpleName.replace("Fragment", "")
    }

    private fun getCsvFilePathError(): String {
        return File(
            DataRecorder.OUTPUT_DIRECTORY,
            "/ERROR/MaximSensorsApp_${dataRecorder?.timestamp}_${dataRecorder?.type}_error.csv"
        ).absolutePath
    }

    private fun saveAnnotation(annotation: String) {
        if (annotationWriter == null) {
            annotationWriter = CsvWriter.open(
                getCsvFilePathAnnotation(),
                arrayOf("timestamp", "annotation")
            )
        }
        annotationWriter?.write(
            System.currentTimeMillis(),
            annotation
        )
    }

    private fun saveError(expected: String, received: String) {
        if (errorWriter == null) {
            errorWriter = CsvWriter.open(
                getCsvFilePathError(),
                arrayOf("timestamp", "expected", "received")
            )
        }
        errorWriter?.write(System.currentTimeMillis(), expected, received)
    }

    private fun showAnnotationDialog() {
        val editText = EditText(context)
        editText.hint = getString(R.string.enter_message)
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        alertDialog.setTitle(getString(R.string.add_annotation))
        alertDialog.setView(editText)
            .setPositiveButton(getString(R.string.save)) { dialog, which ->
                saveAnnotation(editText.text.toString())
                dialog.dismiss()
            }.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                dialog.dismiss()
            }

        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    open fun runFromFile() {

    }

    fun showSnackbar(view: View, message: String, duration: Int) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.setAction(getString(R.string.ok)) {
            snackbar.dismiss()
        }
        snackbar.show()
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isMonitoring) {
                updateChronometer()
                handler.postDelayed(this, 1000)
            }else{
                handler.removeCallbacks(this)
            }
        }
    }

    private fun updateChronometer(){
        val elapsedMillis = SystemClock.elapsedRealtime() - startElapsedTime
        toolbar.subtitle = "Start Time: ${startTime ?: ResultCardView.EMPTY_VALUE} - ${getFormattedTime(elapsedMillis)}"
    }
}