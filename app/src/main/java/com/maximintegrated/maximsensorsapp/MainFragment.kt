package com.maximintegrated.maximsensorsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.alignment.AlignmentFragment
import com.maximintegrated.maximsensorsapp.archive.ArchiveFragment
import com.maximintegrated.maximsensorsapp.exts.addFragment
import com.maximintegrated.maximsensorsapp.hrv.HrvFragment
import com.maximintegrated.maximsensorsapp.log_parser.LogParserFragment
import com.maximintegrated.maximsensorsapp.respiratory.RespiratoryFragment
import com.maximintegrated.maximsensorsapp.sleep.SleepFragment
import com.maximintegrated.maximsensorsapp.spo2.Spo2Fragment
import com.maximintegrated.maximsensorsapp.sports_coaching.SportsCoachingFragment
import com.maximintegrated.maximsensorsapp.stress.StressFragment
import com.maximintegrated.maximsensorsapp.temp.TempFragment
import com.maximintegrated.maximsensorsapp.whrm.WhrmFragment
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_main_fragment_content.*


class MainFragment : Fragment(), LandingPage {

    companion object {
        private const val ARG_DEVICE_SENSORS = "device_sensors"
        private const val ARG_FIRMWARE_ALGORITHMS = "firmware_algorithms"

        fun newInstance(
            deviceSensors: Array<String>,
            firmwareAlgorithms: Array<String>
        ): MainFragment {
            return MainFragment().apply {
                arguments = Bundle().apply {
                    putStringArray(ARG_DEVICE_SENSORS, deviceSensors)
                    putStringArray(ARG_FIRMWARE_ALGORITHMS, firmwareAlgorithms)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_main, container, false)

    private lateinit var hspViewModel: HspViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hspViewModel = ViewModelProviders.of(requireActivity()).get(HspViewModel::class.java)

        spo2MenuItemView.isEnabled = (hspViewModel.deviceModel != HspViewModel.DeviceModel.ME11B)
        spo2MenuItemView.isVisible = (hspViewModel.deviceModel != HspViewModel.DeviceModel.ME11B)

        tempMenuItemView.isEnabled = false //(hspViewModel.deviceModel == HspViewModel.DeviceModel.ME11B)
        tempMenuItemView.isVisible = false //(hspViewModel.deviceModel == HspViewModel.DeviceModel.ME11B)

        ecgMenuItemView.isEnabled = false //(hspViewModel.deviceModel == HspViewModel.DeviceModel.ME11B)
        ecgMenuItemView.isVisible = false //(hspViewModel.deviceModel == HspViewModel.DeviceModel.ME11B)

        whrmMenuItemView.isEnabled = true
        whrmMenuItemView.isVisible = true

        hrvMenuItemView.isEnabled = true
        hrvMenuItemView.isVisible = true

        respiratoryMenuItemView.isEnabled = true
        respiratoryMenuItemView.isVisible = true

        sleepMenuItemView.isEnabled = true
        sleepMenuItemView.isVisible = true

        stressMenuItemView.isEnabled = true
        stressMenuItemView.isVisible = true

        sportsCoachingMenuItemView.isEnabled = true
        sportsCoachingMenuItemView.isVisible = true

        archiveMenuItemView.isEnabled = true
        archiveMenuItemView.isVisible = true

        parserMenuItemView.isEnabled = true
        parserMenuItemView.isVisible = true

        hspViewModel.connectionState
            .observe(this) { (device, connectionState) ->
                toolbar.connectionInfo = if (hspViewModel.bluetoothDevice != null) {
                    BleConnectionInfo(connectionState, device.name, device.address)
                } else {
                    null
                }
            }

        toolbar.pageTitle = getString(R.string.maxim_hsp)


        spo2MenuItemView.setOnClickListener {
            requireActivity().addFragment(Spo2Fragment.newInstance())
        }

        tempMenuItemView.setOnClickListener {
            requireActivity().addFragment(TempFragment.newInstance())
        }

        whrmMenuItemView.setOnClickListener {
            requireActivity().addFragment(WhrmFragment.newInstance())
        }

        hrvMenuItemView.setOnClickListener {
            requireActivity().addFragment(HrvFragment.newInstance())
        }

        respiratoryMenuItemView.setOnClickListener {
            requireActivity().addFragment(RespiratoryFragment.newInstance())
        }

        sleepMenuItemView.setOnClickListener {
            requireActivity().addFragment(SleepFragment.newInstance())
        }

        stressMenuItemView.setOnClickListener {
            requireActivity().addFragment(StressFragment.newInstance())
        }

        sportsCoachingMenuItemView.setOnClickListener {
            requireActivity().addFragment(SportsCoachingFragment.newInstance())
        }

        archiveMenuItemView.setOnClickListener {
            requireActivity().addFragment(ArchiveFragment.newInstance())
        }

        parserMenuItemView.setOnClickListener {
            requireActivity().addFragment(LogParserFragment.newInstance())
        }

        alignmentMenuItemView.setOnClickListener {
            requireActivity().addFragment(AlignmentFragment.newInstance())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }
}