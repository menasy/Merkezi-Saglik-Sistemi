package com.menasy.merkezisagliksistemi.ui.doctor.prescriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.databinding.DialogPrescriptionPreviewBinding
import com.menasy.merkezisagliksistemi.databinding.FragmentDoctorPrescriptionsBinding
import com.menasy.merkezisagliksistemi.di.ServiceLocator
import com.menasy.merkezisagliksistemi.ui.common.adapter.PrescriptionRecordsAdapter
import com.menasy.merkezisagliksistemi.ui.common.util.DialogWindowSizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class DoctorPrescriptionsFragment : Fragment() {

    private var _binding: FragmentDoctorPrescriptionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PrescriptionRecordsAdapter

    private val viewModel: DoctorPrescriptionsViewModel by viewModels {
        DoctorPrescriptionsViewModel.Factory(
            observeDoctorAppointmentsUseCase = ServiceLocator.provideObserveDoctorAppointmentsUseCase(),
            getUserFullNamesByIdsUseCase = ServiceLocator.provideGetUserFullNamesByIdsUseCase(),
            getPrescriptionPreviewsByAppointmentIdsUseCase = ServiceLocator.provideGetPrescriptionPreviewsByAppointmentIdsUseCase(),
            appointmentMapper = ServiceLocator.provideAppointmentMapper()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorPrescriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupFilters()
        observeUiState()
    }

    private fun setupList() {
        adapter = PrescriptionRecordsAdapter(
            onItemClick = { item ->
                showPrescriptionPreviewDialog(
                    personName = item.personName,
                    prescription = item.prescription
                )
            }
        )
        binding.rvPrescriptions.adapter = adapter
    }

    private fun setupFilters() {
        binding.btnFilterToday.setOnClickListener {
            viewModel.selectTodayFilter()
        }

        binding.btnFilterRange.setOnClickListener {
            openDateRangePicker()
        }

        binding.btnFilterAll.setOnClickListener {
            viewModel.selectAllFilter()
        }
    }

    private fun openDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.doctor_prescriptions_filter_range))
            .setTheme(R.style.ThemeOverlay_MerkeziSaglik_DateRangePicker)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first ?: return@addOnPositiveButtonClickListener
            val end = selection.second ?: return@addOnPositiveButtonClickListener
            viewModel.selectRangeFilter(start, end)
        }

        picker.show(childFragmentManager, DATE_RANGE_PICKER_TAG)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: DoctorPrescriptionsUiState) {
        binding.progressLoading.isVisible = state.isLoading
        binding.layoutContent.isVisible = !state.isLoading

        binding.tvActiveFilterValue.text = state.activeFilterLabel

        updateFilterButtonStates(state.selectedFilter)
        adapter.submitList(state.prescriptions)

        val isEmpty = !state.isLoading && state.prescriptions.isEmpty()
        binding.layoutEmpty.isVisible = isEmpty
        binding.tvEmptyMessage.text = state.emptyMessage

        state.errorMessage?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    private fun updateFilterButtonStates(selectedFilter: DoctorPrescriptionFilter) {
        binding.btnFilterToday.isSelected = selectedFilter == DoctorPrescriptionFilter.TODAY
        binding.btnFilterRange.isSelected = selectedFilter == DoctorPrescriptionFilter.RANGE
        binding.btnFilterAll.isSelected = selectedFilter == DoctorPrescriptionFilter.ALL
    }

    private fun showPrescriptionPreviewDialog(
        personName: String,
        prescription: Prescription
    ) {
        if (resources.configuration.smallestScreenWidthDp >= TABLET_WIDTH_DP) {
            showPrescriptionAsCenteredDialog(personName, prescription)
        } else {
            showPrescriptionAsBottomSheet(personName, prescription)
        }
    }

    private fun showPrescriptionAsBottomSheet(
        personName: String,
        prescription: Prescription
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogPrescriptionPreviewBinding.inflate(layoutInflater)
        bindPrescriptionPreview(dialogBinding, personName, prescription)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.btnClosePreview.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showPrescriptionAsCenteredDialog(
        personName: String,
        prescription: Prescription
    ) {
        val dialogBinding = DialogPrescriptionPreviewBinding.inflate(layoutInflater)
        bindPrescriptionPreview(dialogBinding, personName, prescription)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnClosePreview.setOnClickListener { dialog.dismiss() }
        dialog.show()
        DialogWindowSizer.applyCenteredDialogBounds(dialog, dialogBinding.root, requireContext())
    }

    private fun bindPrescriptionPreview(
        dialogBinding: DialogPrescriptionPreviewBinding,
        personName: String,
        prescription: Prescription
    ) {
        dialogBinding.tvPersonNameLabel.text =
            getString(R.string.doctor_appointments_patient_name_label)
        dialogBinding.tvPrescriptionCodeValue.text = prescription.prescriptionCode
        dialogBinding.tvPatientNameValue.text = personName

        dialogBinding.tvCreatedAtValue.text = if (prescription.createdAtMillis > 0L) {
            Instant.ofEpochMilli(prescription.createdAtMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(PREVIEW_DATE_TIME_FORMATTER)
        } else {
            getString(R.string.doctor_appointments_unknown_date)
        }
        dialogBinding.tvMedicinesTitle.text = getString(
            R.string.doctor_appointments_medicines_label_with_count,
            prescription.medicines.size
        )

        // Populate medicines as individual cards
        dialogBinding.layoutMedicines.removeAllViews()
        if (prescription.medicines.isEmpty()) {
            val emptyText = android.widget.TextView(requireContext()).apply {
                text = getString(R.string.doctor_appointments_no_medicines)
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 14f
                setPadding(0, 16, 0, 16)
            }
            dialogBinding.layoutMedicines.addView(emptyText)
        } else {
            prescription.medicines.forEach { medicine ->
                val medicineBinding = com.menasy.merkezisagliksistemi.databinding.ItemMedicinePreviewBinding.inflate(
                    layoutInflater,
                    dialogBinding.layoutMedicines,
                    false
                )
                medicineBinding.tvMedicineName.text = medicine.medicineName
                medicineBinding.tvMedicineDosage.text = medicine.dosage.ifBlank { "-" }
                medicineBinding.tvMedicineFrequency.text = medicine.frequency.ifBlank { "-" }
                medicineBinding.tvMedicineUsage.text = medicine.usageDescription.ifBlank { "-" }

                if (medicine.doctorNote.isNotBlank()) {
                    medicineBinding.layoutMedicineNote.visibility = View.VISIBLE
                    medicineBinding.tvMedicineNote.text = medicine.doctorNote
                } else {
                    medicineBinding.layoutMedicineNote.visibility = View.GONE
                }

                dialogBinding.layoutMedicines.addView(medicineBinding.root)
            }
        }

        // Doctor note section
        val hasNote = prescription.note.isNotBlank()
        dialogBinding.layoutDoctorNote.isVisible = hasNote
        if (hasNote) {
            dialogBinding.tvDoctorNoteValue.text = prescription.note
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DATE_RANGE_PICKER_TAG = "doctor_prescriptions_range_picker"
        const val TABLET_WIDTH_DP = 600

        val PREVIEW_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
    }
}
