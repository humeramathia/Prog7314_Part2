package com.example.prog7314_part2.ui.performance

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.CreatePerformanceRequest
import com.example.prog7314_part2.data.remote.MetricDto
import com.example.prog7314_part2.data.remote.PerformanceSessionDto
import com.example.prog7314_part2.data.remote.SportDto
import com.example.prog7314_part2.databinding.FragmentAddPerformanceBinding
import com.example.prog7314_part2.ui.session
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddPerformanceFragment : Fragment() {

    private var _binding: FragmentAddPerformanceBinding? = null
    private val binding get() = _binding!!

    private val inputs =
        linkedMapOf<String, Pair<TextInputLayout, TextInputEditText>>()

    private var sportCall: Call<SportDto>? = null
    private var saveCall: Call<PerformanceSessionDto>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPerformanceBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        binding.btnSave.isEnabled = false
        binding.btnSave.setOnClickListener { save() }

        loadMetricFields()
    }

    private fun loadMetricFields() {
        val screen = binding
        val sportId = session().sportId

        if (sportId.isBlank()) {
            showMessage("Select a sport before recording a session.")
            return
        }

        val call = ApiClient.service.getSport(sportId)
        sportCall = call

        call.enqueue(object : Callback<SportDto> {

            override fun onResponse(
                call: Call<SportDto>,
                response: Response<SportDto>
            ) {
                if (_binding !== screen) return

                val metrics = response.body()?.metrics.orEmpty()

                if (!response.isSuccessful || metrics.isEmpty()) {
                    showMessage(
                        "Could not load the performance fields."
                    )
                    return
                }

                screen.metricFields.removeAllViews()
                inputs.clear()

                metrics.forEach { metric ->
                    addMetricInput(metric)
                }

                screen.btnSave.isEnabled = true
            }

            override fun onFailure(
                call: Call<SportDto>,
                error: Throwable
            ) {
                if (_binding !== screen || call.isCanceled) return

                showMessage(
                    "Could not connect to the API. " +
                            "Check that it is running."
                )
            }
        })
    }

    private fun addMetricInput(metric: MetricDto) {
        val layout = TextInputLayout(requireContext()).apply {
            hint = metric.label
            isErrorEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (inputs.isNotEmpty()) {
                    topMargin =
                        (16 * resources.displayMetrics.density).toInt()
                }
            }
        }

        val input = TextInputEditText(layout.context).apply {
            inputType =
                InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
        }

        layout.addView(input)
        binding.metricFields.addView(layout)
        inputs[metric.key] = layout to input
    }

    private fun save() {
        val screen = binding
        val values = linkedMapOf<String, Double>()
        var firstInvalid: TextInputEditText? = null

        inputs.forEach { (key, pair) ->
            val (layout, input) = pair
            val text = input.text?.toString().orEmpty().trim()
            val value = text.replace(',', '.').toDoubleOrNull()

            val error = when {
                text.isBlank() ->
                    getString(R.string.required_field)

                value == null || !value.isFinite() ->
                    "Enter a valid number."

                value < 0.0 ->
                    "Enter zero or a positive number."

                else -> null
            }

            layout.error = error

            if (error != null) {
                if (firstInvalid == null) {
                    firstInvalid = input
                }
            } else if (value != null) {
                values[key] = value
            }
        }

        firstInvalid?.let {
            it.requestFocus()
            return
        }

        if (values.isEmpty()) return

        setLoading(true)

        val request = CreatePerformanceRequest(
            sportId = session().sportId,
            recordedAt = System.currentTimeMillis(),
            notes = screen.notesInput.text
                ?.toString()
                .orEmpty()
                .trim(),
            metrics = values
        )

        val call = ApiClient.service.createPerformance(request)
        saveCall = call

        call.enqueue(object : Callback<PerformanceSessionDto> {

            override fun onResponse(
                call: Call<PerformanceSessionDto>,
                response: Response<PerformanceSessionDto>
            ) {
                if (_binding !== screen) return

                if (!response.isSuccessful || response.body() == null) {
                    setLoading(false)
                    showMessage("Could not save the session.")
                    return
                }

                setLoading(false)
                findNavController().popBackStack()
            }

            override fun onFailure(
                call: Call<PerformanceSessionDto>,
                error: Throwable
            ) {
                if (_binding !== screen || call.isCanceled) return

                setLoading(false)
                showMessage(
                    "Could not connect to the API. " +
                            "The session was not saved."
                )
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        _binding?.apply {
            btnSave.isEnabled = !loading && inputs.isNotEmpty()
            notesInput.isEnabled = !loading
            inputs.values.forEach { (_, input) ->
                input.isEnabled = !loading
            }
        }
    }

    private fun showMessage(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        sportCall?.cancel()
        saveCall?.cancel()
        sportCall = null
        saveCall = null
        inputs.clear()
        _binding = null
        super.onDestroyView()
    }
}