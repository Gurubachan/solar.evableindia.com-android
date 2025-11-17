package com.solar.ev.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
            binding.tvQuotationStatus.text = quotation.status
            binding.tvQuotationAmount.text = "₹${quotation.quotationAmount}"
            binding.tvFundingType.text = "Funding: ${quotation.fundingType}"
            binding.tvCreatedAt.text = "Created: ${quotation.createdAt?.substringBefore("T")}"

            if (userRole == "admin") {
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
