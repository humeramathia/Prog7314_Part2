package com.example.prog7314_part2.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.databinding.FragmentRegisterBinding
import com.example.prog7314_part2.ui.session

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnRegister.setOnClickListener { register() }
        binding.goLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun register() {
        val name = binding.nameInput.text?.toString().orEmpty().trim()
        val email = binding.emailInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val confirm = binding.confirmInput.text?.toString().orEmpty()
        binding.nameLayout.error = null
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        binding.confirmLayout.error = null
        var valid = true
        if (name.isBlank()) {
            binding.nameLayout.error = getString(R.string.required_field)
            valid = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.invalid_email)
            valid = false
        }
        if (password.length < 6) {
            binding.passwordLayout.error = getString(R.string.required_field)
            valid = false
        }
        if (password != confirm) {
            binding.confirmLayout.error = getString(R.string.passwords_mismatch)
            valid = false
        }
        if (!binding.termsCheck.isChecked) {
            binding.termsCheck.error = getString(R.string.required_field)
            valid = false
        }
        if (!valid) return
        session().signIn(email, name)
        findNavController().navigate(
            R.id.action_register_to_confirm,
            Bundle().apply { putString("email", email) }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
