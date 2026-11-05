package com.example.prog7314_part2.logging

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/**
 * Logs every activity and fragment lifecycle transition in one place, so
 * the whole app's execution flow can be followed in Logcat without adding
 * `Log.d` calls to each screen.
 *
 * It also counts started activities to report when the app as a whole
 * moves to the foreground or background (for example when the user
 * presses Home, or a push notification brings the app back).
 *
 * Registered once from [com.example.prog7314_part2.SportSphereApp.onCreate].
 * Filter Logcat on `tag:SS.Lifecycle` to see the output.
 *
 * References:
 *  - Android Developers (2025) *The activity lifecycle*.
 *    Available at: https://developer.android.com/guide/components/activities/activity-lifecycle
 *  - Android Developers (2025) *Fragment lifecycle*.
 *    Available at: https://developer.android.com/guide/fragments/lifecycle
 */
class LifecycleLogger : Application.ActivityLifecycleCallbacks {

    /** Number of activities between onStart and onStop. */
    private var startedActivities = 0

    /** Shared fragment callbacks, attached to every FragmentActivity. */
    private val fragmentCallbacks = FragmentLogger()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val restored = if (savedInstanceState == null) "fresh" else "restored"
        log("${name(activity)} onCreate ($restored)")
        if (activity is FragmentActivity) {
            // recursive = true also logs child fragments (e.g. the NavHost's screens).
            activity.supportFragmentManager
                .registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
        log("${name(activity)} onStart")
        if (startedActivities == 1) {
            AppLog.i(AREA, "App moved to the FOREGROUND")
        }
    }

    override fun onActivityResumed(activity: Activity) = log("${name(activity)} onResume")

    override fun onActivityPaused(activity: Activity) = log("${name(activity)} onPause")

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        log("${name(activity)} onStop")
        if (startedActivities == 0 && !activity.isChangingConfigurations) {
            AppLog.i(AREA, "App moved to the BACKGROUND")
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) =
        log("${name(activity)} onSaveInstanceState")

    override fun onActivityDestroyed(activity: Activity) {
        // isChangingConfigurations is true for rotation, theme and language changes.
        val reason = if (activity.isChangingConfigurations) "configuration change" else "finishing"
        log("${name(activity)} onDestroy ($reason)")
    }

    /** Fragment-level counterpart of the activity callbacks above. */
    private class FragmentLogger : FragmentManager.FragmentLifecycleCallbacks() {

        override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) =
            logFragment(f, "onAttach")

        override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) =
            logFragment(f, if (savedInstanceState == null) "onCreate" else "onCreate (restored)")

        override fun onFragmentViewCreated(
            fm: FragmentManager,
            f: Fragment,
            v: View,
            savedInstanceState: Bundle?
        ) = logFragment(f, "onViewCreated")

        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) =
            logFragment(f, "onResume")

        override fun onFragmentPaused(fm: FragmentManager, f: Fragment) =
            logFragment(f, "onPause")

        override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) =
            logFragment(f, "onDestroyView")

        override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) =
            logFragment(f, "onDestroy")

        private fun logFragment(fragment: Fragment, event: String) {
            // The NavHostFragment itself is noise; only log real screens.
            if (fragment is androidx.navigation.fragment.NavHostFragment) return
            AppLog.d(AREA, "  ${fragment.javaClass.simpleName} $event")
        }
    }

    private fun log(message: String) = AppLog.d(AREA, message)

    private fun name(activity: Activity): String = activity.javaClass.simpleName

    companion object {
        const val AREA = "Lifecycle"
    }
}
