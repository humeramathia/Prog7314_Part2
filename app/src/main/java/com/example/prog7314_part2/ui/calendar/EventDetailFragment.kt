package com.example.prog7314_part2.ui.calendar

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.CalendarEventDto
import com.example.prog7314_part2.data.remote.toLocal
import com.example.prog7314_part2.databinding.FragmentEventDetailBinding
import com.example.prog7314_part2.ui.auth.ProfileSync
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Read-only detail screen for a single [CalendarEventDto].
 *
 * Loads the event by id passed as a navigation argument, renders its
 * fields, and offers an "Add to phone calendar" action that fires a
 * standard [CalendarContract.Events] insert intent.
 */
class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private var eventCall: Call<CalendarEventDto>? = null
    private var deleteCall: Call<Void>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Hidden until the event loads — the button needs an event to
        // pre-populate the calendar-insert intent.
        binding.btnAddToCalendar.isVisible = false
        binding.btnEditEvent.isVisible = false
        binding.btnDeleteEvent.isVisible = false
        loadEvent()
    }

    private fun loadEvent() {
        val screen = binding
        val eventId = arguments?.getString("eventId").orEmpty()
        if (eventId.isBlank()) return

        eventCall?.cancel()
        val call = ApiClient.service.getEvent(eventId)
        eventCall = call
        call.enqueue(object : Callback<CalendarEventDto> {
            override fun onResponse(call: Call<CalendarEventDto>, response: Response<CalendarEventDto>) {
                if (_binding !== screen) return
                val dto = response.body()
                if (!response.isSuccessful || dto == null) {
                    Toast.makeText(requireContext(), getString(R.string.error_retry), Toast.LENGTH_LONG).show()
                    return
                }
                bind(dto)
            }

            override fun onFailure(call: Call<CalendarEventDto>, t: Throwable) {
                if (_binding !== screen || call.isCanceled) return
                Toast.makeText(requireContext(), getString(R.string.error_retry), Toast.LENGTH_LONG).show()
            }
        })
    }

    /**
     * Renders the event and wires up the "Add to phone calendar"
     * button. Nullable fields (description, endsAt) collapse to `GONE`
     * so the layout does not leave awkward whitespace behind.
     */
    private fun bind(dto: CalendarEventDto) {
        val event = dto.toLocal()
        val format = SimpleDateFormat("EEEE d MMMM yyyy, HH:mm", Locale.getDefault())

        binding.detailTitle.text = event.title
        binding.detailType.text = event.type.name.replace("_", " ")
        binding.detailWhen.text = getString(R.string.event_start, format.format(Date(event.startsAt)))

        if (event.endsAt != null) {
            binding.detailEnd.isVisible = true
            binding.detailEnd.text = getString(R.string.event_end, format.format(Date(event.endsAt)))
        } else {
            binding.detailEnd.isVisible = false
        }

        binding.detailLocation.text =
            if (event.location.isBlank()) "" else getString(R.string.event_venue, event.location)

        if (event.description.isBlank()) {
            binding.detailDescription.isVisible = false
        } else {
            binding.detailDescription.isVisible = true
            binding.detailDescription.text = event.description
        }
        binding.detailNotes.text =
            if (event.notes.isBlank()) "" else getString(R.string.event_notes, event.notes)

        val canManage = ProfileSync.canManageEvent(dto.createdBy.orEmpty())
        binding.btnEditEvent.isVisible = canManage
        binding.btnDeleteEvent.isVisible = canManage
        binding.btnEditEvent.setOnClickListener {
            findNavController().navigate(
                R.id.action_event_to_edit,
                Bundle().apply { putString("eventId", dto.id) }
            )
        }
        binding.btnDeleteEvent.setOnClickListener { confirmDelete(dto.id) }

        binding.btnAddToCalendar.isVisible = true
        binding.btnAddToCalendar.setOnClickListener {
            val insert = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, event.title)
                putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
                putExtra(
                    CalendarContract.Events.DESCRIPTION,
                    event.description.ifBlank { event.notes }
                )
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startsAt)
                // If the event has no explicit end time, default the
                // insert to a one-hour slot so the calendar app is
                // happy and the user still gets a sensible reminder.
                putExtra(
                    CalendarContract.EXTRA_EVENT_END_TIME,
                    event.endsAt ?: (event.startsAt + 3_600_000L)
                )
            }
            try {
                startActivity(insert)
            } catch (error: Exception) {
                Toast.makeText(requireContext(), "No calendar app available.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(eventId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_event)
            .setMessage(R.string.delete_event_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> deleteEvent(eventId) }
            .show()
    }

    private fun deleteEvent(eventId: String) {
        val call = ApiClient.service.deleteEvent(eventId)
        deleteCall = call
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (_binding == null) return
                if (response.isSuccessful) {
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_delete_event), Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Void>, error: Throwable) {
                if (_binding == null || call.isCanceled) return
                Toast.makeText(requireContext(), ProfileSync.UNAVAILABLE, Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onDestroyView() {
        eventCall?.cancel()
        deleteCall?.cancel()
        eventCall = null
        deleteCall = null
        super.onDestroyView()
        _binding = null
    }
}
