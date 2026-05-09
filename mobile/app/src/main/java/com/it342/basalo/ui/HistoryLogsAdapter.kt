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
import com.it342.basalo.core.data.VisitorRecord
import com.it342.basalo.core.ui.DisplayFormatter

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
            timeInText.text = context.getString(R.string.time_in_format, DisplayFormatter.formatDateTime(item.timeIn))
            timeOutText.text = context.getString(
                R.string.time_out_format,
                item.timeOut?.let { DisplayFormatter.formatDateTime(it) } ?: context.getString(R.string.still_active)
            )
            chip.text = DisplayFormatter.formatStatus(item.status)
            chip.chipBackgroundColor = ContextCompat.getColorStateList(context, statusColor(item.status))

            val showButton = isAdmin && !item.status.equals("Voided", ignoreCase = true)
            voidButton.visibility = if (showButton) View.VISIBLE else View.GONE
            voidButton.setOnClickListener { onVoidRecord(item) }
        }

        private fun statusColor(status: String): Int {
            return when (status) {
                "Checked-Out" -> R.color.status_checked_out
                "Auto-Closed" -> R.color.status_auto_closed
                "Voided" -> R.color.status_voided
                else -> R.color.status_active
            }
        }
    }
}
