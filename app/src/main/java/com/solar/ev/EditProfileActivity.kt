package com.solar.ev

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.solar.ev.model.UpdateProfileRequest
import com.solar.ev.model.User
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.ui.common.LoadingDialogFragment
import com.solar.ev.viewModel.UpdateProfileResult
import com.solar.ev.viewModel.UpdateProfileViewModel
import com.solar.ev.viewModel.UpdateProfileViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone // Added import for TimeZone

class EditProfileActivity : BaseActivity() {

    private lateinit var viewModel: UpdateProfileViewModel
    private lateinit var sessionManager: SessionManager
    private var loadingDialog: LoadingDialogFragment? = null

    private lateinit var editTextName: TextInputEditText
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var radioButtonMale: RadioButton
    private lateinit var radioButtonFemale: RadioButton
    private lateinit var radioButtonOther: RadioButton
    private lateinit var buttonEditDob: Button
    private lateinit var tvEditSelectedDob: TextView

    private lateinit var editTextPhone: TextInputEditText
    private lateinit var editTextBio: TextInputEditText
    private lateinit var editTextAddress1: TextInputEditText
    private lateinit var editTextAddress2: TextInputEditText
    private lateinit var editTextCity: TextInputEditText
    private lateinit var editTextState: TextInputEditText
    private lateinit var editTextPostalCode: TextInputEditText
    private lateinit var editTextCountry: TextInputEditText
    private lateinit var buttonSave: Button

    private lateinit var tilName: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilBio: TextInputLayout
    private lateinit var tilAddress1: TextInputLayout
    private lateinit var tilCity: TextInputLayout
    private lateinit var tilState: TextInputLayout
    private lateinit var tilPostalCode: TextInputLayout
    private lateinit var tilCountry: TextInputLayout

    private var currentUser: User? = null

    companion object {
        const val EXTRA_USER = "extra_user_profile"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val toolbar: Toolbar = findViewById(R.id.toolbar_edit_profile)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"

        sessionManager = SessionManager(applicationContext)
        val factory = UpdateProfileViewModelFactory(sessionManager)
        viewModel = ViewModelProvider(this, factory)[UpdateProfileViewModel::class.java]

        currentUser = intent.getParcelableExtra(EXTRA_USER)

        initializeUI()
        prefillData()
        observeViewModel()

        buttonSave.setOnClickListener {
            clearErrors()
            collectAndSaveData()
        }

        buttonEditDob.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun initializeUI() {
        editTextName = findViewById(R.id.editText_edit_name)
        radioGroupGender = findViewById(R.id.radioGroup_edit_gender)
        radioButtonMale = findViewById(R.id.radioButton_edit_gender_male)
        radioButtonFemale = findViewById(R.id.radioButton_edit_gender_female)
        radioButtonOther = findViewById(R.id.radioButton_edit_gender_other)
        buttonEditDob = findViewById(R.id.button_edit_dob)
        tvEditSelectedDob = findViewById(R.id.tv_edit_selected_dob)
        editTextPhone = findViewById(R.id.editText_edit_phone)
        editTextBio = findViewById(R.id.editText_edit_bio)
        editTextAddress1 = findViewById(R.id.editText_edit_address1)
        editTextAddress2 = findViewById(R.id.editText_edit_address2)
        editTextCity = findViewById(R.id.editText_edit_city)
        editTextState = findViewById(R.id.editText_edit_state)
        editTextPostalCode = findViewById(R.id.editText_edit_postal_code)
        editTextCountry = findViewById(R.id.editText_edit_country)
        buttonSave = findViewById(R.id.button_save_profile)

        tilName = findViewById(R.id.til_edit_name)
        tilPhone = findViewById(R.id.til_edit_phone)
        tilBio = findViewById(R.id.til_edit_bio)
        tilAddress1 = findViewById(R.id.til_edit_address1)
        tilCity = findViewById(R.id.til_edit_city)
        tilState = findViewById(R.id.til_edit_state)
        tilPostalCode = findViewById(R.id.til_edit_postal_code)
        tilCountry = findViewById(R.id.til_edit_country)
    }

    private fun showLoadingDialog(dialogMessage: String = "Loading...") {
        if (loadingDialog == null || loadingDialog?.dialog?.isShowing == false) {
            loadingDialog = LoadingDialogFragment.newInstance(dialogMessage)
            if (!isFinishing && !isDestroyed) {
                loadingDialog?.show(supportFragmentManager, LoadingDialogFragment.TAG)
            }
        }
    }

    private fun hideLoadingDialog() {
        if (!isFinishing && !isDestroyed) {
            loadingDialog?.dismissAllowingStateLoss()
        }
        loadingDialog = null
    }

    private fun prefillData() {
        currentUser?.let { user ->
            editTextName.setText(user.name)
            when (user.gender?.lowercase(Locale.ROOT)) {
                "male" -> radioButtonMale.isChecked = true
                "female" -> radioButtonFemale.isChecked = true
                "other" -> radioButtonOther.isChecked = true
            }

            val dobValue = user.date_of_birth
            val formattedDob: String = when (dobValue) {
                is String -> {
                    formatDateForDisplay(dobValue) 
                }
                is Date -> {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("GMT+5.30") // Apply UTC Timezone
                    sdf.format(dobValue)
                }
                null -> ""
                else -> {
                    Log.w("EditProfileActivity", "Received unexpected type for date_of_birth: ${dobValue.javaClass.name}. Trying toString().")
                    formatDateForDisplay(dobValue.toString())
                }
            }
            tvEditSelectedDob.text = formattedDob

            editTextPhone.setText(user.phone)
            editTextBio.setText(user.bio)
            editTextAddress1.setText(user.address_line_1)
            editTextAddress2.setText(user.address_line_2)
            editTextCity.setText(user.city)
            editTextState.setText(user.state)
            editTextPostalCode.setText(user.postal_code)
            editTextCountry.setText(user.country)
        }
    }

    private fun formatDateForDisplay(apiDate: String?): String {
        if (apiDate.isNullOrEmpty()) return ""
        
        // Parser for yyyy-MM-dd, assuming UTC
        val yyyyMMddParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("GMT+5.30")
        }
        // Parsers for full datetime strings, also assuming UTC if no offset specified
        val dateTimeParsers = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }, // explicit UTC
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()), // Has offset, so system default timezone for parsing is fine initially
            SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH) // General, for Date.toString(), assumes local timezone if zzz isn't UTC
        )

        // Formatter to output yyyy-MM-dd in UTC to avoid day shifts
        val outputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("GMT+5.30")
        }

        // Try parsing yyyy-MM-dd first as UTC
        try {
            yyyyMMddParser.parse(apiDate)?.let { return outputFormatter.format(it) }
        } catch (e: Exception) {
            // Not a simple yyyy-MM-dd or parse failed, try other parsers
        }

        for (parser in dateTimeParsers) {
            try {
                // For parsers that might not be explicitly UTC (like Date.toString() or if Z is missing)
                // it's tricky. The goal is to get the correct underlying moment.
                // If the string contains timezone info (Z or offset), the parser should respect it.
                // If not, it defaults to local. This logic prioritizes yyyy-MM-dd as UTC.
                parser.parse(apiDate)?.let { return outputFormatter.format(it) }
            } catch (e: Exception) {
                // Continue
            }
        }
        Log.e("EditProfileActivity", "Error parsing date for display: $apiDate. Could not match known formats after UTC adjustments.")
        return if (apiDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) apiDate else "" // Fallback
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance() // Uses local timezone, which is fine for DatePicker
        val currentDobString = tvEditSelectedDob.text.toString()

        if (currentDobString.isNotEmpty()) {
            try {
                // When parsing date from display for DatePicker, assume it was displayed in UTC "yyyy-MM-dd"
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("GMT+5.30")
                }
                val date = parser.parse(currentDobString)
                if (date != null) {
                    calendar.time = date
                } else {
                     Log.w("EditProfileActivity", "Could not parse current DOB for DatePicker after setting UTC: $currentDobString")
                }
            } catch (e: Exception) {
                Log.w("EditProfileActivity", "Could not parse current DOB for DatePicker: $currentDobString", e)
                // If parsing fails, DatePicker will open with current date of local timezone
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                // DatePicker returns values based on local timezone calendar.
                // We format this into a "yyyy-MM-dd" string. This string represents the selected local date.
                // To maintain consistency and store/send as UTC, we treat this selected yyyy-MM-dd as UTC.
                val localSelectedDateCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDayOfMonth)
                }
                val outputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("GMT+5.30")
                }
                tvEditSelectedDob.text = outputSdf.format(localSelectedDateCalendar.time)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun observeViewModel() {
        viewModel.updateProfileResult.observe(this) { result ->
            when (result) {
                is UpdateProfileResult.Loading -> {
                    showLoadingDialog("Updating profile...")
                    buttonSave.isEnabled = false
                }
                is UpdateProfileResult.Success -> {
                    hideLoadingDialog()
                    buttonSave.isEnabled = true
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    val resultIntent = Intent()
                    resultIntent.putExtra(ProfileActivity.EXTRA_UPDATED_USER, result.user)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                is UpdateProfileResult.Error -> {
                    hideLoadingDialog()
                    buttonSave.isEnabled = true
                    Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                    Log.e("EditProfileActivity", "Error updating profile: ${result.message}, Details: ${result.errors}")
                    displayFieldErrors(result.errors)
                }
            }
        }
    }

    private fun clearErrors() {
        tilName.error = null
        tilPhone.error = null
        tilBio.error = null
        tilAddress1.error = null
        tilCity.error = null
        tilState.error = null
        tilPostalCode.error = null
        tilCountry.error = null
    }

    private fun displayFieldErrors(errors: Map<String, List<String>>?) {
        errors?.forEach { (field, messages) ->
            val message = messages.joinToString(", ")
            when (field) {
                "name" -> tilName.error = message
                "gender" -> Toast.makeText(this, "Gender error: $message", Toast.LENGTH_LONG).show()
                "date_of_birth" -> Toast.makeText(this, "Date of Birth error: $message", Toast.LENGTH_LONG).show()
                "phone" -> tilPhone.error = message
                "bio" -> tilBio.error = message
                "address_line_1" -> tilAddress1.error = message
                "city" -> tilCity.error = message
                "state" -> tilState.error = message
                "postal_code" -> tilPostalCode.error = message
                "country" -> tilCountry.error = message
                else -> Log.w("EditProfileActivity", "Unhandled error field: $field - $message")
            }
        }
    }

    private fun collectAndSaveData() {
        val name = editTextName.text.toString().trim()
        val selectedGenderId = radioGroupGender.checkedRadioButtonId
        val gender = if (selectedGenderId != -1) {
            findViewById<RadioButton>(selectedGenderId).text.toString().lowercase(Locale.ROOT)
        } else {
            ""
        }
        // tvEditSelectedDob.text is assumed to be in "yyyy-MM-dd" format, representing a UTC date
        val dob = tvEditSelectedDob.text.toString().trim()
        val phone = editTextPhone.text.toString().trim()
        val bio = editTextBio.text.toString().trim()
        val address1 = editTextAddress1.text.toString().trim()
        val address2 = editTextAddress2.text.toString().trim().ifEmpty { null }
        val city = editTextCity.text.toString().trim()
        val state = editTextState.text.toString().trim()
        val postalCode = editTextPostalCode.text.toString().trim()
        val country = editTextCountry.text.toString().trim()

        if (name.isEmpty()) {
            tilName.error = "Name cannot be empty"
            return
        }
        if (dob.isEmpty()) {
            Toast.makeText(this, "Date of birth cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val request = UpdateProfileRequest(
            name = name,
            gender = gender.ifEmpty { null },
            dateOfBirth = dob, // This string is now consistently "yyyy-MM-dd" UTC
            phone = phone.ifEmpty { null },
            bio = bio.ifEmpty { null },
            addressLine1 = address1.ifEmpty { null },
            addressLine2 = address2,
            city = city.ifEmpty { null },
            state = state.ifEmpty { null },
            postalCode = postalCode.ifEmpty { null },
            country = country.ifEmpty { null },
            email = sessionManager.getUserEmail()
        )
        viewModel.updateProfile(request)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
