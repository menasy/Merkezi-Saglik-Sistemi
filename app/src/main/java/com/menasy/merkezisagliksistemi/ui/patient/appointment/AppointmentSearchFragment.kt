package com.menasy.merkezisagliksistemi.ui.patient.appointment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentAppointmentSearchBinding
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class AppointmentSearchFragment : Fragment() {

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

        setupListeners()
        observeUiState()
        viewModel.loadInitialData()
    }

    private fun setupListeners() {
        binding.btnSelectDateRange.setOnClickListener {
            showDateRangePicker()
        }

        binding.actCity.setOnItemClickListener { _, _, position, _ ->
            cityOptions.getOrNull(position)?.id?.let { cityId ->
                viewModel.onCitySelected(cityId)
            }
        }

        binding.actDistrict.setOnItemClickListener { _, _, position, _ ->
            val districtId = districtOptions.getOrNull(position)?.id
            viewModel.onDistrictSelected(districtId)
        }

        binding.actBranch.setOnItemClickListener { _, _, position, _ ->
            branchOptions.getOrNull(position)?.id?.let { branchId ->
                viewModel.onBranchSelected(branchId)
            }
        }

        binding.actHospital.setOnItemClickListener { _, _, position, _ ->
            val hospitalId = hospitalOptions.getOrNull(position)?.id
            viewModel.onHospitalSelected(hospitalId)
        }

        binding.actDoctor.setOnItemClickListener { _, _, position, _ ->
            val doctorId = doctorOptions.getOrNull(position)?.id
            viewModel.onDoctorSelected(doctorId)
        }

        binding.btnSearchAppointments.setOnClickListener {
            val result = viewModel.buildSearchCriteria()
            result.fold(
                onSuccess = { criteria ->
                    navigateToResults(criteria)
                },
                onFailure = { exception ->
                    Toast.makeText(
                        requireContext(),
                        exception.message ?: "Formu kontrol edin",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
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

                state.errorMessage?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindCityOptions(state: AppointmentSearchUiState) {
        cityOptions = state.cities.map { city ->
            DropdownOption(id = city.id, label = city.name)
        }
        updateDropdown(
            labels = cityOptions.map { it.label },
            selectedLabel = cityOptions.firstOrNull { it.id == state.selectedCityId }?.label,
            input = binding.actCity
        )
    }

    private fun bindDistrictOptions(state: AppointmentSearchUiState) {
        districtOptions = listOf(DropdownOption(id = null, label = FARKETMEZ)) +
            state.districts.map { district ->
                DropdownOption(id = district.id, label = district.name)
            }
        updateDropdown(
            labels = districtOptions.map { it.label },
            selectedLabel = districtOptions.firstOrNull { it.id == state.selectedDistrictId }?.label
                ?: FARKETMEZ,
            input = binding.actDistrict
        )
    }

    private fun bindBranchOptions(state: AppointmentSearchUiState) {
        branchOptions = state.branches.map { branch ->
            DropdownOption(id = branch.id, label = branch.name)
        }
        updateDropdown(
            labels = branchOptions.map { it.label },
            selectedLabel = branchOptions.firstOrNull { it.id == state.selectedBranchId }?.label,
            input = binding.actBranch
        )
    }

    private fun bindHospitalOptions(state: AppointmentSearchUiState) {
        hospitalOptions = listOf(DropdownOption(id = null, label = FARKETMEZ)) +
            state.hospitals.map { hospital ->
                DropdownOption(id = hospital.id, label = hospital.name)
            }
        updateDropdown(
            labels = hospitalOptions.map { it.label },
            selectedLabel = hospitalOptions.firstOrNull { it.id == state.selectedHospitalId }?.label
                ?: FARKETMEZ,
            input = binding.actHospital
        )
    }

    private fun bindDoctorOptions(state: AppointmentSearchUiState) {
        doctorOptions = listOf(DropdownOption(id = null, label = FARKETMEZ)) +
            state.doctors.map { doctor ->
                DropdownOption(id = doctor.id, label = doctor.fullName)
            }
        updateDropdown(
            labels = doctorOptions.map { it.label },
            selectedLabel = doctorOptions.firstOrNull { it.id == state.selectedDoctorId }?.label
                ?: FARKETMEZ,
            input = binding.actDoctor
        )
    }

    private fun bindDateRange(startDateMillis: Long?, endDateMillis: Long?) {
        val startText = startDateMillis?.let { millis -> formatDate(millis) } ?: "-"
        val endText = endDateMillis?.let { millis -> formatDate(millis) } ?: "-"

        binding.tvStartDateValue.text = startText
        binding.tvEndDateValue.text = endText
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Tarih araligi secin")
            .build()

        picker.addOnPositiveButtonClickListener { range ->
            val start = range.first ?: return@addOnPositiveButtonClickListener
            val end = range.second ?: return@addOnPositiveButtonClickListener

            val result = viewModel.onDateRangeSelected(start, end)
            result.exceptionOrNull()?.let { exception ->
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Tarih araligi gecersiz",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        picker.show(parentFragmentManager, "appointment_date_range_picker")
    }

    private fun updateDropdown(
        labels: List<String>,
        selectedLabel: String?,
        input: com.google.android.material.textfield.MaterialAutoCompleteTextView
    ) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        input.setAdapter(adapter)
        if (selectedLabel == null) {
            input.setText("", false)
        } else {
            input.setText(selectedLabel, false)
        }
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
    )

    private companion object {
        const val FARKETMEZ = "Farketmez"
    }
}
