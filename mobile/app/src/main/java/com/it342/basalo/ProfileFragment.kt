package com.it342.basalo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.it342.basalo.data.SessionManager

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val profile = sessionManager.getProfile()

        view.findViewById<TextView>(R.id.tvProfileName).text =
            profile?.fullName ?: getString(R.string.default_guard_name)
        view.findViewById<TextView>(R.id.tvProfileEmail).text =
            profile?.email ?: getString(R.string.default_email)
        view.findViewById<TextView>(R.id.tvProfileRole).text =
            getString(R.string.role_label_format, profile?.role ?: getString(R.string.staff_role))

        requireActivity().findViewById<FloatingActionButton>(R.id.fabNewVisitor).hide()

        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
