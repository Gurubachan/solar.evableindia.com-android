package com.solar.ev // Your main package

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log

import android.view.View
import android.widget.Button
import android.widget.ImageView
// import android.widget.ProgressBar // Removed
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import androidx.lifecycle.ViewModelProvider

import com.bumptech.glide.Glide
import com.solar.ev.model.User
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.ui.common.LoadingDialogFragment // Added import
import com.solar.ev.viewModel.ProfileResult
import com.solar.ev.viewModel.ProfileViewModel
import com.solar.ev.viewModel.ProfileViewModelFactory
import com.solar.ev.viewModel.SendVerificationEmailResult
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64 // For Base64 encoding
import android.view.Menu
import android.view.MenuItem
import com.solar.ev.viewModel.UploadPhotoResult
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileActivity : BaseActivity() {

    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var sessionManager: SessionManager
    private var loadingDialog: LoadingDialogFragment? = null // Added

    // UI Elements
    // private lateinit var progressBar: ProgressBar // Removed
    private lateinit var imageViewProfilePhoto: ImageView
    private lateinit var textViewName: TextView
    private lateinit var textViewEmail: TextView
    private lateinit var textViewPhone: TextView
    private lateinit var textViewRole: TextView
    private lateinit var textViewBio: TextView
    private lateinit var textViewAddress: TextView
    private lateinit var buttonEditProfile: Button
    private lateinit var buttonVerifyEmail: Button
    private lateinit var buttonChangeProfilePhoto: Button
    private lateinit var imageViewEmailVerifiedTick: ImageView

    private var currentUser: User? = null // Store current user data

    companion object {
        const val EXTRA_UPDATED_USER = "extra_updated_user_profile"
    }

    private val editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedUser = result.data?.getParcelableExtra<User>(EXTRA_UPDATED_USER)
            if (updatedUser != null) {
                this.currentUser = updatedUser // Update the stored user
                populateUI(updatedUser)
                Toast.makeText(this, "Profile data refreshed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val toolbar: Toolbar = findViewById(R.id.toolbar_profile)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Profile"

        sessionManager = SessionManager(applicationContext)
        val factory = ProfileViewModelFactory(sessionManager)
        profileViewModel = ViewModelProvider(this, factory).get(ProfileViewModel::class.java)
        profileViewModel.fetchUserProfile() // Fetch data when activity is created

        initializeUI()
        observeViewModel()

        buttonEditProfile.visibility=View.GONE

        buttonEditProfile.setOnClickListener {
            currentUser?.let { user ->
                if (user.profile_completed == true) {
                    Toast.makeText(this, "Profile cannot be edited as it's already completed.", Toast.LENGTH_LONG).show()
                } else {
                    val intent = Intent(this, EditProfileActivity::class.java)
                    intent.putExtra(EditProfileActivity.EXTRA_USER, user)
                    editProfileLauncher.launch(intent)
                }
            } ?: Toast.makeText(this, "User data not loaded yet.", Toast.LENGTH_SHORT).show()
        }

        profileViewModel.sendVerificationEmailStatus.observe(this) { result ->
            when (result) {
                is SendVerificationEmailResult.Loading -> {
                    buttonVerifyEmail.isEnabled = false
                    Toast.makeText(this, "Sending verification email...", Toast.LENGTH_SHORT).show()
                }
                is SendVerificationEmailResult.Success -> {
                    buttonVerifyEmail.isEnabled = false // Keep it disabled or hide based on UX
                    Toast.makeText(this, "Verification email sent successfully!", Toast.LENGTH_LONG).show()
                }
                is SendVerificationEmailResult.Error -> {
                    buttonVerifyEmail.isEnabled = true
                    Toast.makeText(this, "Error sending email: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        profileViewModel.uploadPhotoResult.observe(this) { result ->
            // This observer might be triggered after a previous loading state (e.g., general profile load)
            // Ensure the loading dialog is appropriately handled for this specific action.
            when (result) {
                is UploadPhotoResult.Loading -> {
                    showLoadingDialog() // Changed
                    buttonChangeProfilePhoto.isEnabled = false
                    // Toast for "Uploading photo..." can be part of the LoadingDialogFragment if desired,
                    // or shown here if preferred before showLoadingDialog()
                }
                is UploadPhotoResult.Success -> {
                    hideLoadingDialog() // Changed
                    buttonChangeProfilePhoto.isEnabled = true
                    Toast.makeText(this, "Profile photo updated successfully!", Toast.LENGTH_LONG).show()
                    // Profile will be re-fetched by fetchUserProfile() which will update UI via userProfileResult observer.
                }
                is UploadPhotoResult.Error -> {
                    hideLoadingDialog() // Changed
                    buttonChangeProfilePhoto.isEnabled = true
                    Toast.makeText(this, "Photo upload failed: ${result.message}", Toast.LENGTH_LONG).show()
                    Log.e("ProfileActivity", "Photo upload error: ${result.message}")
                }
            }
        }

        buttonChangeProfilePhoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun initializeUI() {
        // progressBar = findViewById(R.id.progressBar_profile) // Removed
        imageViewProfilePhoto = findViewById(R.id.imageView_profile_photo)
        textViewName = findViewById(R.id.textView_profile_name)
        textViewEmail = findViewById(R.id.textView_profile_email)
        textViewPhone = findViewById(R.id.textView_profile_phone)
        textViewRole = findViewById(R.id.textView_profile_role)
        textViewBio = findViewById(R.id.textView_profile_bio)
        textViewAddress = findViewById(R.id.textView_profile_address)
        buttonEditProfile = findViewById(R.id.button_edit_profile)
        buttonVerifyEmail = findViewById(R.id.button_verify_email)
        buttonChangeProfilePhoto = findViewById(R.id.button_change_profile_photo)
        imageViewEmailVerifiedTick = findViewById(R.id.imageView_email_verified_tick)
    }

    // Helper methods for loading dialog (Copied from ApplicationActivity)
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

    private fun observeViewModel() {
        profileViewModel.userProfileResult.observe(this) { result ->
            Log.d("ProfileActivity", "Received result: $result")
            when (result) {
                is ProfileResult.Loading -> {
                    showLoadingDialog() // Changed
                }
                is ProfileResult.Success -> {
                    hideLoadingDialog() // Changed
                    populateUI(result.user ?: null)
                }
                is ProfileResult.Error -> {
                    hideLoadingDialog() // Changed
                    Toast.makeText(this, "Error fetching profile: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun populateUI(user: User?) {
        textViewName.text = user?.name ?: "N/A"
        textViewEmail.text = user?.email ?: "N/A"
        textViewPhone.text = user?.phone ?: "N/A"
        textViewRole.text = user?.role ?: "N/A"
        textViewBio.text = user?.bio ?: "N/A"

        val addressParts = listOfNotNull(
            user?.address_line_1,
            user?.address_line_2,
            user?.city,
            user?.state,
            user?.postal_code,
            user?.country
        ).filter { it.isNotBlank() }
        textViewAddress.text = if (addressParts.isNotEmpty()) addressParts.joinToString(", ") else "N/A"

        val BASE_IMAGE_URL = "https://solar.evableindia.com/core/public/storage/"

        if (user?.profile_photo.isNullOrEmpty()) {
            imageViewProfilePhoto.setImageResource(R.drawable.ic_default_profile)
            buttonChangeProfilePhoto.visibility= View.VISIBLE
        } else {
            val fullImageUrl = BASE_IMAGE_URL + user?.profile_photo
            Log.d("ProfileActivity", "Loading image from URL: $fullImageUrl")
            Glide.with(this@ProfileActivity)
                .load(fullImageUrl)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .circleCrop()
                .into(imageViewProfilePhoto)
            buttonChangeProfilePhoto.visibility= View.GONE
        }
        currentUser=user
        if(user?.profile_completed == true){
            buttonEditProfile.visibility=View.GONE
        }else{
            buttonEditProfile.visibility=View.VISIBLE
        }

        if (user?.email_verified_at!=null){
            buttonVerifyEmail.visibility=View.GONE
            imageViewEmailVerifiedTick.visibility=View.VISIBLE
        }else{
            buttonVerifyEmail.visibility=View.VISIBLE
            imageViewEmailVerifiedTick.visibility=View.GONE
            buttonVerifyEmail.setOnClickListener {
                profileViewModel.sendVerificationEmail()
            }
        }
    }

    private fun handleImageUpload(uri: Uri) {
        Log.d("ProfileActivity", "Selected image URI: $uri")
        val base64Image = uriToBase64(uri)

        if (base64Image != null) {
            profileViewModel.uploadProfilePhoto(base64Image) // This will trigger UploadPhotoResult.Loading
        } else {
            Toast.makeText(this, "Failed to convert image to Base64.", Toast.LENGTH_LONG).show()
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e("ProfileActivity", "Failed to decode bitmap from URI.")
                return null
            }

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error converting URI to Base64", e)
            null
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            handleImageUpload(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu?.findItem(R.id.action_profile)?.let { profileMenuItem ->
            profileMenuItem.title = "Home"
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, MainActivity::class.java))
                true
            }
            R.id.action_logout -> {
                Toast.makeText(this, "Logout Clicked", Toast.LENGTH_SHORT).show()
                sessionManager.clearSession()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
