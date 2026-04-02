package com.menasy.merkezisagliksistemi.ui.doctor.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentDoctorAccountBinding
import com.menasy.merkezisagliksistemi.di.ServiceLocator
import com.menasy.merkezisagliksistemi.di.SessionCache
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorHomeSummaryUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.LogoutUserUseCase
import kotlinx.coroutines.launch

class DoctorAccountFragment : Fragment() {

    private var _binding: FragmentDoctorAccountBinding? = null
    private val binding get() = _binding!!

    private val getCurrentUserUseCase: GetCurrentUserUseCase by lazy {
        ServiceLocator.provideGetCurrentUserUseCase()
    }
    private val getDoctorHomeSummaryUseCase: GetDoctorHomeSummaryUseCase by lazy {
        ServiceLocator.provideGetDoctorHomeSummaryUseCase()
    }
    private val logoutUserUseCase: LogoutUserUseCase by lazy {
        ServiceLocator.provideLogoutUserUseCase()
    }
    private val appointmentRepository by lazy {
        ServiceLocator.provideAppointmentRepository()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindSessionInfo()
        setupLogoutButton()
        refreshUserInfoIfNeeded()
    }

    private fun bindSessionInfo() {
        binding.tvDoctorName.text = SessionCache.fullName?.ifBlank { "-" } ?: "-"
        binding.tvBranchValue.text = getString(R.string.doctor_home_branch_unknown)
        binding.tvHospitalValue.text = getString(R.string.doctor_home_hospital_unknown)
        binding.tvDoctorIdValue.text = SessionCache.doctorId?.ifBlank { "-" } ?: "-"
        binding.tvUidValue.text = SessionCache.userId?.ifBlank { "-" } ?: "-"
        binding.tvExaminationValue.text = "-"
    }

    private fun refreshUserInfoIfNeeded() {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentUserId = getCurrentUserUseCase.getCurrentUserId()
            if (currentUserId.isNullOrBlank()) return@launch

            val cachedRole = SessionCache.role
            val cachedFullName = SessionCache.fullName

            val resolvedRole = if (cachedRole.isNullOrBlank()) {
                getCurrentUserUseCase.getCurrentUserRole().getOrNull()
            } else {
                cachedRole
            }

            val resolvedFullName = if (cachedFullName.isNullOrBlank()) {
                getCurrentUserUseCase.getCurrentUserFullName().getOrNull()
            } else {
                cachedFullName
            }

            when (resolvedRole) {
                "doctor" -> {
                    val doctorId = SessionCache.doctorId
                        ?: getCurrentUserUseCase.getDoctorIdByUserId(currentUserId)
                    if (!doctorId.isNullOrBlank()) {
                        SessionCache.populateDoctor(
                            userId = currentUserId,
                            role = resolvedRole,
                            fullName = resolvedFullName.orEmpty(),
                            doctorId = doctorId
                        )
                    }
                }
                "patient" -> {
                    SessionCache.populate(
                        userId = currentUserId,
                        role = resolvedRole,
                        fullName = resolvedFullName.orEmpty()
                    )
                }
            }

            binding.tvDoctorName.text = resolvedFullName?.ifBlank { "-" } ?: "-"
            val resolvedDoctorId = SessionCache.doctorId?.ifBlank { null }
                ?: getCurrentUserUseCase.getDoctorIdByUserId(currentUserId)
            binding.tvDoctorIdValue.text = resolvedDoctorId ?: "-"
            binding.tvUidValue.text = currentUserId

            if (resolvedRole == "doctor" && !resolvedDoctorId.isNullOrBlank()) {
                val summary = getDoctorHomeSummaryUseCase(resolvedDoctorId).getOrNull()
                if (summary != null) {
                    binding.tvDoctorName.text = summary.doctorFullName.ifBlank {
                        resolvedFullName?.ifBlank { "-" } ?: "-"
                    }
                    binding.tvBranchValue.text = summary.branchName.ifBlank {
                        getString(R.string.doctor_home_branch_unknown)
                    }
                    binding.tvHospitalValue.text = summary.hospitalName.ifBlank {
                        getString(R.string.doctor_home_hospital_unknown)
                    }
                }

                val totalCompleted = appointmentRepository
                    .getDoctorTotalCompletedCount(resolvedDoctorId)
                    .getOrNull() ?: 0
                binding.tvExaminationValue.text = totalCompleted.toString()
            }
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            logoutUserUseCase()
            val navController = findNavController()
            val options = NavOptions.Builder()
                .setPopUpTo(navController.graph.id, true)
                .build()
            navController.navigate(R.id.loginFragment, null, options)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
