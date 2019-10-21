package com.maximintegrated.maximsensorsapp

import android.graphics.Color
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileListAdapter(private val itemClickListener: (File) -> Unit) :
    RecyclerView.Adapter<FileListViewHolder>() {

    var fileList: List<File> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileListViewHolder {
        return FileListViewHolder(itemClickListener, parent)
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    override fun onBindViewHolder(holder: FileListViewHolder, position: Int) {
        holder.bind(fileList[position])
        if (position % 2 != 0) {
            holder.itemView.setBackgroundColor(Color.parseColor("#DDDDDD"))
        }
    }
}