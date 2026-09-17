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

        binding.confirmBody.text =
            getString(R.string.confirm_email_body, email)

        binding.btnConfirmed.setOnClickListener { checkVerification() }
        binding.btnResend.setOnClickListener { resendEmail() }
    }

    private fun checkVerification() {
        val screen = binding
        val user = auth.currentUser

        if (user == null) {
            returnToLogin()
            return
        }

        setLoading(true)

        user.reload().addOnCompleteListener reloadComplete@{ result ->
            if (_binding !== screen) return@reloadComplete

            if (!result.isSuccessful) {
                setLoading(false)
                showMessage("Could not check verification. Check your connection.")
                return@reloadComplete
            }

            val refreshedUser = auth.currentUser

            if (refreshedUser == null) {
                setLoading(false)
                returnToLogin()
                return@reloadComplete
            }

            if (!refreshedUser.isEmailVerified) {
                setLoading(false)
                showMessage(
                    "Your email is not verified yet. Open the link in your email first."
                )
                return@reloadComplete
            }

            refreshedUser.getIdToken(true)
                .addOnCompleteListener tokenComplete@{ tokenResult ->
                    if (_binding !== screen) return@tokenComplete

                    setLoading(false)

                    if (!tokenResult.isSuccessful) {
                        showMessage("Could not refresh your session. Please try again.")
                        return@tokenComplete
                    }

                    val localSession = session()
                    val email = refreshedUser.email.orEmpty()

                    if (localSession.email != email) {
                        localSession.sportId = ""
                        localSession.sportName = ""
                    }

                    localSession.signIn(
                        email,
                        refreshedUser.displayName.orEmpty()
                    )

                    findNavController().navigate(R.id.action_confirm_to_sport)
                }
        }
    }

    private fun resendEmail() {
        val screen = binding
        val user = auth.currentUser

        if (user == null) {
            returnToLogin()
            return
        }

        setLoading(true)

        user.sendEmailVerification()
            .addOnCompleteListener resendComplete@{ result ->
                if (_binding !== screen) return@resendComplete

                setLoading(false)

                if (result.isSuccessful) {
                    showMessage("Verification email sent. Check your inbox and spam.")
                } else {
                    showMessage(
                        "Could not resend the email. Wait a little and try again."
                    )
                }
            }
    }

    private fun returnToLogin() {
        showMessage("Please log in again to verify your account.")

        findNavController().navigate(R.id.loginFragment, null,
            androidx.navigation.navOptions {
                popUpTo(R.id.nav_graph) { inclusive = true }
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        _binding?.apply {
            btnConfirmed.isEnabled = !loading
            btnResend.isEnabled = !loading
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