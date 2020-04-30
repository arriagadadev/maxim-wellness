package com.maximintegrated.maximsensorsapp.bpt

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.maximintegrated.maximsensorsapp.R
import kotlinx.android.synthetic.main.view_old_protocol_calibration_card.view.*
import kotlinx.android.synthetic.main.view_old_protocol_calibration_card.view.progressBar

class OldProtocolCalibrationView @JvmOverloads constructor(
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
                    sbpEditText1.isEnabled = true
                    dbpEditText1.isEnabled = true
                    sbpEditText2.isEnabled = true
                    dbpEditText2.isEnabled = true
                    sbpEditText3.isEnabled = true
                    dbpEditText3.isEnabled = true
                }
                CalibrationStatus.STARTED -> {
                    progressBar.isVisible = false
                    statusImageView.isVisible = false
                    calibrationButton.text = context.getString(R.string.stop)
                    sbpEditText1.isEnabled = false
                    dbpEditText1.isEnabled = false
                    sbpEditText2.isEnabled = false
                    dbpEditText2.isEnabled = false
                    sbpEditText3.isEnabled = false
                    dbpEditText3.isEnabled = false
                }
                CalibrationStatus.PROCESSING -> {
                    progressBar.isVisible = true
                    statusImageView.isVisible = false
                    calibrationButton.text = context.getString(R.string.wait)
                    sbpEditText1.isEnabled = false
                    dbpEditText1.isEnabled = false
                    sbpEditText2.isEnabled = false
                    dbpEditText2.isEnabled = false
                    sbpEditText3.isEnabled = false
                    dbpEditText3.isEnabled = false
                }
                CalibrationStatus.SUCCESS -> {
                    progressBar.isVisible = false
                    statusImageView.isVisible = true
                    statusImageView.setImageResource(R.drawable.ic_check)
                    calibrationButton.text = context.getString(R.string.done)
                    sbpEditText1.isEnabled = true
                    dbpEditText1.isEnabled = true
                    sbpEditText2.isEnabled = true
                    dbpEditText2.isEnabled = true
                    sbpEditText3.isEnabled = true
                    dbpEditText3.isEnabled = true
                }
                CalibrationStatus.FAIL -> {
                    progressBar.isVisible = false
                    statusImageView.isVisible = true
                    statusImageView.setImageResource(R.drawable.ic_warning)
                    calibrationButton.text = context.getString(R.string.start)
                    sbpEditText1.isEnabled = true
                    dbpEditText1.isEnabled = true
                    sbpEditText2.isEnabled = true
                    dbpEditText2.isEnabled = true
                    sbpEditText3.isEnabled = true
                    dbpEditText3.isEnabled = true
                }
            }
        }

    var sbp1: Int = 0
        get() = sbpEditText1.text.toString().toIntOrZero()
        set(value){
            field = value
            sbpEditText1.setText(value)
        }

    var dbp1: Int = 0
        get() = dbpEditText1.text.toString().toIntOrZero()
        set(value){
            field = value
            dbpEditText1.setText(value)
        }

    var sbp2: Int = 0
        get() = sbpEditText2.text.toString().toIntOrZero()
        set(value){
            field = value
            sbpEditText2.setText(value)
        }

    var dbp2: Int = 0
        get() = dbpEditText2.text.toString().toIntOrZero()
        set(value){
            field = value
            dbpEditText2.setText(value)
        }

    var sbp3: Int = 0
        get() = sbpEditText3.text.toString().toIntOrZero()
        set(value){
            field = value
            sbpEditText3.setText(value)
        }

    var dbp3: Int = 0
        get() = dbpEditText3.text.toString().toIntOrZero()
        set(value){
            field = value
            dbpEditText3.setText(value)
        }

    init {
        View.inflate(context, R.layout.view_old_protocol_calibration_card, this)

        with(
            context.obtainStyledAttributes(
                attrs,
                R.styleable.OldProtocolCalibrationView,
                defStyleAttr,
                0
            )
        ) {
            titleTextView.text = getText(R.styleable.OldProtocolCalibrationView_opcv_title) ?: ""
            recycle()
        }
    }
}