package com.it342.basalo.features.visitor

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
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.it342.basalo.core.data.RoleUtils
import com.it342.basalo.core.data.SessionManager
import com.it342.basalo.ui.HistoryLogsAdapter
import com.it342.basalo.core.network.RetrofitClient
import com.it342.basalo.core.network.HistoricalResponse
import com.it342.basalo.core.data.VisitorRecord
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.it342.basalo.R

class HistoryFragment : Fragment(R.layout.fragment_history_logs) {

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: HistoryLogsAdapter
    private var allItems: List<VisitorRecord> = emptyList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val profile = sessionManager.getProfile()
        val isAdmin = RoleUtils.isAdmin(profile?.role)
        val emptyState = view.findViewById<TextView>(R.id.tvHistoryEmpty)
        val summaryText = view.findViewById<TextView>(R.id.tvHistorySummary)
        val searchInput = view.findViewById<EditText>(R.id.etHistorySearch)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvHistoryLogs)
        val loading = view.findViewById<LinearProgressIndicator>(R.id.historyLoadingIndicator)
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fabNewVisitor)

        adapter = HistoryLogsAdapter(isAdmin = isAdmin) { visitor ->
            val adminEmail = profile?.email ?: "admin@vigilo.com"
            RetrofitClient.visitorApi.voidVisitorLog(visitor.id, adminEmail).enqueue(object : Callback<VisitorRecord> {
                override fun onResponse(call: Call<VisitorRecord>, response: Response<VisitorRecord>) {
                    fetchHistory(searchInput.text.toString(), emptyState, summaryText, loading)
                    Toast.makeText(requireContext(), R.string.record_voided, Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(call: Call<VisitorRecord>, t: Throwable) {
                    Toast.makeText(requireContext(), t.message ?: getString(R.string.request_failed), Toast.LENGTH_SHORT).show()
                }
            })
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        fab.hide()

        searchInput.doAfterTextChanged {
            applyFilter(it?.toString().orEmpty(), emptyState, summaryText)
        }

        fetchHistory("", emptyState, summaryText, loading)
    }

    private fun fetchHistory(
        query: String,
        emptyState: TextView,
        summaryText: TextView,
        loading: LinearProgressIndicator
    ) {
        loading.visibility = View.VISIBLE
        RetrofitClient.visitorApi.getHistoricalLogs().enqueue(object : Callback<HistoricalResponse> {
            override fun onResponse(call: Call<HistoricalResponse>, response: Response<HistoricalResponse>) {
                loading.visibility = View.GONE
                allItems = response.body()?.content.orEmpty()
                applyFilter(query, emptyState, summaryText)
            }
            override fun onFailure(call: Call<HistoricalResponse>, t: Throwable) {
                loading.visibility = View.GONE
                allItems = emptyList()
                summaryText.text = getString(R.string.history_logs_count_format, 0)
                emptyState.visibility = View.VISIBLE
                emptyState.text = getString(R.string.logs_load_failed)
                adapter.submitList(emptyList())
            }
        })
    }

    private fun applyFilter(query: String, emptyState: TextView, summaryText: TextView) {
        val items = allItems.filter { it.fullName.contains(query, true) }
        adapter.submitList(items)
        summaryText.text = getString(R.string.history_logs_count_format, items.size)
        emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        if (items.isEmpty() && allItems.isNotEmpty()) {
            emptyState.text = getString(R.string.no_history_records)
        }
    }
}
