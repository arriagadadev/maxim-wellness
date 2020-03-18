package com.maximintegrated.maximsensorsapp.sports_coaching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig
import com.maximintegrated.algorithms.sports.SportsCoachingSession
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.maximsensorsapp.HelpDialog
import com.maximintegrated.maximsensorsapp.MeasurementBaseFragment
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.ResultCardView
import com.maximintegrated.maximsensorsapp.exts.set
import kotlinx.android.synthetic.main.fragment_sports_coaching_recovery_time.*
import kotlinx.android.synthetic.main.statistics_layout.view.*

class SportsCoachingRecoveryTimeFragment : MeasurementBaseFragment() {

    companion object {
        fun newInstance() = SportsCoachingRecoveryTimeFragment()
    }

    private lateinit var algorithmInitConfig: AlgorithmInitConfig
    private val algorithmInput = AlgorithmInput()
    private val algorithmOutput = AlgorithmOutput()

    private var hr: Int? = null
        set(value) {
            field = value
            hrView.emptyValue = value?.toString() ?: ResultCardView.EMPTY_VALUE
        }

    private var recoveryTime: Int? = null
        set(value) {
            field = value
            recoveryTimeView.emptyValue = value?.toString() ?: ResultCardView.EMPTY_VALUE
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sports_coaching_recovery_time, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        algorithmInitConfig = AlgorithmInitConfig()
        algorithmInitConfig.hrvConfig = HrvAlgorithmInitConfig(40f, 90, 30)
        with(algorithmInitConfig.sportCoachingConfig) {
            this.samplingRate = 25
            this.session = SportsCoachingSession.RECOVERY_TIME
            this.user = SportsCoachingManager.currentUser!!
            this.history = getHistoryFromFiles(SportsCoachingManager.currentUser!!.userName)
            val historyItem =
                this.history.records.firstOrNull { it.session == SportsCoachingSession.RECOVERY_TIME }
            if (historyItem != null) {
                this.recoveryConfig.lastEpocRecoveryTimestamp = historyItem.timestamp
                this.recoveryConfig.lastRecoveryEstimateInMinutes =
                    historyItem.scores.recovery.recoveryTimeMin
                this.recoveryConfig.lastHr = historyItem.scores.recovery.lastHr
            }
        }
        algorithmInitConfig.enableAlgorithmsFlag =
            MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_SPORTS
        algorithmOutput.sports.user = SportsCoachingManager.currentUser!!
        setupToolbar(getString(R.string.recovery_time))
        menuItemArbitraryCommand.isVisible = false
        menuItemLogToFlash.isVisible = false
        menuItemSettings.isVisible = false
    }

    override fun addStreamData(streamData: HspStreamData) {
        hr = streamData.hr
        dataRecorder?.record(streamData)

        algorithmInput.set(streamData)

        val success = MaximAlgorithms.run(algorithmInput, algorithmOutput)
        val percentage = algorithmOutput.hrv.percentCompleted
        percentCompleted.measurementProgress = percentage
        notificationResults[MXM_KEY] = "Sports Coaching progress: $percentage%"
        updateNotification()
        if (success && percentage == 100) {
            recoveryTime = algorithmOutput.sports.estimates.recovery.recoveryTimeMin
            statisticLayout.minHrTextView.text = algorithmOutput.sports.hrStats.minHr.toString()
            statisticLayout.maxHrTextView.text = algorithmOutput.sports.hrStats.maxHr.toString()
            statisticLayout.meanHrTextView.text = algorithmOutput.sports.hrStats.meanHr.toString()
            algorithmOutput.sports.session = SportsCoachingSession.RECOVERY_TIME
            saveMeasurement(algorithmOutput.sports, dataRecorder!!.timestamp, getMeasurementType())
            notificationResults[MXM_KEY] = "Recovery time: $recoveryTime"
            updateNotification()
            stopMonitoring()
        }
    }

    override fun getMeasurementType(): String {
        return getString(R.string.recovery_time)
    }

    override fun startMonitoring() {
        super.startMonitoring()
        menuItemEnabledScd.isEnabled = false
        clearCardViewValues()
        MaximAlgorithms.init(algorithmInitConfig)

        percentCompleted.measurementProgress = 0
        percentCompleted.isMeasuring = true
        percentCompleted.result = null
        percentCompleted.isTimeout = false

        hspViewModel.isDeviceSupported
            .observe(this) {
                sendDefaultSettings()
                sendLogToFlashCommand()
                sendAlgoMode()
                hspViewModel.startStreaming()
            }
    }

    override fun stopMonitoring() {
        super.stopMonitoring()
        menuItemEnabledScd.isEnabled = true
        percentCompleted.isMeasuring = false
        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_STRESS)
        hspViewModel.stopStreaming()
    }

    override fun dataLoggingToggled() {

    }

    override fun showSettingsDialog() {

    }

    override fun showInfoDialog() {
        val helpDialog =
            HelpDialog.newInstance(getString(R.string.recovery_time_info), getString(R.string.info))
        fragmentManager?.let { helpDialog.show(it, "helpDialog") }
    }

    private fun clearCardViewValues() {
        hr = null
        recoveryTime = null
    }
}