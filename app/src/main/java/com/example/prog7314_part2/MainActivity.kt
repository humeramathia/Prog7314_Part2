package com.example.prog7314_part2

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.prog7314_part2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)

        val session = (application as SportSphereApp).session
        if (savedInstanceState == null && session.isLoggedIn) {
            val start = if (session.sportId.isBlank()) R.id.sportSelectFragment else R.id.homeFragment
            navController.navigate(start) {
                popUpTo(R.id.welcomeFragment) { inclusive = true }
            }
        }

        val tabs = setOf(
            R.id.homeFragment,
            R.id.calendarFragment,
            R.id.performanceFragment,
            R.id.learnFragment
        )
        val hideChrome = setOf(R.id.welcomeFragment)
        binding.toolbar.setNavigationOnClickListener { navController.navigateUp() }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = if (destination.id in tabs) View.VISIBLE else View.GONE
            val showToolbar = destination.id !in tabs && destination.id !in hideChrome
            binding.toolbar.visibility = if (showToolbar) View.VISIBLE else View.GONE
            binding.toolbar.title = destination.label
        }
    }
}
