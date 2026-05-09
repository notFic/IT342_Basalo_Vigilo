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
import com.it342.basalo.network.RetrofitClient
import com.it342.basalo.data.VisitorRecord
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
            val userEmail = sessionManager.getProfile()?.email ?: "guard@vigilo.com"
            RetrofitClient.visitorApi.checkOutVisitor(visitor.logId, userEmail).enqueue(object : Callback<VisitorRecord> {
                override fun onResponse(call: Call<VisitorRecord>, response: Response<VisitorRecord>) {
                    refreshData(searchInput.text.toString(), emptyState)
                    Toast.makeText(requireContext(), R.string.visitor_checked_out, Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(call: Call<VisitorRecord>, t: Throwable) {
                    Toast.makeText(requireContext(), "Failed to checkout", Toast.LENGTH_SHORT).show()
                }
            })
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
        RetrofitClient.visitorApi.getActiveLogs().enqueue(object : Callback<List<VisitorRecord>> {
            override fun onResponse(call: Call<List<VisitorRecord>>, response: Response<List<VisitorRecord>>) {
                val items = response.body()?.filter { it.fullName.contains(query, true) } ?: emptyList()
                adapter.submitList(items)
                emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onFailure(call: Call<List<VisitorRecord>>, t: Throwable) {
                emptyState.visibility = View.VISIBLE
            }
        })
    }
}
