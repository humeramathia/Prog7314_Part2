package com.example.prog7314_part2.ui

import androidx.fragment.app.Fragment
import com.example.prog7314_part2.SportSphereApp
import com.example.prog7314_part2.data.SessionPrefs

fun Fragment.session(): SessionPrefs =
    (requireActivity().application as SportSphereApp).session
