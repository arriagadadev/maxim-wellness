package com.maximintegrated.maximsensorsapp

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.archive_file_item.view.*
import java.io.File

class FileListViewHolder(
    private val onItemClicked: (File) -> Unit,
    parent: ViewGroup
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(
        R.layout.archive_file_item,
        parent,
        false
    )
) {

    private val textViewFileName: TextView  by lazy { itemView.file_name }
    private val textViewCreateDate: TextView  by lazy { itemView.file_create_date }

    private lateinit var item: File

    init {
        itemView.setOnClickListener { onItemClicked(item) }
    }

    fun bind(item: File) {
        this.item = item

        val fileInfo = item.name.split("_")
        textViewFileName.text = fileInfo[2]
        textViewCreateDate.text = fileInfo[1]

    }
}