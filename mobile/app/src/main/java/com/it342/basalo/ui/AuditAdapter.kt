package com.it342.basalo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.it342.basalo.R
import com.it342.basalo.core.data.AuditRecord
import com.it342.basalo.core.ui.DisplayFormatter

class AuditAdapter : RecyclerView.Adapter<AuditAdapter.AuditViewHolder>() {
    private val items = mutableListOf<AuditRecord>()

    fun submitList(data: List<AuditRecord>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuditViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audit_row, parent, false)
        return AuditViewHolder(view as ViewGroup)
    }

    override fun onBindViewHolder(holder: AuditViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AuditViewHolder(view: ViewGroup) : RecyclerView.ViewHolder(view) {
        private val action = view.findViewById<TextView>(R.id.tvAuditAction)
        private val user = view.findViewById<TextView>(R.id.tvAuditUser)
        private val details = view.findViewById<TextView>(R.id.tvAuditDetails)
        private val timestamp = view.findViewById<TextView>(R.id.tvAuditTimestamp)

        fun bind(item: AuditRecord) {
            action.text = item.actionPerformed
            user.text = itemView.context.getString(R.string.audit_user_format, item.userEmail)
            details.text = item.details
            timestamp.text = itemView.context.getString(
                R.string.audit_timestamp_format,
                DisplayFormatter.formatDateTime(item.timestamp)
            )
        }
    }
}
