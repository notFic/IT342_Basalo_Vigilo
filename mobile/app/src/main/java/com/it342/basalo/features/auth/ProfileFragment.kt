package com.it342.basalo.features.auth

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.it342.basalo.core.data.RoleUtils
import com.it342.basalo.core.data.SessionManager
import com.it342.basalo.core.network.ApiConfigManager
import com.it342.basalo.R

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val profile = sessionManager.getProfile()

        view.findViewById<TextView>(R.id.tvProfileName).text =
            profile?.fullName ?: getString(R.string.default_guard_name)
        view.findViewById<TextView>(R.id.tvProfileEmail).text =
            profile?.email ?: getString(R.string.default_email)
        val role = RoleUtils.normalize(profile?.role ?: getString(R.string.staff_role))
        val roleView = view.findViewById<TextView>(R.id.tvProfileRole)
        roleView.text = if (RoleUtils.isAdmin(role)) {
            getString(R.string.admin_badge)
        } else {
            getString(R.string.staff_badge)
        }
        roleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.role_badge_text))
        val apiBaseUrl = view.findViewById<TextView>(R.id.tvProfileApiBaseUrl)
        apiBaseUrl.text = getString(R.string.backend_target_format, ApiConfigManager.getBaseUrl())

        requireActivity().findViewById<FloatingActionButton>(R.id.fabNewVisitor).hide()

        view.findViewById<Button>(R.id.btnEditBackendUrl).setOnClickListener {
            showBackendSettingsDialog(apiBaseUrl)
        }

        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun showBackendSettingsDialog(targetView: TextView) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
        }
        val hintText = TextView(context).apply {
            text = getString(R.string.backend_settings_hint)
        }
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(ApiConfigManager.getBaseUrl())
            this.hint = getString(R.string.backend_url_label)
        }
        container.addView(hintText)
        container.addView(input)

        AlertDialog.Builder(context)
            .setTitle(R.string.backend_settings)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.reset_backend_url) { _, _ ->
                ApiConfigManager.resetBaseUrl()
                targetView.text = getString(R.string.backend_target_format, ApiConfigManager.getBaseUrl())
                Toast.makeText(context, R.string.backend_url_saved, Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton(R.string.save_backend_url) { _, _ ->
                val value = input.text.toString().trim()
                if (value.isBlank()) {
                    Toast.makeText(context, R.string.backend_url_required, Toast.LENGTH_SHORT).show()
                } else {
                    ApiConfigManager.saveBaseUrl(value)
                    targetView.text = getString(R.string.backend_target_format, ApiConfigManager.getBaseUrl())
                    Toast.makeText(context, R.string.backend_url_saved, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
}
