package com.solar.ev

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView // Added for tvAppVersion
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.ui.common.LoadingDialogFragment
import com.solar.ev.viewModel.login.LoginResult
import com.solar.ev.viewModel.login.LoginViewModel

class LoginActivity : AppCompatActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private var loadingDialog: LoadingDialogFragment? = null

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var sessionManager: SessionManager
    private lateinit var tvAppVersion: TextView // Added

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        sessionManager = SessionManager(applicationContext)

        emailEditText = findViewById(R.id.editTextEmailLogin)
        passwordEditText = findViewById(R.id.editTextPasswordLogin)
        loginButton = findViewById(R.id.buttonLogin)
        tvAppVersion = findViewById(R.id.tv_app_version) // Added

        // Display App Version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            tvAppVersion.text = "Version: $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("LoginActivity", "Error getting package info: ", e)
            tvAppVersion.text = "Version: N/A"
        }

        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginViewModel.performLogin(email, password)
        }

        loginViewModel.loginResult.observe(this) { result ->
            when (result) {
                is LoginResult.Loading -> {
                    showLoadingDialog()
                    loginButton.isEnabled = false
                }
                is LoginResult.Success -> {
                    hideLoadingDialog()
                    loginButton.isEnabled = true
                    Toast.makeText(this, result.response.message, Toast.LENGTH_LONG).show()
                    sessionManager.saveAuthToken(result.response.data.token)
                    val user = result.response.data.user
                    sessionManager.saveUserData(user)
                    if(user.profile_completed == true){
                        navigateToMain()
                    } else {
                        navigateToProfile()
                    }
                    //navigateToMain()
                }
                is LoginResult.Error -> {
                    hideLoadingDialog()
                    loginButton.isEnabled = true
                    Toast.makeText(this, "Login Failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        val navigateToSignupTextView = findViewById<com.google.android.material.textview.MaterialTextView>(R.id.textViewNavigateToSignup)
        navigateToSignupTextView.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
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

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToProfile(){
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
