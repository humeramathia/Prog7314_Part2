package com.example.prog7314_part2.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.databinding.FragmentLoginBinding
import com.example.prog7314_part2.ui.session

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnLogin.setOnClickListener { login() }
        binding.btnGoogle.setOnClickListener { loginWithGoogle() }
        binding.goRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
        binding.forgotPassword.setOnClickListener {
            Toast.makeText(requireContext(), "Reset email will use Firebase Auth", Toast.LENGTH_SHORT).show()
        }
    }

    private fun login() {
        val email = binding.emailInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.invalid_email)
            return
        }
        if (password.isBlank()) {
            binding.passwordLayout.error = getString(R.string.required_field)
            return
        }
        val session = session()
        session.signIn(email, session.displayName)
        goNext()
    }

    private fun loginWithGoogle() {
        session().signIn("google.user@sportsphere.app", "Google user")
        goNext()
    }

    private fun goNext() {
        val dest = if (session().sportId.isBlank()) {
            R.id.action_login_to_sport
        } else {
            R.id.action_login_to_home
        }
        findNavController().navigate(dest)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
