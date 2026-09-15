package com.example.prog7314_part2.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.databinding.FragmentHomeBinding
import com.example.prog7314_part2.ui.session
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val session = session()
        val name = session.displayName.ifBlank { "athlete" }
        binding.greeting.text = "Hi, $name"
        binding.sportChip.text = session.sportName.ifBlank { getString(R.string.choose_sport) }

        val next = FakeRepository.nextEvent(session.sportId)
        if (next == null) {
            binding.nextPracticeTitle.text = getString(R.string.empty_events)
            binding.nextPracticeWhen.text = ""
        } else {
            binding.nextPracticeTitle.text = next.title
            val format = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
            binding.nextPracticeWhen.text = format.format(Date(next.startsAt))
        }

        val latest = FakeRepository.latestSession(session.sportId)
        binding.latestScoreValue.text = latest?.let { "${it.score.toInt()}  ·  ${it.notes}" }
            ?: getString(R.string.empty_history)

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
        binding.nextPracticeCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_calendar)
        }
        binding.latestScoreCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_performance)
        }
        binding.btnLearn.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_learn)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
