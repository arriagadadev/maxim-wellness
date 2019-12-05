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
                WhrmSettings.sampledModeTimeInterval = editText.text.toString().toLong() * 1000

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