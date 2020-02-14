package com.maximintegrated.maximsensorsapp.sports_coaching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.maximintegrated.algorithms.sports.SportsCoachingAlgorithmOutput
import com.maximintegrated.bpt.hsp.HspViewModel
import com.maximintegrated.maximsensorsapp.BleConnectionInfo
import com.maximintegrated.maximsensorsapp.R
import kotlinx.android.synthetic.main.fragment_sports_coaching_history.*
import kotlinx.android.synthetic.main.include_app_bar.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class SportsCoachingHistoryFragment : Fragment(), HistoryViewHolder.HistoryItemClickListener {

    companion object {
        fun newInstance() = SportsCoachingHistoryFragment()
    }

    private lateinit var hspViewModel: HspViewModel

    private val adapter: SportsCoachingHistoryAdapter by lazy { SportsCoachingHistoryAdapter(this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        hspViewModel = ViewModelProviders.of(requireActivity()).get(HspViewModel::class.java)

        hspViewModel.connectionState
            .observe(this) { (device, connectionState) ->
                toolbar.connectionInfo = if (hspViewModel.bluetoothDevice != null) {
                    BleConnectionInfo(connectionState, device.name, device.address)
                } else {
                    null
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sports_coaching_history, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.pageTitle = getString(R.string.history)

        //TODO progress bar async
        initRecyclerView()

        progressBar.visibility = View.VISIBLE
        doAsync {
            adapter.outputs = getSportsCoachingOutputsFromFiles(SportsCoachingManager.currentUser!!.userName)
            uiThread {
                progressBar.visibility = View.GONE
                if(adapter.outputs.isEmpty()){
                    fileRecyclerView.visibility = View.GONE
                    warningGroup.visibility = View.VISIBLE
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun initRecyclerView() {
        fileRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        fileRecyclerView.adapter = adapter
    }

    override fun onItemClicked(output: SportsCoachingAlgorithmOutput) {
        //TODO go to detailed page
    }
}