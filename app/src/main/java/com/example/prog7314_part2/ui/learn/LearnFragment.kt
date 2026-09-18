package com.example.prog7314_part2.ui.learn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.LearnGuideDto
import com.example.prog7314_part2.databinding.FragmentLearnBinding
import com.example.prog7314_part2.ui.session
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LearnFragment : Fragment() {

    private var _binding: FragmentLearnBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: GuideAdapter
    private var selectedCategory: String? = null
    private var learnCall: Call<List<LearnGuideDto>>? = null

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

        addChip(getString(R.string.all_categories), null)
        LearnSupport.CATEGORIES.forEach { category ->
            addChip(LearnSupport.categoryLabel(category), category)
        }
        binding.categoryChips.setOnCheckedStateChangeListener { group, checkedIds ->
            val chipId = checkedIds.firstOrNull()
            selectedCategory = if (chipId == null) null else group.findViewById<Chip>(chipId).tag as? String
            loadGuides()
        }
        binding.btnRetryLearn.setOnClickListener { loadGuides() }
        loadGuides()
    }

    private fun addChip(text: String, category: String?) {
        val chip = Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            isChecked = category == null
            tag = category
            chipBackgroundColor = ResourcesCompat.getColorStateList(resources, R.color.chip_background_selector, null)
            setTextColor(ResourcesCompat.getColorStateList(resources, R.color.chip_text_selector, null))
        }
        binding.categoryChips.addView(chip)
    }

    private fun loadGuides() {
        val screen = _binding ?: return
        val sportId = session().sportId
        if (sportId.isBlank()) {
            adapter.submit(emptyList())
            screen.emptyLearn.isVisible = true
            screen.learnErrorGroup.isVisible = false
            screen.learnLoading.isVisible = false
            return
        }

        learnCall?.cancel()
        screen.learnLoading.isVisible = true
        screen.learnErrorGroup.isVisible = false
        screen.emptyLearn.isVisible = false

        val call = ApiClient.service.getLearn(sportId, selectedCategory)
        learnCall = call

        call.enqueue(object : Callback<List<LearnGuideDto>> {
            override fun onResponse(call: Call<List<LearnGuideDto>>, response: Response<List<LearnGuideDto>>) {
                if (_binding !== screen) return
                screen.learnLoading.isVisible = false

                if (!response.isSuccessful) {
                    showError(screen)
                    return
                }

                val guides = LearnSupport.filter(response.body().orEmpty(), sportId, selectedCategory)
                adapter.submit(guides)
                screen.learnErrorGroup.isVisible = false
                screen.emptyLearn.isVisible = LearnSupport.shouldShowEmpty(guides, isLoading = false, hasError = false)
            }

            override fun onFailure(call: Call<List<LearnGuideDto>>, error: Throwable) {
                if (_binding !== screen || call.isCanceled) return
                screen.learnLoading.isVisible = false
                showError(screen)
            }
        })
    }

    private fun showError(screen: FragmentLearnBinding) {
        adapter.submit(emptyList())
        screen.emptyLearn.isVisible = false
        screen.learnErrorGroup.isVisible = true
    }

    override fun onDestroyView() {
        learnCall?.cancel()
        learnCall = null
        super.onDestroyView()
        _binding = null
    }
}
