package com.maximintegrated.maximsensorsapp.sports_coaching

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.maximintegrated.algorithms.sports.SportsCoachingUser
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.BleConnectionInfo
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.exts.addFragment
import kotlinx.android.synthetic.main.fragment_sports_coaching.*
import kotlinx.android.synthetic.main.include_app_bar.*
import kotlinx.android.synthetic.main.login_layout.*
import kotlinx.android.synthetic.main.profile_layout.*


class SportsCoachingFragment : Fragment() {

    companion object {
        fun newInstance() = SportsCoachingFragment()
    }

    private lateinit var hspViewModel: HspViewModel

    private val gson = Gson()

    private var users: ArrayList<SportsCoachingUser> = arrayListOf()

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
        return inflater.inflate(R.layout.fragment_sports_coaching, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.pageTitle = getString(R.string.sports_coaching)
        haveAccountTextView.paintFlags = haveAccountTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        noAccountTextView.paintFlags = noAccountTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        val usernameList: ArrayList<String> = arrayListOf(getString(R.string.select_user))

        if (SportsCoachingSettings.userListJson != "") {
            val listType = object : TypeToken<ArrayList<SportsCoachingUser>>() {}.type
            users = gson.fromJson<ArrayList<SportsCoachingUser>>(
                SportsCoachingSettings.userListJson,
                listType
            ) ?: arrayListOf()
        }

        usernameList.addAll(users.map { it.userName })
        ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            usernameList
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            userSpinner.adapter = adapter
        }

        if (usernameList.size > 1) {
            userSpinner.setSelection(1)
        }

        haveAccountTextView.setOnClickListener {
            loginLayout.visibility = View.VISIBLE
            profileLayout.visibility = View.GONE
        }

        noAccountTextView.setOnClickListener {
            loginLayout.visibility = View.GONE
            profileLayout.visibility = View.VISIBLE
        }

        unitChipGroup.setOnCheckedChangeListener { group, checkedId ->
            run {
                if (checkedId == R.id.metricsChip) {
                    weightLayout.hint = getString(R.string.weight_in_kg)
                    heightLayout.hint = getString(R.string.height_in_cm)
                } else {
                    weightLayout.hint = getString(R.string.weight_in_lbs)
                    heightLayout.hint = getString(R.string.height_in_inch)
                }
            }
        }

        loginButton.setOnClickListener {
            var index = userSpinner.selectedItemPosition
            if (index > 0) {
                index--
            } else {
                if (usernameList.size == 1) {
                    showWarningMessage(getString(R.string.should_create_profile))
                } else {
                    showWarningMessage(getString(R.string.should_select_user))
                }
                return@setOnClickListener
            }
            val user = users[index]
            SportsCoachingSettings.currentUserJson = gson.toJson(user)
            requireActivity().addFragment(SportsCoachingLandingFragment.newInstance())
        }

        signupButton.setOnClickListener {
            val name = usernameEditText.text.toString()
            val birthYear = birthYearEditText.text.toString().toIntOrNull()
            val gender =
                if (maleChip.isChecked) SportsCoachingUser.Gender.MALE else SportsCoachingUser.Gender.FEMALE
            val isUnitInMetric = metricsChip.isChecked
            val weight = weightEditText.text.toString().toIntOrNull()
            val height = heightEditText.text.toString().toIntOrNull()
            if (name == "" || birthYear == null || weight == null || height == null) {
                showWarningMessage(getString(R.string.all_fields_required))
                return@setOnClickListener
            }
            val user = SportsCoachingUser(name, birthYear, gender, weight, height, isUnitInMetric)
            users.add(user)
            SportsCoachingSettings.currentUserJson = gson.toJson(user)
            SportsCoachingSettings.userListJson = gson.toJson(users)
            requireActivity().addFragment(SportsCoachingLandingFragment.newInstance())
        }
    }

    private fun showWarningMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}