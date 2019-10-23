package com.maximintegrated.maximsensorsapp

import java.io.File

interface RecyclerViewClickListener {
    fun onRowClicked(file: File)
    fun onDeleteClicked(file: File)
    fun onShareClicked(file: File)
}