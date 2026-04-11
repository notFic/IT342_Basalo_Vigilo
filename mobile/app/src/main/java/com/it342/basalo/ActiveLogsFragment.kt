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
import com.it342.basalo.ui.ActiveLogsAdapter

class ActiveLogsFragment : Fragment(R.layout.fragment_active_logs) {

    private lateinit var adapter: ActiveLogsAdapter
    private lateinit var sessionManager: SessionManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchInput = view.findViewById<EditText>(R.id.etActiveSearch)
        val holidayBanner = view.findViewById<TextView>(R.id.tvHolidayBanner)
        val emptyState = view.findViewById<TextView>(R.id.tvActiveEmpty)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvActiveLogs)
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fabNewVisitor)

        adapter = ActiveLogsAdapter { visitor ->
            val userName = sessionManager.getProfile()?.fullName ?: getString(R.string.default_guard_name)
            DemoDataStore.checkOut(visitor.logId, userName)
            refreshData(searchInput.text.toString(), emptyState)
            Toast.makeText(requireContext(), R.string.visitor_checked_out, Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val holiday = DemoDataStore.getHolidayStatus()
        holidayBanner.text = if (holiday.isHoliday) {
            getString(R.string.holiday_banner_format, holiday.holidayName ?: getString(R.string.restricted_access_day))
        } else {
            getString(R.string.normal_access_day)
        }

        searchInput.doAfterTextChanged {
            refreshData(it?.toString().orEmpty(), emptyState)
        }

        fab.show()
        refreshData("", emptyState)
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<TextView>(R.id.tvActiveEmpty)?.let { refreshData("", it) }
        requireActivity().findViewById<FloatingActionButton>(R.id.fabNewVisitor).show()
    }

    private fun refreshData(query: String, emptyState: TextView) {
        val items = DemoDataStore.getActiveLogs(query)
        adapter.submitList(items)
        emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}
