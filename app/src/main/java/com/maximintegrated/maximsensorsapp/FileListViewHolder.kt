package com.maximintegrated.maximsensorsapp

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.archive_file_item.view.*
import java.io.File

class FileListViewHolder(
    private val onItemClicked: RecyclerViewClickListener,
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
    private val imageViewDelete: ImageView  by lazy { itemView.delete_icon }
    private val imageViewShare: ImageView  by lazy { itemView.share_icon }

    private lateinit var item: File

    init {
        itemView.setOnClickListener { onItemClicked.onRowClicked(item) }
        imageViewDelete.setOnClickListener { onItemClicked.onDeleteClicked(item) }
        imageViewShare.setOnClickListener { onItemClicked.onShareClicked(item) }
    }

    fun bind(item: File) {
        this.item = item

        val fileInfo = item.name.split("_")
        textViewFileName.text = fileInfo[2]
        textViewCreateDate.text = fileInfo[1]

    }
}