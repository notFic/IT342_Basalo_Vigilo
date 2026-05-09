package com.it342.basalo.features.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.it342.basalo.R
import com.it342.basalo.core.data.LocationRecord
import com.it342.basalo.core.network.LocationRequest
import com.it342.basalo.core.network.RetrofitClient
import com.it342.basalo.ui.LocationAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationManagementFragment : Fragment(R.layout.fragment_management_list) {
    private lateinit var adapter: LocationAdapter

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

        title.text = getString(R.string.location_management_title)
        subtitle.text = getString(R.string.location_management_subtitle)
        empty.text = getString(R.string.no_locations_found)
        action.text = getString(R.string.add_location_action)

        adapter = LocationAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        action.setOnClickListener { showAddLocationDialog() }
        loadLocations(summary, empty, loading)
    }

    private fun loadLocations(summary: TextView, empty: TextView, loading: LinearProgressIndicator) {
        loading.visibility = View.VISIBLE
        RetrofitClient.adminApi.getLocations().enqueue(object : Callback<List<LocationRecord>> {
            override fun onResponse(call: Call<List<LocationRecord>>, response: Response<List<LocationRecord>>) {
                loading.visibility = View.GONE
                val locations = response.body().orEmpty().sortedBy { it.displayName.lowercase() }
                adapter.submitList(locations)
                summary.text = getString(R.string.location_count_format, locations.size)
                empty.visibility = if (locations.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onFailure(call: Call<List<LocationRecord>>, t: Throwable) {
                loading.visibility = View.GONE
                adapter.submitList(emptyList())
                summary.text = getString(R.string.location_count_format, 0)
                empty.visibility = View.VISIBLE
                empty.text = getString(R.string.logs_load_failed)
            }
        })
    }

    private fun showAddLocationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_location, null)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.location_management_title)
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_location_action, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val areaName = dialogView.findViewById<TextInputEditText>(R.id.etDialogAreaName).text.toString().trim()
                        val roomNumber = dialogView.findViewById<TextInputEditText>(R.id.etDialogRoomNumber).text.toString().trim()
                        val floorLevel = dialogView.findViewById<TextInputEditText>(R.id.etDialogFloorLevel).text.toString().trim()

                        if (areaName.isBlank() || roomNumber.isBlank() || floorLevel.isBlank()) {
                            Toast.makeText(requireContext(), R.string.complete_required_fields, Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        RetrofitClient.adminApi.addLocation(LocationRequest(areaName, roomNumber, floorLevel))
                            .enqueue(object : Callback<LocationRecord> {
                                override fun onResponse(call: Call<LocationRecord>, response: Response<LocationRecord>) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(requireContext(), R.string.add_location_action, Toast.LENGTH_SHORT).show()
                                        view?.findViewById<TextView>(R.id.tvManagementSummary)?.let { summary ->
                                            view?.findViewById<TextView>(R.id.tvManagementEmpty)?.let { empty ->
                                                view?.findViewById<LinearProgressIndicator>(R.id.managementLoadingIndicator)?.let { loading ->
                                                    loadLocations(summary, empty, loading)
                                                }
                                            }
                                        }
                                        dialog.dismiss()
                                    } else {
                                        Toast.makeText(requireContext(), getString(R.string.request_failed), Toast.LENGTH_LONG).show()
                                    }
                                }

                                override fun onFailure(call: Call<LocationRecord>, t: Throwable) {
                                    Toast.makeText(requireContext(), t.message ?: getString(R.string.request_failed), Toast.LENGTH_LONG).show()
                                }
                            })
                    }
                }
                dialog.show()
            }
    }
}
