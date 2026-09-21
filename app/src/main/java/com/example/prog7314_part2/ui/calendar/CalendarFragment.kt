package com.example.prog7314_part2.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.CalendarEvent
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.CalendarEventDto
import com.example.prog7314_part2.data.remote.toLocal
import com.example.prog7314_part2.databinding.FragmentCalendarBinding
import com.example.prog7314_part2.ui.session
import com.google.android.material.tabs.TabLayout
import java.util.Calendar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: EventAdapter
    private var agendaOnly = false
    private var selectedDay: Long? = null
    private var allEvents: List<CalendarEvent> = emptyList()
    private var eventsCall: Call<List<CalendarEventDto>>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = EventAdapter { event ->
            findNavController().navigate(
                R.id.action_calendar_to_event,
                Bundle().apply { putString("eventId", event.id) }
            )
        }
        binding.eventList.layoutManager = LinearLayoutManager(requireContext())
        binding.eventList.adapter = adapter
        binding.retryButton.setOnClickListener { loadEvents() }

        binding.calendarTabs.addTab(binding.calendarTabs.newTab().setText(R.string.month))
        binding.calendarTabs.addTab(binding.calendarTabs.newTab().setText(R.string.agenda))
        binding.calendarTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                agendaOnly = tab.position == 1
                binding.monthView.isVisible = !agendaOnly
                showFiltered()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        binding.monthView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            selectedDay = cal.timeInMillis
            showFiltered()
        }
        loadEvents()
    }

    private fun loadEvents() {
        val screen = _binding ?: return
        val sportId = session().sportId
        screen.retryButton.isVisible = false
        screen.emptyEvents.isVisible = false

        if (sportId.isBlank()) {
            allEvents = emptyList()
            showFiltered()
            return
        }

        eventsCall?.cancel()
        val call = ApiClient.service.getEvents(sportId)
        eventsCall = call
        call.enqueue(object : Callback<List<CalendarEventDto>> {
            override fun onResponse(
                call: Call<List<CalendarEventDto>>,
                response: Response<List<CalendarEventDto>>
            ) {
                if (_binding !== screen) return
                if (!response.isSuccessful) {
                    showError()
                    return
                }
                allEvents = response.body().orEmpty().map { it.toLocal() }
                showFiltered()
            }

            override fun onFailure(call: Call<List<CalendarEventDto>>, error: Throwable) {
                if (_binding !== screen || call.isCanceled) return
                showError()
            }
        })
    }

    private fun showError() {
        val screen = _binding ?: return
        allEvents = emptyList()
        adapter.submit(emptyList())
        screen.emptyEvents.text = getString(R.string.error_retry)
        screen.emptyEvents.isVisible = true
        screen.retryButton.isVisible = true
    }

    private fun showFiltered() {
        val screen = _binding ?: return
        screen.retryButton.isVisible = false
        screen.emptyEvents.text = getString(R.string.empty_events)
        val items = if (agendaOnly) {
            allEvents
        } else if (selectedDay != null) {
            val dayStart = startOfDay(selectedDay!!)
            val dayEnd = dayStart + 86_400_000L
            allEvents.filter { it.startsAt in dayStart until dayEnd }
        } else {
            allEvents
        }
        adapter.submit(items)
        screen.emptyEvents.isVisible = items.isEmpty()

        val days = allEvents
            .map {
                Calendar.getInstance().apply { timeInMillis = it.startsAt }.get(Calendar.DAY_OF_MONTH)
            }
            .distinct()
            .sorted()
        screen.emptyEvents.contentDescription =
            if (days.isEmpty()) getString(R.string.empty_events)
            else getString(R.string.event_days_hint, days.joinToString(", "))
    }

    private fun startOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    override fun onDestroyView() {
        eventsCall?.cancel()
        eventsCall = null
        super.onDestroyView()
        _binding = null
    }
}
