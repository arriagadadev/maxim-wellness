package com.maximintegrated.maximsensorsapp.sports_coaching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.google.gson.Gson
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.BleConnectionInfo
import com.maximintegrated.maximsensorsapp.R
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.profile_layout.*

class SportsCoachingProfileFragment : Fragment() {
    companion object {
        fun newInstance() = SportsCoachingProfileFragment()
    }

    private lateinit var hspViewModel: HspViewModel

    private val gson = Gson()

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
        return inflater.inflate(R.layout.fragment_sports_coaching_profile, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.pageTitle = getString(R.string.sports_coaching)

        usernameEditText.isEnabled = false
        signupButton.text = getString(R.string.save)
        haveAccountTextView.visibility = View.INVISIBLE

        val user = gson.fromJson<SportsCoachingUser>(
            SportsCoachingSettings.currentUserJson,
            SportsCoachingUser::class.java
        )

        if (user != null) {
            usernameEditText.setText(user.userName)
            birthYearEditText.setText(user.birthYear.toString())
            genderChipGroup.check(if (user.gender == Gender.MALE) R.id.maleChip else R.id.femaleChip)
            if (user.isUnitInMetrics) {
                unitChipGroup.check(R.id.metricsChip)
                weightLayout.hint = getString(R.string.weight_in_kg)
                heightLayout.hint = getString(R.string.height_in_cm)
            } else {
                unitChipGroup.check(R.id.usChip)
                weightLayout.hint = getString(R.string.weight_in_lbs)
                heightLayout.hint = getString(R.string.height_in_inch)
            }
            weightEditText.setText(user.weight.toString())
            heightEditText.setText(user.height.toString())
        }

        signupButton.setOnClickListener {
            val birthYear = birthYearEditText.text.toString().toIntOrNull()
            val gender = if (maleChip.isChecked) Gender.MALE else Gender.FEMALE
            val isUnitInMetric = metricsChip.isChecked
            val weight = weightEditText.text.toString().toIntOrNull()
            val height = heightEditText.text.toString().toIntOrNull()
            if (birthYear == null || weight == null || height == null) {
                showWarningMessage(getString(R.string.all_fields_required))
                return@setOnClickListener
            }

            if (user != null) {
                user.birthYear = birthYear
                user.gender = gender
                user.isUnitInMetrics = isUnitInMetric
                user.weight = weight
                user.height = height
                SportsCoachingSettings.currentUserJson = gson.toJson(user)
            }
            requireActivity().onBackPressed()
        }
    }

    private fun showWarningMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}