package com.menasy.merkezisagliksistemi.ui.doctor.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentDoctorHomeBinding
import com.menasy.merkezisagliksistemi.di.ServiceLocator
import com.menasy.merkezisagliksistemi.domain.usecase.DoctorHomeSummary
import com.menasy.merkezisagliksistemi.ui.common.state.UiState
import kotlinx.coroutines.launch

class DoctorHomeFragment : Fragment() {

    private var _binding: FragmentDoctorHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoctorHomeViewModel by viewModels {
        DoctorHomeViewModel.Factory(
            getDoctorHomeSummaryUseCase = ServiceLocator.provideGetDoctorHomeSummaryUseCase()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        // This ensures the summary is updated after appointments are booked
        viewModel.refresh()
    }

    private fun setupClickListeners() {
        binding.btnRetry.setOnClickListener {
            viewModel.refresh()
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

    private fun renderState(state: DoctorHomeUiState) {
        when (val summaryState = state.summaryState) {
            is UiState.Loading -> showLoading()
            is UiState.Success -> showContent(summaryState.data)
            is UiState.Error -> showError(summaryState.message)
            is UiState.Empty -> showError("Veri bulunamadi")
        }
    }

    private fun showLoading() {
        binding.progressLoading.isVisible = true
        binding.nsvDoctorHome.isVisible = false
        binding.layoutError.isVisible = false
    }

    private fun showContent(summary: DoctorHomeSummary) {
        binding.progressLoading.isVisible = false
        binding.layoutError.isVisible = false
        binding.nsvDoctorHome.isVisible = true

        binding.tvDoctorName.text = summary.doctorFullName
        binding.tvBranchBadge.text = summary.branchName.ifBlank {
            getString(R.string.doctor_home_branch_unknown)
        }
        binding.tvHospitalName.text = summary.hospitalName.ifBlank {
            getString(R.string.doctor_home_hospital_unknown)
        }
        binding.tvPendingCount.text = summary.pendingAppointmentCount.toString()
        binding.tvCompletedCount.text = summary.completedTodayCount.toString()
    }

    private fun showError(message: String) {
        binding.progressLoading.isVisible = false
        binding.nsvDoctorHome.isVisible = false
        binding.layoutError.isVisible = true
        binding.tvErrorMessage.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
