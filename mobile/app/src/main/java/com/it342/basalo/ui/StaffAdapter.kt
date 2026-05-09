package com.it342.basalo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.it342.basalo.R
import com.it342.basalo.core.data.RoleUtils
import com.it342.basalo.core.data.SystemUser
import com.it342.basalo.core.ui.DisplayFormatter

class StaffAdapter : RecyclerView.Adapter<StaffAdapter.StaffViewHolder>() {
    private val items = mutableListOf<SystemUser>()

    fun submitList(data: List<SystemUser>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_staff_row, parent, false)
        return StaffViewHolder(view as ViewGroup)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class StaffViewHolder(view: ViewGroup) : RecyclerView.ViewHolder(view) {
        private val name = view.findViewById<TextView>(R.id.tvStaffName)
        private val email = view.findViewById<TextView>(R.id.tvStaffEmail)
        private val role = view.findViewById<TextView>(R.id.tvStaffRole)
        private val createdAt = view.findViewById<TextView>(R.id.tvStaffCreatedAt)

        fun bind(item: SystemUser) {
            name.text = item.fullName
            email.text = item.email
            role.text = if (RoleUtils.isAdmin(item.role)) {
                itemView.context.getString(R.string.admin_badge)
            } else {
                itemView.context.getString(R.string.staff_badge)
            }
            createdAt.text = itemView.context.getString(
                R.string.staff_created_at_format,
                DisplayFormatter.formatDateTime(item.createdAt)
            )
        }
    }
}
