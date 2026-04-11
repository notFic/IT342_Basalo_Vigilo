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
            val adminName = profile?.fullName ?: getString(R.string.default_admin_name)
            DemoDataStore.voidRecord(visitor.logId, adminName)
            refreshData(searchInput.text.toString(), emptyState)
            Toast.makeText(requireContext(), R.string.record_voided, Toast.LENGTH_SHORT).show()
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
        val items = DemoDataStore.getHistoricalLogs(query)
        adapter.submitList(items)
        emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}
