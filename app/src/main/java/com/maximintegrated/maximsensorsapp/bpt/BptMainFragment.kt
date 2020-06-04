package com.maximintegrated.maximsensorsapp.bpt

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.BleConnectionInfo
import com.maximintegrated.maximsensorsapp.LandingPage
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.exts.addFragment
import com.maximintegrated.maximsensorsapp.showAlertDialog
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.include_bpt_main_fragment_content.*
import java.text.SimpleDateFormat
import java.util.*

class BptMainFragment : Fragment(), LandingPage {

    companion object {
        fun newInstance(): BptMainFragment {
            return BptMainFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_bpt_main, container, false)

    private lateinit var hspViewModel: HspViewModel

    private lateinit var bptViewModel: BptViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hspViewModel = ViewModelProviders.of(requireActivity()).get(HspViewModel::class.java)

        bptViewModel = ViewModelProviders.of(requireActivity()).get(BptViewModel::class.java)

        hspViewModel.connectionState
            .observe(this) { (device, connectionState) ->
                toolbar.connectionInfo = if (hspViewModel.bluetoothDevice != null) {
                    BleConnectionInfo(connectionState, device.name, device.address)
                } else {
                    null
                }
            }

        toolbar.pageTitle = getString(R.string.bp_trending)

        bptViewModel.userList.observe(this) {
            setupAdapter(it)
            if (BptSettings.currentUser != "") {
                val index = BptSettings.users.indexOf(BptSettings.currentUser)
                if (index == -1) {
                    BptSettings.currentUser = ""
                } else {
                    userSpinner.setSelection(index + 1)
                }
            }
        }

        bptViewModel.historyDataList.observe(this){
            if(it == null){
                calibrationMenuItemView.isEnabled = false
                measureBpMenuItemView.isEnabled = false
                bpHistoryMenuItemView.isEnabled = false
            }else{
                calibrationMenuItemView.isEnabled = true
                measureBpMenuItemView.isEnabled = true
                bpHistoryMenuItemView.isEnabled = true
            }
        }

        bptViewModel.refreshHistoryData()


        userSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 0) {
                    BptSettings.currentUser = ""
                } else {
                    BptSettings.currentUser = bptViewModel.userList.value?.get(position) ?: ""
                }
                bptViewModel.refreshHistoryData()
            }
        }

        newUserButton.setOnClickListener {
            val name = newUserEditText.text.toString().trim()
            if (name == "") {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.username_empty_error),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                BptSettings.currentUser = name
                bptViewModel.addNewUser(name)
                newUserEditText.setText("")
                newUserEditText.onEditorAction(EditorInfo.IME_ACTION_DONE)
                bptViewModel.refreshHistoryData()
            }
        }

        calibrationMenuItemView.setOnClickListener {
            if (BptSettings.currentUser == "") {
                showUserNotSelectedError()
            } else{
                val lastCalibration = bptViewModel.historyDataList.value?.findLast { it.isCalibration }
                if(lastCalibration != null && !lastCalibration.isExpired()){
                    val c = requireContext()
                    val date = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date(lastCalibration.timestamp))
                    val ref = "${lastCalibration.sbp1}/${lastCalibration.dbp1}  ${lastCalibration.sbp2}/${lastCalibration.dbp2}  ${lastCalibration.sbp3}/${lastCalibration.dbp3}"
                    showAlertDialog(c, c.getString(R.string.warning), c.getString(R.string.valid_calibration_warning, date, ref), c.getString(R.string.yes)){
                        requireActivity().addFragment(BptCalibrationFragment.newInstance())
                    }
                }else{
                    requireActivity().addFragment(BptCalibrationFragment.newInstance())
                }
            }
        }

        measureBpMenuItemView.setOnClickListener {
            if (BptSettings.currentUser == "") {
                showUserNotSelectedError()
            } else {
                val lastCalibration = bptViewModel.historyDataList.value?.findLast { it.isCalibration }
                when {
                    lastCalibration == null -> {
                        val c = requireContext()
                        showAlertDialog(c, c.getString(R.string.warning), c.getString(R.string.no_calibration_warning), c.getString(R.string.ok)){
                            requireActivity().addFragment(BptCalibrationFragment.newInstance())
                        }
                    }
                    lastCalibration.isExpired() -> {
                        val c = requireContext()
                        val date = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date(lastCalibration.timestamp))
                        showAlertDialog(c, c.getString(R.string.warning), c.getString(R.string.expired_calibration_warning, date), c.getString(R.string.ok)){
                            requireActivity().addFragment(BptCalibrationFragment.newInstance())
                        }
                    }
                    else -> requireActivity().addFragment(BptMeasurementFragment.newInstance())
                }
            }
        }

        bpHistoryMenuItemView.setOnClickListener {
            if (BptSettings.currentUser == "") {
                showUserNotSelectedError()
            } else {
                requireActivity().addFragment(BptHistoryFragment.newInstance())
            }
        }

        bpTutorialMenuItemView.setOnClickListener {
            requireActivity().startActivity(Intent(requireContext(), BpTutorialActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }


    private fun setupAdapter(list: List<String>?) {
        ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            list ?: emptyList()
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            userSpinner.adapter = adapter
        }
    }

    private fun showUserNotSelectedError() {
        Toast.makeText(
            requireContext(),
            getString(R.string.user_not_selected_error),
            Toast.LENGTH_SHORT
        ).show()
    }
}