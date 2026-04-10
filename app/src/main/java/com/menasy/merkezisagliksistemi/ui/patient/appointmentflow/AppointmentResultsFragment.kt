package com.menasy.merkezisagliksistemi.ui.patient.appointmentflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentAppointmentResultsBinding
import com.menasy.merkezisagliksistemi.ui.common.base.BaseFragment
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.utils.DateTimeUtils
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

class AppointmentResultsFragment : BaseFragment() {

    private var _binding: FragmentAppointmentResultsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppointmentResultsViewModel by viewModels {
        AppointmentResultsViewModelFactory()
    }

    private lateinit var resultAdapter: AppointmentResultsAdapter
    private var searchArgs: AppointmentSearchArgs? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchArgs = AppointmentSearchArgs.fromBundle(arguments)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (searchArgs == null) {
            showError(AppErrorReason.SEARCH_CRITERIA_MISSING)
            findNavController().navigateUp()
            return
        }

        setupToolbar()
        setupRecyclerView()
        observeUiEvents(viewModel.uiEvents)
        observeUiState()
        bindSearchRange()
        viewModel.loadAppointments(searchArgs!!)
    }

    private fun setupToolbar() {
        binding.btnBackResults.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        resultAdapter = AppointmentResultsAdapter { selectedItem ->
            navigateToDoctorAvailability(selectedItem)
        }

        binding.rvAppointmentResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = resultAdapter
        }
    }

    private fun bindSearchRange() {
        val args = searchArgs ?: return
        val start = formatDate(args.startDateMillis)
        val end = formatDate(args.endDateMillis)
        binding.tvResultDateRange.text = "$start - $end"
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.tvResultSummary.text = state.resultSummary
                binding.tvEmptyState.text = state.emptyMessage ?: ""
                binding.tvEmptyState.visibility = if (state.emptyMessage != null) View.VISIBLE else View.GONE
                binding.rvAppointmentResults.visibility =
                    if (state.appointments.isNotEmpty()) View.VISIBLE else View.GONE

                resultAdapter.submitList(state.appointments)
            }
        }
    }

    private fun navigateToDoctorAvailability(item: AppointmentResultUiModel) {
        val args = searchArgs ?: return
        val availabilityArgs = DoctorAvailabilityArgs(
            searchArgs = args,
            doctorId = item.doctorId,
            doctorName = item.doctorName,
            hospitalId = item.hospitalId,
            hospitalName = item.hospitalName,
            branchId = item.branchId,
            branchName = item.branchName,
            slotStartHour = item.slotStartHour,
            slotEndHour = item.slotEndHour,
            slotDurationMinutes = item.slotDurationMinutes
        )

        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.appointmentResultsFragment) {
            navController.navigate(
                R.id.action_appointmentResultsFragment_to_doctorAvailabilityFragment,
                availabilityArgs.toBundle()
            )
        }
    }

    private fun formatDate(dateMillis: Long): String {
        val localDate = DateTimeUtils.millisToLocalDate(dateMillis)
        return DATE_FORMATTER.format(localDate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("tr-TR"))
    }
}
