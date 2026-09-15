package com.example.prog7314_part2.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part2.data.CalendarEvent
import com.example.prog7314_part2.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter(
    private val onClick: (CalendarEvent) -> Unit
) : RecyclerView.Adapter<EventAdapter.Holder>() {

    private val items = mutableListOf<CalendarEvent>()
    private val format = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())

    fun submit(events: List<CalendarEvent>) {
        items.clear()
        items.addAll(events)
        notifyDataSetChanged()
    }

    inner class Holder(val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemEventBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val event = items[position]
        holder.binding.eventTitle.text = event.title
        holder.binding.eventMeta.text = "${event.type.name.lowercase()}  ·  ${format.format(Date(event.startsAt))}"
        holder.itemView.setOnClickListener { onClick(event) }
    }

    override fun getItemCount(): Int = items.size
}
