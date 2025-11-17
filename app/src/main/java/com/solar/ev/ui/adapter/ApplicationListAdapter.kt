package com.solar.ev.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.solar.ev.BankDetailsActivity
import com.solar.ev.DiscomDetailsActivity
import com.solar.ev.InstallationDetailsActivity
import com.solar.ev.KYCActivity
import com.solar.ev.R
import com.solar.ev.databinding.ItemApplicationCardBinding
import com.solar.ev.model.application.ApplicationListItem



class ApplicationListAdapter(
    private val onEditClickListener: (applicationId: String) -> Unit,
    private val onDeleteClickListener: (applicationId: String) -> Unit,
    private val onProcessSuryaGharClickListener: (applicationId: String, applicantName: String?) -> Unit,
    private val onUploadQuotationClickListener: (applicationId: String) -> Unit,
    private val currentUserRole: String?
) : ListAdapter<ApplicationListItem, ApplicationListAdapter.ApplicationViewHolder>(ApplicationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val binding = ItemApplicationCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ApplicationViewHolder(binding, onEditClickListener, onDeleteClickListener, onProcessSuryaGharClickListener, onUploadQuotationClickListener, currentUserRole)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position) // Pass position to bind method
    }

    class ApplicationViewHolder(
        private val binding: ItemApplicationCardBinding,
        private val onEditClickListener: (applicationId: String) -> Unit,
        private val onDeleteClickListener: (applicationId: String) -> Unit,
        private val onProcessSuryaGharClickListener: (applicationId: String, applicantName: String?) -> Unit,
        private val onUploadQuotationClickListener: (applicationId: String) -> Unit,
        private val currentUserRole: String?
    ) : RecyclerView.ViewHolder(binding.root) {

        private val deleteButton: ImageButton = binding.btnDeleteApplication
        private val adminLikeRolesForAppliedBy = listOf("admin", "supervisor", "back-office")

        fun bind(item: ApplicationListItem, position: Int) { // Added position parameter
            // Assuming you will add a TextView with android:id="@+id/tv_serial_number" to your item_application_card.xml
            binding.tvSerialNumber.text = "${position + 1}." 

            binding.tvApplicationName.text = item.name ?: "N/A"
            binding.tvApplicationDob.text = item.dob ?: "N/A"
            binding.tvApplicationIdCard.text = item.id ?: "N/A"
            binding.tvApplicationContact.text = item.contactNumber ?: "N/A"
            binding.tvApplicationStatusCard.text =
                item.status?.replaceFirstChar { it.uppercase() } ?: "N/A"

            if (adminLikeRolesForAppliedBy.any { it.equals(currentUserRole, ignoreCase = true) } && !item.user?.name.isNullOrBlank()) {
                binding.tvAppliedByUserNameLabel.visibility = View.VISIBLE
                binding.tvAppliedByUserName.visibility = View.VISIBLE
                binding.tvAppliedByUserName.text = item.user?.name
            } else {
                binding.tvAppliedByUserNameLabel.visibility = View.GONE
                binding.tvAppliedByUserName.visibility = View.GONE
            }

            val context = binding.root.context

            item.id?.let { applicationId ->
                if (item.isFullyVerified) {
                    binding.btnEditApplication.text = "Process for Surya Ghar"
                    binding.btnEditApplication.setOnClickListener { 
                        onProcessSuryaGharClickListener(applicationId, item.name)
                    }
                } else {
                    binding.btnEditApplication.text = "Edit Details"
                    binding.btnEditApplication.setOnClickListener { 
                        onEditClickListener(applicationId)
                    }
                }
                binding.btnEditApplication.visibility = View.VISIBLE

                if (currentUserRole.equals("admin", ignoreCase = true)) {
                    deleteButton.visibility = View.VISIBLE
                    deleteButton.setOnClickListener {
                        onDeleteClickListener(applicationId)
                    }
                } else {
                    deleteButton.visibility = View.GONE
                }

                binding.btnManageKycItem.setOnClickListener {
                    val kycId = item.kyc?.id
                    val intent = Intent(context, KYCActivity::class.java).apply {
                        putExtra(KYCActivity.EXTRA_APPLICATION_ID, applicationId)
                        kycId?.takeIf { it.isNotEmpty() }?.let {
                            putExtra(KYCActivity.EXTRA_KYC_ID, it) 
                        }
                    }
                    context.startActivity(intent)
                }
                val kycColor = if (item.kyc?.verificationStatus.equals("verified", ignoreCase = true)) R.color.green 
                               else if (item.kyc?.id?.isNotEmpty() == true) R.color.blue
                               else R.color.yellow
                binding.btnManageKycItem.setIconTintResource(kycColor)
                binding.btnManageKycItem.visibility = View.VISIBLE 

                binding.btnManageBankItem.setOnClickListener {
                    val intent = Intent(context, BankDetailsActivity::class.java).apply {
                        putExtra(BankDetailsActivity.EXTRA_APPLICATION_ID, applicationId)
                        item.name?.let { applicantName ->
                            putExtra(BankDetailsActivity.EXTRA_APPLICANT_NAME, applicantName)
                        }
                        item.bank?.id?.takeIf { it.isNotEmpty() }?.let {
                            putExtra(BankDetailsActivity.EXTRA_BANK_ID, it)
                        }
                    }
                    context.startActivity(intent)
                }
                val bankColor = if (item.bank?.verificationStatus.equals("verified", ignoreCase = true)) R.color.green
                                else if (item.bank?.id?.isNotEmpty() == true) R.color.blue
                                else R.color.yellow
                binding.btnManageBankItem.setIconTintResource(bankColor)
                binding.btnManageBankItem.visibility = View.VISIBLE

                binding.btnManageDiscomItem.setOnClickListener {
                    val intent = Intent(context, DiscomDetailsActivity::class.java).apply {
                        putExtra(DiscomDetailsActivity.EXTRA_APPLICATION_ID, applicationId)
                        item.discom?.id?.takeIf { it.isNotEmpty() }?.let {
                            putExtra(DiscomDetailsActivity.EXTRA_DISCOM_ID, it)
                        }
                    }
                    context.startActivity(intent)
                }
                val discomColor = if (item.discom?.verificationStatus.equals("verified", ignoreCase = true)) R.color.green
                                  else if (item.discom?.id?.isNotEmpty() == true) R.color.blue
                                  else R.color.yellow
                binding.btnManageDiscomItem.setIconTintResource(discomColor)
                binding.btnManageDiscomItem.visibility = View.VISIBLE

                binding.btnManageInstallationItem.setOnClickListener {
                    val intent = Intent(context, InstallationDetailsActivity::class.java).apply {
                        putExtra(InstallationDetailsActivity.EXTRA_APPLICATION_ID, applicationId)
                        item.installation?.id?.takeIf { it.isNotEmpty() }?.let {
                            putExtra(InstallationDetailsActivity.EXTRA_INSTALLATION_ID, it)
                        }
                    }
                    context.startActivity(intent)
                }
                val installationColor = if (item.installation?.verificationStatus.equals("verified", ignoreCase = true)) R.color.green
                                        else if (item.installation?.id?.isNotEmpty() == true) R.color.blue
                                        else R.color.yellow
                binding.btnManageInstallationItem.setIconTintResource(installationColor)
                binding.btnManageInstallationItem.visibility = View.VISIBLE

                binding.btnUploadQuotation.setOnClickListener { onUploadQuotationClickListener(applicationId) }

            } ?: run {
                binding.btnEditApplication.visibility = View.GONE
                deleteButton.visibility = View.GONE
                binding.btnManageKycItem.visibility = View.GONE
                binding.btnManageBankItem.visibility = View.GONE
                binding.btnManageDiscomItem.visibility = View.GONE
                binding.btnManageInstallationItem.visibility = View.GONE
                binding.btnUploadQuotation.visibility = View.GONE
                
                val defaultColorRes = R.color.grey
                binding.btnManageKycItem.setIconTintResource(defaultColorRes)
                binding.btnManageBankItem.setIconTintResource(defaultColorRes)
                binding.btnManageDiscomItem.setIconTintResource(defaultColorRes)
                binding.btnManageInstallationItem.setIconTintResource(defaultColorRes)
            }
        }
    }

    class ApplicationDiffCallback : DiffUtil.ItemCallback<ApplicationListItem>() {
        override fun areItemsTheSame(oldItem: ApplicationListItem, newItem: ApplicationListItem): Boolean {
            return oldItem.id == newItem.id 
        }

        override fun areContentsTheSame(oldItem: ApplicationListItem, newItem: ApplicationListItem): Boolean {
            return oldItem == newItem 
        }
    }
}
