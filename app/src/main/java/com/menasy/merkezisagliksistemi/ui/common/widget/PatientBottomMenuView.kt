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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.ViewPatientBottomMenuBinding

class PatientBottomMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class Role {
        PATIENT,
        DOCTOR
    }

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
    private var role: Role = Role.PATIENT
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
        applyRole(role)
        setSelectedTab(Tab.HOME, animate = false)
    }

    fun setRole(rawRole: String?) {
        val resolvedRole = if (rawRole.equals(ROLE_DOCTOR, ignoreCase = true)) {
            Role.DOCTOR
        } else {
            Role.PATIENT
        }

        if (resolvedRole == role) return
        role = resolvedRole
        applyRole(role)
    }

    fun setOnTabSelectedListener(listener: (Tab) -> Unit) {
        onTabSelectedListener = listener
    }

    fun setSelectedTab(tab: Tab, animate: Boolean) {
        val visibleTabs = visibleTabsByRole(role)
        val targetTab = when {
            tab in visibleTabs -> tab
            visibleTabs.isNotEmpty() -> visibleTabs.first()
            else -> return
        }
        selectedTab = targetTab
        items.forEach { (currentTab, item) ->
            if (!item.surface.isVisible) return@forEach
            if (currentTab == targetTab) {
                applySelectedState(item, animate)
            } else {
                applyIdleState(item)
            }
        }
    }

    private fun setupClickListeners() {
        items.forEach { (tab, item) ->
            item.surface.setOnClickListener {
                if (!item.surface.isVisible) return@setOnClickListener
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

    private fun applyRole(role: Role) {
        when (role) {
            Role.PATIENT -> applyPatientLabels()
            Role.DOCTOR -> applyDoctorLabels()
        }
        applyVisibility(role)
        applyWeights(role)
        setSelectedTab(selectedTab, animate = false)
    }

    private fun applyPatientLabels() {
        binding.tvHome.text = context.getString(R.string.bottom_menu_home)
        binding.tvSearch.text = context.getString(R.string.bottom_menu_search)
        binding.tvAppointments.text = context.getString(R.string.bottom_menu_appointments)
        binding.tvPrescriptions.text = context.getString(R.string.bottom_menu_prescriptions)
        binding.tvAccount.text = context.getString(R.string.bottom_menu_account)

        binding.ivHome.contentDescription = context.getString(R.string.cd_bottom_menu_home)
        binding.ivSearch.contentDescription = context.getString(R.string.cd_bottom_menu_search)
        binding.ivAppointments.contentDescription = context.getString(R.string.cd_bottom_menu_appointments)
        binding.ivPrescriptions.contentDescription = context.getString(R.string.cd_bottom_menu_prescriptions)
        binding.ivAccount.contentDescription = context.getString(R.string.cd_bottom_menu_account)
    }

    private fun applyDoctorLabels() {
        binding.tvHome.text = context.getString(R.string.doctor_bottom_menu_home)
        binding.tvAppointments.text = context.getString(R.string.doctor_bottom_menu_appointments)
        binding.tvPrescriptions.text = context.getString(R.string.doctor_bottom_menu_prescriptions)
        binding.tvAccount.text = context.getString(R.string.doctor_bottom_menu_account)

        binding.ivHome.contentDescription = context.getString(R.string.cd_doctor_bottom_menu_home)
        binding.ivAppointments.contentDescription = context.getString(R.string.cd_doctor_bottom_menu_appointments)
        binding.ivPrescriptions.contentDescription = context.getString(R.string.cd_doctor_bottom_menu_prescriptions)
        binding.ivAccount.contentDescription = context.getString(R.string.cd_doctor_bottom_menu_account)
    }

    private fun applyVisibility(role: Role) {
        items.forEach { (tab, item) ->
            item.surface.isVisible = tab in visibleTabsByRole(role)
        }
    }

    private fun applyWeights(role: Role) {
        val visibleTabs = visibleTabsByRole(role)
        items.forEach { (tab, item) ->
            val layoutParams = item.surface.layoutParams as? LinearLayout.LayoutParams ?: return@forEach
            layoutParams.weight = when (role) {
                Role.PATIENT -> if (tab == Tab.APPOINTMENTS) 1.1f else 1f
                Role.DOCTOR -> 1f
            }
            val edgeMargin = dp(2f).toInt()
            layoutParams.marginStart = if (tab == visibleTabs.firstOrNull()) 0 else edgeMargin
            layoutParams.marginEnd = if (tab == visibleTabs.lastOrNull()) 0 else edgeMargin
            item.surface.layoutParams = layoutParams
        }
    }

    private fun visibleTabsByRole(role: Role): List<Tab> {
        return when (role) {
            Role.PATIENT -> PATIENT_TABS
            Role.DOCTOR -> DOCTOR_TABS
        }
    }

    companion object {
        private const val ROLE_DOCTOR = "doctor"
        private val PATIENT_TABS = listOf(
            Tab.HOME,
            Tab.SEARCH,
            Tab.APPOINTMENTS,
            Tab.PRESCRIPTIONS,
            Tab.ACCOUNT
        )
        private val DOCTOR_TABS = listOf(
            Tab.HOME,
            Tab.APPOINTMENTS,
            Tab.PRESCRIPTIONS,
            Tab.ACCOUNT
        )

        @JvmStatic
        fun destinationToTab(@IdRes destinationId: Int, role: String?): Tab? {
            return if (role.equals(ROLE_DOCTOR, ignoreCase = true)) {
                destinationToDoctorTab(destinationId)
            } else {
                destinationToPatientTab(destinationId)
            }
        }

        @JvmStatic
        fun isPatientDestination(@IdRes destinationId: Int): Boolean {
            return destinationToPatientTab(destinationId) != null
        }

        @JvmStatic
        fun isDoctorDestination(@IdRes destinationId: Int): Boolean {
            return destinationToDoctorTab(destinationId) != null
        }

        private fun destinationToPatientTab(@IdRes destinationId: Int): Tab? {
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

        private fun destinationToDoctorTab(@IdRes destinationId: Int): Tab? {
            return when (destinationId) {
                R.id.doctorHomeFragment -> Tab.HOME
                R.id.doctorAppointmentsFragment,
                R.id.doctorExaminationFragment -> Tab.APPOINTMENTS
                R.id.doctorPrescriptionsFragment -> Tab.PRESCRIPTIONS
                R.id.doctorAccountFragment -> Tab.ACCOUNT
                else -> null
            }
        }

        @JvmStatic
        fun tabToDestination(tab: Tab, role: String?): Int? {
            return if (role.equals(ROLE_DOCTOR, ignoreCase = true)) {
                when (tab) {
                    Tab.HOME -> R.id.doctorHomeFragment
                    Tab.SEARCH -> null
                    Tab.APPOINTMENTS -> R.id.doctorAppointmentsFragment
                    Tab.PRESCRIPTIONS -> R.id.doctorPrescriptionsFragment
                    Tab.ACCOUNT -> R.id.doctorAccountFragment
                }
            } else {
                when (tab) {
                    Tab.HOME -> R.id.patientHomeFragment
                    Tab.SEARCH -> R.id.appointmentSearchFragment
                    Tab.APPOINTMENTS -> R.id.patientAppointmentsFragment
                    Tab.PRESCRIPTIONS -> R.id.patientPrescriptionsFragment
                    Tab.ACCOUNT -> R.id.patientAccountFragment
                }
            }
        }
    }
}
