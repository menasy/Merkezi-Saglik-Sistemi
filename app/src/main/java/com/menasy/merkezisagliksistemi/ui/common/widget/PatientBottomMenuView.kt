package com.menasy.merkezisagliksistemi.ui.common.widget

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.ViewPatientBottomMenuBinding

class PatientBottomMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class Tab {
        HOME,
        SEARCH,
        APPOINTMENTS,
        PRESCRIPTIONS,
        ACCOUNT
    }

    private data class MenuItem(
        val surface: View,
        val icon: ImageView,
        val label: TextView
    )

    private val binding = ViewPatientBottomMenuBinding.inflate(LayoutInflater.from(context), this, true)
    private val idleTextColor = ContextCompat.getColor(context, R.color.bottom_menu_item_idle_text)
    private val selectedTextColor = ContextCompat.getColor(context, R.color.bottom_menu_item_selected_text)
    private val idleTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val selectedTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    private val selectInterpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
    private var selectedTab: Tab = Tab.HOME
    private var onTabSelectedListener: ((Tab) -> Unit)? = null

    private val items: Map<Tab, MenuItem> = mapOf(
        Tab.HOME to MenuItem(
            surface = binding.surfaceHome,
            icon = binding.ivHome,
            label = binding.tvHome
        ),
        Tab.SEARCH to MenuItem(
            surface = binding.surfaceSearch,
            icon = binding.ivSearch,
            label = binding.tvSearch
        ),
        Tab.APPOINTMENTS to MenuItem(
            surface = binding.surfaceAppointments,
            icon = binding.ivAppointments,
            label = binding.tvAppointments
        ),
        Tab.PRESCRIPTIONS to MenuItem(
            surface = binding.surfacePrescriptions,
            icon = binding.ivPrescriptions,
            label = binding.tvPrescriptions
        ),
        Tab.ACCOUNT to MenuItem(
            surface = binding.surfaceAccount,
            icon = binding.ivAccount,
            label = binding.tvAccount
        )
    )

    init {
        setupClickListeners()
        setSelectedTab(Tab.HOME, animate = false)
    }

    fun setOnTabSelectedListener(listener: (Tab) -> Unit) {
        onTabSelectedListener = listener
    }

    fun setSelectedTab(tab: Tab, animate: Boolean) {
        selectedTab = tab
        items.forEach { (currentTab, item) ->
            if (currentTab == tab) {
                applySelectedState(item, animate)
            } else {
                applyIdleState(item)
            }
        }
    }

    private fun setupClickListeners() {
        items.forEach { (tab, item) ->
            item.surface.setOnClickListener {
                if (selectedTab == tab) {
                    animateReselect(item.surface)
                    return@setOnClickListener
                }
                setSelectedTab(tab, animate = true)
                onTabSelectedListener?.invoke(tab)
            }
        }
    }

    private fun applySelectedState(item: MenuItem, animate: Boolean) {
        item.surface.background = ContextCompat.getDrawable(context, R.drawable.bg_bottom_menu_item_selected)
        item.icon.setColorFilter(selectedTextColor)
        item.label.setTextColor(selectedTextColor)
        item.label.typeface = selectedTypeface

        if (animate) {
            animateSelect(item.surface)
        } else {
            item.surface.animate().cancel()
            item.surface.scaleX = 1f
            item.surface.scaleY = 1f
            item.surface.translationY = dp(1f)
        }
    }

    private fun applyIdleState(item: MenuItem) {
        item.surface.background = ContextCompat.getDrawable(context, R.drawable.bg_bottom_menu_item_idle)
        item.icon.setColorFilter(idleTextColor)
        item.label.setTextColor(idleTextColor)
        item.label.typeface = idleTypeface
        item.surface.animate().cancel()
        item.surface.scaleX = 1f
        item.surface.scaleY = 1f
        item.surface.translationY = 0f
    }

    private fun animateSelect(view: View) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.96f, 1.03f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.96f, 1.03f, 1f)
        val translateY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -dp(1f), dp(1f))
        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, translateY).apply {
            duration = 240
            interpolator = selectInterpolator
            start()
        }
    }

    private fun animateReselect(view: View) {
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.86f, 1f)
        ObjectAnimator.ofPropertyValuesHolder(view, alpha).apply {
            duration = 160
            interpolator = selectInterpolator
            start()
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    companion object {
        @JvmStatic
        fun destinationToTab(@IdRes destinationId: Int): Tab? {
            return when (destinationId) {
                R.id.patientHomeFragment -> Tab.HOME
                R.id.appointmentSearchFragment,
                R.id.appointmentResultsFragment,
                R.id.doctorAvailabilityFragment,
                R.id.appointmentConfirmationFragment -> Tab.SEARCH
                R.id.patientAppointmentsFragment -> Tab.APPOINTMENTS
                R.id.patientPrescriptionsFragment -> Tab.PRESCRIPTIONS
                R.id.patientAccountFragment -> Tab.ACCOUNT
                else -> null
            }
        }

        @JvmStatic
        fun tabToDestination(tab: Tab): Int {
            return when (tab) {
                Tab.HOME -> R.id.patientHomeFragment
                Tab.SEARCH -> R.id.appointmentSearchFragment
                Tab.APPOINTMENTS -> R.id.patientAppointmentsFragment
                Tab.PRESCRIPTIONS -> R.id.patientPrescriptionsFragment
                Tab.ACCOUNT -> R.id.patientAccountFragment
            }
        }
    }
}
