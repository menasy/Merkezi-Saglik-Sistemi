package com.menasy.merkezisagliksistemi.ui.doctor.examination

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.data.model.Medicine
import com.menasy.merkezisagliksistemi.databinding.DialogMarkPatientMissedBinding
import com.menasy.merkezisagliksistemi.databinding.DialogMedicineNoteEditBinding
import com.menasy.merkezisagliksistemi.databinding.FragmentDoctorExaminationBinding
import com.menasy.merkezisagliksistemi.di.ServiceLocator
import com.menasy.merkezisagliksistemi.ui.common.base.BaseFragment
import com.menasy.merkezisagliksistemi.ui.common.message.UiMessage
import com.menasy.merkezisagliksistemi.ui.common.util.DialogWindowSizer
import kotlinx.coroutines.launch

class DoctorExaminationFragment : BaseFragment() {

    private var _binding: FragmentDoctorExaminationBinding? = null
    private val binding get() = _binding!!

    private lateinit var medicinesAdapter: SelectedMedicinesAdapter

    private val viewModel: DoctorExaminationViewModel by viewModels {
        DoctorExaminationViewModel.Factory(
            getDoctorExaminationAppointmentUseCase = ServiceLocator.provideGetDoctorExaminationAppointmentUseCase(),
            updateDoctorAppointmentResultUseCase = ServiceLocator.provideUpdateDoctorAppointmentResultUseCase(),
            getMedicinesUseCase = ServiceLocator.provideGetMedicinesUseCase()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorExaminationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeUiEvents(viewModel.uiEvents)
        setupMedicineSelectionResultListener()
        setupMedicinesAdapter()
        setupInputListeners()
        setupButtons()
        observeUiState()

        val appointmentId = arguments?.getString(ARG_APPOINTMENT_ID)
        val prefetchedPatientName = arguments?.getString(ARG_PREFETCHED_PATIENT_NAME)
        viewModel.initialize(
            appointmentId = appointmentId,
            prefetchedPatientName = prefetchedPatientName
        )
    }

    private fun setupMedicinesAdapter() {
        medicinesAdapter = SelectedMedicinesAdapter(
            onEditMedicineNote = { medicine ->
                showMedicineNoteDialog(medicine)
            },
            onRemoveMedicine = { medicine ->
                viewModel.removeMedicine(medicine.medicineId)
            }
        )
        binding.rvSelectedMedicines.adapter = medicinesAdapter
    }

    private fun setupMedicineSelectionResultListener() {
        childFragmentManager.setFragmentResultListener(
            MedicineSelectionBottomSheet.RESULT_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedIds =
                bundle.getStringArrayList(MedicineSelectionBottomSheet.RESULT_SELECTED_MEDICINE_IDS)
                    .orEmpty()
            selectedIds.forEach(viewModel::addMedicine)
        }
    }

    private fun setupInputListeners() {
        binding.etExaminationNote.doAfterTextChanged { value ->
            viewModel.onExaminationNoteChanged(value?.toString().orEmpty())
        }
        binding.etDoctorNote.doAfterTextChanged { value ->
            viewModel.onDoctorNoteChanged(value?.toString().orEmpty())
        }

        binding.rgPrescriptionDecision.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbPrescriptionRequiredYes -> {
                    viewModel.onPrescriptionDecisionChanged(PrescriptionDecision.REQUIRED)
                }

                R.id.rbPrescriptionRequiredNo -> {
                    viewModel.onPrescriptionDecisionChanged(PrescriptionDecision.NOT_REQUIRED)
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnBackExamination.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAddMedicine.setOnClickListener {
            showMedicineSelectionBottomSheet()
        }

        binding.btnMarkPatientMissed.setOnClickListener {
            showMarkPatientMissedDialog()
        }

        binding.btnCompleteExamination.setOnClickListener {
            viewModel.completeExamination()
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

    private fun renderState(state: DoctorExaminationUiState) {
        binding.progressLoading.isVisible = state.isLoading || state.isSubmitting
        binding.scrollContent.isVisible = !state.isLoading

        binding.tvPatientName.text = state.patientName
        binding.tvAppointmentDate.text = state.appointmentDateLabel
        binding.tvAppointmentTime.text = state.appointmentTimeLabel
        binding.tvHospitalName.text = state.hospitalName
        binding.tvBranchName.text = state.branchName
        binding.tvDoctorName.text = state.doctorName

        binding.layoutMedicineSection.isVisible = state.showMedicineSection
        medicinesAdapter.submitList(state.selectedMedicines)
        binding.tvNoMedicineSelected.isVisible =
            state.showMedicineSection && state.selectedMedicines.isEmpty()
        binding.rvSelectedMedicines.isVisible =
            state.showMedicineSection && state.selectedMedicines.isNotEmpty()

        if (binding.etExaminationNote.text?.toString() != state.examinationNote) {
            binding.etExaminationNote.setText(state.examinationNote)
        }
        if (binding.etDoctorNote.text?.toString() != state.doctorNote) {
            binding.etDoctorNote.setText(state.doctorNote)
        }

        val selectedDecisionId = when (state.prescriptionDecision) {
            PrescriptionDecision.REQUIRED -> R.id.rbPrescriptionRequiredYes
            PrescriptionDecision.NOT_REQUIRED -> R.id.rbPrescriptionRequiredNo
            null -> View.NO_ID
        }
        if (binding.rgPrescriptionDecision.checkedRadioButtonId != selectedDecisionId) {
            if (selectedDecisionId == View.NO_ID) {
                binding.rgPrescriptionDecision.clearCheck()
            } else {
                binding.rgPrescriptionDecision.check(selectedDecisionId)
            }
        }

        val canInteract = state.isAccessible && !state.isLoading && !state.isSubmitting
        binding.btnMarkPatientMissed.isEnabled = canInteract
        binding.btnCompleteExamination.isEnabled = canInteract
        binding.btnAddMedicine.isEnabled = canInteract && state.showMedicineSection
        binding.etExaminationNote.isEnabled = canInteract
        binding.etDoctorNote.isEnabled = canInteract
        binding.rbPrescriptionRequiredYes.isEnabled = canInteract
        binding.rbPrescriptionRequiredNo.isEnabled = canInteract

        if (state.shouldCloseScreen) {
            viewModel.consumeCloseSignal()
            findNavController().popBackStack()
        }
    }

    private fun showMedicineSelectionBottomSheet() {
        val selectableMedicines = viewModel.getSelectableMedicines()
        if (selectableMedicines.isEmpty()) {
            showMessage(
                UiMessage.info(
                    title = getString(R.string.doctor_examination_medicine_dialog_title),
                    description = getString(R.string.doctor_examination_all_medicines_added)
                )
            )
            return
        }

        MedicineSelectionBottomSheet.newInstance(
            medicines = selectableMedicines
        ).show(childFragmentManager, MedicineSelectionBottomSheet.TAG)
    }

    private fun showMedicineSelectionDialog() {
        showMedicineSelectionBottomSheet()
    }

    private fun showMarkPatientMissedDialog() {
        val dialogBinding = DialogMarkPatientMissedBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            viewModel.markPatientMissed()
            dialog.dismiss()
        }

        dialog.show()
        DialogWindowSizer.applyCenteredDialogBounds(dialog, dialogBinding.root, requireContext())
    }

    private fun showMedicineNoteDialog(medicine: Medicine) {
        val dialogBinding = DialogMedicineNoteEditBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = getString(
            R.string.doctor_examination_medicine_note_dialog_title,
            medicine.medicineName
        )
        dialogBinding.etMedicineNote.setText(medicine.doctorNote)
        dialogBinding.etMedicineNote.setSelection(dialogBinding.etMedicineNote.text?.length ?: 0)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.btnSave.setOnClickListener {
            viewModel.updateMedicineDoctorNote(
                medicineId = medicine.medicineId,
                doctorNote = dialogBinding.etMedicineNote.text?.toString().orEmpty()
            )
            dialog.dismiss()
        }

        dialog.show()
        DialogWindowSizer.applyCenteredDialogBounds(dialog, dialogBinding.root, requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_APPOINTMENT_ID = "appointmentId"
        const val ARG_PREFETCHED_PATIENT_NAME = "prefetchedPatientName"
    }
}
