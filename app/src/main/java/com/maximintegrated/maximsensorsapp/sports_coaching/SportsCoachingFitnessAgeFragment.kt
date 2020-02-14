package com.maximintegrated.maximsensorsapp.sports_coaching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.algorithms.AlgorithmInitConfig
import com.maximintegrated.algorithms.AlgorithmInput
import com.maximintegrated.algorithms.AlgorithmOutput
import com.maximintegrated.algorithms.MaximAlgorithms
import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig
import com.maximintegrated.algorithms.sports.SportsCoachingSession
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.BleConnectionInfo
import com.maximintegrated.maximsensorsapp.R
import kotlinx.android.synthetic.main.fragment_sports_coaching_fitness_age.*
import kotlinx.android.synthetic.main.include_app_bar.*
import java.util.*

class SportsCoachingFitnessAgeFragment : Fragment() {

    companion object {
        fun newInstance() = SportsCoachingFitnessAgeFragment()
    }

    private lateinit var algorithmInitConfig: AlgorithmInitConfig
    private val algorithmInput = AlgorithmInput()
    private val algorithmOutput = AlgorithmOutput()
    private lateinit var hspViewModel: HspViewModel

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
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sports_coaching_fitness_age, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.pageTitle = getString(R.string.fitness_age)
        algorithmInitConfig = AlgorithmInitConfig()
        algorithmInitConfig.hrvConfig = HrvAlgorithmInitConfig(40f, 90, 30)
        with(algorithmInitConfig.sportCoachingConfig) {
            this.samplingRate = 25
            this.session = SportsCoachingSession.VO2MAX_FROM_HISTORY
            this.user = SportsCoachingManager.currentUser!!
            this.history = getHistoryFromFiles(SportsCoachingManager.currentUser!!.userName)
        }
        algorithmInitConfig.enableAlgorithmsFlag =
            MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_SPORTS
        algorithmOutput.sports.user = SportsCoachingManager.currentUser!!
        MaximAlgorithms.init(algorithmInitConfig)
        MaximAlgorithms.run(algorithmInput, algorithmOutput)
        val fitnessAge = algorithmOutput.sports.estimates.vo2max.fitnessAge.toInt()
        fitnessAgeTextView.text = fitnessAge.toString()
        val actualAge = Calendar.getInstance().get(Calendar.YEAR) - SportsCoachingManager.currentUser!!.birthYear
        when {
            fitnessAge == 0 -> fitnessAgeInfoTextView.visibility = View.INVISIBLE
            fitnessAge > actualAge -> fitnessAgeInfoTextView.text =
                getString(R.string.fitness_age_is_older_than_actual_age)
            else -> fitnessAgeInfoTextView.text =
                getString(R.string.fitness_age_is_younger_than_actual_age)
        }
        MaximAlgorithms.end(MaximAlgorithms.FLAG_HRV or MaximAlgorithms.FLAG_SPORTS)
    }
}