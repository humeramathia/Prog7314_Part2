package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.data.MetricField
import com.example.prog7314_part2.data.SportMetrics
import com.example.prog7314_part2.databinding.FragmentAddPerformanceBinding
import com.example.prog7314_part2.ui.session
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AddPerformanceFragment : Fragment() {

    private var _binding: FragmentAddPerformanceBinding? = null
    private val binding get() = _binding!!
    private val inputs = mutableMapOf<String, Pair<TextInputLayout, TextInputEditText>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddPerformanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        SportMetrics.fields(session().sportId).forEach { field -> addMetricInput(field) }
        binding.btnSave.setOnClickListener { save() }
    }

    private fun addMetricInput(field: MetricField) {
        val layout = TextInputLayout(requireContext()).apply {
            hint = field.label
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (inputs.isNotEmpty()) {
                    topMargin = (16 * resources.displayMetrics.density).toInt()
                }
            }
        }
        val input = TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(input)
        binding.metricFields.addView(layout)
        inputs[field.key] = layout to input
    }

    private fun save() {
        val metrics = mutableMapOf<String, Double>()
        var valid = true
        inputs.forEach { (key, pair) ->
            val (layout, input) = pair
            val value = input.text?.toString()?.toDoubleOrNull()
            if (value == null) {
                layout.error = getString(R.string.required_field)
                valid = false
            } else {
                layout.error = null
                metrics[key] = value
            }
        }
        if (!valid) return
        FakeRepository.addSession(
            sportId = session().sportId,
            metrics = metrics,
            notes = binding.notesInput.text?.toString().orEmpty()
        )
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
