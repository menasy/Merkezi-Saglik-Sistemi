package com.menasy.merkezisagliksistemi.ui.patient.appointmentlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.databinding.DialogPrescriptionPreviewBinding
import com.menasy.merkezisagliksistemi.databinding.FragmentPatientAppointmentsBinding
import com.menasy.merkezisagliksistemi.databinding.ItemMedicinePreviewBinding
import com.menasy.merkezisagliksistemi.di.ServiceLocator
import com.menasy.merkezisagliksistemi.ui.common.util.DialogWindowSizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class PatientAppointmentsFragment : Fragment() {

    private var _binding: FragmentPatientAppointmentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PatientAppointmentsViewModel by viewModels {
        PatientAppointmentsViewModel.Factory(
            observePatientAppointmentsUseCase = ServiceLocator.provideObservePatientAppointmentsUseCase(),
            cancelAppointmentUseCase = ServiceLocator.provideCancelAppointmentUseCase(),
            getCurrentUserUseCase = ServiceLocator.provideGetCurrentUserUseCase(),
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
        _binding = FragmentPatientAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupTabs()
        observeUiState()
    }

    private fun setupAdapter() {
        adapter = PatientAppointmentsAdapter(
            onActionClick = { appointment ->
                handleAppointmentAction(appointment)
            }
        )
        binding.rvAppointments.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabActive.setOnClickListener {
            viewModel.selectTab(AppointmentTab.ACTIVE)
        }

        binding.tabPast.setOnClickListener {
            viewModel.selectTab(AppointmentTab.PAST)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Loading
                binding.progressBar.isVisible =
                    state.isLoading || state.isCancelling ||
                        (state.selectedTab == AppointmentTab.PAST && state.isPrescriptionLoading)

                // Tab selection
                updateTabSelection(state.selectedTab)

                // Appointments list
                val appointments = when (state.selectedTab) {
                    AppointmentTab.ACTIVE -> state.activeAppointments
                    AppointmentTab.PAST -> state.pastAppointments
                }
                adapter.submitList(appointments)

                // Empty state
                val isEmpty = appointments.isEmpty() && !state.isLoading
                binding.layoutEmpty.isVisible = isEmpty
                binding.rvAppointments.isVisible = !isEmpty
                binding.tvEmptyMessage.text = state.emptyMessage

                // Error handling
                state.errorMessage?.let { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun updateTabSelection(selectedTab: AppointmentTab) {
        when (selectedTab) {
            AppointmentTab.ACTIVE -> {
                binding.tvTabActive.setTextColor(requireContext().getColor(R.color.primary))
                binding.tvTabPast.setTextColor(requireContext().getColor(R.color.text_secondary))
                binding.indicatorActive.visibility = View.VISIBLE
                binding.indicatorPast.visibility = View.INVISIBLE
            }

            AppointmentTab.PAST -> {
                binding.tvTabActive.setTextColor(requireContext().getColor(R.color.text_secondary))
                binding.tvTabPast.setTextColor(requireContext().getColor(R.color.primary))
                binding.indicatorActive.visibility = View.INVISIBLE
                binding.indicatorPast.visibility = View.VISIBLE
            }
        }
    }

    private fun handleAppointmentAction(item: PatientAppointmentItem) {
        when (item.actionType) {
            AppointmentActionType.CANCEL -> {
                viewModel.cancelAppointment(item)
            }

            AppointmentActionType.VIEW_PRESCRIPTION -> {
                val prescription = item.prescription ?: return
                showPrescriptionPreviewDialog(
                    doctorName = item.doctorName,
                    prescription = prescription
                )
            }

            else -> Unit
        }
    }

    private fun showPrescriptionPreviewDialog(
        doctorName: String,
        prescription: Prescription
    ) {
        if (resources.configuration.smallestScreenWidthDp >= TABLET_WIDTH_DP) {
            showPrescriptionAsCenteredDialog(doctorName, prescription)
        } else {
            showPrescriptionAsBottomSheet(doctorName, prescription)
        }
    }

    private fun showPrescriptionAsBottomSheet(
        doctorName: String,
        prescription: Prescription
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogPrescriptionPreviewBinding.inflate(layoutInflater)
        bindPrescriptionPreview(dialogBinding, doctorName, prescription)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.btnClosePreview.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showPrescriptionAsCenteredDialog(
        doctorName: String,
        prescription: Prescription
    ) {
        val dialogBinding = DialogPrescriptionPreviewBinding.inflate(layoutInflater)
        bindPrescriptionPreview(dialogBinding, doctorName, prescription)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnClosePreview.setOnClickListener { dialog.dismiss() }
        dialog.show()
        DialogWindowSizer.applyCenteredDialogBounds(dialog, dialogBinding.root, requireContext())
    }

    private fun bindPrescriptionPreview(
        dialogBinding: DialogPrescriptionPreviewBinding,
        doctorName: String,
        prescription: Prescription
    ) {
        dialogBinding.tvPersonNameLabel.text =
            getString(R.string.doctor_appointments_doctor_name_label)
        dialogBinding.tvPrescriptionCodeValue.text = prescription.prescriptionCode
        dialogBinding.tvPatientNameValue.text = doctorName

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
                val medicineBinding = ItemMedicinePreviewBinding.inflate(
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
        const val TABLET_WIDTH_DP = 600

        val PREVIEW_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
    }
}
