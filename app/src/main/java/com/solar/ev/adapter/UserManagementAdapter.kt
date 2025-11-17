package com.solar.ev.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.solar.ev.databinding.ItemUserCardBinding
import com.solar.ev.model.user.UserInfo
import com.solar.ev.model.user.UpdateUserManagementRequest

class UserManagementAdapter(
    private val context: Context, // Needed for ArrayAdapter
    private val onSaveChangesClickListener: (userId: String, request: UpdateUserManagementRequest) -> Unit
) : ListAdapter<UserInfo, UserManagementAdapter.UserViewHolder>(UserDiffCallback()), Filterable {

    private var originalList: List<UserInfo> = emptyList()
    private val userRoles = arrayOf("client", "agent", "supervisor", "back-office") // Available roles

    fun setUsers(users: List<UserInfo>) {
        originalList = users
        submitList(users)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    inner class UserViewHolder(private val binding: ItemUserCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserInfo) {
            binding.tvUserName.text = user.name ?: "N/A"
            binding.tvUserEmail.text = user.email ?: "N/A"
            binding.tvCurrentRole.text = user.role?.replaceFirstChar { it.uppercase() } ?: "N/A"

            // Setup Role Dropdown
            val roleAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, userRoles)
            binding.actUserRole.setAdapter(roleAdapter)
            if (userRoles.contains(user.role)) {
                binding.actUserRole.setText(user.role, false)
            }

            // Setup Login Allowed Switch
            binding.switchLoginAllowed.isChecked = user.loginAllowed ?: true // Default to true if null

            // Setup Is Active Switch
            binding.switchIsActive.isChecked = user.isActive ?: true // Default to true if null

            binding.buttonSaveUserChanges.setOnClickListener { // Corrected ID here
                val selectedRole = binding.actUserRole.text.toString()
                val isLoginAllowed = binding.switchLoginAllowed.isChecked
                val isActive = binding.switchIsActive.isChecked

                var roleToUpdate: String? = null
                if (selectedRole.isNotEmpty() && selectedRole != user.role) {
                    roleToUpdate = selectedRole
                }

                var loginAllowedToUpdate: Boolean? = null
                if (isLoginAllowed != (user.loginAllowed ?: true) ) { // Compare against default if original is null
                    loginAllowedToUpdate = isLoginAllowed
                }

                var isActiveToUpdate: Boolean? = null
                if (isActive != (user.isActive ?: true) ) { // Compare against default if original is null
                    isActiveToUpdate = isActive
                }

                if (roleToUpdate != null || loginAllowedToUpdate != null || isActiveToUpdate != null) {
                    user.id?.let {
                        userId ->
                        val request = UpdateUserManagementRequest(
                            role = roleToUpdate,
                            loginAllowed = loginAllowedToUpdate,
                            isActive = isActiveToUpdate
                        )
                        onSaveChangesClickListener(userId, request)
                    }
                } else {
                    // Optionally, show a toast that no changes were made
                    // Toast.makeText(context, "No changes detected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserInfo>() {
        override fun areItemsTheSame(oldItem: UserInfo, newItem: UserInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserInfo, newItem: UserInfo): Boolean {
            return oldItem == newItem
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredList = if (constraint.isNullOrEmpty()) {
                    originalList
                } else {
                    val filterPattern = constraint.toString().lowercase().trim()
                    originalList.filter {
                        it.name?.lowercase()?.contains(filterPattern) == true ||
                        it.email?.lowercase()?.contains(filterPattern) == true ||
                        it.role?.lowercase()?.contains(filterPattern) == true
                    }
                }
                return FilterResults().apply { values = filteredList }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                submitList(results?.values as? List<UserInfo>)
            }
        }
    }
}
