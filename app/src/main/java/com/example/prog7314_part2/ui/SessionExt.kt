package com.example.prog7314_part2.ui

import androidx.fragment.app.Fragment
import com.example.prog7314_part2.SportSphereApp
import com.example.prog7314_part2.data.SessionPrefs

/**
 * Shorthand for retrieving the process-wide [SessionPrefs] from any
 * fragment without having to cast `requireActivity().application` at
 * every call site.
 *
 * Usage: `session().sportId`, `session().signOut()`, etc.
 */
fun Fragment.session(): SessionPrefs =
    (requireActivity().application as SportSphereApp).session
