package com.menasy.merkezisagliksistemi.ui.patient.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.menasy.merkezisagliksistemi.databinding.FragmentPatientHomeBinding

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

        binding.tvTitle.text = "Hasta Ana Sayfa"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}