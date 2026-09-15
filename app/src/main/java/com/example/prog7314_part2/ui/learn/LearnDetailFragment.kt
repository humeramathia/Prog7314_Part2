package com.example.prog7314_part2.ui.learn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.data.label
import com.example.prog7314_part2.databinding.FragmentLearnDetailBinding

class LearnDetailFragment : Fragment() {

    private var _binding: FragmentLearnDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLearnDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val guide = FakeRepository.guideById(arguments?.getString("guideId").orEmpty()) ?: return
        binding.guideTitle.text = guide.title
        binding.guideCategory.text = guide.category.label().replaceFirstChar { it.uppercase() }
        binding.guideBody.text = guide.body
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
