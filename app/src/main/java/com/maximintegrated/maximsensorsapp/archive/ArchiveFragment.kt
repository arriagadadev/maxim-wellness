package com.maximintegrated.maximsensorsapp.archive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.addFragment
import kotlinx.android.synthetic.main.fragment_archive.*
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

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

        initRecyclerView()
        adapter.fileList = files
        adapter.notifyDataSetChanged()
    }


    private fun initRecyclerView() {
        fileRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        fileRecyclerView.adapter = adapter
    }

    private fun handleListItemClick(file: File) {
        val rows = file.readLines().drop(1)

        val offlineDataList: ArrayList<OfflineDataModel> = arrayListOf()

        for (row in rows) {
            val items = row.split(",")
            offlineDataList.add(
                OfflineDataModel(
                    green = items[2].toFloat(),
                    ir = items[4].toFloat(),
                    red = items[5].toFloat(),
                    hr = items[10].toFloat(),
                    rr = items[12].toFloat(),
                    rrConfidence = items[13].toInt(),
                    spo2 = items[17].toFloat(),
                    motion = sqrt(
                        items[6].toFloat().pow(2)
                                + items[7].toFloat().pow(2)
                                + items[8].toFloat().pow(2)
                    ),
                    steps = items[25].toFloat() + items[26].toFloat(),
                    date = DataRecorder.TIMESTAMP_FORMAT.parse(items[29]).time.toFloat()
                )
            )
        }

        requireActivity().addFragment(OfflineDataFragment.newInstance(offlineDataList))
    }
}