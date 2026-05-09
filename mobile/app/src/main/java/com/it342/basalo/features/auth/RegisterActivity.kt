package com.it342.basalo.features.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.it342.basalo.core.network.AuthResponse
import com.it342.basalo.core.network.RegisterRequest
import com.it342.basalo.core.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.it342.basalo.R

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etRegEmail = findViewById<EditText>(R.id.etRegEmail)
        val etRegPassword = findViewById<EditText>(R.id.etRegPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val registerProgress = findViewById<LinearProgressIndicator>(R.id.registerProgress)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        // Close this screen and go back to Login
        tvBackToLogin.setOnClickListener {
            finish()
        }

        // Handle Registration API Call
        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etRegEmail.text.toString().trim()
            val password = etRegPassword.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = RegisterRequest(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                role = "STAFF"
            )

            btnRegister.isEnabled = false
            registerProgress.visibility = View.VISIBLE
            RetrofitClient.authApi.registerUser(request).enqueue(object : Callback<AuthResponse> {

                override fun onResponse(
                    call: Call<AuthResponse>,
                    response: Response<AuthResponse>
                ) {
                    val payload = response.body()
                    val message = payload?.message ?: "Registration failed."
                    if (response.isSuccessful && payload?.success == true) {
                        Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        btnRegister.isEnabled = true
                        registerProgress.visibility = View.GONE
                        Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    btnRegister.isEnabled = true
                    registerProgress.visibility = View.GONE
                    Toast.makeText(this@RegisterActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
