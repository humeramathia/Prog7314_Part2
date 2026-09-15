package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.databinding.FragmentAddPerformanceBinding
import com.example.prog7314_part2.ui.session

class AddPerformanceFragment : Fragment() {

    private var _binding: FragmentAddPerformanceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddPerformanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnSave.setOnClickListener {
            val score = binding.scoreInput.text?.toString()?.toDoubleOrNull()
            if (score == null) {
                binding.scoreLayout.error = getString(R.string.required_field)
                return@setOnClickListener
            }
            FakeRepository.addSession(
                sportId = session().sportId,
                score = score,
                notes = binding.notesInput.text?.toString().orEmpty()
            )
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
