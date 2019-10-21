package com.maximintegrated.maximsensorsapp.archive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.maximintegrated.maximsensorsapp.DataRecorder
import com.maximintegrated.maximsensorsapp.FileListAdapter
import com.maximintegrated.maximsensorsapp.R
import kotlinx.android.synthetic.main.fragment_archive.*
import timber.log.Timber
import java.io.File

class ArchiveFragment : Fragment() {
    companion object {
        fun newInstance() = ArchiveFragment()
    }

    private val adapter: FileListAdapter by lazy { FileListAdapter(::handleListItemClick) }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputDirectory = DataRecorder.OUTPUT_DIRECTORY
        val directory = File(inputDirectory.absolutePath)
        val files = directory.listFiles().toList().filter { !it.name.contains("1Hz") }
        for (file in files) {
            Timber.tag("AAAA").d("File name : ${file.name}")
        }
        initRecyclerView()
        adapter.fileList = files
        adapter.notifyDataSetChanged()
    }


    private fun initRecyclerView() {
        fileRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        fileRecyclerView.adapter = adapter
    }

    private fun handleListItemClick(file: File) {
        Timber.tag("ARCHIVE").d("handleListItemClick")
    }
}