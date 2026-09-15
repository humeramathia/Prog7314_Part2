package com.example.prog7314_part2.ui.learn

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
import com.example.prog7314_part2.data.LearnCategory
import com.example.prog7314_part2.data.label
import com.example.prog7314_part2.databinding.FragmentLearnBinding
import com.example.prog7314_part2.ui.session
import com.google.android.material.chip.Chip

class LearnFragment : Fragment() {

    private var _binding: FragmentLearnBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: GuideAdapter
    private var selectedCategory: LearnCategory? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLearnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = GuideAdapter { guide ->
            findNavController().navigate(
                R.id.action_learn_to_detail,
                Bundle().apply { putString("guideId", guide.id) }
            )
        }
        binding.guideList.layoutManager = LinearLayoutManager(requireContext())
        binding.guideList.adapter = adapter
        binding.categoryChips.isSingleSelection = true

        addChip("All", null)
        LearnCategory.entries.forEach { category ->
            addChip(category.label().replaceFirstChar { it.uppercase() }, category)
        }
        binding.categoryChips.setOnCheckedStateChangeListener { group, checkedIds ->
            val chipId = checkedIds.firstOrNull()
            selectedCategory = if (chipId == null) null else group.findViewById<Chip>(chipId).tag as? LearnCategory
            render()
        }
        render()
    }

    private fun addChip(text: String, category: LearnCategory?) {
        val chip = Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            isChecked = category == null
            tag = category
        }
        binding.categoryChips.addView(chip)
    }

    private fun render() {
        val guides = FakeRepository.guidesFor(session().sportId, selectedCategory)
        adapter.submit(guides)
        binding.emptyLearn.isVisible = guides.isEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
