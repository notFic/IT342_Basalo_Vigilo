package com.it342.basalo.features.admin

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.it342.basalo.R
import com.it342.basalo.core.data.AuditRecord
import com.it342.basalo.core.network.RetrofitClient
import com.it342.basalo.ui.AuditAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuditLogFragment : Fragment(R.layout.fragment_management_list) {
    private lateinit var adapter: AuditAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().findViewById<FloatingActionButton>(R.id.fabNewVisitor).hide()

        val title = view.findViewById<TextView>(R.id.tvManagementTitle)
        val subtitle = view.findViewById<TextView>(R.id.tvManagementSubtitle)
        val summary = view.findViewById<TextView>(R.id.tvManagementSummary)
        val empty = view.findViewById<TextView>(R.id.tvManagementEmpty)
        val action = view.findViewById<Button>(R.id.btnManagementAction)
        val loading = view.findViewById<LinearProgressIndicator>(R.id.managementLoadingIndicator)
        val recycler = view.findViewById<RecyclerView>(R.id.rvManagement)

        title.text = getString(R.string.audit_log_title)
        subtitle.text = getString(R.string.audit_log_subtitle)
        empty.text = getString(R.string.no_audit_found)
        action.visibility = View.GONE

        adapter = AuditAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        loadAuditLogs(summary, empty, loading)
    }

    private fun loadAuditLogs(summary: TextView, empty: TextView, loading: LinearProgressIndicator) {
        loading.visibility = View.VISIBLE
        RetrofitClient.adminApi.getAuditLogs().enqueue(object : Callback<List<AuditRecord>> {
            override fun onResponse(call: Call<List<AuditRecord>>, response: Response<List<AuditRecord>>) {
                loading.visibility = View.GONE
                val items = response.body().orEmpty()
                adapter.submitList(items)
                summary.text = getString(R.string.audit_count_format, items.size)
                empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onFailure(call: Call<List<AuditRecord>>, t: Throwable) {
                loading.visibility = View.GONE
                adapter.submitList(emptyList())
                summary.text = getString(R.string.audit_count_format, 0)
                empty.visibility = View.VISIBLE
                empty.text = getString(R.string.logs_load_failed)
            }
        })
    }
}
