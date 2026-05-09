package com.it342.basalo.features.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.it342.basalo.core.data.SystemUser
import com.it342.basalo.core.network.AuthResponse
import com.it342.basalo.core.network.RegisterRequest
import com.it342.basalo.core.network.RetrofitClient
import com.it342.basalo.ui.StaffAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StaffManagementFragment : Fragment(R.layout.fragment_management_list) {
    private lateinit var adapter: StaffAdapter

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

        title.text = getString(R.string.staff_management_title)
        subtitle.text = getString(R.string.staff_management_subtitle)
        empty.text = getString(R.string.no_staff_found)
        action.text = getString(R.string.register_user_action)

        adapter = StaffAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        action.setOnClickListener { showCreateStaffDialog() }
        loadStaff(summary, empty, loading)
    }

    private fun loadStaff(summary: TextView, empty: TextView, loading: LinearProgressIndicator) {
        loading.visibility = View.VISIBLE
        RetrofitClient.adminApi.getUsers().enqueue(object : Callback<List<SystemUser>> {
            override fun onResponse(call: Call<List<SystemUser>>, response: Response<List<SystemUser>>) {
                loading.visibility = View.GONE
                val users = response.body().orEmpty().sortedBy { it.lastName.lowercase() }
                adapter.submitList(users)
                summary.text = getString(R.string.staff_count_format, users.size)
                empty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onFailure(call: Call<List<SystemUser>>, t: Throwable) {
                loading.visibility = View.GONE
                adapter.submitList(emptyList())
                summary.text = getString(R.string.staff_count_format, 0)
                empty.visibility = View.VISIBLE
                empty.text = getString(R.string.logs_load_failed)
            }
        })
    }

    private fun showCreateStaffDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_staff, null)
        val roleInput = dialogView.findViewById<AutoCompleteTextView>(R.id.etDialogRole)
        roleInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listOf("STAFF", "ADMIN"))
        )
        roleInput.setText("STAFF", false)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.register_new_staff)
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.register_user_action, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val firstName = dialogView.findViewById<TextInputEditText>(R.id.etDialogFirstName).text.toString().trim()
                        val lastName = dialogView.findViewById<TextInputEditText>(R.id.etDialogLastName).text.toString().trim()
                        val email = dialogView.findViewById<TextInputEditText>(R.id.etDialogEmail).text.toString().trim()
                        val password = dialogView.findViewById<TextInputEditText>(R.id.etDialogPassword).text.toString().trim()
                        val role = roleInput.text.toString().trim().ifBlank { "STAFF" }

                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
                            Toast.makeText(requireContext(), R.string.complete_required_fields, Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        RetrofitClient.authApi.registerUser(
                            RegisterRequest(firstName, lastName, email, password, role)
                        ).enqueue(object : Callback<AuthResponse> {
                            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                                if (response.isSuccessful && response.body()?.success == true) {
                                    Toast.makeText(requireContext(), response.body()?.message ?: getString(R.string.register), Toast.LENGTH_SHORT).show()
                                    view?.findViewById<TextView>(R.id.tvManagementSummary)?.let { summary ->
                                        view?.findViewById<TextView>(R.id.tvManagementEmpty)?.let { empty ->
                                            view?.findViewById<LinearProgressIndicator>(R.id.managementLoadingIndicator)?.let { loading ->
                                                loadStaff(summary, empty, loading)
                                            }
                                        }
                                    }
                                    dialog.dismiss()
                                } else {
                                    Toast.makeText(requireContext(), response.body()?.message ?: getString(R.string.request_failed), Toast.LENGTH_LONG).show()
                                }
                            }

                            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                                Toast.makeText(requireContext(), t.message ?: getString(R.string.request_failed), Toast.LENGTH_LONG).show()
                            }
                        })
                    }
                }
                dialog.show()
            }
    }
}
