package com.maximintegrated.maximsensorsapp.sports_coaching

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.observe
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig
import com.maximintegrated.algorithms.sports.SportsCoachingEpocConfig
import com.maximintegrated.algorithms.sports.SportsCoachingSession
import com.maximintegrated.bpt.hsp.HspStreamData
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.set
import com.obsez.android.lib.filechooser.ChooserDialog
import kotlinx.android.synthetic.main.fragment_sports_coaching_epoc_recovery.*
import kotlinx.android.synthetic.main.statistics_layout.view.*
import kotlinx.android.synthetic.main.view_result_card.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

class SportsCoachingEpocRecoveryFragment : MeasurementBaseFragment() {

    companion object {
        fun newInstance() = SportsCoachingEpocRecoveryFragment()
    }

    private lateinit var algorithmInitConfig: AlgorithmInitConfig
    private val algorithmInput = AlgorithmInput()
    private val algorithmOutput = AlgorithmOutput()

    private var hr: Int? = null
        set(value) {
            field = value
            hrView.emptyValue = value?.toString() ?: ResultCardView.EMPTY_VALUE
        }

    private var epocRecovery: Int? = null
        set(value) {
            field = value
            epocRecoveryView.emptyValue = value?.toString() ?: ResultCardView.EMPTY_VALUE
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sports_coaching_epoc_recovery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        algorithmInitConfig = AlgorithmInitConfig()
        algorithmInitConfig.hrvConfig = HrvAlgorithmInitConfig(40f, 90, 30)
        with(algorithmInitConfig.sportCoachingConfig) {
            this.samplingRate = 25
            this.session = SportsCoachingSession.EPOC_RECOVERY
            this.user = SportsCoachingManager.currentUser!!
            this.history = getHistoryFromFiles(SportsCoachingManager.currentUser!!.userName)
        }
        algorithmInitConfig.enableAlgorithmsFlag =
            MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_SPORTS
        algorithmOutput.sports.user = SportsCoachingManager.currentUser!!
        setupToolbar(getString(R.string.epoc_recovery))
        menuItemArbitraryCommand.isVisible = false
        menuItemLogToFlash.isVisible = false
        menuItemSettings.isVisible = false
        readFromFile.isVisible = true

        val drawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(
                ContextCompat.getColor(context!!, R.color.progress_red),
                ContextCompat.getColor(context!!, R.color.progress_orange),
                ContextCompat.getColor(context!!, R.color.progress_green)
            )
        )
        drawable.let {
            it.cornerRadius = 10f
            it.mutate()
            it.setGradientCenter(0.8f, 0.2f)
        }
        epocRecoveryView.confidenceProgressBar.progressDrawable = drawable
        epocRecoveryView.confidenceProgressBar.progress = 80
    }

    override fun addStreamData(streamData: HspStreamData) {
        hr = streamData.hr
        dataRecorder?.record(streamData)

        algorithmInput.set(streamData)

        val success = MaximAlgorithms.run(algorithmInput, algorithmOutput)
        val percentage = algorithmOutput.sports.percentCompleted
        percentCompleted.measurementProgress = percentage
        notificationResults[MXM_KEY] = "Sports Coaching progress: $percentage%"
        updateNotification()
        if (success && percentage == 100) {
            epocRecovery = algorithmOutput.sports.estimates.recovery.epoc.toInt()
            statisticLayout.minHrTextView.text = algorithmOutput.sports.hrStats.minHr.toString()
            statisticLayout.maxHrTextView.text = algorithmOutput.sports.hrStats.maxHr.toString()
            statisticLayout.meanHrTextView.text = algorithmOutput.sports.hrStats.meanHr.toString()
            algorithmOutput.sports.session = SportsCoachingSession.EPOC_RECOVERY
            saveMeasurement(algorithmOutput.sports, dataRecorder!!.timestamp, getMeasurementType())
            notificationResults[MXM_KEY] = "EPOC Recovery: $epocRecovery"
            updateNotification()
            stopMonitoring()
        }
        epocRecoveryView.confidenceProgressBar.progress = streamData.hrConfidence
    }

    override fun getMeasurementType(): String {
        return getString(R.string.epoc_recovery)
    }

    private fun checkForEpocInput() {
        val exerciseDuration =
            when (durationChipGroup.checkedChipId) {
                R.id.durationChip1 -> 5
                R.id.durationChip2 -> 20
                R.id.durationChip3 -> 60
                R.id.durationChip4 -> 90
                else -> 0
            }
        val intensity =
            when (durationChipGroup.checkedChipId) {
                R.id.intensityChip1 -> 1
                R.id.intensityChip2 -> 2
                R.id.intensityChip3 -> 3
                else -> 0
            }
        val delay =
            when (delayChipGroup.checkedChipId) {
                R.id.delayChip1 -> 0
                R.id.delayChip2 -> 1
                R.id.delayChip3 -> 2
                R.id.delayChip4 -> 3
                else -> 0
            }
        algorithmInitConfig.sportCoachingConfig.epocConfig =
            SportsCoachingEpocConfig(exerciseDuration, intensity, delay)
    }

    override fun startMonitoring() {
        checkForEpocInput()
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
                hspViewModel.startStreaming()
            }
    }

    override fun stopMonitoring() {
        super.stopMonitoring()
        menuItemEnabledScd.isEnabled = true
        percentCompleted.isMeasuring = false
        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_SPORTS)
        hspViewModel.stopStreaming()
    }

    override fun runFromFile() {
        checkForEpocInput()
        ChooserDialog(requireContext())
            .withStartFile(DataRecorder.OUTPUT_DIRECTORY.absolutePath)
            .withChosenListener { dir, dirFile ->
                run {
                    MaximAlgorithms.init(algorithmInitConfig)
                    doAsync {
                        val inputs = readAlgorithmInputsFromFile(dirFile)
                        for (input in inputs) {
                            MaximAlgorithms.run(input, algorithmOutput)
                        }
                        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_SPORTS)

                        uiThread {
                            epocRecovery = algorithmOutput.sports.estimates.recovery.epoc.toInt()
                            statisticLayout.minHrTextView.text =
                                algorithmOutput.sports.hrStats.minHr.toString()
                            statisticLayout.maxHrTextView.text =
                                algorithmOutput.sports.hrStats.maxHr.toString()
                            statisticLayout.meanHrTextView.text =
                                algorithmOutput.sports.hrStats.meanHr.toString()
                            saveMeasurement(
                                algorithmOutput.sports, DataRecorder.TIMESTAMP_FORMAT.format(
                                    Date()
                                ), getMeasurementType()
                            )
                        }
                    }
                }
            }
            .build()
            .show()
    }

    override fun dataLoggingToggled() {

    }

    override fun showSettingsDialog() {

    }

    override fun showInfoDialog() {
        val helpDialog =
            HelpDialog.newInstance(getString(R.string.epoc_recovery_info), getString(R.string.info))
        fragmentManager?.let { helpDialog.show(it, "helpDialog") }
    }

    private fun clearCardViewValues() {
        hr = null
        epocRecovery = null
    }
}