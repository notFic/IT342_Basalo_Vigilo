package com.it342.basalo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.it342.basalo.R
import com.it342.basalo.core.data.LocationRecord

class LocationAdapter : RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {
    private val items = mutableListOf<LocationRecord>()

    fun submitList(data: List<LocationRecord>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_location_row, parent, false)
        return LocationViewHolder(view as ViewGroup)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class LocationViewHolder(view: ViewGroup) : RecyclerView.ViewHolder(view) {
        private val primary = view.findViewById<TextView>(R.id.tvLocationPrimary)
        private val secondary = view.findViewById<TextView>(R.id.tvLocationSecondary)
        private val floor = view.findViewById<TextView>(R.id.tvLocationFloor)

        fun bind(item: LocationRecord) {
            primary.text = item.areaName
            secondary.text = itemView.context.getString(R.string.location_room_format, item.roomNumber)
            floor.text = itemView.context.getString(R.string.location_floor_format, item.floorLevel)
        }
    }
}
