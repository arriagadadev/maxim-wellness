package com.maximintegrated.maximsensorsapp.stress

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.bpt.hsp.protocol.SetConfigurationCommand
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.set
import com.maximintegrated.maximsensorsapp.whrm.WhrmFragment
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_stress_fragment_content.*
import kotlinx.android.synthetic.main.view_result_card.view.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class StressFragment : Fragment(), IOnBackPressed {
    companion object {
        fun newInstance() = StressFragment()
    }

    private lateinit var hspViewModel: HspViewModel

    private var measurementStartTimestamp: Long? = null
    private var minConfidenceLevel = 0
    private var hrExpireDuration = 30
    private var lastValidHrTimestamp: Long = 0L

    private lateinit var menuItemStartMonitoring: MenuItem
    private lateinit var menuItemStopMonitoring: MenuItem
    private lateinit var menuItemLogToFile: MenuItem
    private lateinit var menuItemLogToFlash: MenuItem
    private lateinit var menuItemSettings: MenuItem
    private lateinit var menuItemEnabledScd: MenuItem

    private var algorithmInitConfig: AlgorithmInitConfig? = null
    private val algorithmInput = AlgorithmInput()
    private val algorithmOutput = AlgorithmOutput()

    private var dataRecorder: DataRecorder? = null

    private var startTime: String? = null

    private var isMonitoring: Boolean = false
        set(value) {
            field = value
            menuItemStopMonitoring.isVisible = value
            menuItemStartMonitoring.isVisible = !value

        }

    private var hr: Int? = null
        set(value) {
            field = value
            hrView.emptyValue = value?.toString() ?: ResultCardView.EMPTY_VALUE
        }

    private var stress: Int? = null
        set(value) {
            field = value
            when (value) {
                in 0..2 -> setStressView(
                    R.drawable.ic_stress,
                    R.color.stress_red,
                    "${value}: Overwhelmed"
                )
                in 3..5 -> setStressView(
                    R.drawable.ic_stress,
                    R.color.stress_orange,
                    "${value}: Frustrated"
                )
                6, 7 -> setStressView(
                    R.drawable.ic_stress_neutral,
                    R.color.stress_orange,
                    "${value}: Manageable stress"
                )
                8, 9 -> setStressView(
                    R.drawable.ic_stress_neutral,
                    R.color.stress_green,
                    "${value}: Feeling okay"
                )
                10, 11 -> setStressView(
                    R.drawable.ic_stress_smile,
                    R.color.stress_green,
                    "${value}: Doing great"
                )
                in 12..14 -> setStressView(
                    R.drawable.ic_stress_smile,
                    R.color.stress_orange,
                    "${value}: Very relaxed"
                )
                in 15..18 -> setStressView(
                    R.drawable.ic_stress_smile,
                    R.color.stress_red,
                    "${value}: Uncomfortably relaxed"
                )
                else -> setStressView(
                    R.drawable.ic_stress_neutral,
                    R.color.stress_orange,
                    ResultCardView.EMPTY_VALUE
                )
            }
        }

    private fun setStressView(iconRes: Int, colorRes: Int, message: String) {
        stressView.emptyValue = message
        if (context != null) {
            stressView.iconImageView.setImageResource(iconRes)
            val color = ContextCompat.getColor(context!!, colorRes)
            ImageViewCompat.setImageTintList(
                stressView.iconImageView,
                ColorStateList.valueOf(color)
            )
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

        hspViewModel.streamData
            .observe(this) { hspStreamData ->
                addStreamData(hspStreamData)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        algorithmInitConfig = AlgorithmInitConfig()
        algorithmInitConfig?.hrvConfig = HrvAlgorithmInitConfig(40f, 90, 30)
        algorithmInitConfig?.enableAlgorithmsFlag =
            MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_STRESS

        initializeChronometer()
        setupToolbar()
    }

    private fun initializeChronometer() {

        stressChronometer.format = "Start Time ${startTime ?: ResultCardView.EMPTY_VALUE} 00:%s"
        stressChronometer.setOnChronometerTickListener { cArg ->
            val elapsedMillis = SystemClock.elapsedRealtime() - cArg.base
            if (elapsedMillis > 3600000L) {
                cArg.format = "Start Time ${startTime ?: ResultCardView.EMPTY_VALUE} 0%s"
            } else {
                cArg.format = "Start Time ${startTime ?: ResultCardView.EMPTY_VALUE} 00:%s"
            }
        }
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
                    R.id.info_menu_item -> showInfoDialog()
                    R.id.send_arbitrary_command -> showArbitraryCommandDialog()
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }
            setTitle(R.string.stress)
        }

        toolbar.pageTitle = requireContext().getString(R.string.stress)

    }

    fun addStreamData(streamData: HspStreamData) {
        renderHrmModel(streamData)
        dataRecorder?.record(streamData)

        algorithmInput.set(streamData)

        val success = MaximAlgorithms.run(algorithmInput, algorithmOutput)

        percentCompleted.measurementProgress = algorithmOutput.hrv.percentCompleted

        if (success) {
            stress = algorithmOutput.stress.stressScore
            stopMonitoring()
        }
    }

    private fun renderHrmModel(streamData: HspStreamData) {
        if (measurementStartTimestamp == null) {
            measurementStartTimestamp = System.currentTimeMillis()
        }

        val shouldWaitForHRMeasuringPeriod = shouldWaitForHRMeasuringPeriod()
        if (shouldWaitForHRMeasuringPeriod) {
            hr = null
        }

        if (isHrConfidenceHighEnough(streamData) && !shouldWaitForHRMeasuringPeriod) {
            hr = streamData.hr
            lastValidHrTimestamp = System.currentTimeMillis()
        } else if (isHrObsolete()) {
            // show HR as empty
            hr = null
        }
    }

    private fun shouldWaitForHRMeasuringPeriod(): Boolean {
        return (System.currentTimeMillis() - (measurementStartTimestamp
            ?: 0L)) < WhrmFragment.HR_MEASURING_PERIOD_IN_MILLIS
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

    private fun sendDefaultSettings() {
        hspViewModel.sendCommand(SetConfigurationCommand("wearablesuite", "scdenable", if (menuItemEnabledScd.isChecked) "1" else "0"))
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

    private fun startMonitoring() {
        isMonitoring = true
        menuItemEnabledScd.isEnabled = false
        menuItemLogToFlash.isEnabled = false
        dataRecorder = DataRecorder("Stress")

        clearCardViewValues()

        measurementStartTimestamp = null

        startTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        stressChronometer.base = SystemClock.elapsedRealtime()
        stressChronometer.start()

        MaximAlgorithms.init(algorithmInitConfig)

        percentCompleted.measurementProgress = 0
        percentCompleted.isMeasuring = true
        percentCompleted.result = null
        percentCompleted.isTimeout = false

        hspViewModel.isDeviceSupported
            .observe(this) {
                sendDefaultSettings()
                sendLogToFlashCommand()
                hspViewModel.startStreaming()
            }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        menuItemEnabledScd.isEnabled = true
        menuItemLogToFlash.isEnabled = true

        dataRecorder?.close()
        dataRecorder = null

        startTime = null
        stressChronometer.stop()

        percentCompleted.isMeasuring = false
        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_STRESS)

        hspViewModel.stopStreaming()
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

    private fun dataLoggingToggled() {

    }

    private fun flashLoggingToggled() {
        menuItemLogToFlash.isChecked = !menuItemLogToFlash.isChecked
        menuItemLogToFile.isChecked = !menuItemLogToFlash.isChecked
    }

    private fun enableScdToggled() {
        ScdSettings.scdEnabled = !menuItemEnabledScd.isChecked
        menuItemEnabledScd.isChecked = ScdSettings.scdEnabled
    }

    private fun showSettingsDialog() {

    }

    private fun showInfoDialog() {
        val helpDialog = HelpDialog.newInstance(getString(R.string.stress_info), getString(R.string.info))
        fragmentManager?.let { helpDialog.show(it, "helpDialog") }
    }

    private fun showArbitraryCommandDialog() {
        val arbitraryCommandDialog = ArbitraryCommandFragmentDialog.newInstance()
        arbitraryCommandDialog.setTargetFragment(this, 1338)
        fragmentManager?.let { arbitraryCommandDialog.show(it, "arbitraryCommandDialog") }
    }

    private fun clearCardViewValues() {
        hr = null
        stress = null
    }

    override fun onBackPressed(): Boolean {
        return isMonitoring
    }

    override fun onStopMonitoring() {
        stopMonitoring()
    }
}