package com.example.prog7314_part2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.databinding.FragmentConfirmEmailBinding

class ConfirmEmailFragment : Fragment() {

    private var _binding: FragmentConfirmEmailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConfirmEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val email = arguments?.getString("email").orEmpty()
        binding.confirmBody.text = getString(R.string.confirm_email_body, email)
        binding.btnConfirmed.setOnClickListener {
            findNavController().navigate(R.id.action_confirm_to_sport)
        }
        binding.btnResend.setOnClickListener {
            Toast.makeText(requireContext(), "Confirmation email resent", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
