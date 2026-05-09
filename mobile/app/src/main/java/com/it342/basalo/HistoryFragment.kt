package com.it342.basalo

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.it342.basalo.data.DemoDataStore
import com.it342.basalo.data.SessionManager
import com.it342.basalo.ui.HistoryLogsAdapter
import com.it342.basalo.network.RetrofitClient
import com.it342.basalo.network.HistoricalResponse
import com.it342.basalo.data.VisitorRecord
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HistoryFragment : Fragment(R.layout.fragment_history_logs) {

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: HistoryLogsAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val profile = sessionManager.getProfile()
        val isAdmin = profile?.role.equals("ADMIN", ignoreCase = true)
        val emptyState = view.findViewById<TextView>(R.id.tvHistoryEmpty)
        val searchInput = view.findViewById<EditText>(R.id.etHistorySearch)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvHistoryLogs)
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fabNewVisitor)

        adapter = HistoryLogsAdapter(isAdmin = isAdmin) { visitor ->
            val adminEmail = profile?.email ?: "admin@vigilo.com"
            RetrofitClient.visitorApi.voidVisitorLog(visitor.logId, adminEmail).enqueue(object : Callback<VisitorRecord> {
                override fun onResponse(call: Call<VisitorRecord>, response: Response<VisitorRecord>) {
                    refreshData(searchInput.text.toString(), emptyState)
                    Toast.makeText(requireContext(), R.string.record_voided, Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(call: Call<VisitorRecord>, t: Throwable) {
                    Toast.makeText(requireContext(), "Failed to void", Toast.LENGTH_SHORT).show()
                }
            })
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        fab.hide()

        searchInput.doAfterTextChanged {
            refreshData(it?.toString().orEmpty(), emptyState)
        }

        refreshData("", emptyState)
    }

    private fun refreshData(query: String, emptyState: TextView) {
        RetrofitClient.visitorApi.getHistoricalLogs().enqueue(object : Callback<HistoricalResponse> {
            override fun onResponse(call: Call<HistoricalResponse>, response: Response<HistoricalResponse>) {
                val items = response.body()?.content?.filter { it.fullName.contains(query, true) } ?: emptyList()
                adapter.submitList(items)
                emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onFailure(call: Call<HistoricalResponse>, t: Throwable) {
                emptyState.visibility = View.VISIBLE
            }
        })
    }
}
