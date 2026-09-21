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
import com.example.prog7314_part2.databinding.FragmentRegisterBinding
import com.example.prog7314_part2.ui.session
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * Collects the fields required to create a new Firebase email/password
 * account, then triggers a verification email and hands off to
 * [ConfirmEmailFragment].
 *
 * All input validation is done client-side before we hit Firebase so we
 * do not burn API calls on obviously-broken forms.
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnRegister.setOnClickListener { register() }
        binding.goLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    /**
     * Validates → creates → sets display name → sends verification email →
     * navigates to [ConfirmEmailFragment]. Any failure surfaces inline
     * on the offending field or as a toast.
     */
    private fun register() {
        val screen = binding
        val name = screen.nameInput.text?.toString().orEmpty().trim()
        val email = screen.emailInput.text?.toString().orEmpty().trim()
        val password = screen.passwordInput.text?.toString().orEmpty()
        val confirm = screen.confirmInput.text?.toString().orEmpty()

        // Reset all error states before revalidating.
        screen.nameLayout.error = null
        screen.emailLayout.error = null
        screen.passwordLayout.error = null
        screen.confirmLayout.error = null
        screen.termsCheck.error = null

        var valid = true

        if (name.isBlank()) {
            screen.nameLayout.error = "Enter your display name."
            valid = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            screen.emailLayout.error = "Enter a valid email address."
            valid = false
        }
        if (password.length < 6) {
            screen.passwordLayout.error = "Use at least 6 characters."
            valid = false
        }
        if (confirm.isBlank()) {
            screen.confirmLayout.error = "Confirm your password."
            valid = false
        } else if (password != confirm) {
            screen.confirmLayout.error = "Passwords do not match."
            valid = false
        }
        if (!screen.termsCheck.isChecked) {
            screen.termsCheck.error = "Accept the terms and conditions."
            valid = false
        }
        if (!valid) return

        // Wipe any leftover session state from a previous account so
        // one user's sport/preferences never leaks into the next signup.
        session().signOut()
        auth.signOut()
        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener createComplete@{ result ->
                if (_binding !== screen) return@createComplete

                if (!result.isSuccessful) {
                    setLoading(false)
                    // Show the Firebase message inline on the email field
                    // (most failures are "email already in use").
                    screen.emailLayout.error =
                        result.exception?.localizedMessage
                            ?: "Registration failed. Please try again."
                    return@createComplete
                }

                val user = result.result?.user
                if (user == null) {
                    setLoading(false)
                    showMessage("Account response unavailable. Try logging in.")
                    return@createComplete
                }

                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                user.updateProfile(profile)
                    .addOnCompleteListener profileComplete@{ profileResult ->
                        if (_binding !== screen) return@profileComplete

                        if (!profileResult.isSuccessful) {
                            // The account is still valid — carry on but
                            // warn about the missing display name.
                            showMessage(
                                "Account created, but your display name could not be saved."
                            )
                        }

                        // Fire off the verification email. Regardless of
                        // the send result we move the user to the confirm
                        // screen so they can Resend from there.
                        EmailVerification.send(user) { _, message ->
                            if (_binding !== screen) return@send
                            setLoading(false)
                            showMessage(message)
                            findNavController().navigate(
                                R.id.action_register_to_confirm,
                                Bundle().apply { putString("email", email) }
                            )
                        }
                    }
            }
    }

    private fun setLoading(loading: Boolean) {
        _binding?.apply {
            btnRegister.isEnabled = !loading
            goLogin.isEnabled = !loading
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
