package com.it342.basalo.features.visitor

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.it342.basalo.core.data.LocationRecord
import com.it342.basalo.core.data.SessionManager
import com.it342.basalo.core.network.RetrofitClient
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.it342.basalo.R
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class VisitorCheckInActivity : AppCompatActivity() {

    private var selectedAttachmentUri: Uri? = null
    private lateinit var attachmentLabel: TextView
    private lateinit var locationInput: AutoCompleteTextView
    private lateinit var btnSubmit: Button
    private lateinit var progress: LinearProgressIndicator
    private var locations: List<LocationRecord> = emptyList()

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            selectedAttachmentUri = uri
            attachmentLabel.text = uri?.lastPathSegment ?: getString(R.string.no_file_selected)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visitor_check_in)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.new_visitor_entry)

        attachmentLabel = findViewById(R.id.tvAttachmentStatus)
        progress = findViewById(R.id.checkInProgress)

        val visitorTypes = resources.getStringArray(R.array.visitor_types)

        val visitorTypeInput = findViewById<AutoCompleteTextView>(R.id.etVisitorType)
        locationInput = findViewById(R.id.etDestination)
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etContactNumber = findViewById<EditText>(R.id.etContactNumber)
        val etHostName = findViewById<EditText>(R.id.etHostName)
        val etPurpose = findViewById<EditText>(R.id.etPurpose)
        val cbExtendedVisit = findViewById<CheckBox>(R.id.cbExtendedVisit)
        val btnUpload = findViewById<Button>(R.id.btnUploadId)
        btnSubmit = findViewById(R.id.btnSubmitCheckIn)
        btnSubmit.isEnabled = false

        visitorTypeInput.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, visitorTypes)
        )
        loadLocations()

        btnUpload.setOnClickListener {
            filePickerLauncher.launch("image/*")
        }

        btnSubmit.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val contactNumber = etContactNumber.text.toString().trim()
            val hostName = etHostName.text.toString().trim()
            val visitorType = visitorTypeInput.text.toString().trim()
            val destination = locationInput.text.toString().trim()
            val purpose = etPurpose.text.toString().trim()

            if (fullName.isBlank() || hostName.isBlank() || visitorType.isBlank() || destination.isBlank() || purpose.isBlank()) {
                Toast.makeText(this, R.string.complete_required_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contactNumber.length !in 7..15 || !contactNumber.all { it.isDigit() }) {
                Toast.makeText(this, R.string.invalid_contact_number, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (locations.none { it.displayName == destination }) {
                Toast.makeText(this, R.string.select_valid_location, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedAttachmentUri == null) {
                Toast.makeText(this, R.string.attach_id_prompt, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val createdByEmail = SessionManager(this).getProfile()?.email
            if (createdByEmail.isNullOrBlank()) {
                Toast.makeText(this, R.string.session_expired_message, Toast.LENGTH_LONG).show()
                finish()
                return@setOnClickListener
            }

            val imagePart = createImagePart(selectedAttachmentUri!!)
            if (imagePart == null) {
                Toast.makeText(this, R.string.attach_id_prompt, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            progress.visibility = android.view.View.VISIBLE
            RetrofitClient.visitorApi.checkInVisitor(
                fullName = createTextBody(fullName),
                contactNumber = createTextBody(contactNumber),
                hostName = createTextBody(hostName),
                visitorType = createTextBody(visitorType),
                destinationRoom = createTextBody(destination),
                purpose = createTextBody(purpose),
                extendedVisit = cbExtendedVisit.isChecked,
                createdByEmail = createTextBody(createdByEmail),
                idImage = imagePart
            ).enqueue(object : Callback<com.it342.basalo.core.data.VisitorRecord> {
                override fun onResponse(
                    call: Call<com.it342.basalo.core.data.VisitorRecord>,
                    response: Response<com.it342.basalo.core.data.VisitorRecord>
                ) {
                    btnSubmit.isEnabled = true
                    progress.visibility = android.view.View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@VisitorCheckInActivity, R.string.check_in_success, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@VisitorCheckInActivity, response.errorBody()?.string() ?: getString(R.string.request_failed), Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<com.it342.basalo.core.data.VisitorRecord>, t: Throwable) {
                    btnSubmit.isEnabled = true
                    progress.visibility = android.view.View.GONE
                    Toast.makeText(this@VisitorCheckInActivity, t.message ?: getString(R.string.request_failed), Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun loadLocations() {
        progress.visibility = android.view.View.VISIBLE
        RetrofitClient.adminApi.getLocations().enqueue(object : Callback<List<LocationRecord>> {
            override fun onResponse(call: Call<List<LocationRecord>>, response: Response<List<LocationRecord>>) {
                progress.visibility = android.view.View.GONE
                locations = response.body().orEmpty().sortedBy { it.displayName }
                locationInput.setAdapter(
                    ArrayAdapter(
                        this@VisitorCheckInActivity,
                        android.R.layout.simple_list_item_1,
                        locations.map { it.displayName }
                    )
                )
                btnSubmit.isEnabled = locations.isNotEmpty()
            }

            override fun onFailure(call: Call<List<LocationRecord>>, t: Throwable) {
                progress.visibility = android.view.View.GONE
                Toast.makeText(
                    this@VisitorCheckInActivity,
                    "Unable to load facility locations from backend.",
                    Toast.LENGTH_LONG
                ).show()
                btnSubmit.isEnabled = false
            }
        })
    }

    private fun createTextBody(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }

    private fun createImagePart(uri: Uri): MultipartBody.Part? {
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "visitor-id.jpg"
        val tempFile = File(cacheDir, fileName)
        contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null

        val mimeType = contentResolver.getType(uri) ?: "image/*"
        val requestBody = RequestBody.create(MediaType.parse(mimeType), tempFile)
        return MultipartBody.Part.createFormData("idImage", tempFile.name, requestBody)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
