package com.maximintegrated.maximsensorsapp.archive

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.maximintegrated.algorithm_sleep_qa.SleepQaAlgorithm
import com.maximintegrated.algorithm_sleep_qa.SleepQaAlgorithmInput
import com.maximintegrated.maximsensorsapp.*
import com.maximintegrated.maximsensorsapp.exts.addFragment
import kotlinx.android.synthetic.main.fragment_archive.*
import timber.log.Timber
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt


class ArchiveFragment : RecyclerViewClickListener, Fragment() {
    companion object {
        private val OUTPUT_DIRECTORY =
            File(Environment.getExternalStorageDirectory(), "MaximSleepQa")

        fun newInstance() = ArchiveFragment()
    }

    private val adapter: FileListAdapter by lazy { FileListAdapter(this) }


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
        adapter.fileList = files.toMutableList()
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

    override fun onRowClicked(file: File) {
        handleListItemClick(file)
    }

    override fun onDeleteClicked(file: File) {
        showDeleteDialog(file)
    }

    override fun onShareClicked(file: File) {

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "vnd.android.cursor.dir/email"
            setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "MaximSensorsApp Csv File")
            putExtra(Intent.EXTRA_TEXT, "File Name: ${file.name}")
            val uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().packageName + ".fileprovider",
                file
            )
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        startActivity(intent)
    }

    override fun onSleepClicked(file: File) {
        val success = runSleepAlgo(file.absolutePath)
        showSleepAlgoResultDialog(success)
    }

    private fun showDeleteDialog(file: File) {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Delete File")
        alertDialog.setMessage("Are you sure you want to delete this file ?")
            .setPositiveButton("Delete") { dialog, which ->
                val deleted = file.delete()
                if (deleted) {
                    adapter.fileList.remove(file)
                    adapter.notifyDataSetChanged()
                }
                dialog.dismiss()
            }.setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun runSleepAlgo(inputFilePath: String): Boolean {
        val outputDirectory = OUTPUT_DIRECTORY

        val input =
            SleepQaAlgorithmInput(inputFilePath, outputDirectory.absolutePath, 20, 180, 70, 1, 80f)

        val success = SleepQaAlgorithm.run(input)

        Timber.tag("Archive Fragment").d("SleeoQaAlgo run success result: $success")

        return success
    }

    private fun showSleepAlgoResultDialog(success: Boolean) {
        var message: String

        if (success) {
            message = "Algorithm finished successfully"
        } else {
            message = "Algorithm did not found a sleep state in the given data"
        }
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Sleep Algorithm Result")
        alertDialog.setMessage(message)
            .setPositiveButton("OK") { dialog, which ->
                dialog.dismiss()
            }

        alertDialog.show()
    }
}