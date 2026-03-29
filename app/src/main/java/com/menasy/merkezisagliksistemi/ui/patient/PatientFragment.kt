package com.menasy.merkezisagliksistemi.ui.patient.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentPatientHomeBinding
import com.menasy.merkezisagliksistemi.di.SessionCache

class PatientHomeFragment : Fragment() {

    private var _binding: FragmentPatientHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindWelcomeMessage()

        binding.btnTakeAppointment.setOnClickListener {
            navigateToAppointmentSearch()
        }
    }

    private fun bindWelcomeMessage() {
        val displayName = SessionCache.fullName?.takeIf { it.isNotBlank() }
            ?: SessionCache.userId?.takeIf { it.isNotBlank() }
            ?: getString(R.string.home_welcome_default_user)

        binding.tvWelcomeUser.text = getString(R.string.home_welcome_user_format, displayName)
    }

    private fun navigateToAppointmentSearch() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.patientHomeFragment) {
            navController.navigate(R.id.action_patientHomeFragment_to_appointmentSearchFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
