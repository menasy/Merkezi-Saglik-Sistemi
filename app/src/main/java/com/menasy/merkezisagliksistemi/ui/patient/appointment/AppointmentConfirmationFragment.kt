package com.menasy.merkezisagliksistemi.ui.patient.appointment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.menasy.merkezisagliksistemi.databinding.FragmentAppointmentConfirmationBinding
import com.menasy.merkezisagliksistemi.ui.common.base.BaseFragment
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import kotlinx.coroutines.launch

class AppointmentConfirmationFragment : BaseFragment() {

    private var _binding: FragmentAppointmentConfirmationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppointmentConfirmationViewModel by viewModels {
        AppointmentConfirmationViewModelFactory()
    }

    private var confirmationArgs: AppointmentConfirmationArgs? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        confirmationArgs = AppointmentConfirmationArgs.fromBundle(arguments)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (confirmationArgs == null) {
            showError(AppErrorReason.APPOINTMENT_CONFIRMATION_MISSING)
            findNavController().navigateUp()
            return
        }

        setupToolbar()
        observeUiEvents(viewModel.uiEvents)
        observeUiState()
        setupActions()
        viewModel.load(confirmationArgs!!)
    }

    private fun setupToolbar() {
        binding.toolbarAppointmentConfirmation.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupActions() {
        binding.btnConfirmAppointment.setOnClickListener {
            viewModel.confirm()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.tvConfirmDoctorValue.text = state.doctorName
                binding.tvConfirmHospitalValue.text = state.hospitalName
                binding.tvConfirmBranchValue.text = state.branchName
                binding.tvConfirmDateValue.text = state.dateLabel
                binding.tvConfirmTimeValue.text = state.timeLabel
                binding.tvConfirmPatientValue.text = state.patientName
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
