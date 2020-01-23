package com.maximintegrated.maximsensorsapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.maximsensorsapp.exts.CsvWriter
import com.maximintegrated.maximsensorsapp.exts.ioThread
import com.maximintegrated.maximsensorsapp.service.ForegroundService
import kotlinx.android.synthetic.main.include_app_bar.*
import timber.log.Timber
import java.io.File
import java.util.*

abstract class MeasurementBaseFragment : Fragment(), IOnBackPressed,
    DataRecorder.DataRecorderListener {

    companion object {
        const val MXM_KEY = "MXM"
        const val REF_KEY = "REF"
    }

    var dataRecorder: DataRecorder? = null

    lateinit var hspViewModel: HspViewModel

    lateinit var menuItemStartMonitoring: MenuItem
    lateinit var menuItemStopMonitoring: MenuItem
    lateinit var menuItemLogToFile: MenuItem
    lateinit var menuItemLogToFlash: MenuItem
    lateinit var menuItemSettings: MenuItem
    lateinit var menuItemEnabledScd: MenuItem

    var notificationResults: HashMap<String, String> = hashMapOf() // MXM, REF --> KEYS

    var isMonitoring: Boolean = false
        set(value) {
            field = value
            menuItemStopMonitoring.isVisible = value
            menuItemStartMonitoring.isVisible = !value
            isMonitoringChanged()
        }
    var expectingSampleCount: Int = 0

    abstract fun initializeChronometer()

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
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }
            setTitle(title)
        }

        toolbar.pageTitle = title
    }

    open fun startMonitoring() {
        expectingSampleCount = 0
        startService()
    }

    open fun stopMonitoring() {
        expectingSampleCount = 0
        stopService()
    }

    open fun isMonitoringChanged() {

    }

    abstract fun sendDefaultSettings()

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

    private val dataStreamObserver = Observer<HspStreamData> { data ->
        if (!isMonitoring) return@Observer
        addStreamData(data)
        if (expectingSampleCount != data.sampleCount) {
            val error =
                "expectingSampleCount = $expectingSampleCount  receivedSampleCount = ${data.sampleCount}"
            Timber.d("Error: $error")
            saveError(error)
            expectingSampleCount = data.sampleCount
        }
        expectingSampleCount++
        if (expectingSampleCount == 256) {
            expectingSampleCount = 0
        }
        //Timber.d("MELIK: $data")
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
        errorWriter?.close()
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

    private val timestamp = DataRecorder.TIMESTAMP_FORMAT.format(Date())
    private var annotationWriter: CsvWriter? = null
    private var errorWriter: CsvWriter? = null

    private fun getCsvFilePathAnnotation(): String {
        val name = this.javaClass.simpleName.replace("Fragment", "")
        return File(
            DataRecorder.OUTPUT_DIRECTORY,
            "/ANNOTATIONS/MaximSensorsApp_${timestamp}_${name}_annotation.csv"
        ).absolutePath
    }

    private fun getCsvFilePathError(): String {
        val name = this.javaClass.simpleName.replace("Fragment", "")
        return File(
            DataRecorder.OUTPUT_DIRECTORY,
            "/ERROR/MaximSensorsApp_${timestamp}_${name}_error.csv"
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

    private fun saveError(error: String) {
        if (errorWriter == null) {
            errorWriter = CsvWriter.open(
                getCsvFilePathError(),
                arrayOf("timestamp", "error")
            )
        }
        errorWriter?.write(
            System.currentTimeMillis(),
            error
        )
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
}