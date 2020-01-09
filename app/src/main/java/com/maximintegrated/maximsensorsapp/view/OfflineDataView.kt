package com.maximintegrated.maximsensorsapp.view

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.maximintegrated.maximsensorsapp.OfflineChartData
import com.maximintegrated.maximsensorsapp.R
import kotlinx.android.synthetic.main.offline_data_item.view.*
import java.util.*

class OfflineDataView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var chartMap: HashMap<Int, LineData> = hashMapOf()

    var title: CharSequence
        get() = chartTitle.text
        set(value) {
            chartTitle.text = value
        }

    init {
        inflate(context, R.layout.offline_data_item, this)

        with(
            context.obtainStyledAttributes(
                attrs,
                R.styleable.OfflineDataView,
                defStyleAttr,
                0
            )
        ) {
            title = getString(R.styleable.OfflineDataView_odv_title) ?: ""
            recycle()
        }

        setupChart()
    }

    fun put(key: Int, data: OfflineChartData) {

        val dataSet = LineDataSet(emptyList(), "")
        dataSet.setDrawCircles(false)
        dataSet.setDrawFilled(false)
        dataSet.lineWidth = 2f
        dataSet.label = data.title
        dataSet.values = data.dataSetValues
        chartMap[key] = LineData(dataSet)
    }

    fun display(key: Int) {
        if (chartMap.containsKey(key)) {
            lineChart.data = chartMap[key]
            chartTitle.text = lineChart.data.dataSetLabels[0]
            lineChart.invalidate()
        }
    }

    private fun setupChart() {
        lineChart.extraBottomOffset = 10f
        lineChart.description.isEnabled = false
        lineChart.setDrawGridBackground(false)
        lineChart.legend.isEnabled = false
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(false)
        lineChart.axisRight.isEnabled = false
        lineChart.axisLeft.setDrawGridLines(false)
        lineChart.setVisibleXRangeMaximum(1000f)

        val xAxis = lineChart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
//        xAxis.valueFormatter = IAxisValueFormatter { value, axis -> convert(value) } as ValueFormatter?
        xAxis.textSize = 12f
        lineChart.axisLeft.textSize = 12f
    }

    fun convert(time: Float): String {
        val cal = Calendar.getInstance()
        cal.time = Date(time.toLong())
        return cal.get(Calendar.HOUR_OF_DAY).toString() + ":" + if (cal.get(Calendar.MINUTE) < 10) ("0" + cal.get(
            Calendar.MINUTE
        )) else cal.get(Calendar.MINUTE)
    }
}