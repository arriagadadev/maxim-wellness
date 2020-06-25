package com.maximintegrated.maximsensorsapp.sports_coaching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.observe
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig
import com.maximintegrated.algorithms.sports.SportsCoachingSession
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.set
import com.obsez.android.lib.filechooser.ChooserDialog
import kotlinx.android.synthetic.main.fragment_sports_coaching_recovery_time.*
import kotlinx.android.synthetic.main.statistics_layout.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*


class SportsCoachingRecoveryTimeFragment : MeasurementBaseFragment() {

    companion object {
        fun newInstance() = SportsCoachingRecoveryTimeFragment()
    }

    private lateinit var algorithmInitConfig: AlgorithmInitConfig
    private val algorithmInput = AlgorithmInput()
    private val algorithmOutput = AlgorithmOutput()

    private var epocFound = false

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
                this.history.records.firstOrNull { it.session == SportsCoachingSession.EPOC_RECOVERY }
            if (historyItem != null) {
                this.recoveryConfig.lastEpocRecoveryTimestamp = historyItem.timestamp
                this.recoveryConfig.lastRecoveryEstimateInMinutes =
                    historyItem.scores.recovery.recoveryTimeMin
                this.recoveryConfig.lastHr = historyItem.scores.recovery.lastHr
                epocFound = true
            }
        }
        algorithmInitConfig.enableAlgorithmsFlag =
            MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_SPORTS
        algorithmOutput.sports.user = SportsCoachingManager.currentUser!!
        setupToolbar(getString(R.string.recovery_time))
        menuItemArbitraryCommand.isVisible = false
        menuItemLogToFlash.isVisible = false
        menuItemSettings.isVisible = false

        if (BuildConfig.DEBUG) {
            readFromFile.isVisible = true
        }
    }

    override fun addStreamData(streamData: HspStreamData) {
        hr = streamData.hr
        dataRecorder?.record(streamData)

        algorithmInput.set(streamData)

        MaximAlgorithms.run(algorithmInput, algorithmOutput)
        val percentage = algorithmOutput.sports.percentCompleted
        percentCompleted.measurementProgress = percentage
        notificationResults[MXM_KEY] = "Sports Coaching progress: $percentage%"
        updateNotification()
        if (algorithmOutput.sports.isNewOutputReady && percentage == 100) {
            recoveryTime = algorithmOutput.sports.estimates.recovery.recoveryTimeMin
            statisticLayout.minHrTextView.text = algorithmOutput.sports.hrStats.minHr.toString()
            statisticLayout.maxHrTextView.text = algorithmOutput.sports.hrStats.maxHr.toString()
            statisticLayout.meanHrTextView.text = algorithmOutput.sports.hrStats.meanHr.toString()
            algorithmOutput.sports.session = SportsCoachingSession.RECOVERY_TIME
            saveMeasurement(algorithmOutput.sports, dataRecorder!!.timestamp, getMeasurementType())
            stopMonitoring()
        }
    }

    override fun getMeasurementType(): String {
        return getString(R.string.recovery_time)
    }

    override fun startMonitoring() {
        if (!epocFound) {
            Toast.makeText(
                requireContext(),
                R.string.epoc_requirement,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        super.startMonitoring()
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
        percentCompleted.isMeasuring = false
        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_STRESS)
        hspViewModel.stopStreaming()
    }

    override fun dataLoggingToggled() {

    }

    override fun showSettingsDialog() {

    }

    override fun runFromFile() {
        if (!epocFound) {
            Toast.makeText(
                requireContext(),
                R.string.epoc_requirement,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        ChooserDialog(requireContext())
            .withStartFile(DataRecorder.OUTPUT_DIRECTORY.parent)
            .withChosenListener { dir, dirFile ->
                MaximAlgorithms.init(algorithmInitConfig)
                doAsync {
                    val inputs = readAlgorithmInputsFromFile(dirFile)
                    for (input in inputs) {
                        MaximAlgorithms.run(input, algorithmOutput)
                        if (algorithmOutput.sports.isNewOutputReady && algorithmOutput.sports.percentCompleted == 100) {
                            with(algorithmOutput.sports) {
                                recoveryTime = estimates.recovery.recoveryTimeMin
                                session = SportsCoachingSession.RECOVERY_TIME
                                uiThread {
                                    statisticLayout.minHrTextView.text = hrStats.minHr.toString()
                                    statisticLayout.maxHrTextView.text = hrStats.maxHr.toString()
                                    statisticLayout.meanHrTextView.text = hrStats.meanHr.toString()

                                    saveMeasurement(
                                        this, HspStreamData.TIMESTAMP_FORMAT.format(
                                            Date()
                                        ), getMeasurementType()
                                    )
                                }
                            }
                            break
                        }
                    }
                    MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_SPORTS)

                }
            }
            .build()
            .show()
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
