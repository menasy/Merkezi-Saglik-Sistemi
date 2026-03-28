package com.menasy.merkezisagliksistemi.ui.patient.appointments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentPatientAppointmentsBinding

class PatientAppointmentsFragment : Fragment() {

    private var _binding: FragmentPatientAppointmentsBinding? = null
    private val binding get() = _binding!!

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

        binding.btnStartAppointmentFlow.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.patientAppointmentsFragment) {
                findNavController().navigate(R.id.action_patientAppointmentsFragment_to_appointmentSearchFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
