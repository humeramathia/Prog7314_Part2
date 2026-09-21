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
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.databinding.FragmentCalendarBinding
import com.example.prog7314_part2.ui.session
import com.google.android.material.tabs.TabLayout
import java.util.Calendar

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: EventAdapter
    private var agendaOnly = false
    private var selectedDay: Long? = null

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

        binding.calendarTabs.addTab(binding.calendarTabs.newTab().setText(R.string.month))
        binding.calendarTabs.addTab(binding.calendarTabs.newTab().setText(R.string.agenda))
        binding.calendarTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                agendaOnly = tab.position == 1
                binding.monthView.isVisible = !agendaOnly
                render()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        binding.monthView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            selectedDay = cal.timeInMillis
            render()
        }
        render()
    }

    private fun render() {
        val sportId = session().sportId
        val all = FakeRepository.eventsForSport(sportId)

        val items = if (agendaOnly) {
            all
        } else if (selectedDay != null) {
            val dayStart = startOfDay(selectedDay!!)
            val dayEnd = dayStart + 86_400_000L
            all.filter { it.startsAt in dayStart until dayEnd }
        } else {
            all
        }

        adapter.submit(items)
        binding.emptyEvents.isVisible = items.isEmpty()
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
        super.onDestroyView()
        _binding = null
    }
}
