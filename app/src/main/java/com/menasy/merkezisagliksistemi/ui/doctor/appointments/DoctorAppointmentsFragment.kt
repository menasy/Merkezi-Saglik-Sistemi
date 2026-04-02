package com.menasy.merkezisagliksistemi.ui.doctor.appointments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.databinding.DialogPrescriptionPreviewBinding
import com.menasy.merkezisagliksistemi.databinding.FragmentDoctorAppointmentsBinding
import com.menasy.merkezisagliksistemi.di.ServiceLocator
import com.menasy.merkezisagliksistemi.ui.common.util.DialogWindowSizer
import com.menasy.merkezisagliksistemi.ui.doctor.examination.DoctorExaminationFragment
import com.menasy.merkezisagliksistemi.ui.patient.appointmentlist.AppointmentActionType
import com.menasy.merkezisagliksistemi.ui.patient.appointmentlist.PatientAppointmentItem
import com.menasy.merkezisagliksistemi.ui.patient.appointmentlist.PatientAppointmentsAdapter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class DoctorAppointmentsFragment : Fragment() {

    private var _binding: FragmentDoctorAppointmentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoctorAppointmentsViewModel by viewModels {
        DoctorAppointmentsViewModel.Factory(
            observeDoctorAppointmentsUseCase = ServiceLocator.provideObserveDoctorAppointmentsUseCase(),
            getUserFullNamesByIdsUseCase = ServiceLocator.provideGetUserFullNamesByIdsUseCase(),
            getPrescriptionPreviewsByAppointmentIdsUseCase = ServiceLocator.provideGetPrescriptionPreviewsByAppointmentIdsUseCase(),
            appointmentMapper = ServiceLocator.provideAppointmentMapper()
        )
    }

    private lateinit var adapter: PatientAppointmentsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupTabs()
        setupDateSelection()
        observeUiState()
    }

    private fun setupAdapter() {
        adapter = PatientAppointmentsAdapter(
            onActionClick = { item ->
                handleAppointmentAction(item)
            }
        )
        binding.rvAppointments.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabPending.setOnClickListener {
            viewModel.selectTab(DoctorAppointmentTab.PENDING)
        }
        binding.tabUpcoming.setOnClickListener {
            viewModel.selectTab(DoctorAppointmentTab.UPCOMING)
        }
        binding.tabPast.setOnClickListener {
            viewModel.selectTab(DoctorAppointmentTab.PAST)
        }
    }

    private fun setupDateSelection() {
        binding.btnSelectDate.setOnClickListener {
            val state = viewModel.uiState.value
            val currentSelection = state.selectedDate
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.doctor_appointments_date_picker_title))
                .setSelection(currentSelection)
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                viewModel.onDateSelected(selection)
            }
            picker.show(childFragmentManager, DATE_PICKER_TAG)
        }
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

    private fun renderState(state: DoctorAppointmentsUiState) {
        binding.progressBar.isVisible = state.isLoading
        binding.progressPrescription.isVisible =
            state.isPrescriptionLoading && state.selectedTab == DoctorAppointmentTab.PAST

        updateTabSelection(state.selectedTab)
        updateSectionInfo(state)

        val list = when (state.selectedTab) {
            DoctorAppointmentTab.PENDING -> state.pendingAppointments
            DoctorAppointmentTab.UPCOMING -> state.upcomingAppointments
            DoctorAppointmentTab.PAST -> state.pastAppointments
        }
        adapter.submitList(list)

        val isEmpty = list.isEmpty() && !state.isLoading
        binding.layoutEmpty.isVisible = isEmpty
        binding.rvAppointments.isVisible = !isEmpty
        binding.tvEmptyMessage.text = when (state.selectedTab) {
            DoctorAppointmentTab.PENDING -> getString(R.string.doctor_appointments_empty_pending)
            DoctorAppointmentTab.UPCOMING -> getString(R.string.doctor_appointments_empty_upcoming)
            DoctorAppointmentTab.PAST -> getString(R.string.doctor_appointments_empty_past)
        }

        state.errorMessage?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    private fun updateTabSelection(selectedTab: DoctorAppointmentTab) {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val idleColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        // Update text colors
        binding.tvTabPending.setTextColor(
            if (selectedTab == DoctorAppointmentTab.PENDING) selectedColor else idleColor
        )
        binding.tvTabUpcoming.setTextColor(
            if (selectedTab == DoctorAppointmentTab.UPCOMING) selectedColor else idleColor
        )
        binding.tvTabPast.setTextColor(
            if (selectedTab == DoctorAppointmentTab.PAST) selectedColor else idleColor
        )

        // Update indicator visibility
        binding.indicatorPending.visibility =
            if (selectedTab == DoctorAppointmentTab.PENDING) View.VISIBLE else View.INVISIBLE
        binding.indicatorUpcoming.visibility =
            if (selectedTab == DoctorAppointmentTab.UPCOMING) View.VISIBLE else View.INVISIBLE
        binding.indicatorPast.visibility =
            if (selectedTab == DoctorAppointmentTab.PAST) View.VISIBLE else View.INVISIBLE

        binding.layoutDateFilter.isVisible = selectedTab == DoctorAppointmentTab.UPCOMING
    }

    private fun updateSectionInfo(state: DoctorAppointmentsUiState) {
        binding.tvSectionSummary.text = when (state.selectedTab) {
            DoctorAppointmentTab.PENDING -> getString(
                R.string.doctor_appointments_pending_count_format,
                state.pendingCount
            )
            DoctorAppointmentTab.UPCOMING -> getString(
                R.string.doctor_appointments_upcoming_count_format,
                state.upcomingCount
            )
            DoctorAppointmentTab.PAST -> getString(
                R.string.doctor_appointments_past_count_format,
                state.pastCount
            )
        }

        if (state.selectedTab == DoctorAppointmentTab.UPCOMING) {
            val selectedDateLabel = state.selectedDate.format(SECTION_DATE_FORMATTER)
            binding.tvSelectedDate.text = getString(
                R.string.doctor_appointments_selected_date_format,
                selectedDateLabel
            )
        }
    }

    private fun handleAppointmentAction(item: PatientAppointmentItem) {
        when (item.actionType) {
            AppointmentActionType.EXAMINE -> {
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.doctorAppointmentsFragment) {
                    navController.navigate(
                        R.id.action_doctorAppointmentsFragment_to_doctorExaminationFragment,
                        Bundle().apply {
                            putString(DoctorExaminationFragment.ARG_APPOINTMENT_ID, item.id)
                            putString(
                                DoctorExaminationFragment.ARG_PREFETCHED_PATIENT_NAME,
                                item.doctorName
                            )
                        }
                    )
                }
            }
            AppointmentActionType.VIEW_PRESCRIPTION -> {
                val prescription = item.prescription ?: return
                showPrescriptionPreviewDialog(
                    patientName = item.doctorName,
                    prescription = prescription
                )
            }
            else -> Unit
        }
    }

    private fun showPrescriptionPreviewDialog(
        patientName: String,
        prescription: Prescription
    ) {
        if (resources.configuration.smallestScreenWidthDp >= TABLET_WIDTH_DP) {
            showPrescriptionAsCenteredDialog(patientName, prescription)
        } else {
            showPrescriptionAsBottomSheet(patientName, prescription)
        }
    }

    private fun showPrescriptionAsBottomSheet(
        patientName: String,
        prescription: Prescription
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogPrescriptionPreviewBinding.inflate(layoutInflater)
        bindPrescriptionPreview(dialogBinding, patientName, prescription)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.btnClosePreview.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showPrescriptionAsCenteredDialog(
        patientName: String,
        prescription: Prescription
    ) {
        val dialogBinding = DialogPrescriptionPreviewBinding.inflate(layoutInflater)
        bindPrescriptionPreview(dialogBinding, patientName, prescription)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnClosePreview.setOnClickListener { dialog.dismiss() }
        dialog.show()
        DialogWindowSizer.applyCenteredDialogBounds(dialog, dialogBinding.root, requireContext())
    }

    private fun bindPrescriptionPreview(
        dialogBinding: DialogPrescriptionPreviewBinding,
        patientName: String,
        prescription: Prescription
    ) {
        dialogBinding.tvPersonNameLabel.text =
            getString(R.string.doctor_appointments_patient_name_label)
        dialogBinding.tvPrescriptionCodeValue.text = prescription.prescriptionCode
        dialogBinding.tvPatientNameValue.text = patientName

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

        // Build medicine list using modern card layout
        dialogBinding.layoutMedicines.removeAllViews()
        if (prescription.medicines.isEmpty()) {
            dialogBinding.tvMedicinesValue.visibility = View.VISIBLE
            dialogBinding.tvMedicinesValue.text = getString(R.string.doctor_appointments_no_medicines)
        } else {
            dialogBinding.tvMedicinesValue.visibility = View.GONE
            prescription.medicines.forEach { medicine ->
                val medicineView = layoutInflater.inflate(
                    R.layout.item_medicine_preview,
                    dialogBinding.layoutMedicines,
                    false
                )
                val medicineBinding = com.menasy.merkezisagliksistemi.databinding.ItemMedicinePreviewBinding.bind(medicineView)
                
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
                
                dialogBinding.layoutMedicines.addView(medicineView)
            }
        }

        if (prescription.note.isNotBlank()) {
            dialogBinding.layoutDoctorNote.visibility = View.VISIBLE
            dialogBinding.tvDoctorNoteValue.text = prescription.note
        } else {
            dialogBinding.layoutDoctorNote.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DATE_PICKER_TAG = "doctor_appointments_date_picker"
        const val TABLET_WIDTH_DP = 600

        val SECTION_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr-TR"))

        val PREVIEW_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
    }
}
