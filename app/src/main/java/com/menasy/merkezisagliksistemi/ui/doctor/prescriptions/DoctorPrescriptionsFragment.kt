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
import com.menasy.merkezisagliksistemi.ui.common.util.PrescriptionPreviewDialogBinder
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
                    prescription = item.prescription,
                    examinationNote = item.examinationNote
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
        prescription: Prescription,
        examinationNote: String
    ) {
        if (resources.configuration.smallestScreenWidthDp >= TABLET_WIDTH_DP) {
            showPrescriptionAsCenteredDialog(personName, prescription, examinationNote)
        } else {
            showPrescriptionAsBottomSheet(personName, prescription, examinationNote)
        }
    }

    private fun showPrescriptionAsBottomSheet(
        personName: String,
        prescription: Prescription,
        examinationNote: String
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogPrescriptionPreviewBinding.inflate(layoutInflater)
        bindPrescriptionPreview(dialogBinding, personName, prescription, examinationNote)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.btnClosePreview.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showPrescriptionAsCenteredDialog(
        personName: String,
        prescription: Prescription,
        examinationNote: String
    ) {
        val dialogBinding = DialogPrescriptionPreviewBinding.inflate(layoutInflater)
        bindPrescriptionPreview(dialogBinding, personName, prescription, examinationNote)

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
        prescription: Prescription,
        examinationNote: String
    ) {
        PrescriptionPreviewDialogBinder.bind(
            context = requireContext(),
            inflater = layoutInflater,
            dialogBinding = dialogBinding,
            personNameLabelRes = R.string.doctor_appointments_patient_name_label,
            personName = personName,
            prescription = prescription,
            examinationNote = examinationNote
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DATE_RANGE_PICKER_TAG = "doctor_prescriptions_range_picker"
        const val TABLET_WIDTH_DP = 600
    }
}
