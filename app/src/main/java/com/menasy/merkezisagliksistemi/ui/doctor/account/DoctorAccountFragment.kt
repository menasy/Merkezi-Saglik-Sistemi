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
import com.menasy.merkezisagliksistemi.domain.usecase.LogoutUserUseCase
import kotlinx.coroutines.launch

class DoctorAccountFragment : Fragment() {

    private var _binding: FragmentDoctorAccountBinding? = null
    private val binding get() = _binding!!
    private val getCurrentUserUseCase: GetCurrentUserUseCase by lazy {
        ServiceLocator.provideGetCurrentUserUseCase()
    }
    private val logoutUserUseCase: LogoutUserUseCase by lazy {
        ServiceLocator.provideLogoutUserUseCase()
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
        binding.tvAccountFullNameValue.text = SessionCache.fullName?.ifBlank { "-" } ?: "-"
        binding.tvAccountRoleValue.text = roleToLabel(SessionCache.role)
        binding.tvAccountUserIdValue.text = SessionCache.userId ?: "-"
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
                    val doctorId = SessionCache.doctorId ?: getCurrentUserUseCase.getDoctorIdByUserId(currentUserId)
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

            binding.tvAccountFullNameValue.text = resolvedFullName?.ifBlank { "-" } ?: "-"
            binding.tvAccountRoleValue.text = roleToLabel(resolvedRole)
            binding.tvAccountUserIdValue.text = currentUserId
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

    private fun roleToLabel(role: String?): String {
        return when (role) {
            "patient" -> "Hasta"
            "doctor" -> "Doktor"
            null, "" -> "-"
            else -> role
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
