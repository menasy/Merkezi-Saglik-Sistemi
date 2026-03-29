package com.menasy.merkezisagliksistemi.ui.doctor.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentDoctorHomeBinding
import com.menasy.merkezisagliksistemi.di.SessionCache

class DoctorHomeFragment : Fragment() {

    private var _binding: FragmentDoctorHomeBinding? = null
    private val binding get() = _binding!!

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
        bindWelcomeMessage()
    }

    private fun bindWelcomeMessage() {
        val displayName = SessionCache.fullName?.takeIf { it.isNotBlank() }
            ?: SessionCache.userId?.takeIf { it.isNotBlank() }
            ?: getString(R.string.home_welcome_default_user)

        binding.tvWelcomeUser.text = getString(R.string.home_welcome_user_format, displayName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
