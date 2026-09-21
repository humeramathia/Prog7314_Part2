package com.example.prog7314_part2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_part2.R
import com.example.prog7314_part2.databinding.FragmentWelcomeBinding

/**
 * The first screen a brand-new user sees.
 *
 * It's intentionally trivial — just two entry points into the auth flow:
 * "Get started" (register a new account) and "Log in" (returning user).
 * All navigation targets are declared in `nav_graph.xml`.
 */
class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnGetStarted.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_register)
        }
        binding.btnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
