package com.example.prog7314_part2.ui.auth

import android.os.Bundle
import android.os.SystemClock
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

/**
 * Post-registration / pre-login screen shown when the current Firebase
 * user has not yet clicked the verification link in their inbox.
 *
 * The screen provides three actions:
 *  - **I have confirmed** — reloads the Firebase user and, if verified,
 *    hydrates the local session and continues into the app.
 *  - **Resend email** — throttled to one send per [RESEND_COOLDOWN_MS];
 *    delegates the actual send to [EmailVerification.send].
 *  - **Open email app** — best-effort intent that launches the default
 *    mail client.
 *
 * On resume we also silently poll for verification so a user who taps
 * the link in Chrome and returns via the recents switcher is moved
 * forward automatically.
 */
class ConfirmEmailFragment : Fragment() {

    private var _binding: FragmentConfirmEmailBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    /** Prevents concurrent Firebase user-reload calls. */
    private var checking = false

    /** Tracks whether the user has left for their inbox at least once. */
    private var openedInbox = false

    /** Elapsed-realtime timestamp of the last resend attempt. */
    private var lastResendAt = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Prefer Firebase's live email over the nav argument in case the
        // user got here from Login (where only the argument is populated).
        val email = auth.currentUser?.email
            ?: arguments?.getString("email").orEmpty()

        binding.confirmBody.text = getString(R.string.confirm_email_body, email)
        binding.btnConfirmed.setOnClickListener { checkVerification(quiet = false) }
        binding.btnResend.setOnClickListener { resendEmail() }
        binding.btnOpenEmail.setOnClickListener { openEmailApp() }
    }

    override fun onResume() {
        super.onResume()
        // Silent check — if the user tapped the link elsewhere while we
        // were paused, we can skip straight to the next screen.
        checkVerification(quiet = true)
    }

    /**
     * Reloads the current Firebase user and either continues into the
     * app when verified, or shows a hint otherwise.
     *
     * @param quiet When true, suppresses toasts and loading spinners so
     *   the poll on [onResume] is invisible to the user.
     */
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

    /**
     * Called only when the reloaded Firebase user reports
     * `isEmailVerified == true`. Writes the account to the local
     * session and navigates onward to Sport Select or Home.
     */
    private fun continueAfterVerified() {
        val refreshedUser = auth.currentUser ?: return
        val localSession = session()
        val email = refreshedUser.email.orEmpty()

        // Different account than last time on this device → drop the
        // cached sport so we don't inherit someone else's pick.
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

    /**
     * Sends a new verification email, subject to a client-side cooldown
     * that shields the account from Firebase's harsher device-level
     * rate limits ("blocked all requests from this device").
     */
    private fun resendEmail() {
        val screen = binding
        val user = auth.currentUser
        if (user == null) {
            returnToLogin()
            return
        }

        val waitMs = RESEND_COOLDOWN_MS - (SystemClock.elapsedRealtime() - lastResendAt)
        if (lastResendAt > 0L && waitMs > 0L) {
            val seconds = ((waitMs + 999) / 1000).toInt()
            showMessage("Wait $seconds seconds before sending another email.")
            return
        }

        lastResendAt = SystemClock.elapsedRealtime()
        setLoading(true)
        EmailVerification.send(user) { _, message ->
            if (_binding !== screen) return@send
            setLoading(false)
            showMessage(message)
        }
    }

    /** Best-effort launch of the user's default email app. */
    private fun openEmailApp() {
        openedInbox = true
        val opened = context?.let { EmailVerification.openInbox(it) } == true
        if (!opened) {
            showMessage("Open Gmail or your mail app, then tap the Verify link.")
        }
    }

    /**
     * Sent back to Login with the whole nav-graph popped so the user
     * cannot accidentally back-navigate into this screen from Login.
     */
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

    companion object {
        /** Minimum time between two "Resend email" taps. */
        private const val RESEND_COOLDOWN_MS = 60_000L
    }
}
