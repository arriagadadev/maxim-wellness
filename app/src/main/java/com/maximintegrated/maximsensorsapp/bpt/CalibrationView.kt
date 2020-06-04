package com.maximintegrated.maximsensorsapp.bpt

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.maximintegrated.maximsensorsapp.R
import kotlinx.android.synthetic.main.view_calibration_card.view.*
import kotlinx.android.synthetic.main.view_calibration_card.view.progressBar

class CalibrationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) :  MaterialCardView(context, attrs, defStyleAttr) {

    var status = CalibrationStatus.IDLE
        set(value) {
            field = value
            when(value){
                CalibrationStatus.IDLE -> {
                    progressBar.isVisible = false
                    statusImageView.isVisible = true
                    calibrationButton.text = context.getString(R.string.start)
                    sbpTextInputLayout.isEnabled = true
                    dbpTextInputLayout.isEnabled = true
                }
                CalibrationStatus.STARTED -> {
                    progressBar.isVisible = false
                    statusImageView.isVisible = false
                    calibrationButton.text = context.getString(R.string.stop)
                    sbpTextInputLayout.isEnabled = false
                    dbpTextInputLayout.isEnabled = false
                }
                CalibrationStatus.PROCESSING -> {
                    progressBar.isVisible = true
                    statusImageView.isVisible = false
                    calibrationButton.text = context.getString(R.string.wait)
                    sbpTextInputLayout.isEnabled = false
                    dbpTextInputLayout.isEnabled = false
                }
                CalibrationStatus.SUCCESS -> {
                    progressBar.isVisible = false
                    statusImageView.isVisible = true
                    statusImageView.setImageResource(R.drawable.ic_check)
                    calibrationButton.text = context.getString(R.string.start)
                    sbpTextInputLayout.isEnabled = true
                    dbpTextInputLayout.isEnabled = true
                }
                CalibrationStatus.FAIL -> {
                    progressBar.isVisible = false
                    statusImageView.isVisible = true
                    statusImageView.setImageResource(R.drawable.ic_warning)
                    calibrationButton.text = context.getString(R.string.start)
                    sbpTextInputLayout.isEnabled = true
                    dbpTextInputLayout.isEnabled = true
                }
            }
        }

    var sbp: Int = 0
        get() = sbpTextInputLayout.editText?.text.toString().toIntOrZero()
        set(value){
            field = value
            sbpTextInputLayout.editText?.setText(value)
        }

    var dbp: Int = 0
        get() = dbpTextInputLayout.editText?.text.toString().toIntOrZero()
        set(value){
            field = value
            dbpTextInputLayout.editText?.setText(value)
        }

    init {
        View.inflate(context, R.layout.view_calibration_card, this)

        with(
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CalibrationView,
                defStyleAttr,
                0
            )
        ) {
            titleTextView.text = getText(R.styleable.CalibrationView_cv_title) ?: ""
            recycle()
        }
    }


}