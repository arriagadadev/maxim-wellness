package com.maximintegrated.maximsensorsapp.bpt

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maximintegrated.bpt.hsp.HspBptStreamData
import com.maximintegrated.maximsensorsapp.R
import kotlinx.android.synthetic.main.bpt_history_item.view.*
import java.util.*

class BptHistoryAdapter :
    ListAdapter<BptHistoryData, BptHistoryAdapter.BptHistoryViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<BptHistoryData>() {

        override fun areItemsTheSame(oldItem: BptHistoryData, newItem: BptHistoryData): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(
            oldItem: BptHistoryData,
            newItem: BptHistoryData
        ): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }
    }

    class BptHistoryViewHolder(private val parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.bpt_history_item, parent, false
        )
    ) {
        private val dateTextView: TextView by lazy { itemView.historyDateTextView }
        private val typeTextView: TextView by lazy { itemView.historyTypeTextView }
        private val measurementTextView: TextView by lazy { itemView.historyMeasurementTextView }
        private val calibrationTextView: TextView by lazy { itemView.historyCalibrationRefTextView }
        private val hrSpo2TextView: TextView by lazy { itemView.historyHrSpo2TextView }

        fun bind(item: BptHistoryData) {
            dateTextView.text = HspBptStreamData.TIMESTAMP_FORMAT.format(Date(item.timestamp))
            typeTextView.text =
                parent.context.getString(if (item.isCalibration) R.string.calibration else R.string.measurement)
            if (item.isCalibration) {
                calibrationTextView.isInvisible = false
                measurementTextView.isInvisible = true
                calibrationTextView.text = buildSpannedString {
                    bold { append("Ref 1: ") }
                    append("${item.sbp1} / ${item.dbp1}\n")
                    bold { append("Ref 2: ") }
                    append("${item.sbp2} / ${item.dbp2}\n")
                    bold { append("Ref 3: ") }
                    append("${item.sbp3} / ${item.dbp3}")
                }
            } else {
                calibrationTextView.isInvisible = true
                measurementTextView.isInvisible = false
                measurementTextView.text =
                    parent.context.getString(R.string.sbp_dbp_format, item.sbp1, item.dbp1)
            }
            hrSpo2TextView.text = buildSpannedString {
                bold { append("HR: ") }
                append("${item.hr} bpm  ")
                bold { append("SpO2: ") }
                append("${item.spo2} %")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BptHistoryViewHolder {
        return BptHistoryViewHolder(parent)
    }

    override fun onBindViewHolder(holder: BptHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}