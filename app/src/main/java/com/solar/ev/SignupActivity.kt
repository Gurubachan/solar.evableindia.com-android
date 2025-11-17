package com.solar.ev

// import android.app.ProgressDialog // Removed
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer // Keep this Observer for EventObserver
import com.solar.ev.databinding.ActivitySignupBinding
import com.solar.ev.model.SignupRequest
// import com.solar.ev.network.ApiService // Assuming ApiService is in this package, adjust if not
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.ui.common.LoadingDialogFragment // Added import
// No longer directly using SessionManager here, so the import can be removed if not used elsewhere
// import com.solar.ev.sharedPreferences.SessionManager 

// Assuming SignupViewModel, SignupViewModelFactory, SignupResult, Event are in these locations
import com.solar.ev.viewModel.signup.SignupViewModel
import com.solar.ev.viewModel.signup.SignupViewModelFactory
import com.solar.ev.viewModel.signup.SignupResult
import com.solar.ev.viewModel.signup.Event


class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private var loadingDialog: LoadingDialogFragment? = null // Added for new dialog
    // private var progressDialog: ProgressDialog? = null // Removed old dialog

    private val viewModel: SignupViewModel by viewModels {
        SignupViewModelFactory(application, RetrofitInstance.api)
    }

    companion object {
        private const val PHONE_NUMBER_LENGTH = 10
        private const val MIN_PASSWORD_LENGTH = 6
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.buttonSignup.setOnClickListener {
            if (validateInput()) {
                val signupRequest = SignupRequest(
                    name = binding.editTextUsernameSignup.text.toString().trim(),
                    email = binding.editTextEmailSignup.text.toString().trim(),
                    password = binding.editTextPasswordSignup.text.toString(),
                    passwordConfirmation = binding.editTextConfirmPasswordSignup.text.toString(),
                    phone = binding.editTextPhoneSignup.text.toString().trim()
                )
                viewModel.signupUser(signupRequest)
            }
        }

        binding.textViewNavigateToLogin.setOnClickListener {
            finish() 
        }
    }

    private fun observeViewModel() {
        viewModel.signupResult.observe(this, EventObserver { result ->
            when (result) {
                is SignupResult.Loading -> {
                    showLoadingDialog() // Changed to new dialog method
                }
                is SignupResult.Success -> {
                    hideLoadingDialog() // Changed to new dialog method
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    navigateToLogin()
                }
                is SignupResult.Error -> {
                    hideLoadingDialog() // Changed to new dialog method
                    Toast.makeText(this, result.errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun validateInput(): Boolean {
        binding.textInputLayoutUsernameSignup.error = null
        binding.textInputLayoutEmailSignup.error = null
        binding.textInputLayoutPhoneSignup.error = null
        binding.textInputLayoutPasswordSignup.error = null
        binding.textInputLayoutConfirmPasswordSignup.error = null

        var isValid = true

        val username = binding.editTextUsernameSignup.text.toString().trim()
        val email = binding.editTextEmailSignup.text.toString().trim()
        val phone = binding.editTextPhoneSignup.text.toString().trim()
        val password = binding.editTextPasswordSignup.text.toString()
        val confirmPassword = binding.editTextConfirmPasswordSignup.text.toString()

        if (username.isEmpty()) {
            binding.textInputLayoutUsernameSignup.error = getString(R.string.error_field_required)
            isValid = false
        }
        if (email.isEmpty()) {
            binding.textInputLayoutEmailSignup.error = getString(R.string.error_field_required)
            isValid = false
        } else if (!isValidEmail(email)) {
            binding.textInputLayoutEmailSignup.error = getString(R.string.error_invalid_email_format)
            isValid = false
        }
        if (phone.isEmpty()) {
            binding.textInputLayoutPhoneSignup.error = getString(R.string.error_field_required)
            isValid = false
        } else if (phone.length != PHONE_NUMBER_LENGTH) {
            binding.textInputLayoutPhoneSignup.error = getString(R.string.error_phone_length, PHONE_NUMBER_LENGTH)
            isValid = false
        }
        if (password.isEmpty()) {
            binding.textInputLayoutPasswordSignup.error = getString(R.string.error_field_required)
            isValid = false
        } else if (password.length < MIN_PASSWORD_LENGTH) {
            binding.textInputLayoutPasswordSignup.error = getString(R.string.error_password_length, MIN_PASSWORD_LENGTH)
            isValid = false
        }
        if (confirmPassword.isEmpty()) {
            binding.textInputLayoutConfirmPasswordSignup.error = getString(R.string.error_field_required)
            isValid = false
        } else if (password != confirmPassword) {
            binding.textInputLayoutConfirmPasswordSignup.error = getString(R.string.error_passwords_do_not_match)
            isValid = false
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // New helper methods for LoadingDialogFragment
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

    // Old showLoading and hideLoading methods removed

    class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
        override fun onChanged(value: Event<T>) { 
            value.getContentIfNotHandled()?.let {
                onEventUnhandledContent(it)
            }
        }
    }
}
