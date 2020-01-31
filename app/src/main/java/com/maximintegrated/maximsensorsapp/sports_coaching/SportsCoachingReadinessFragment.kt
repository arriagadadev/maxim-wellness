package com.maximintegrated.maximsensorsapp.sports_coaching

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.set
import com.maximintegrated.maximsensorsapp.whrm.WhrmFragment
import kotlinx.android.synthetic.main.fragment_sports_coaching_readiness.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SportsCoachingReadinessFragment : MeasurementBaseFragment() {

    companion object {
        fun newInstance() = SportsCoachingReadinessFragment()
    }

    private var measurementStartTimestamp: Long? = null
    private var minConfidenceLevel = 0
    private var hrExpireDuration = 30
    private var lastValidHrTimestamp: Long = 0L

    private var algorithmInitConfig: AlgorithmInitConfig? = null
    private val algorithmInput = AlgorithmInput()
    private val algorithmOutput = AlgorithmOutput()

    private var hr: Int? = null
        set(value) {
            field = value
            hrView.emptyValue = value?.toString() ?: ResultCardView.EMPTY_VALUE
        }

    private var readiness: Int? = null
        set(value) {
            field = value
            readinessView.emptyValue = value?.toString() ?: ResultCardView.EMPTY_VALUE
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sports_coaching_readiness, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        algorithmInitConfig = AlgorithmInitConfig()
        algorithmInitConfig?.hrvConfig = HrvAlgorithmInitConfig(40f, 60, 15)
        algorithmInitConfig?.enableAlgorithmsFlag =
            MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_STRESS

        setupToolbar(getString(R.string.readiness))
        menuItemArbitraryCommand.isVisible = false
        menuItemLogToFlash.isVisible = false
        menuItemSettings.isVisible = false
    }

    override fun addStreamData(streamData: HspStreamData) {
        renderHrmModel(streamData)
        dataRecorder?.record(streamData)

        algorithmInput.set(streamData)

        val success = MaximAlgorithms.run(algorithmInput, algorithmOutput)
        val percentage = algorithmOutput.hrv.percentCompleted
        percentCompleted.measurementProgress = percentage
        notificationResults[MXM_KEY] = "Sports Coaching progress: $percentage%"
        updateNotification()
        if (success) {
            readiness = algorithmOutput.stress.stressScore
            notificationResults[MXM_KEY] = "Readiness score: $readiness"
            updateNotification()
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

    override fun getMeasurementType(): String {
        return getString(R.string.readiness)
    }

    override fun startMonitoring() {
        super.startMonitoring()
        menuItemEnabledScd.isEnabled = false
        clearCardViewValues()
        measurementStartTimestamp = null
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
            HelpDialog.newInstance(getString(R.string.readiness_info), getString(R.string.info))
        fragmentManager?.let { helpDialog.show(it, "helpDialog") }
    }

    private fun clearCardViewValues() {
        hr = null
        readiness = null
    }
}