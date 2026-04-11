package com.it342.basalo.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.it342.basalo.R
import com.it342.basalo.data.VisitorRecord

class ActiveLogsAdapter(
    private val onCheckOut: (VisitorRecord) -> Unit
) : RecyclerView.Adapter<ActiveLogsAdapter.ActiveLogViewHolder>() {

    private val items = mutableListOf<VisitorRecord>()

    fun submitList(data: List<VisitorRecord>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_log, parent, false)
        return ActiveLogViewHolder(view, onCheckOut)
    }

    override fun onBindViewHolder(holder: ActiveLogViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ActiveLogViewHolder(
        itemView: View,
        private val onCheckOut: (VisitorRecord) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameText: TextView = itemView.findViewById(R.id.tvVisitorName)
        private val metaText: TextView = itemView.findViewById(R.id.tvVisitorMeta)
        private val hostText: TextView = itemView.findViewById(R.id.tvHost)
        private val destinationText: TextView = itemView.findViewById(R.id.tvDestination)
        private val timeInText: TextView = itemView.findViewById(R.id.tvTimeIn)
        private val chip: Chip = itemView.findViewById(R.id.chipVisitorType)
        private val button: Button = itemView.findViewById(R.id.btnCheckOut)

        fun bind(item: VisitorRecord) {
            nameText.text = item.fullName
            metaText.text = item.contactNumber
            hostText.text = itemView.context.getString(R.string.host_format, item.hostName)
            destinationText.text = itemView.context.getString(R.string.destination_format, item.destination)
            timeInText.text = itemView.context.getString(R.string.time_in_format, item.timeIn)
            chip.text = item.visitorType
            button.setOnClickListener { onCheckOut(item) }
        }
    }
}
