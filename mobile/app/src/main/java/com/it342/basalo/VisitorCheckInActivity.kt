package com.it342.basalo

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
import com.it342.basalo.data.DemoDataStore
import com.it342.basalo.data.SessionManager

class VisitorCheckInActivity : AppCompatActivity() {

    private var selectedAttachmentUri: Uri? = null
    private lateinit var attachmentLabel: TextView

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

        val visitorTypes = resources.getStringArray(R.array.visitor_types)
        val locations = resources.getStringArray(R.array.location_options)

        val visitorTypeInput = findViewById<AutoCompleteTextView>(R.id.etVisitorType)
        val locationInput = findViewById<AutoCompleteTextView>(R.id.etDestination)
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etContactNumber = findViewById<EditText>(R.id.etContactNumber)
        val etHostName = findViewById<EditText>(R.id.etHostName)
        val etPurpose = findViewById<EditText>(R.id.etPurpose)
        val cbExtendedVisit = findViewById<CheckBox>(R.id.cbExtendedVisit)
        val btnUpload = findViewById<Button>(R.id.btnUploadId)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitCheckIn)

        visitorTypeInput.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, visitorTypes)
        )
        locationInput.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, locations)
        )

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

            if (contactNumber.length !in 10..13 || !contactNumber.all { it.isDigit() }) {
                Toast.makeText(this, R.string.invalid_contact_number, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedAttachmentUri == null) {
                Toast.makeText(this, R.string.attach_id_prompt, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val createdBy = SessionManager(this).getProfile()?.fullName ?: getString(R.string.default_guard_name)
            DemoDataStore.addVisitor(
                fullName = fullName,
                contactNumber = contactNumber,
                hostName = hostName,
                visitorType = visitorType,
                destination = destination,
                purpose = purpose,
                isExtendedVisit = cbExtendedVisit.isChecked,
                idAttachmentLabel = selectedAttachmentUri?.lastPathSegment,
                createdBy = createdBy
            )

            Toast.makeText(this, R.string.check_in_success, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
