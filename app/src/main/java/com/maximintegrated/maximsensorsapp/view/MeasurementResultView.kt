package com.maximintegrated.maximsensorsapp.view

import android.content.Context
import android.util.AttributeSet
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.maximintegrated.maximsensorsapp.R
import kotlinx.android.synthetic.main.view_measurement_result.view.*

class MeasurementResultView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    var title: CharSequence
        get() = titleView.text
        set(value) {
            titleView.text = value
        }

    var unit: CharSequence = ""
        set(value) {
            field = value
            updateResultText()
        }

    var isMeasuring: Boolean = false
        set(value) {
            field = value
            updateViewVisibilities()
        }

    var isTimeout: Boolean = false
        set(value) {
            field = value
            updateViewVisibilities()
        }

    var measurementProgress: Int = 0
        set(value) {
            field = value
            updateViewVisibilities()
        }

    var result: Int? = null
        set(value) {
            field = value
            updateResultText()
            updateViewVisibilities()
            flashResultView(flash)
        }

    var flash = false

    var alphaAnimation: Animation

    init {
        inflate(context, R.layout.view_measurement_result, this)

        with(
            context.obtainStyledAttributes(
                attrs,
                R.styleable.MeasurementResultView,
                defStyleAttr,
                0
            )
        ) {
            title = getText(R.styleable.MeasurementResultView_mrv_title)
            unit = getString(R.styleable.MeasurementResultView_mrv_unit) ?: ""
            flash = getBoolean(R.styleable.MeasurementResultView_mrv_enable_flash, false)
            recycle()
        }

        alphaAnimation = AlphaAnimation(0.1f, 1f)
        alphaAnimation.duration = 1000L
        alphaAnimation.startOffset = 0
        alphaAnimation.repeatMode = Animation.RESTART
        alphaAnimation.repeatCount = 0
    }

    private fun showAsReadyToMeasure() {
        measuringGroup.isVisible = false
        resultView.isVisible = false
        timeoutGroup.isVisible = false

        readyToStartMessageView.isVisible = true
    }

    private fun showAsMeasuringIndeterminate() {
        readyToStartMessageView.isVisible = false
        resultView.isVisible = false
        timeoutGroup.isVisible = false

        measuringCircleView.spin()
        measuringGroup.isVisible = true
    }

    private fun showAsMeasuringWithProgress() {
        readyToStartMessageView.isVisible = false
        resultView.isVisible = false
        timeoutGroup.isVisible = false

        measuringCircleView.stopSpinning()
        measuringCircleView.setValue(measurementProgress.toFloat())
        measuringGroup.isVisible = true
    }

    private fun showAsOperationTimeout() {
        readyToStartMessageView.isVisible = false
        measuringGroup.isVisible = false
        resultView.isVisible = false

        timeoutGroup.isVisible = true
    }

    private fun showResult() {
        readyToStartMessageView.isVisible = false
        measuringGroup.isVisible = false
        timeoutGroup.isVisible = false

        resultView.isVisible = true
    }

    private fun flashResultView(flashEnabled: Boolean) {
        if (flashEnabled && resultView.isVisible) {
            if (!alphaAnimation.hasStarted() || alphaAnimation.hasEnded()) {
                resultView.startAnimation(alphaAnimation)
            }
        }
    }

    private fun updateResultText() {
        resultView.text = buildSpannedString {
            append((result ?: 0).toString())
            scale(0.6f) {
                append(' ')
                append(unit)
            }
        }
    }

    private fun updateViewVisibilities() {
        if (result == null) {
            if (isMeasuring) {
                if (measurementProgress == 0) {
                    showAsMeasuringIndeterminate()
                } else {
                    showAsMeasuringWithProgress()
                }
            } else if (isTimeout) {
                showAsOperationTimeout()
            } else {
                showAsReadyToMeasure()
            }
        } else if (result == 0) {
            if (isMeasuring) {
                resultView.text = "--"
            } else {
                showAsReadyToMeasure()
            }
        } else {
            showResult()
        }
    }
}