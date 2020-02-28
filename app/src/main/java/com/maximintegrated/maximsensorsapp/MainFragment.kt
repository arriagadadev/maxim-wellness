package com.maximintegrated.maximsensorsapp

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.archive.ArchiveFragment
import com.maximintegrated.maximsensorsapp.exts.addFragment
import com.maximintegrated.maximsensorsapp.hrv.HrvFragment
import com.maximintegrated.maximsensorsapp.respiratory.RespiratoryFragment
import com.maximintegrated.maximsensorsapp.spo2.Spo2Fragment
import com.maximintegrated.maximsensorsapp.sports_coaching.SportsCoachingFragment
import com.maximintegrated.maximsensorsapp.stress.StressFragment
import com.maximintegrated.maximsensorsapp.temp.TempFragment
import com.maximintegrated.maximsensorsapp.whrm.WhrmFragment
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_main_fragment_content.*


class MainFragment : Fragment() {

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

        tempMenuItemView.isEnabled = (hspViewModel.deviceModel == HspViewModel.DeviceModel.ME11B)
        tempMenuItemView.isVisible = (hspViewModel.deviceModel == HspViewModel.DeviceModel.ME11B)

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
            val intent = Intent(Intent.ACTION_SEND)
            intent.component =
                ComponentName("maximintegrated.com", "maximintegrated.com.MainActivity")
            try {
                startActivity(intent)
            } catch (ignore: Exception) {
                if (context != null) {
                    Toast.makeText(
                        context!!,
                        "Sleep Quality Assessment does not exist",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        stressMenuItemView.setOnClickListener {
            requireActivity().addFragment(StressFragment.newInstance())
        }

        sportsCoachingMenuItemView.setOnClickListener {
            requireActivity().addFragment(SportsCoachingFragment.newInstance())
            /*val intent = Intent(Intent.ACTION_SEND)
            intent.component = ComponentName(
                "com.maximintegrated.sportscoaching",
                "com.maximintegrated.sportscoaching.view.MainActivity"
            )
            try {
                startActivity(intent)
            } catch (ignore: Exception) {
                if (context != null) {
                    Toast.makeText(
                        context!!,
                        "Sports Coaching does not exist",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }*/
        }

        archiveMenuItemView.setOnClickListener {
            requireActivity().addFragment(ArchiveFragment.newInstance())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }
}