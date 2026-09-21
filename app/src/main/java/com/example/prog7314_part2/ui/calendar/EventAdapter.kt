package com.example.prog7314_part2.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part2.data.CalendarEvent
import com.example.prog7314_part2.databinding.ItemEventBinding
import com.example.prog7314_part2.databinding.ItemEventDateHeaderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter(
    private val onClick: (CalendarEvent) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<CalendarItem>()

    private val eventFormat =
        SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())

    private val dateFormat =
        SimpleDateFormat("EEEE d MMMM", Locale.getDefault())

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_EVENT = 1
    }

    fun submit(events: List<CalendarEvent>) {
        items.clear()

        var lastDate = ""

        for (event in events.sortedBy { it.startsAt }) {
            val date = dateFormat.format(Date(event.startsAt))

            if (date != lastDate) {
                items.add(CalendarItem.DateHeader(date))
                lastDate = date
            }

            items.add(CalendarItem.EventItem(event))
        }

        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CalendarItem.DateHeader -> TYPE_DATE_HEADER
            is CalendarItem.EventItem -> TYPE_EVENT
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == TYPE_DATE_HEADER) {
            DateHeaderHolder(
                ItemEventDateHeaderBinding.inflate(inflater, parent, false)
            )
        } else {
            EventHolder(
                ItemEventBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (val item = items[position]) {

            is CalendarItem.DateHeader -> {
                (holder as DateHeaderHolder)
                    .binding.eventDateHeader.text = item.date
            }

            is CalendarItem.EventItem -> {
                val event = item.event
                val eventHolder = holder as EventHolder

                eventHolder.binding.eventTitle.text = event.title
                eventHolder.binding.eventType.text =
                    event.type.name.replace("_", " ")
                eventHolder.binding.eventMeta.text =
                    eventFormat.format(Date(event.startsAt))
                eventHolder.binding.eventLocation.text =
                    event.location

                eventHolder.itemView.setOnClickListener {
                    onClick(event)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class DateHeaderHolder(
        val binding: ItemEventDateHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root)

    class EventHolder(
        val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root)

    sealed class CalendarItem {
        data class DateHeader(val date: String) : CalendarItem()
        data class EventItem(val event: CalendarEvent) : CalendarItem()
    }
}