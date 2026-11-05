package com.example.prog7314_part2.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.EventType
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.CalendarEventDto
import com.example.prog7314_part2.data.remote.UpsertEventRequest
import com.example.prog7314_part2.databinding.FragmentEditEventBinding
import com.example.prog7314_part2.ui.auth.ProfileSync
import com.example.prog7314_part2.ui.session
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditEventFragment : Fragment() {

    private var _binding: FragmentEditEventBinding? = null
    private val binding get() = _binding!!

    private val types = listOf(
        EventType.PRACTICE,
        EventType.SOCIAL_EVENT,
        EventType.ANNOUNCEMENT
    )
    private var startsAt = System.currentTimeMillis()
    private var loadCall: Call<CalendarEventDto>? = null
    private var saveCall: Call<CalendarEventDto>? = null

    private val eventId: String
        get() = arguments?.getString("eventId").orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.typeSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            types.map { it.name.replace("_", " ") }
        )
        binding.btnPickDate.setOnClickListener { pickDate() }
        binding.btnSaveEvent.setOnClickListener { save() }
        renderDate()

        if (eventId.isNotBlank()) {
            binding.editEventTitle.text = getString(R.string.edit_event)
            loadEvent()
        }
    }

    private fun pickDate() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.pick_date))
            .setSelection(startsAt)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            startsAt = millis
            renderDate()
        }
        picker.show(parentFragmentManager, "event_date")
    }

    private fun renderDate() {
        binding.btnPickDate.text = SimpleDateFormat(
            "EEE d MMM yyyy",
            Locale.getDefault()
        ).format(Date(startsAt))
    }

    private fun loadEvent() {
        val screen = binding
        val call = ApiClient.service.getEvent(eventId)
        loadCall = call
        call.enqueue(object : Callback<CalendarEventDto> {
            override fun onResponse(call: Call<CalendarEventDto>, response: Response<CalendarEventDto>) {
                if (_binding !== screen) return
                val dto = response.body()
                if (!response.isSuccessful || dto == null) {
                    showMessage(getString(R.string.error_retry))
                    return
                }
                screen.titleInput.setText(dto.title)
                screen.locationInput.setText(dto.location.orEmpty())
                screen.descriptionInput.setText(dto.description.orEmpty())
                screen.notesInput.setText(dto.notes.orEmpty())
                startsAt = dto.startsAt
                renderDate()
                val index = types.indexOfFirst { it.name == dto.type }
                if (index >= 0) screen.typeSpinner.setSelection(index)
            }

            override fun onFailure(call: Call<CalendarEventDto>, error: Throwable) {
                if (_binding !== screen || call.isCanceled) return
                showMessage(ProfileSync.UNAVAILABLE)
            }
        })
    }

    private fun save() {
        val screen = binding
        val title = screen.titleInput.text?.toString().orEmpty().trim()
        screen.titleLayout.error = null
        if (title.isBlank()) {
            screen.titleLayout.error = getString(R.string.required_field)
            return
        }

        val type = types[screen.typeSpinner.selectedItemPosition].name
        val request = UpsertEventRequest(
            sportId = session().sportId,
            title = title,
            type = type,
            startsAt = startsAt,
            location = screen.locationInput.text?.toString().orEmpty().trim(),
            description = screen.descriptionInput.text?.toString().orEmpty().trim(),
            notes = screen.notesInput.text?.toString().orEmpty().trim()
        )

        setLoading(true)
        val call = if (eventId.isBlank()) {
            ApiClient.service.createEvent(request)
        } else {
            ApiClient.service.updateEvent(eventId, request)
        }
        saveCall = call
        call.enqueue(object : Callback<CalendarEventDto> {
            override fun onResponse(call: Call<CalendarEventDto>, response: Response<CalendarEventDto>) {
                if (_binding !== screen) return
                setLoading(false)
                if (!response.isSuccessful || response.body() == null) {
                    showMessage(getString(R.string.error_save_event))
                    return
                }
                findNavController().popBackStack()
            }

            override fun onFailure(call: Call<CalendarEventDto>, error: Throwable) {
                if (_binding !== screen || call.isCanceled) return
                setLoading(false)
                showMessage(ProfileSync.UNAVAILABLE)
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        _binding?.btnSaveEvent?.isEnabled = !loading
    }

    private fun showMessage(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    override fun onDestroyView() {
        loadCall?.cancel()
        saveCall?.cancel()
        loadCall = null
        saveCall = null
        _binding = null
        super.onDestroyView()
    }
}
