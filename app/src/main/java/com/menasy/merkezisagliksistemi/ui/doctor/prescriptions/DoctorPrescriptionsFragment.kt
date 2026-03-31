package com.menasy.merkezisagliksistemi.ui.doctor.prescriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.menasy.merkezisagliksistemi.databinding.FragmentDoctorPrescriptionsBinding

class DoctorPrescriptionsFragment : Fragment() {

    private var _binding: FragmentDoctorPrescriptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorPrescriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
