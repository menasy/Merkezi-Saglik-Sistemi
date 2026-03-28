package com.menasy.merkezisagliksistemi.ui.patient.appointment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.menasy.merkezisagliksistemi.databinding.FragmentAppointmentConfirmationBinding
import kotlinx.coroutines.launch

class AppointmentConfirmationFragment : Fragment() {

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
            Toast.makeText(requireContext(), "Randevu onay bilgisi bulunamadı", Toast.LENGTH_SHORT)
                .show()
            findNavController().navigateUp()
            return
        }

        setupToolbar()
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
            val result = viewModel.confirm()
            result.fold(
                onSuccess = { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        requireContext(),
                        exception.message ?: "Randevu onayı sırasında bir sorun oluştu",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
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

                state.errorMessage?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
