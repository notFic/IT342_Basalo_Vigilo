package com.it342.basalo.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.it342.basalo.R
import com.it342.basalo.data.VisitorRecord
import com.it342.basalo.data.VisitorStatus

class HistoryLogsAdapter(
    private val isAdmin: Boolean,
    private val onVoidRecord: (VisitorRecord) -> Unit
) : RecyclerView.Adapter<HistoryLogsAdapter.HistoryLogViewHolder>() {

    private val items = mutableListOf<VisitorRecord>()

    fun submitList(data: List<VisitorRecord>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_log, parent, false)
        return HistoryLogViewHolder(view, isAdmin, onVoidRecord)
    }

    override fun onBindViewHolder(holder: HistoryLogViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class HistoryLogViewHolder(
        itemView: View,
        private val isAdmin: Boolean,
        private val onVoidRecord: (VisitorRecord) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameText: TextView = itemView.findViewById(R.id.tvHistoryVisitorName)
        private val infoText: TextView = itemView.findViewById(R.id.tvHistoryVisitorInfo)
        private val timeInText: TextView = itemView.findViewById(R.id.tvHistoryTimeIn)
        private val timeOutText: TextView = itemView.findViewById(R.id.tvHistoryTimeOut)
        private val chip: Chip = itemView.findViewById(R.id.chipStatus)
        private val voidButton: Button = itemView.findViewById(R.id.btnVoidRecord)

        fun bind(item: VisitorRecord) {
            val context = itemView.context
            nameText.text = item.fullName
            infoText.text = context.getString(
                R.string.history_info_format,
                item.visitorType,
                item.hostName
            )
            timeInText.text = context.getString(R.string.time_in_format, item.timeIn)
            timeOutText.text = context.getString(
                R.string.time_out_format,
                item.timeOut ?: context.getString(R.string.still_active)
            )
            chip.text = formatStatus(item.status)
            chip.chipBackgroundColor = ContextCompat.getColorStateList(context, statusColor(item.status))

            val showButton = isAdmin && item.status != VisitorStatus.VOIDED
            voidButton.visibility = if (showButton) View.VISIBLE else View.GONE
            voidButton.setOnClickListener { onVoidRecord(item) }
        }

        private fun formatStatus(status: VisitorStatus): String {
            return status.name.replace('_', ' ')
        }

        private fun statusColor(status: VisitorStatus): Int {
            return when (status) {
                VisitorStatus.CHECKED_OUT -> R.color.status_checked_out
                VisitorStatus.AUTO_CLOSED -> R.color.status_auto_closed
                VisitorStatus.VOIDED -> R.color.status_voided
                VisitorStatus.ACTIVE -> R.color.status_active
            }
        }
    }
}
