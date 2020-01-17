package com.maximintegrated.maximsensorsapp

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.maximsensorsapp.exts.ioThread
import kotlinx.android.synthetic.main.include_app_bar.*

abstract class MeasurementBaseFragment : Fragment(), IOnBackPressed,
    DataRecorder.DataRecorderListener {
    var dataRecorder: DataRecorder? = null

    lateinit var hspViewModel: HspViewModel

    lateinit var menuItemStartMonitoring: MenuItem
    lateinit var menuItemStopMonitoring: MenuItem
    lateinit var menuItemLogToFile: MenuItem
    lateinit var menuItemLogToFlash: MenuItem
    lateinit var menuItemSettings: MenuItem
    lateinit var menuItemEnabledScd: MenuItem

    var isMonitoring: Boolean = false
        set(value) {
            field = value
            menuItemStopMonitoring.isVisible = value
            menuItemStartMonitoring.isVisible = !value
            isMonitoringChanged()
        }

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
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }
            setTitle(title)
        }

        toolbar.pageTitle = title
    }

    abstract fun startMonitoring()

    abstract fun stopMonitoring()

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
        stopMonitoring()
    }

    private val dataStreamObserver = Observer<HspStreamData> { data ->
        addStreamData(data)
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
        hspViewModel.streamData.removeObserver(dataStreamObserver)
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
}