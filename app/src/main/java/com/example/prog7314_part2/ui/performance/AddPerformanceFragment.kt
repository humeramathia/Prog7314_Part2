package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.data.FakeRepository
import com.example.prog7314_part2.data.PerformanceMetric
import com.example.prog7314_part2.data.PerformanceMetrics
import com.example.prog7314_part2.databinding.FragmentAddPerformanceBinding
import com.example.prog7314_part2.ui.session
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AddPerformanceFragment : Fragment() {

    private var _binding: FragmentAddPerformanceBinding? = null
    private val binding get() = _binding!!

    private data class MetricField(
        val definition: PerformanceMetric,
        val layout: TextInputLayout,
        val input: TextInputEditText
    )

    private val fields = mutableListOf<MetricField>()
    private var sportId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPerformanceBinding.inflate(
            inflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sportId = session().sportId
        val definitions = PerformanceMetrics.forSport(sportId)

        binding.sportTitle.text = session().sportName
        fields.clear()

        definitions.forEachIndexed { index, definition ->
            val layout = TextInputLayout(requireContext()).apply {
                hint = definition.label
                isErrorEnabled = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (12 * resources.displayMetrics.density).toInt()
                }
            }

            val input = TextInputEditText(layout.context).apply {
                // Stable IDs allow Android to restore entered values.
                id = METRIC_INPUT_ID_BASE + index
                inputType = if (definition.wholeNumber) {
                    InputType.TYPE_CLASS_NUMBER
                } else {
                    InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_DECIMAL
                }
                setSingleLine(true)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            layout.addView(input)
            binding.metricsContainer.addView(layout)
            fields += MetricField(definition, layout, input)
        }

        binding.btnSave.isEnabled = fields.isNotEmpty()

        if (fields.isEmpty()) {
            binding.sportTitle.text =
                "Select a supported sport before recording a session."
        }

        binding.btnSave.setOnClickListener { save() }
    }

    private fun save() {
        if (fields.isEmpty()) return

        val values = linkedMapOf<String, Double>()
        var firstInvalid: TextInputEditText? = null

        fields.forEach { field ->
            val text = field.input.text?.toString().orEmpty().trim()
            val value = text.replace(',', '.').toDoubleOrNull()
            val definition = field.definition

            val error = when {
                text.isBlank() ->
                    "This field is required."

                value == null || !value.isFinite() ->
                    "Enter a valid number."

                value < 0.0 ->
                    "Enter zero or a positive number."

                !definition.allowZero && value == 0.0 ->
                    "Enter a number greater than zero."

                definition.wholeNumber && value % 1.0 != 0.0 ->
                    "Enter a whole number."

                else -> null
            }

            field.layout.error = error

            if (error != null) {
                if (firstInvalid == null) firstInvalid = field.input
            } else if (value != null) {
                values[definition.key] = value
            }
        }

        firstInvalid?.let {
            it.requestFocus()
            return
        }

        binding.btnSave.isEnabled = false

        // Keep the first metric as the temporary headline score.
        val headlineKey = fields.first().definition.key

        FakeRepository.addSession(
            sportId = sportId,
            score = values.getValue(headlineKey),
            notes = binding.notesInput.text?.toString().orEmpty().trim(),
            metrics = values
        )

        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        fields.clear()
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val METRIC_INPUT_ID_BASE = 0x00F10000
    }
}