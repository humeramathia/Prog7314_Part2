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
import com.google.firebase.auth.FirebaseAuth
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.example.prog7314_part2.data.remote.ApiClient
import com.example.prog7314_part2.data.remote.UserProfileDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private var googleCancellation: CancellationSignal? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnLogin.setOnClickListener { login() }
        binding.passwordInput.setOnEditorActionListener { _, actionId, event ->
            val enterPressed = event?.keyCode == KeyEvent.KEYCODE_ENTER
            val submit = actionId == EditorInfo.IME_ACTION_DONE ||
                    (enterPressed && event?.action == KeyEvent.ACTION_UP)

            when {
                submit -> {
                    if (binding.btnLogin.isEnabled) {
                        login()
                    }
                    true
                }

                // Consume Enter's key-down event to prevent default focus movement.
                enterPressed -> true

                else -> false
            }
        }

        // Remove the old hardcoded Google user.
        binding.btnGoogle.setOnClickListener {
            loginWithGoogle()
        }

        binding.goRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.forgotPassword.setOnClickListener { resetPassword() }
    }

    private fun login() {
        val screen = binding
        val email = screen.emailInput.text?.toString().orEmpty().trim()
        val password = screen.passwordInput.text?.toString().orEmpty()

        screen.emailLayout.error = null
        screen.passwordLayout.error = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            screen.emailLayout.error = "Enter a valid email address."
            return
        }

        if (password.isBlank()) {
            screen.passwordLayout.error = "Enter your password."
            return
        }

        session().isLoggedIn = false
        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener loginComplete@{ result ->
                if (_binding !== screen) return@loginComplete

                if (!result.isSuccessful) {
                    setLoading(false)

                    val message = when (result.exception) {
                        is com.google.firebase.FirebaseNetworkException ->
                            "No connection. Check your internet and try again."

                        is com.google.firebase.FirebaseTooManyRequestsException ->
                            "Too many attempts. Please try again later."

                        else ->
                            "Could not sign in. Check your email and password."
                    }

                    screen.passwordLayout.error = message
                    screen.passwordInput.requestFocus()

                    // Also show a message if the keyboard obscures the inline error.
                    showMessage(message)

                    return@loginComplete
                }

                val user = auth.currentUser
                if (user == null) {
                    setLoading(false)
                    showMessage("Session unavailable. Please try again.")
                    return@loginComplete
                }

                user.reload().addOnCompleteListener reloadComplete@{ reloadResult ->
                    if (_binding !== screen) return@reloadComplete

                    setLoading(false)

                    if (!reloadResult.isSuccessful) {
                        showMessage(
                            "Could not check your account. Check your connection."
                        )
                        return@reloadComplete
                    }

                    val refreshedUser = auth.currentUser
                    if (refreshedUser == null) {
                        showMessage("Please try logging in again.")
                        return@reloadComplete
                    }

                    if (!refreshedUser.isEmailVerified) {
                        findNavController().navigate(
                            R.id.action_login_to_confirm,
                            Bundle().apply {
                                putString("email", refreshedUser.email.orEmpty())
                            }
                        )
                        return@reloadComplete
                    }

                    val localSession = session()
                    val verifiedEmail = refreshedUser.email.orEmpty()

                    // Do not reuse another user's selected sport.
                    if (localSession.email != verifiedEmail) {
                        localSession.sportId = ""
                        localSession.sportName = ""
                    }

                    localSession.signIn(
                        verifiedEmail,
                        refreshedUser.displayName.orEmpty()
                    )

                    syncProfileAndContinue()
                }
            }
    }

    private fun loginWithGoogle() {
        val screen = binding

        if (!screen.btnGoogle.isEnabled) return

        screen.emailLayout.error = null
        screen.passwordLayout.error = null
        session().isLoggedIn = false

        val credentialManager = CredentialManager.create(requireContext())

        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            // Include accounts that have never signed into SportSphere.
            .setFilterByAuthorizedAccounts(false)
            // Let the user choose an account.
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val cancellation = CancellationSignal()
        googleCancellation = cancellation
        setLoading(true)

        credentialManager.getCredentialAsync(
            context = requireActivity(),
            request = request,
            cancellationSignal = cancellation,
            executor = ContextCompat.getMainExecutor(requireContext()),
            callback = object :
                CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {

                override fun onResult(result: GetCredentialResponse) {
                    if (_binding !== screen) return
                    googleCancellation = null

                    val credential = result.credential

                    if (
                        credential !is CustomCredential ||
                        credential.type !=
                        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        setLoading(false)
                        showMessage("An unsupported sign-in response was received.")
                        return
                    }

                    val googleCredential = try {
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } catch (error: GoogleIdTokenParsingException) {
                        setLoading(false)
                        showMessage("Could not read the Google sign-in response.")
                        return
                    }

                    val firebaseCredential = GoogleAuthProvider.getCredential(
                        googleCredential.idToken,
                        null
                    )

                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener googleComplete@{ task ->
                            if (_binding !== screen) return@googleComplete

                            setLoading(false)

                            if (!task.isSuccessful) {
                                val message = when (task.exception) {
                                    is com.google.firebase.FirebaseNetworkException ->
                                        "No connection. Check your internet and try again."

                                    is com.google.firebase.FirebaseTooManyRequestsException ->
                                        "Too many attempts. Please try again later."

                                    is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                                        "This email already uses another sign-in method. " +
                                                "Log in with that method first."

                                    else ->
                                        "Google sign-in failed. Please try again."
                                }

                                showMessage(message)
                                return@googleComplete
                            }

                            val user = task.result?.user

                            if (user == null) {
                                showMessage("Session unavailable. Please try again.")
                                return@googleComplete
                            }

                            // Keep the app's verified-email rule consistent.
                            // Verified Google users skip the confirmation screen.
                            if (!user.isEmailVerified) {
                                findNavController().navigate(
                                    R.id.action_login_to_confirm,
                                    Bundle().apply {
                                        putString("email", user.email.orEmpty())
                                    }
                                )
                                return@googleComplete
                            }

                            val localSession = session()
                            val email = user.email.orEmpty()

                            if (localSession.email != email) {
                                localSession.sportId = ""
                                localSession.sportName = ""
                            }

                            localSession.signIn(
                                email,
                                user.displayName.orEmpty()
                            )

                            syncProfileAndContinue()
                        }
                }

                override fun onError(error: GetCredentialException) {
                    if (_binding !== screen) return
                    googleCancellation = null
                    setLoading(false)

                    val message = when (error) {
                        is GetCredentialCancellationException ->
                            "Google sign-in cancelled."

                        is NoCredentialException ->
                            "No Google account is available. Add one in the " +
                                    "device's Settings, then try again."

                        else ->
                            "Could not open Google sign-in. Check your connection, " +
                                    "Google Play services and Firebase configuration."
                    }

                    showMessage(message)
                }
            }
        )
    }

    private fun resetPassword() {
        val screen = binding
        val email = screen.emailInput.text?.toString().orEmpty().trim()

        screen.emailLayout.error = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            screen.emailLayout.error = "Enter your email address first."
            return
        }

        setLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener resetComplete@{ result ->
                if (_binding !== screen) return@resetComplete

                setLoading(false)

                if (result.isSuccessful) {
                    showMessage(
                        "If this email has an account, check its inbox for reset instructions."
                    )
                } else {
                    showMessage(
                        "Could not request a password reset. Please try again later."
                    )
                }
            }
    }

    private fun syncProfileAndContinue() {
        val screen = _binding ?: return

        setLoading(true)

        ApiClient.service.getMyProfile().enqueue(
            object : Callback<UserProfileDto> {

                override fun onResponse(
                    call: Call<UserProfileDto>,
                    response: Response<UserProfileDto>
                ) {
                    if (_binding !== screen) return

                    val profile = response.body()

                    if (!response.isSuccessful || profile == null) {
                        setLoading(false)
                        showMessage(
                            "Could not load your profile. " +
                                    "Check that the API is running."
                        )
                        return
                    }

                    val localSession = session()
                    localSession.sportId = profile.sportId
                    localSession.sportName = profile.sportName

                    setLoading(false)
                    goNext()
                }

                override fun onFailure(
                    call: Call<UserProfileDto>,
                    error: Throwable
                ) {
                    if (_binding !== screen || call.isCanceled) return

                    setLoading(false)
                    showMessage(
                        "Could not connect to the API. " +
                                "Check that it is running on port 3000."
                    )
                }
            }
        )
    }

    private fun goNext() {
        val destination = if (session().sportId.isBlank()) {
            R.id.action_login_to_sport
        } else {
            R.id.action_login_to_home
        }

        findNavController().navigate(destination)
    }

    private fun setLoading(loading: Boolean) {
        _binding?.apply {
            btnLogin.isEnabled = !loading
            btnGoogle.isEnabled = !loading
            goRegister.isEnabled = !loading
            forgotPassword.isEnabled = !loading
        }
    }

    private fun showMessage(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        googleCancellation?.cancel()
        googleCancellation = null
        super.onDestroyView()
    }
}