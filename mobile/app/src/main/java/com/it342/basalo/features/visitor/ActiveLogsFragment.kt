package com.it342.basalo.features.visitor

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.it342.basalo.core.data.SessionManager
import com.it342.basalo.ui.ActiveLogsAdapter
import com.it342.basalo.core.network.PublicHolidayDto
import com.it342.basalo.core.network.RetrofitClient
import com.it342.basalo.core.data.VisitorRecord
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.it342.basalo.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActiveLogsFragment : Fragment(R.layout.fragment_active_logs) {

    private lateinit var adapter: ActiveLogsAdapter
    private lateinit var sessionManager: SessionManager
    private var allItems: List<VisitorRecord> = emptyList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchInput = view.findViewById<EditText>(R.id.etActiveSearch)
        val holidayBanner = view.findViewById<TextView>(R.id.tvHolidayBanner)
        val summaryText = view.findViewById<TextView>(R.id.tvActiveSummary)
        val emptyState = view.findViewById<TextView>(R.id.tvActiveEmpty)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvActiveLogs)
        val loading = view.findViewById<LinearProgressIndicator>(R.id.activeLoadingIndicator)
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fabNewVisitor)

        adapter = ActiveLogsAdapter { visitor ->
            val userEmail = sessionManager.getProfile()?.email ?: "guard@vigilo.com"
            RetrofitClient.visitorApi.checkOutVisitor(visitor.id, userEmail).enqueue(object : Callback<VisitorRecord> {
                override fun onResponse(call: Call<VisitorRecord>, response: Response<VisitorRecord>) {
                    fetchActiveLogs(searchInput.text.toString(), emptyState, summaryText, loading)
                    Toast.makeText(requireContext(), R.string.visitor_checked_out, Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(call: Call<VisitorRecord>, t: Throwable) {
                    Toast.makeText(requireContext(), t.message ?: getString(R.string.request_failed), Toast.LENGTH_SHORT).show()
                }
            })
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val visitor = adapter.getItemAt(viewHolder.bindingAdapterPosition) ?: return
                val userEmail = sessionManager.getProfile()?.email ?: "guard@vigilo.com"
                RetrofitClient.visitorApi.checkOutVisitor(visitor.id, userEmail)
                    .enqueue(object : Callback<VisitorRecord> {
                        override fun onResponse(call: Call<VisitorRecord>, response: Response<VisitorRecord>) {
                            fetchActiveLogs(searchInput.text.toString(), emptyState, summaryText, loading)
                            Toast.makeText(requireContext(), R.string.visitor_checked_out, Toast.LENGTH_SHORT).show()
                        }

                        override fun onFailure(call: Call<VisitorRecord>, t: Throwable) {
                            fetchActiveLogs(searchInput.text.toString(), emptyState, summaryText, loading)
                            Toast.makeText(requireContext(), t.message ?: getString(R.string.request_failed), Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }).attachToRecyclerView(recyclerView)

        holidayBanner.text = getString(R.string.checking_holiday_status)
        loadHolidayBanner(holidayBanner)

        searchInput.doAfterTextChanged {
            applyFilter(it?.toString().orEmpty(), emptyState, summaryText)
        }

        fab.show()
        fetchActiveLogs("", emptyState, summaryText, loading)
    }

    override fun onResume() {
        super.onResume()
        val rootView = view ?: return
        fetchActiveLogs(
            rootView.findViewById<EditText>(R.id.etActiveSearch).text.toString(),
            rootView.findViewById(R.id.tvActiveEmpty),
            rootView.findViewById(R.id.tvActiveSummary),
            rootView.findViewById(R.id.activeLoadingIndicator)
        )
        requireActivity().findViewById<FloatingActionButton>(R.id.fabNewVisitor).show()
    }

    private fun loadHolidayBanner(holidayBanner: TextView) {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        RetrofitClient.publicHolidayApi.getPhilippinePublicHolidays(currentYear)
            .enqueue(object : Callback<List<PublicHolidayDto>> {
                override fun onResponse(
                    call: Call<List<PublicHolidayDto>>,
                    response: Response<List<PublicHolidayDto>>
                ) {
                    val holidays = response.body().orEmpty()
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val upcomingHoliday = holidays.firstOrNull { it.date >= today }

                    holidayBanner.text = if (upcomingHoliday != null) {
                        getString(
                            R.string.upcoming_holiday_format,
                            upcomingHoliday.name,
                            upcomingHoliday.date
                        )
                    } else {
                        getString(R.string.no_upcoming_holidays_ph)
                    }
                }

                override fun onFailure(call: Call<List<PublicHolidayDto>>, t: Throwable) {
                    holidayBanner.text = getString(R.string.normal_access_day_ph)
                }
            })
    }

    private fun fetchActiveLogs(
        query: String,
        emptyState: TextView,
        summaryText: TextView,
        loading: LinearProgressIndicator
    ) {
        loading.visibility = View.VISIBLE
        RetrofitClient.visitorApi.getActiveLogs().enqueue(object : Callback<List<VisitorRecord>> {
            override fun onResponse(call: Call<List<VisitorRecord>>, response: Response<List<VisitorRecord>>) {
                loading.visibility = View.GONE
                allItems = response.body().orEmpty()
                applyFilter(query, emptyState, summaryText)
            }
            override fun onFailure(call: Call<List<VisitorRecord>>, t: Throwable) {
                loading.visibility = View.GONE
                allItems = emptyList()
                summaryText.text = getString(R.string.active_logs_count_format, 0)
                emptyState.visibility = View.VISIBLE
                emptyState.text = getString(R.string.logs_load_failed)
                adapter.submitList(emptyList())
            }
        })
    }

    private fun applyFilter(query: String, emptyState: TextView, summaryText: TextView) {
        val items = allItems.filter { it.fullName.contains(query, true) }
        adapter.submitList(items)
        summaryText.text = getString(R.string.active_logs_count_format, items.size)
        emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        if (items.isEmpty() && allItems.isNotEmpty()) {
            emptyState.text = getString(R.string.no_active_visitors)
        }
    }
}
