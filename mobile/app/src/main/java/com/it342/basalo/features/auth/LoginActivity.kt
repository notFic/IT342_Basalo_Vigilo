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
import androidx.appcompat.app.AppCompatActivity
import com.it342.basalo.core.data.SessionManager
import com.it342.basalo.core.data.StaffProfile
import com.it342.basalo.core.data.RoleUtils
import com.it342.basalo.core.network.AuthResponse
import com.it342.basalo.core.network.ApiConfigManager
import com.it342.basalo.core.network.LoginRequest
import com.it342.basalo.core.network.RetrofitClient
import com.google.android.material.progressindicator.LinearProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.it342.basalo.MainActivity
import com.it342.basalo.R

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        val loginProgress = findViewById<LinearProgressIndicator>(R.id.loginProgress)
        val tvBackendTarget = findViewById<TextView>(R.id.tvBackendTarget)
        val tvBackendSettings = findViewById<TextView>(R.id.tvBackendSettings)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        updateBackendTarget(tvBackendTarget)

        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        tvBackendSettings.setOnClickListener {
            showBackendSettingsDialog(tvBackendTarget)
        }

        btnGoogle.setOnClickListener {
            Toast.makeText(this, getString(R.string.google_not_ready), Toast.LENGTH_LONG).show()
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            loginProgress.visibility = View.VISIBLE
            val request = LoginRequest(email = email, password = password)

            RetrofitClient.authApi.loginUser(request).enqueue(object : Callback<AuthResponse> {

                override fun onResponse(
                    call: Call<AuthResponse>,
                    response: Response<AuthResponse>
                ) {
                    val payload = response.body()
                    val message = payload?.message ?: "Login failed."
                    if (response.isSuccessful && payload?.success == true && payload.data != null) {
                        saveSession(payload)
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        btnLogin.isEnabled = true
                        loginProgress.visibility = View.GONE
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    btnLogin.isEnabled = true
                    loginProgress.visibility = View.GONE
                    Toast.makeText(
                        this@LoginActivity,
                        getString(
                            R.string.backend_unreachable_message,
                            ApiConfigManager.getBaseUrl()
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
    }

    private fun saveSession(response: AuthResponse) {
        val user = response.data ?: return
        sessionManager.saveSession(
            token = "session-${user.id}",
            profile = StaffProfile(
                id = user.id,
                firstName = user.firstName,
                lastName = user.lastName,
                email = user.email,
                role = RoleUtils.normalize(user.role)
            )
        )
    }

    private fun updateBackendTarget(view: TextView) {
        view.text = getString(R.string.backend_target_format, ApiConfigManager.getBaseUrl())
    }

    private fun showBackendSettingsDialog(targetView: TextView) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
        }
        val hintText = TextView(this).apply {
            text = getString(R.string.backend_settings_hint)
        }
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(ApiConfigManager.getBaseUrl())
            this.hint = getString(R.string.backend_url_label)
        }
        container.addView(hintText)
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle(R.string.backend_settings)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.reset_backend_url) { _, _ ->
                ApiConfigManager.resetBaseUrl()
                updateBackendTarget(targetView)
                Toast.makeText(this, R.string.backend_url_saved, Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton(R.string.save_backend_url) { _, _ ->
                val value = input.text.toString().trim()
                if (value.isBlank()) {
                    Toast.makeText(this, R.string.backend_url_required, Toast.LENGTH_SHORT).show()
                } else {
                    ApiConfigManager.saveBaseUrl(value)
                    updateBackendTarget(targetView)
                    Toast.makeText(this, R.string.backend_url_saved, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
}

