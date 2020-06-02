package com.maximintegrated.maximsensorsapp.bpt

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.card.MaterialCardView
import com.maximintegrated.maximsensorsapp.R
import kotlinx.android.synthetic.main.view_old_protocol_calibration_card.view.*
import java.util.*

class OldProtocolCalibrationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    companion object {
        const val WAITING_TIME_FOR_NEW_REFERENCE_DATA_IN_SEC = 60
    }

    var status = CalibrationStatus.IDLE
        set(value) {
            field = value
            when (value) {
                CalibrationStatus.IDLE -> {
                    progressBar.isVisible = false
                    statusImageView.isVisible = true
                    calibrationButton.text = context.getString(R.string.start)
                    repeatButton.isVisible = false
                    confirmCheckBox1.isEnabled = true
                    confirmCheckBox2.isEnabled = true
                    confirmCheckBox3.isEnabled = true
                }
                CalibrationStatus.STARTED -> {
                    progressBar.isVisible = false
                    statusImageView.isVisible = false
                    calibrationButton.text = context.getString(R.string.stop)
                    repeatButton.isVisible = false
                    confirmCheckBox1.isEnabled = false
                    confirmCheckBox2.isEnabled = false
                    confirmCheckBox3.isEnabled = false
                }
                CalibrationStatus.PROCESSING -> {
                    progressBar.isVisible = true
                    statusImageView.isVisible = false
                    calibrationButton.text = context.getString(R.string.wait)
                    repeatButton.isVisible = false
                    confirmCheckBox1.isEnabled = false
                    confirmCheckBox2.isEnabled = false
                    confirmCheckBox3.isEnabled = false
                }
                CalibrationStatus.SUCCESS -> {
                    progressBar.isVisible = false
                    statusImageView.isVisible = true
                    statusImageView.setImageResource(R.drawable.ic_check)
                    calibrationButton.text = context.getString(R.string.done)
                    repeatButton.isVisible = true
                    confirmCheckBox1.isEnabled = true
                    confirmCheckBox2.isEnabled = true
                    confirmCheckBox3.isEnabled = true
                }
                CalibrationStatus.FAIL -> {
                    progressBar.isVisible = false
                    statusImageView.isVisible = true
                    statusImageView.setImageResource(R.drawable.ic_warning)
                    calibrationButton.text = context.getString(R.string.start)
                    repeatButton.isVisible = false
                    confirmCheckBox1.isEnabled = true
                    confirmCheckBox2.isEnabled = true
                    confirmCheckBox3.isEnabled = true
                }
            }
        }

    var sbp1: Int = 0
        get() = sbpEditText1.text.toString().toIntOrZero()
        set(value) {
            field = value
            sbpEditText1.setText(value)
        }

    var dbp1: Int = 0
        get() = dbpEditText1.text.toString().toIntOrZero()
        set(value) {
            field = value
            dbpEditText1.setText(value)
        }

    var sbp2: Int = 0
        get() = sbpEditText2.text.toString().toIntOrZero()
        set(value) {
            field = value
            sbpEditText2.setText(value)
        }

    var dbp2: Int = 0
        get() = dbpEditText2.text.toString().toIntOrZero()
        set(value) {
            field = value
            dbpEditText2.setText(value)
        }

    var sbp3: Int = 0
        get() = sbpEditText3.text.toString().toIntOrZero()
        set(value) {
            field = value
            sbpEditText3.setText(value)
        }

    var dbp3: Int = 0
        get() = dbpEditText3.text.toString().toIntOrZero()
        set(value) {
            field = value
            dbpEditText3.setText(value)
        }

    private var referenceMeasurementState = 1
        set(value) {
            field = value
            updateCalibrationViews()
        }

    private val order = LinkedList<Int>()

    private fun updateCalibrationViews() {
        when (referenceMeasurementState) {
            1 -> {
                sbpEditText1.isEnabled = true
                dbpEditText1.isEnabled = true
                availableTextView1.isInvisible = false
                timerTextView1.isInvisible = false
                confirmCheckBox1.isInvisible = true
            }
            2 -> {
                sbpEditText2.isEnabled = true
                dbpEditText2.isEnabled = true
                availableTextView2.isInvisible = false
                timerTextView2.isInvisible = false
                confirmCheckBox2.isInvisible = true
            }
            3 -> {
                sbpEditText3.isEnabled = true
                dbpEditText3.isEnabled = true
                availableTextView3.isInvisible = false
                timerTextView3.isInvisible = false
                confirmCheckBox3.isInvisible = true
            }
        }
    }

    private var counter = WAITING_TIME_FOR_NEW_REFERENCE_DATA_IN_SEC
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (counter != 0) {
                counter--
                handler.postDelayed(this, 1000)
            }
            if (counter == 0) {
                when (referenceMeasurementState) {
                    1 -> {
                        confirmCheckBox1.isInvisible = false
                        availableTextView1.isInvisible = true
                        timerTextView1.isInvisible = true
                    }
                    2 -> {
                        confirmCheckBox2.isInvisible = false
                        availableTextView2.isInvisible = true
                        timerTextView2.isInvisible = true
                    }
                    3 -> {
                        confirmCheckBox3.isInvisible = false
                        availableTextView3.isInvisible = true
                        timerTextView3.isInvisible = true
                    }
                }
                handler.removeCallbacks(this)
            }
            when (referenceMeasurementState) {
                1 -> {
                    timerTextView1.text = String.format("00:%02d", counter)
                }
                2 -> {
                    timerTextView2.text = String.format("00:%02d", counter)
                }
                3 -> {
                    timerTextView3.text = String.format("00:%02d", counter)
                }
            }
        }
    }

    init {
        View.inflate(context, R.layout.view_old_protocol_calibration_card, this)
        order.addAll(listOf(1, 2, 3))
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

        confirmCheckBox1.setOnCheckedChangeListener { _, isChecked ->
            sbpEditText1.isEnabled = !isChecked
            dbpEditText1.isEnabled = !isChecked
            val enableButton = confirmCheckBox1.isChecked && confirmCheckBox2.isChecked && confirmCheckBox3.isChecked
            calibrationButton.isEnabled = enableButton
            repeatButton.isEnabled = enableButton
            if (referenceMeasurementState == 1) {
                if (order.isNotEmpty()) {
                    handler.removeCallbacks(tickRunnable)
                    counter = WAITING_TIME_FOR_NEW_REFERENCE_DATA_IN_SEC
                    handler.postDelayed(tickRunnable, 1000)
                    referenceMeasurementState = order.removeFirst()
                } else {
                    referenceMeasurementState = 0
                }
            }
        }
        confirmCheckBox2.setOnCheckedChangeListener { _, isChecked ->
            sbpEditText2.isEnabled = !isChecked
            dbpEditText2.isEnabled = !isChecked
            val enableButton = confirmCheckBox1.isChecked && confirmCheckBox2.isChecked && confirmCheckBox3.isChecked
            calibrationButton.isEnabled = enableButton
            repeatButton.isEnabled = enableButton
            if (referenceMeasurementState == 2) {
                if (order.isNotEmpty()) {
                    handler.removeCallbacks(tickRunnable)
                    counter = WAITING_TIME_FOR_NEW_REFERENCE_DATA_IN_SEC
                    handler.postDelayed(tickRunnable, 1000)
                    referenceMeasurementState = order.removeFirst()
                } else {
                    referenceMeasurementState = 0
                }
            }
        }
        confirmCheckBox3.setOnCheckedChangeListener { _, isChecked ->
            sbpEditText3.isEnabled = !isChecked
            dbpEditText3.isEnabled = !isChecked
            val enableButton = confirmCheckBox1.isChecked && confirmCheckBox2.isChecked && confirmCheckBox3.isChecked
            calibrationButton.isEnabled = enableButton
            repeatButton.isEnabled = enableButton
            if (referenceMeasurementState == 3) {
                if (order.isNotEmpty()) {
                    handler.removeCallbacks(tickRunnable)
                    counter = WAITING_TIME_FOR_NEW_REFERENCE_DATA_IN_SEC
                    handler.postDelayed(tickRunnable, 1000)
                    referenceMeasurementState = order.removeFirst()
                } else {
                    referenceMeasurementState = 0
                }
            }
        }

        sbpEditText1.doAfterTextChanged {
            confirmCheckBox1.isEnabled = checkValueExists(it, dbpEditText1.text)
        }
        dbpEditText1.doAfterTextChanged {
            confirmCheckBox1.isEnabled = checkValueExists(sbpEditText1.text, it)
        }
        sbpEditText2.doAfterTextChanged {
            confirmCheckBox2.isEnabled = checkValueExists(it, dbpEditText2.text)
        }
        dbpEditText2.doAfterTextChanged {
            confirmCheckBox2.isEnabled = checkValueExists(sbpEditText2.text, it)
        }
        sbpEditText3.doAfterTextChanged {
            confirmCheckBox3.isEnabled = checkValueExists(it, dbpEditText3.text)
        }
        dbpEditText3.doAfterTextChanged {
            confirmCheckBox3.isEnabled = checkValueExists(sbpEditText3.text, it)
        }

        order.removeFirst()
    }

    private fun checkValueExists(sbpEditable: Editable?, dbpEditable: Editable?): Boolean {
        return if (sbpEditable == null || dbpEditable == null) {
            false
        } else {
            !(sbpEditable.toString() == "" || dbpEditable.toString() == "")
        }
    }
}