package com.menasy.merkezisagliksistemi.ui.patient.appointments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.menasy.merkezisagliksistemi.databinding.FragmentPatientAppointmentsBinding
import com.menasy.merkezisagliksistemi.di.ServiceLocator
import kotlinx.coroutines.launch

class PatientAppointmentsFragment : Fragment() {

    private var _binding: FragmentPatientAppointmentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PatientAppointmentsViewModel by viewModels {
        PatientAppointmentsViewModel.Factory(
            observePatientAppointmentsUseCase = ServiceLocator.provideObservePatientAppointmentsUseCase(),
            cancelAppointmentUseCase = ServiceLocator.provideCancelAppointmentUseCase(),
            getCurrentUserUseCase = ServiceLocator.provideGetCurrentUserUseCase(),
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
            onCancelClick = { appointment ->
                viewModel.cancelAppointment(appointment)
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
                binding.progressBar.visibility = if (state.isLoading || state.isCancelling) View.VISIBLE else View.GONE

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
                binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.rvAppointments.visibility = if (isEmpty) View.GONE else View.VISIBLE
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
                binding.tvTabActive.setTextColor(requireContext().getColor(com.menasy.merkezisagliksistemi.R.color.primary))
                binding.tvTabPast.setTextColor(requireContext().getColor(com.menasy.merkezisagliksistemi.R.color.text_secondary))
                binding.indicatorActive.visibility = View.VISIBLE
                binding.indicatorPast.visibility = View.INVISIBLE
            }
            AppointmentTab.PAST -> {
                binding.tvTabActive.setTextColor(requireContext().getColor(com.menasy.merkezisagliksistemi.R.color.text_secondary))
                binding.tvTabPast.setTextColor(requireContext().getColor(com.menasy.merkezisagliksistemi.R.color.primary))
                binding.indicatorActive.visibility = View.INVISIBLE
                binding.indicatorPast.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
