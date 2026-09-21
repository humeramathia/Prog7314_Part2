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
import com.example.prog7314_part2.ui.session
import com.google.firebase.auth.FirebaseAuth

class ConfirmEmailFragment : Fragment() {

    private var _binding: FragmentConfirmEmailBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private var checking = false
    private var openedInbox = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val email = auth.currentUser?.email
            ?: arguments?.getString("email").orEmpty()

        binding.confirmBody.text = getString(R.string.confirm_email_body, email)
        binding.btnConfirmed.setOnClickListener { checkVerification(quiet = false) }
        binding.btnResend.setOnClickListener { resendEmail() }
        binding.btnOpenEmail.setOnClickListener { openEmailApp() }
    }

    override fun onResume() {
        super.onResume()
        checkVerification(quiet = true)
    }

    private fun checkVerification(quiet: Boolean) {
        val screen = binding
        if (checking) return

        val user = auth.currentUser
        if (user == null) {
            if (!quiet) returnToLogin()
            return
        }

        checking = true
        if (!quiet) setLoading(true)

        EmailVerification.refreshVerified { verified, error ->
            if (_binding !== screen) return@refreshVerified
            checking = false
            setLoading(false)

            if (error == "signed-out") {
                if (!quiet) returnToLogin()
                return@refreshVerified
            }

            if (error != null) {
                if (!quiet) {
                    showMessage(error.ifBlank { "Could not check verification. Check your connection." })
                }
                return@refreshVerified
            }

            if (!verified) {
                if (!quiet) {
                    showMessage(getString(R.string.confirm_email_not_yet))
                }
                return@refreshVerified
            }

            continueAfterVerified()
        }
    }

    private fun continueAfterVerified() {
        val refreshedUser = auth.currentUser ?: return
        val localSession = session()
        val email = refreshedUser.email.orEmpty()

        if (localSession.email != email) {
            localSession.sportId = ""
            localSession.sportName = ""
        }

        localSession.signIn(email, refreshedUser.displayName.orEmpty())

        val destination = if (localSession.sportId.isBlank()) {
            R.id.action_confirm_to_sport
        } else {
            R.id.action_confirm_to_home
        }
        findNavController().navigate(destination)
    }

    private fun resendEmail() {
        val screen = binding
        val user = auth.currentUser
        if (user == null) {
            returnToLogin()
            return
        }

        setLoading(true)
        EmailVerification.send(user) { _, message ->
            if (_binding !== screen) return@send
            setLoading(false)
            showMessage(message)
        }
    }

    private fun openEmailApp() {
        openedInbox = true
        val opened = context?.let { EmailVerification.openInbox(it) } == true
        if (!opened) {
            showMessage("Open Gmail or your mail app, then tap the Verify link.")
        }
    }

    private fun returnToLogin() {
        showMessage("Please log in again to verify your account.")
        findNavController().navigate(
            R.id.loginFragment,
            null,
            androidx.navigation.navOptions {
                popUpTo(R.id.nav_graph) { inclusive = true }
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        _binding?.apply {
            btnConfirmed.isEnabled = !loading
            btnResend.isEnabled = !loading
            btnOpenEmail.isEnabled = !loading
        }
    }

    private fun showMessage(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
