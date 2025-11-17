package com.solar.ev

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.solar.ev.adapter.UserManagementAdapter
import com.solar.ev.databinding.ActivityUserManagementBinding
import com.solar.ev.model.user.UserInfo
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.viewModel.user.UserListResult
import com.solar.ev.viewModel.user.UserManagementViewModel
import com.solar.ev.viewModel.user.UserManagementViewModelFactory
import com.solar.ev.viewModel.user.UserUpdateResult

class UserManagementActivity : BaseActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var userManagementAdapter: UserManagementAdapter
    private lateinit var sessionManager: SessionManager

    private val viewModel: UserManagementViewModel by viewModels {
        UserManagementViewModelFactory(RetrofitInstance.api)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        fetchUsersList()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarUserManagement)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        userManagementAdapter = UserManagementAdapter(this) { userId, request ->
            val token = sessionManager.getUserToken()
            if (token != null && userId.isNotEmpty()) {
                viewModel.updateUser(token, userId, request)
            } else {
                Toast.makeText(this, "Session error or invalid user.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvUserList.apply {
            adapter = userManagementAdapter
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.userListResult.observe(this) { result ->
            when (result) {
                is UserListResult.Loading -> {
                    binding.progressBarUserManagement.visibility = View.VISIBLE
                    binding.rvUserList.visibility = View.GONE
                }
                is UserListResult.Success -> {
                    binding.progressBarUserManagement.visibility = View.GONE
                    if (result.users.isEmpty()) {
                        Toast.makeText(this, "No users found.", Toast.LENGTH_LONG).show()
                        binding.rvUserList.visibility = View.GONE
                    } else {
                        binding.rvUserList.visibility = View.VISIBLE
                        userManagementAdapter.setUsers(result.users)
                    }
                }
                is UserListResult.Error -> {
                    binding.progressBarUserManagement.visibility = View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    binding.rvUserList.visibility = View.GONE
                }
            }
        }

        viewModel.userUpdateResult.observe(this) { result ->
            // You might want a more granular loading indicator here (e.g., on the item itself)
            when (result) {
                is UserUpdateResult.Loading -> {
                     Toast.makeText(this, "Updating user...", Toast.LENGTH_SHORT).show() // Simple toast for now
                }
                is UserUpdateResult.Success -> {
                    Toast.makeText(this, result.originalMessage ?: "User updated successfully!", Toast.LENGTH_LONG).show()
                    // Refresh the specific item in the list or the whole list
                    // For simplicity, refetching the list. Better: update the item in adapter's list.
                    fetchUsersList() 
                }
                is UserUpdateResult.Error -> {
                    Toast.makeText(this, "Failed to update user: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchUsersList() {
        val token = sessionManager.getUserToken()
        if (token != null) {
            viewModel.fetchUsers(token)
        } else {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            finish() // Or redirect to login
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
