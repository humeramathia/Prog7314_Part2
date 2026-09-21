package com.example.prog7314_part2.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.databinding.FragmentEventDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val event = FakeRepository.eventById(arguments?.getString("eventId").orEmpty()) ?: return
        val format = SimpleDateFormat("EEEE d MMMM yyyy, HH:mm", Locale.getDefault())
        binding.detailTitle.text = event.title
        binding.detailType.text = event.type.name
        binding.detailWhen.text = "Start: ${format.format(Date(event.startsAt))}"
        binding.detailLocation.text =
            if (event.location.isBlank()) "" else "Venue: ${event.location}"
        binding.detailNotes.text =
            if (event.notes.isBlank()) "" else "Notes: ${event.notes}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
