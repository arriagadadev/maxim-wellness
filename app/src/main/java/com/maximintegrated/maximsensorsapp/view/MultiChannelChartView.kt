package com.maximintegrated.maximsensorsapp.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.get
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.exts.getThemeColor
import java.text.DecimalFormat

data class DataSetInfo(@StringRes val nameRes: Int, @ColorRes val colorRes: Int)

class MultiChannelChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_MAX_ENTRY_COUNT = 1000
    }

    private val chipGroupView: ChipGroup
    private val chipStates = arrayOf(
        intArrayOf(android.R.attr.state_checked),
        intArrayOf()
    )

    private val lineChartView: LineChart
    private var dataSetList: List<LineDataSet> = emptyList()

    var dataSetInfoList: List<DataSetInfo> = emptyList()
        set(value) {
            field = value
            setupDataSets()
        }

    var maximumEntryCount = DEFAULT_MAX_ENTRY_COUNT
        set(value) {
            field = value
            clearChart()
            setupChart()
        }

    init {
        inflate(context, R.layout.view_multi_channel_chart, this)

        chipGroupView = findViewById(R.id.chip_group_view)
        lineChartView = findViewById(R.id.line_chart_view)

        setupChart()
    }

    private fun setupChart() {
        with(lineChartView) {
            data = LineData()

            setNoDataText("No data to display")
            setNoDataTextColor(context.getThemeColor(R.attr.colorAccent))

            legend.isEnabled = false
            description.isEnabled = false

            setTouchEnabled(false)
            isAutoScaleMinMaxEnabled = true
            axisRight.isEnabled = false
            xAxis.isEnabled = false

            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = maximumEntryCount.toFloat()

            setVisibleXRangeMaximum(maximumEntryCount.toFloat())
        }
    }

    fun setFormatterForYAxis(formatter: ValueFormatter) {
        lineChartView.axisLeft.valueFormatter = formatter
    }

    private fun setupDataSets() {
        clearChart()
        chipGroupView.removeAllViews()

        for (index in 0..dataSetInfoList.lastIndex) {
            val dataSet = dataSetInfoList[index]

            chipGroupView.addView(
                Chip(chipGroupView.context).apply {
                    isCheckable = true
                    isChecked = true
                    checkedIcon = null

                    text = context.getString(dataSet.nameRes)
                    setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text))

                    setRippleColorResource(dataSet.colorRes)
                    setChipStrokeColorResource(dataSet.colorRes)
                    setChipStrokeWidthResource(R.dimen.chip_stroke_width)

                    chipBackgroundColor = ColorStateList(
                        chipStates,
                        intArrayOf(
                            ContextCompat.getColor(context, dataSet.colorRes),
                            Color.TRANSPARENT
                        )
                    )

                    setOnCheckedChangeListener { chipView, isChecked ->
                        if (!isChecked && areAllChipsUnchecked()) {
                            chipView.isChecked = true
                        } else {
                            toggleDataSetVisibility(index, isChecked)
                        }
                    }
                }
            )
        }

        dataSetList = dataSetInfoList.map {
            LineDataSet(null, context.getString(it.nameRes)).apply {
                color = ContextCompat.getColor(context, it.colorRes)
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
            }
        }
    }

    fun clearChart() {
        for (dataSet in dataSetList) {
            dataSet.clear()
        }

        with(lineChartView) {
            data.dataSets.clear()

            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = maximumEntryCount.toFloat()

            data.notifyDataChanged()
            notifyDataSetChanged()
        }

        lineChartView.isInvisible = true
//        noDataMessageView.isVisible = true
    }

    private fun addEntryToDataSet(dataSet: LineDataSet, xValue: Float, yValue: Float) {
        if (dataSet.entryCount == maximumEntryCount) {
            dataSet.removeFirst()
        }

        dataSet.addEntry(Entry(xValue, yValue))
    }

    fun addData(vararg channelData: Int) {
        if (dataSetList.isEmpty() || channelData.size != dataSetList.size) return

        lineChartView.isVisible = true

        with(lineChartView.data) {
            if (dataSets.isEmpty()) {
                for (index in 0..dataSetList.lastIndex) {
                    val dataSet = dataSetList[index]
                    addDataSet(dataSet)

                }
            }
        }

        val newEntryX = if (dataSetList[0].entryCount != 0) {
            dataSetList[0].getEntryForIndex(dataSetList[0].entryCount - 1).x + 1f
        } else {
            1f
        }

        for (index in 0..channelData.lastIndex) {
            addEntryToDataSet(dataSetList[index], newEntryX, channelData[index].toFloat())
        }

        with(lineChartView) {
            if (newEntryX > maximumEntryCount) {
                xAxis.resetAxisMinimum()
                xAxis.resetAxisMaximum()
            }

            data.notifyDataChanged()
            notifyDataSetChanged()
            moveViewToX(newEntryX)
        }

    }

    fun addData(vararg channelData: Float) {
        if (dataSetList.isEmpty() || channelData.size != dataSetList.size) return

        lineChartView.isVisible = true

        with(lineChartView.data) {
            if (dataSets.isEmpty()) {
                for (index in 0..dataSetList.lastIndex) {
                    val dataSet = dataSetList[index]
                    addDataSet(dataSet)

                }
            }
        }

        val newEntryX = if (dataSetList[0].entryCount != 0) {
            dataSetList[0].getEntryForIndex(dataSetList[0].entryCount - 1).x + 1f
        } else {
            1f
        }

        for (index in 0..channelData.lastIndex) {
            addEntryToDataSet(dataSetList[index], newEntryX, channelData[index])
        }

        with(lineChartView) {
            if (newEntryX > maximumEntryCount) {
                xAxis.resetAxisMinimum()
                xAxis.resetAxisMaximum()
            }

            data.notifyDataChanged()
            notifyDataSetChanged()
            moveViewToX(newEntryX)
        }
    }

    private fun toggleDataSetVisibility(index: Int, visible: Boolean) {
        with(lineChartView.data) {
            if (visible) {
                if (!dataSets.contains(dataSetList[index])) {
                    addDataSet(dataSetList[index])
                }
            } else if (dataSetCount != 1) {
                removeDataSet(dataSetList[index])
            }
        }
    }

    private fun areAllChipsUnchecked(): Boolean {
        return !chipGroupView.children.any { it is Chip && it.isChecked }
    }

    private fun isChannelAtIndexChecked(index: Int): Boolean {
        if (index < chipGroupView.childCount) {
            return (chipGroupView[index] as Chip).isChecked
        }

        return false
    }

}

class FloatValueFormatter(private val decimalFormat: DecimalFormat) : ValueFormatter() {

    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return decimalFormat.format(value)
    }
}