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
import com.menasy.merkezisagliksistemi.di.ServiceLocator
import com.menasy.merkezisagliksistemi.ui.common.util.DialogWindowSizer
import com.menasy.merkezisagliksistemi.ui.common.util.PrescriptionPreviewDialogBinder
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
                    prescription = prescription,
                    examinationNote = item.examinationNote
                )
            }

            else -> Unit
        }
    }

    private fun showPrescriptionPreviewDialog(
        doctorName: String,
        prescription: Prescription,
        examinationNote: String
    ) {
        if (resources.configuration.smallestScreenWidthDp >= TABLET_WIDTH_DP) {
            showPrescriptionAsCenteredDialog(doctorName, prescription, examinationNote)
        } else {
            showPrescriptionAsBottomSheet(doctorName, prescription, examinationNote)
        }
    }

    private fun showPrescriptionAsBottomSheet(
        doctorName: String,
        prescription: Prescription,
        examinationNote: String
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogPrescriptionPreviewBinding.inflate(layoutInflater)
        bindPrescriptionPreview(dialogBinding, doctorName, prescription, examinationNote)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.btnClosePreview.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showPrescriptionAsCenteredDialog(
        doctorName: String,
        prescription: Prescription,
        examinationNote: String
    ) {
        val dialogBinding = DialogPrescriptionPreviewBinding.inflate(layoutInflater)
        bindPrescriptionPreview(dialogBinding, doctorName, prescription, examinationNote)

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
        prescription: Prescription,
        examinationNote: String
    ) {
        PrescriptionPreviewDialogBinder.bind(
            context = requireContext(),
            inflater = layoutInflater,
            dialogBinding = dialogBinding,
            personNameLabelRes = R.string.doctor_appointments_doctor_name_label,
            personName = doctorName,
            prescription = prescription,
            examinationNote = examinationNote
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TABLET_WIDTH_DP = 600
    }
}
