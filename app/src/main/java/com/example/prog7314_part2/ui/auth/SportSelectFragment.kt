package com.example.prog7314_part2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.data.Sport
import com.example.prog7314_part2.databinding.FragmentSportSelectBinding
import com.example.prog7314_part2.ui.session

class SportSelectFragment : Fragment() {

    private var _binding: FragmentSportSelectBinding? = null
    private val binding get() = _binding!!
    private var selected: Sport? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSportSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = SportAdapter(FakeRepository.sports, session().sportId) { sport ->
            selected = sport
            binding.btnContinue.isEnabled = true
        }
        binding.sportList.layoutManager = LinearLayoutManager(requireContext())
        binding.sportList.adapter = adapter
        selected = FakeRepository.sports.find { it.id == session().sportId }
        binding.btnContinue.isEnabled = selected != null
        binding.btnContinue.setOnClickListener {
            val sport = selected ?: return@setOnClickListener
            session().sportId = sport.id
            session().sportName = sport.name
            findNavController().navigate(R.id.action_sport_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
