package com.example.prog7314_part2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.prog7314_part2.data.biometric.BiometricDecision
import com.example.prog7314_part2.data.biometric.BiometricSession
import com.example.prog7314_part2.databinding.ActivityMainBinding
import com.example.prog7314_part2.ui.auth.BiometricPromptHost
import com.example.prog7314_part2.ui.auth.EmailVerification
import com.example.prog7314_part2.ui.auth.ProfileSync

/**
 * The single [AppCompatActivity] that hosts every fragment in the app.
 *
 * Responsibilities:
 *  - Hosts the [NavHostFragment] and wires the bottom navigation to it.
 *  - Decides the initial destination based on the persisted session
 *    (welcome → login → sport select → home).
 *  - Toggles the toolbar and bottom navigation per destination so auth
 *    screens are chrome-less and the four tab screens show the bar.
 *  - Handles the Firebase email-verification deep link from Chrome / Gmail
 *    (`https://.../verify-email?oobCode=...`).
 *  - Redirects background-killed users back to Login on resume.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pad the content below the status bar; leave navigation-bar
        // handling to individual fragments that draw list content there.
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
        val app = application as SportSphereApp
        val session = app.session

        // First launch after a return to the app: skip the welcome screen
        // if a valid Firebase session is already on device. When biometric
        // unlock is on, stop on the lock screen so Home never loads data
        // first (PROG7314 POE tips, s. 3).
        if (savedInstanceState == null && session.isLoggedIn) {
            val start = startDestination(app)
            Log.i(TAG, "cold start destination=$start unlocked=${BiometricSession.unlocked}")

            navController.navigate(
                start,
                null,
                androidx.navigation.navOptions {
                    popUpTo(R.id.welcomeFragment) {
                        inclusive = true
                    }
                }
            )
        }

        // Destinations that should show the bottom nav bar.
        val tabs = setOf(
            R.id.homeFragment,
            R.id.calendarFragment,
            R.id.performanceFragment,
            R.id.learnFragment
        )
        // Auth screens draw their own headers, so hide the toolbar there.
        val hideChrome = setOf(
            R.id.welcomeFragment,
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.confirmEmailFragment,
            R.id.biometricLockFragment
        )
        binding.toolbar.setNavigationOnClickListener { navController.navigateUp() }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in tabs) View.VISIBLE else View.GONE
            val showToolbar = destination.id !in tabs && destination.id !in hideChrome
            binding.toolbar.visibility = if (showToolbar) View.VISIBLE else View.GONE
            binding.toolbar.title = destination.label
        }
        handleEmailLink(intent)
    }

    /**
     * Deep-link entry point. Firebase verification links open the app via
     * `singleTop`, so we forward the new intent to [handleEmailLink].
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleEmailLink(intent)
    }

    /** Applies a `verifyEmail` oobCode when the app is opened from an email link. */
    private fun handleEmailLink(intent: Intent?) {
        EmailVerification.applyCodeFrom(intent?.data) { _, message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * If the user was signed out in the background (token revoked, account
     * deleted elsewhere, etc.) while on a protected screen, bounce back to
     * the login screen instead of showing a stale UI.
     */
    override fun onResume() {
        super.onResume()

        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController

        val publicScreens = setOf(
            R.id.welcomeFragment,
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.confirmEmailFragment,
            R.id.biometricLockFragment
        )

        val destination = navController.currentDestination?.id ?: return
        val app = application as SportSphereApp
        val session = app.session

        if (destination !in publicScreens && !session.isLoggedIn) {
            navController.navigate(
                R.id.loginFragment,
                null,
                androidx.navigation.navOptions {
                    popUpTo(R.id.nav_graph) { inclusive = true }
                }
            )
            return
        }

        if (destination !in publicScreens && needsBiometricGate(app)) {
            Log.i(TAG, "resume re-gate destination=$destination")
            navController.navigate(
                R.id.biometricLockFragment,
                null,
                androidx.navigation.navOptions {
                    popUpTo(R.id.nav_graph) { inclusive = true }
                }
            )
        }
    }

    private fun startDestination(app: SportSphereApp): Int {
        if (needsBiometricGate(app)) return R.id.biometricLockFragment
        return if (app.session.sportId.isBlank()) {
            R.id.sportSelectFragment
        } else {
            R.id.homeFragment
        }
    }

    private fun needsBiometricGate(app: SportSphereApp): Boolean {
        val status = BiometricPromptHost(this).canAuthenticateStatus()
        val enabled = app.biometrics.enabledFor(ProfileSync.currentUserId())
        return BiometricDecision.shouldGate(
            loggedIn = app.session.isLoggedIn,
            enabledForUser = enabled,
            unlockedThisProcess = BiometricSession.unlocked,
            canAuthenticateStatus = status
        )
    }

    private companion object {
        const val TAG = "Biometric"
    }
}
