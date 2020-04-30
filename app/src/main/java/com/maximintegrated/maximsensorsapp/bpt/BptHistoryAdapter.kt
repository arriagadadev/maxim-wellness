package com.maximintegrated.maximsensorsapp.bpt

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.bold
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maximintegrated.bpt.hsp.HspBptStreamData
import com.maximintegrated.maximsensorsapp.R
import com.maximintegrated.maximsensorsapp.exts.getHtmlSpannedString
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
        private val sbpDbpTextView: TextView by lazy { itemView.historySbpDbpTextView }
        private val hrSpo2TextView: TextView by lazy { itemView.historyHrSpo2TextView }

        fun bind(item: BptHistoryData) {
            dateTextView.text = HspBptStreamData.TIMESTAMP_FORMAT.format(Date(item.timestamp))
            typeTextView.text =
                parent.context.getString(if (item.isCalibration) R.string.calibration else R.string.measurement)
            sbpDbpTextView.text = parent.context.getString(R.string.sbp_dbp_format, item.sbp, item.dbp)
            val spannedText = SpannableStringBuilder().bold { append() }
            hrSpo2TextView.setText(parent.context.getHtmlSpannedString(R.string.hr_spo2_format, item.hr, item.spo2))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BptHistoryViewHolder {
        return BptHistoryViewHolder(parent)
    }

    override fun onBindViewHolder(holder: BptHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}