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
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.set
import com.obsez.android.lib.filechooser.ChooserDialog
import kotlinx.android.synthetic.main.fragment_sports_coaching_epoc_recovery.*
import kotlinx.android.synthetic.main.fragment_sports_coaching_vo2max.*
import kotlinx.android.synthetic.main.fragment_sports_coaching_vo2max.hrView
import kotlinx.android.synthetic.main.fragment_sports_coaching_vo2max.percentCompleted
import kotlinx.android.synthetic.main.fragment_sports_coaching_vo2max.statisticLayout
import kotlinx.android.synthetic.main.statistics_layout.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

class SportsCoachingVO2MaxFragment : MeasurementBaseFragment() {

    companion object {
        fun newInstance() = SportsCoachingVO2MaxFragment()
    }

    private lateinit var algorithmInitConfig: AlgorithmInitConfig
    private val algorithmInput = AlgorithmInput()
    private val algorithmOutput = AlgorithmOutput()

    private var hr: Int? = null
        set(value) {
            field = value
            hrView.emptyValue = value?.toString() ?: ResultCardView.EMPTY_VALUE
        }

    private var vo2Max: Int? = null
        set(value) {
            field = value
            vo2maxView.emptyValue = value?.toString() ?: ResultCardView.EMPTY_VALUE
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sports_coaching_vo2max, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        algorithmInitConfig = AlgorithmInitConfig()
        algorithmInitConfig.hrvConfig = HrvAlgorithmInitConfig(40f, 90, 30)
        with(algorithmInitConfig.sportCoachingConfig) {
            this.samplingRate = 25
            this.session = SportsCoachingSession.VO2MAX_RELAX
            this.user = SportsCoachingManager.currentUser!!
            this.history = getHistoryFromFiles(SportsCoachingManager.currentUser!!.userName)
        }
        algorithmInitConfig.enableAlgorithmsFlag =
            MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_SPORTS
        algorithmOutput.sports.user = SportsCoachingManager.currentUser!!
        setupToolbar(getString(R.string.resting_vo2max))
        menuItemArbitraryCommand.isVisible = false
        menuItemLogToFlash.isVisible = false
        menuItemSettings.isVisible = false
        if(BuildConfig.DEBUG){
            readFromFile.isVisible = true
        }
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
            vo2Max = algorithmOutput.sports.estimates.vo2max.relax.toInt()
            statisticLayout.minHrTextView.text = algorithmOutput.sports.hrStats.minHr.toString()
            statisticLayout.maxHrTextView.text = algorithmOutput.sports.hrStats.maxHr.toString()
            statisticLayout.meanHrTextView.text = algorithmOutput.sports.hrStats.meanHr.toString()
            algorithmOutput.sports.session = SportsCoachingSession.VO2MAX_RELAX
            saveMeasurement(algorithmOutput.sports, dataRecorder!!.timestamp, getMeasurementType())
            notificationResults[MXM_KEY] = "Resting VO2Max: $vo2Max"
            updateNotification()
            stopMonitoring()
        }
    }

    override fun getMeasurementType(): String {
        return getString(R.string.vo2max)
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
                sendAlgoMode()
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
        ChooserDialog(requireContext())
            .withStartFile(DataRecorder.OUTPUT_DIRECTORY.parent)
            .withChosenListener { dir, dirFile ->
                run {
                    MaximAlgorithms.init(algorithmInitConfig)
                    doAsync {
                        val inputs = readAlgorithmInputsFromFile(dirFile)
                        for (input in inputs) {
                            MaximAlgorithms.run(input, algorithmOutput)
                            if(algorithmOutput.sports.percentCompleted == 100){
                                break
                            }
                        }
                        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_SPORTS)

                        uiThread {
                            vo2Max = algorithmOutput.sports.estimates.vo2max.relax.toInt()
                            statisticLayout.minHrTextView.text =
                                algorithmOutput.sports.hrStats.minHr.toString()
                            statisticLayout.maxHrTextView.text =
                                algorithmOutput.sports.hrStats.maxHr.toString()
                            statisticLayout.meanHrTextView.text =
                                algorithmOutput.sports.hrStats.meanHr.toString()
                            saveMeasurement(
                                algorithmOutput.sports, HspStreamData.TIMESTAMP_FORMAT.format(
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
            HelpDialog.newInstance(getString(R.string.vo2max_info), getString(R.string.info))
        fragmentManager?.let { helpDialog.show(it, "helpDialog") }
    }

    private fun clearCardViewValues() {
        hr = null
        vo2Max = null
    }
}