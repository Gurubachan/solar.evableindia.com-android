package com.solar.ev.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.solar.ev.QuotationActivity
import com.solar.ev.databinding.ItemQuotationBinding
import com.solar.ev.model.quotation.QuotationListItem

class QuotationAdapter(
    private val quotations: List<QuotationListItem>,
    private val userRole: String?,
    private val onAction: (QuotationListItem, String) -> Unit
) : RecyclerView.Adapter<QuotationAdapter.QuotationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuotationViewHolder {
        val binding = ItemQuotationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuotationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuotationViewHolder, position: Int) {
        holder.bind(quotations[position])
    }

    override fun getItemCount() = quotations.size

    inner class QuotationViewHolder(private val binding: ItemQuotationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(quotation: QuotationListItem) {
            binding.tvQuotationRefId.text = quotation.quotationReferenceId
            binding.tvQuotationStatus.text = quotation.status?.replaceFirstChar { it.uppercase() }
            binding.tvQuotationAmount.text = "₹${quotation.quotationAmount}"
            binding.tvFundingType.text = "Funding: ${quotation.fundingType}"
            binding.tvQuotationDetails.text = quotation.quotationDetails

            binding.tvAppliedByName.text = "Applied by: ${quotation.appliedBy?.name}"
            binding.tvAppliedByRole.text = quotation.appliedBy?.role?.replaceFirstChar { it.uppercase() }
            Glide.with(binding.root.context).load(QuotationActivity.BASE_IMAGE_URL + quotation.appliedBy?.profilePhoto).into(binding.ivAppliedBy)

            if (quotation.approvedBy != null) {
                binding.llApprovedBy.visibility = View.VISIBLE
                binding.tvApprovedByName.text = "Approved by: ${quotation.approvedBy.name}"
                binding.tvApprovedByRole.text = quotation.approvedBy.role.replaceFirstChar { it.uppercase() }
                Glide.with(binding.root.context).load(QuotationActivity.BASE_IMAGE_URL + quotation.approvedBy.profilePhoto).into(binding.ivApprovedBy)
            } else {
                binding.llApprovedBy.visibility = View.GONE
            }

            if (quotation.remarks != null) {
                binding.tvRemarks.visibility = View.VISIBLE
                binding.tvRemarks.text = "Remarks: ${quotation.remarks}"
            } else {
                binding.tvRemarks.visibility = View.GONE
            }

            binding.tvSubmittedAt.text = "Submitted: ${quotation.submittedAt?.substringBefore("T")}"
            binding.tvApprovedAt.text = "Approved: ${quotation.approvedAt?.substringBefore("T")}"

            binding.btnViewQuotation.setOnClickListener {
                onAction(quotation, "view")
            }

            if (userRole == "admin" || userRole == "supervisor") {
                binding.btnDeleteQuotation.visibility = View.VISIBLE
                binding.btnDeleteQuotation.setOnClickListener {
                    onAction(quotation, "delete")
                }
            } else {
                binding.btnDeleteQuotation.visibility = View.GONE
            }
        }
    }
}
