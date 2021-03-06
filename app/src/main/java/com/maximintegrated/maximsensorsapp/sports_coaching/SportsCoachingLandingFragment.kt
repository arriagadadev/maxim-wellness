package com.maximintegrated.maximsensorsapp.sports_coaching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.BleConnectionInfo
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.exts.addFragment
import kotlinx.android.synthetic.main.fragment_sports_coaching_landing.*
import kotlinx.android.synthetic.main.include_app_bar.*

class SportsCoachingLandingFragment : Fragment() {

    companion object {
        fun newInstance() = SportsCoachingLandingFragment()
    }

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
        return inflater.inflate(R.layout.fragment_sports_coaching_landing, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.pageTitle = getString(R.string.sports_coaching)

        logoutMenuItemView.setOnClickListener {
            requireActivity().onBackPressed()
        }
        profileMenuItemView.setOnClickListener {
            requireActivity().addFragment(SportsCoachingProfileFragment.newInstance())
        }
        trainingMenuItemView.setOnClickListener {
            requireActivity().addFragment(SportsCoachingTrainingFragment.newInstance())
        }
        historyMenuItemView.setOnClickListener {
            requireActivity().addFragment(SportsCoachingHistoryFragment.newInstance())
        }
    }
}
