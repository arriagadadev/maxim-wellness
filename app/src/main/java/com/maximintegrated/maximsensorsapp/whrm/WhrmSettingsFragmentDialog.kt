package com.maximintegrated.maximsensorsapp.whrm

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.WhrmSettings
import kotlinx.android.synthetic.main.dialog_whrm_settings.*
import kotlinx.android.synthetic.main.dialog_whrm_settings.view.*
import kotlin.math.max

class WhrmSettingsFragmentDialog : DialogFragment() {

    companion object {

        fun newInstance(): WhrmSettingsFragmentDialog {
            return WhrmSettingsFragmentDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val contentView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_whrm_settings, null)
        contentView.editTextCycleTime.setText((WhrmSettings.sampledModeTimeInterval/1000).toString())

        val settingsDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.whrm_settings)
            .setView(contentView)
            .setPositiveButton(R.string.save) { dialog, which ->
                val d = dialog as AlertDialog
                val editText = d.editTextCycleTime
                WhrmSettings.sampledModeTimeInterval = max(editText.text.toString().toLong() * 1000, WhrmFragment.MIN_CYCLE_TIME_IN_MILLIS)

                val fragment = targetFragment as WhrmFragment
                fragment.setupTimer()

            }.setNegativeButton(R.string.cancel) { dialog, which ->
                dialog.dismiss()
            }
            .create()

        settingsDialog.setCancelable(false)
        settingsDialog.setCanceledOnTouchOutside(false)

        return settingsDialog
    }
}