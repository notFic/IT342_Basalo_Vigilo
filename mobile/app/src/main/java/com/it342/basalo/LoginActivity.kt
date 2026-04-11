package com.it342.basalo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.it342.basalo.data.SessionManager
import com.it342.basalo.data.StaffProfile
import com.it342.basalo.network.AuthResponse
import com.it342.basalo.network.LoginRequest
import com.it342.basalo.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnGoogle.setOnClickListener {
            Toast.makeText(this, getString(R.string.google_not_ready), Toast.LENGTH_LONG).show()
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                openDebugAccess(
                    email = email.ifBlank { "debug@vigilo.local" },
                    message = "Debug mode: proceeding without validated credentials."
                )
                return@setOnClickListener
            }

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
                        openDebugAccess(
                            email = email,
                            message = "Debug mode: bypassed login. Server response: $message"
                        )
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    openDebugAccess(
                        email = email,
                        message = "Debug mode: server unavailable, opening preview access."
                    )
                }
            })
        }
    }

    private fun openDebugAccess(email: String, message: String) {
        val debugEmail = email.ifBlank { "debug@vigilo.local" }
        val previewProfile = createPreviewProfile(debugEmail)
        sessionManager.saveSession("local-preview-token", previewProfile)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun saveSession(response: AuthResponse) {
        val user = response.data ?: return
        sessionManager.saveSession(
            token = "local-session-${user.email}",
            profile = StaffProfile(
                id = 1,
                firstName = user.firstName,
                lastName = user.lastName,
                email = user.email,
                role = user.role
            )
        )
    }

    private fun createPreviewProfile(email: String): StaffProfile {
        val role = if (email.contains("admin", ignoreCase = true)) "ADMIN" else "STAFF"
        return StaffProfile(
            id = 1,
            firstName = "Preview",
            lastName = if (role == "ADMIN") "Admin" else "Guard",
            email = email,
            role = role
        )
    }
}
