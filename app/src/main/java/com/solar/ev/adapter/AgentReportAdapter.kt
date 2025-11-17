package com.solar.ev.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.solar.ev.databinding.ItemAgentReportBinding
import com.solar.ev.model.report.AgentReportItem

class AgentReportAdapter : ListAdapter<AgentReportItem, AgentReportAdapter.AgentReportViewHolder>(AgentReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgentReportViewHolder {
        val binding = ItemAgentReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AgentReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AgentReportViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class AgentReportViewHolder(private val binding: ItemAgentReportBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AgentReportItem) {
            binding.tvAgentName.text = item.userName
            binding.tvApplicationsCount.text = item.applicationsCount.toString()
            binding.tvKycCount.text = item.kycCount.toString()
            binding.tvBankCount.text = item.bankCount.toString()
            binding.tvDiscomCount.text = item.discomCount.toString()
            binding.tvInstallationCount.text = item.installationCount.toString()
        }
    }

    class AgentReportDiffCallback : DiffUtil.ItemCallback<AgentReportItem>() {
        override fun areItemsTheSame(oldItem: AgentReportItem, newItem: AgentReportItem): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: AgentReportItem, newItem: AgentReportItem): Boolean {
            return oldItem == newItem
        }
    }
}
