package com.menasy.merkezisagliksistemi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.menasy.merkezisagliksistemi.databinding.ActivityMainBinding
import com.menasy.merkezisagliksistemi.di.SessionCache
import com.menasy.merkezisagliksistemi.ui.common.message.GlobalMessageController
import com.menasy.merkezisagliksistemi.ui.common.message.MessageHost
import com.menasy.merkezisagliksistemi.ui.common.message.UiMessage
import com.menasy.merkezisagliksistemi.ui.common.widget.PatientBottomMenuView

class MainActivity : AppCompatActivity(), MessageHost {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var messageController: GlobalMessageController
    private var activeBottomMenuRole: String = "patient"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        messageController = GlobalMessageController(binding.globalMessageView)

        setupBottomMenu()
        observeDestinationChanges()
        applyWindowInsets()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun setupBottomMenu() {
        binding.patientBottomMenu.setOnTabSelectedListener { tab ->
            val role = activeBottomMenuRole
            val destinationId = PatientBottomMenuView.tabToDestination(tab, role) ?: return@setOnTabSelectedListener
            if (navController.currentDestination?.id == destinationId) return@setOnTabSelectedListener

            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_graph, false)
                .build()
            navController.navigate(destinationId, null, options)
        }
    }

    private fun observeDestinationChanges() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val doctorTab = PatientBottomMenuView.destinationToTab(destination.id, "doctor")
            val patientTab = PatientBottomMenuView.destinationToTab(destination.id, "patient")

            when {
                doctorTab != null -> {
                    activeBottomMenuRole = "doctor"
                    binding.patientBottomMenu.setRole(activeBottomMenuRole)
                    binding.patientBottomMenu.isVisible = true
                    binding.patientBottomMenu.setSelectedTab(doctorTab, animate = false)
                }
                patientTab != null -> {
                    activeBottomMenuRole = "patient"
                    binding.patientBottomMenu.setRole(activeBottomMenuRole)
                    binding.patientBottomMenu.isVisible = true
                    binding.patientBottomMenu.setSelectedTab(patientTab, animate = false)
                }
                else -> {
                    activeBottomMenuRole = if (SessionCache.role.equals("doctor", ignoreCase = true)) {
                        "doctor"
                    } else {
                        "patient"
                    }
                    binding.patientBottomMenu.isVisible = false
                }
            }
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.navHostFragment.updatePadding(top = topInset)
            binding.globalMessageView.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                topMargin = topInset + dp(12)
            }
            binding.patientBottomMenu.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                bottomMargin = dp(6)
            }
            insets
        }
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView) ?: return
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun showMessage(message: UiMessage) {
        messageController.show(message)
    }
}
