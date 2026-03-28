package com.menasy.merkezisagliksistemi.ui.patient.appointment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentDoctorAvailabilityBinding
import com.menasy.merkezisagliksistemi.ui.common.base.BaseFragment
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import kotlinx.coroutines.launch

class DoctorAvailabilityFragment : BaseFragment() {

    private var _binding: FragmentDoctorAvailabilityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoctorAvailabilityViewModel by viewModels {
        DoctorAvailabilityViewModelFactory()
    }

    private lateinit var slotsAdapter: DoctorDaySlotsAdapter
    private var availabilityArgs: DoctorAvailabilityArgs? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        availabilityArgs = DoctorAvailabilityArgs.fromBundle(arguments)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorAvailabilityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (availabilityArgs == null) {
            showError(AppErrorReason.DOCTOR_AVAILABILITY_MISSING)
            findNavController().navigateUp()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupActions()
        observeUiEvents(viewModel.uiEvents)
        observeUiState()
        viewModel.load(availabilityArgs!!)
    }

    private fun setupToolbar() {
        binding.toolbarDoctorAvailability.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        slotsAdapter = DoctorDaySlotsAdapter(
            onHourSelected = { dayIndex, hourIndex ->
                viewModel.onHourSelected(dayIndex, hourIndex)
            },
            onSlotSelected = { dayIndex, timeLabel ->
                viewModel.onSlotSelected(dayIndex, timeLabel)
            }
        )

        binding.rvDoctorDays.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = slotsAdapter
        }
    }

    private fun setupActions() {
        binding.btnContinueToConfirmation.setOnClickListener {
            val confirmationArgs = viewModel.buildConfirmationArgs() ?: return@setOnClickListener
            navigateToConfirmation(confirmationArgs)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.tvDoctorName.text = state.doctorName
                binding.tvHospitalName.text = state.hospitalName
                binding.tvBranchName.text = state.branchName
                binding.tvSelectedSlotValue.text = state.selectedSummaryText ?: "Henüz seçilmedi"
                binding.btnContinueToConfirmation.isEnabled =
                    state.selectedDateMillis != null && state.selectedTimeLabel != null

                slotsAdapter.submitList(state.dayAvailabilities)
            }
        }
    }

    private fun navigateToConfirmation(confirmationArgs: AppointmentConfirmationArgs) {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.doctorAvailabilityFragment) {
            navController.navigate(
                R.id.action_doctorAvailabilityFragment_to_appointmentConfirmationFragment,
                confirmationArgs.toBundle()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
