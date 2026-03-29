package com.menasy.merkezisagliksistemi.ui.patient.appointment

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentAppointmentSearchBinding
import com.menasy.merkezisagliksistemi.ui.common.base.BaseFragment
import com.menasy.merkezisagliksistemi.ui.common.message.UiMessage
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class AppointmentSearchFragment : BaseFragment() {

    private var _binding: FragmentAppointmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppointmentSearchViewModel by viewModels {
        AppointmentSearchViewModelFactory()
    }

    private val dateFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))

    private var cityOptions: List<DropdownOption> = emptyList()
    private var districtOptions: List<DropdownOption> = emptyList()
    private var branchOptions: List<DropdownOption> = emptyList()
    private var hospitalOptions: List<DropdownOption> = emptyList()
    private var doctorOptions: List<DropdownOption> = emptyList()
    private val inputStates: MutableMap<Int, InputFieldState> = mutableMapOf()
    private var lastBlockedMessageAt: Long = 0L
    private var lastBlockedMessage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSearchableDropdowns()
        setupListeners()
        observeUiEvents(viewModel.uiEvents)
        observeUiState()
        viewModel.loadInitialData()
    }

    private fun setupToolbar() {
        binding.btnBackSearch.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSearchableDropdowns() {
        configureSearchableDropdown(binding.tilCity, binding.actCity)
        configureSearchableDropdown(binding.tilDistrict, binding.actDistrict)
        configureSearchableDropdown(binding.tilBranch, binding.actBranch)
        configureSearchableDropdown(binding.tilHospital, binding.actHospital)
        configureSearchableDropdown(binding.tilDoctor, binding.actDoctor)
    }

    private fun configureSearchableDropdown(
        layout: TextInputLayout,
        input: MaterialAutoCompleteTextView
    ) {
        input.threshold = 1

        input.setOnClickListener {
            if (handleDisabledInteraction(input.id)) return@setOnClickListener
            enableTextInputMode(input)
            input.requestFocus()
            input.setSelection(input.text?.length ?: 0)
        }

        layout.setEndIconOnClickListener {
            if (handleDisabledInteraction(input.id)) return@setEndIconOnClickListener
            disableTextInputMode(input)
            input.requestFocus()

            // Filter'ı temizle ve tamamlanınca dropdown'u göster
            val adapter = input.adapter as? ArrayAdapter<*>
            if (adapter != null) {
                adapter.filter.filter("") {
                    input.post { input.showDropDown() }
                }
            } else {
                input.showDropDown()
            }
        }

        layout.setOnClickListener {
            if (handleDisabledInteraction(input.id)) return@setOnClickListener
            input.performClick()
        }
    }

    private fun setupListeners() {
        binding.btnSelectDateRange.setOnClickListener {
            showDateRangePicker()
        }

        binding.actCity.setOnItemClickListener { _, _, _, _ ->
            val selectedText = binding.actCity.text.toString()
            val cityId = cityOptions.firstOrNull { it.label == selectedText }?.id
            cityId?.let { viewModel.onCitySelected(it) }
            disableTextInputMode(binding.actCity)
        }

        binding.actDistrict.setOnItemClickListener { _, _, _, _ ->
            val selectedText = binding.actDistrict.text.toString()
            val districtId = districtOptions.firstOrNull { it.label == selectedText }?.id
            viewModel.onDistrictSelected(districtId)
            disableTextInputMode(binding.actDistrict)
        }

        binding.actBranch.setOnItemClickListener { _, _, _, _ ->
            val selectedText = binding.actBranch.text.toString()
            val branchId = branchOptions.firstOrNull { it.label == selectedText }?.id
            branchId?.let { viewModel.onBranchSelected(it) }
            disableTextInputMode(binding.actBranch)
        }

        binding.actHospital.setOnItemClickListener { _, _, _, _ ->
            val selectedText = binding.actHospital.text.toString()
            val hospitalId = hospitalOptions.firstOrNull { it.label == selectedText }?.id
            viewModel.onHospitalSelected(hospitalId)
            disableTextInputMode(binding.actHospital)
        }

        binding.actDoctor.setOnItemClickListener { _, _, _, _ ->
            val selectedText = binding.actDoctor.text.toString()
            val doctorId = doctorOptions.firstOrNull { it.label == selectedText }?.id
            viewModel.onDoctorSelected(doctorId)
            disableTextInputMode(binding.actDoctor)
        }

        binding.btnSearchAppointments.setOnClickListener {
            val criteria = viewModel.buildSearchCriteria() ?: return@setOnClickListener
            navigateToResults(criteria)
        }
    }

    private fun navigateToResults(criteria: AppointmentSearchCriteria) {
        val args = AppointmentSearchArgs(
            startDateMillis = criteria.startDateMillis,
            endDateMillis = criteria.endDateMillis,
            cityId = criteria.cityId,
            districtId = criteria.districtId,
            branchId = criteria.branchId,
            hospitalId = criteria.hospitalId,
            doctorId = criteria.doctorId
        )

        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.appointmentSearchFragment) {
            navController.navigate(
                R.id.action_appointmentSearchFragment_to_appointmentResultsFragment,
                args.toBundle()
            )
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                bindCityOptions(state)
                bindDistrictOptions(state)
                bindBranchOptions(state)
                bindHospitalOptions(state)
                bindDoctorOptions(state)
                bindDateRange(state.startDateMillis, state.endDateMillis)

                bindFieldEnabledState(state)
            }
        }
    }

    private fun bindCityOptions(state: AppointmentSearchUiState) {
        cityOptions = state.cities.map { city ->
            DropdownOption(id = city.id, label = city.name)
        }
        updateDropdown(
            options = cityOptions,
            selectedId = state.selectedCityId,
            input = binding.actCity
        )
    }

    private fun bindDistrictOptions(state: AppointmentSearchUiState) {
        districtOptions = listOf(DropdownOption(id = null, label = FARKETMEZ)) +
            state.districts.map { district ->
                DropdownOption(id = district.id, label = district.name)
            }
        updateDropdown(
            options = districtOptions,
            selectedId = state.selectedDistrictId,
            input = binding.actDistrict
        )
    }

    private fun bindBranchOptions(state: AppointmentSearchUiState) {
        branchOptions = state.branches.map { branch ->
            DropdownOption(id = branch.id, label = branch.name)
        }
        updateDropdown(
            options = branchOptions,
            selectedId = state.selectedBranchId,
            input = binding.actBranch
        )
    }

    private fun bindHospitalOptions(state: AppointmentSearchUiState) {
        hospitalOptions = listOf(DropdownOption(id = null, label = FARKETMEZ)) +
            state.hospitals.map { hospital ->
                DropdownOption(id = hospital.id, label = hospital.name)
            }
        updateDropdown(
            options = hospitalOptions,
            selectedId = state.selectedHospitalId,
            input = binding.actHospital
        )
    }

    private fun bindDoctorOptions(state: AppointmentSearchUiState) {
        doctorOptions = listOf(DropdownOption(id = null, label = FARKETMEZ)) +
            state.doctors.map { doctor ->
                DropdownOption(id = doctor.id, label = doctor.fullName)
            }
        updateDropdown(
            options = doctorOptions,
            selectedId = state.selectedDoctorId,
            input = binding.actDoctor
        )
    }

    private fun bindFieldEnabledState(state: AppointmentSearchUiState) {
        bindDropdownField(
            layout = binding.tilDistrict,
            input = binding.actDistrict,
            isEnabled = state.isDistrictFieldEnabled,
            enabledHint = "İlçe (Opsiyonel)",
            disabledHint = "İlçe (Önce il seçiniz)",
            activeStartIconColorRes = R.color.secondary,
            disabledMessage = "Önce il seçmelisiniz."
        )

        bindDropdownField(
            layout = binding.tilBranch,
            input = binding.actBranch,
            isEnabled = state.isBranchFieldEnabled,
            enabledHint = "Poliklinik *",
            disabledHint = "Poliklinik (Önce il seçiniz)",
            activeStartIconColorRes = R.color.primary,
            disabledMessage = "Önce il seçmelisiniz."
        )

        bindDropdownField(
            layout = binding.tilHospital,
            input = binding.actHospital,
            isEnabled = state.isHospitalFieldEnabled,
            enabledHint = "Hastane (Opsiyonel)",
            disabledHint = "Hastane (Önce poliklinik seçiniz)",
            activeStartIconColorRes = R.color.error,
            disabledMessage = "Önce poliklinik seçmelisiniz."
        )

        bindDropdownField(
            layout = binding.tilDoctor,
            input = binding.actDoctor,
            isEnabled = state.isDoctorFieldEnabled,
            enabledHint = "Hekim (Opsiyonel)",
            disabledHint = "Hekim (Önce hastane seçiniz)",
            activeStartIconColorRes = R.color.primary_dark,
            disabledMessage = "Önce hastane seçmelisiniz."
        )
    }

    private fun bindDropdownField(
        layout: TextInputLayout,
        input: MaterialAutoCompleteTextView,
        isEnabled: Boolean,
        enabledHint: String,
        disabledHint: String,
        activeStartIconColorRes: Int,
        disabledMessage: String
    ) {
        input.isEnabled = isEnabled
        input.isClickable = isEnabled
        input.isFocusable = isEnabled
        input.isFocusableInTouchMode = isEnabled
        input.isCursorVisible = isEnabled
        input.isLongClickable = isEnabled
        layout.hint = if (isEnabled) enabledHint else disabledHint
        inputStates[input.id] = InputFieldState(
            isEnabled = isEnabled,
            disabledMessage = disabledMessage
        )

        applyDropdownVisualState(
            layout = layout,
            isEnabled = isEnabled,
            activeStartIconColorRes = activeStartIconColorRes
        )

        if (!isEnabled) {
            input.clearFocus()
            input.dismissDropDown()
            disableTextInputMode(input)
        }
    }

    private fun bindDateRange(startDateMillis: Long?, endDateMillis: Long?) {
        val startText = startDateMillis?.let { millis -> formatDate(millis) } ?: "-"
        val endText = endDateMillis?.let { millis -> formatDate(millis) } ?: "-"

        binding.tvStartDateValue.text = "Başlangıç: $startText"
        binding.tvEndDateValue.text = "Bitiş: $endText"
    }

    private fun showDateRangePicker() {
        val todayMillis = System.currentTimeMillis()

        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.from(todayMillis))

        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Tarih aralığı seçin")
            .setTheme(R.style.ThemeOverlay_MerkeziSaglik_DateRangePicker)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        picker.addOnPositiveButtonClickListener { range ->
            val start = range.first ?: return@addOnPositiveButtonClickListener
            val end = range.second ?: return@addOnPositiveButtonClickListener

            viewModel.onDateRangeSelected(start, end)
        }

        picker.show(parentFragmentManager, "appointment_date_range_picker")
    }

    private fun updateDropdown(
        options: List<DropdownOption>,
        selectedId: String?,
        input: MaterialAutoCompleteTextView
    ) {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown_option,
            android.R.id.text1,
            options
        )
        input.setAdapter(adapter)
        val selectedOption = options.firstOrNull { it.id == selectedId }
        if (selectedOption == null) {
            input.setText("", false)
        } else {
            input.setText(selectedOption.label, false)
        }
    }

    private fun enableTextInputMode(input: MaterialAutoCompleteTextView) {
        input.showSoftInputOnFocus = true
        input.post {
            inputMethodManager()?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun disableTextInputMode(input: MaterialAutoCompleteTextView) {
        input.showSoftInputOnFocus = false
        inputMethodManager()?.hideSoftInputFromWindow(input.windowToken, 0)
    }

    private fun applyDropdownVisualState(
        layout: TextInputLayout,
        isEnabled: Boolean,
        activeStartIconColorRes: Int
    ) {
        val disabledTint = ColorStateList.valueOf(resolveColor(R.color.text_secondary))

        layout.alpha = if (isEnabled) ENABLED_FIELD_ALPHA else DISABLED_FIELD_ALPHA
        layout.boxStrokeColor =
            resolveColor(if (isEnabled) R.color.primary else R.color.message_card_stroke)
        layout.boxBackgroundColor =
            resolveColor(if (isEnabled) R.color.surface else R.color.background)
        layout.setHintTextColor(
            ColorStateList.valueOf(
                resolveColor(if (isEnabled) R.color.text_secondary else R.color.text_secondary)
            )
        )

        val startTint = if (isEnabled) {
            ColorStateList.valueOf(resolveColor(activeStartIconColorRes))
        } else {
            disabledTint
        }
        val endTint = if (isEnabled) {
            ColorStateList.valueOf(resolveColor(R.color.primary_dark))
        } else {
            disabledTint
        }
        layout.setStartIconTintList(startTint)
        layout.setEndIconTintList(endTint)
    }

    private fun handleDisabledInteraction(inputId: Int): Boolean {
        val state = inputStates[inputId] ?: return false
        if (state.isEnabled) return false

        val now = SystemClock.elapsedRealtime()
        val shouldShowMessage = state.disabledMessage != lastBlockedMessage ||
            now - lastBlockedMessageAt >= DISABLED_MESSAGE_COOLDOWN_MS

        if (shouldShowMessage) {
            showMessage(
                UiMessage.info(
                    title = "Bilgilendirme",
                    description = state.disabledMessage,
                    autoDismissMillis = 2_200L
                )
            )
            lastBlockedMessageAt = now
            lastBlockedMessage = state.disabledMessage
        }
        return true
    }

    private fun resolveColor(colorRes: Int): Int {
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun inputMethodManager(): InputMethodManager? {
        val safeContext = context ?: return null
        return safeContext.getSystemService(InputMethodManager::class.java)
    }

    private fun formatDate(millis: Long): String {
        val localDate = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return dateFormatter.format(localDate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class DropdownOption(
        val id: String?,
        val label: String
    ) {
        override fun toString(): String = label
    }

    private data class InputFieldState(
        val isEnabled: Boolean,
        val disabledMessage: String
    )

    private companion object {
        const val FARKETMEZ = "Fark etmez"
        const val DISABLED_MESSAGE_COOLDOWN_MS = 1_200L
        const val ENABLED_FIELD_ALPHA = 1f
        const val DISABLED_FIELD_ALPHA = 0.62f
    }
}
