package com.menasy.merkezisagliksistemi.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentSplashBinding
import com.menasy.merkezisagliksistemi.ui.common.base.BaseFragment
import kotlinx.coroutines.launch

class SplashFragment : BaseFragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SplashViewModel by viewModels {
        SplashViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeUiEvents(viewModel.uiEvents)
        observeNavigationState()
        viewModel.initializeApp()
    }

    private fun observeNavigationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.navigationState.collect { state ->
                when (state) {
                    is SplashNavigationState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    is SplashNavigationState.GoToLogin -> {
                        binding.progressBar.visibility = View.GONE
                        navigateIfCurrent(R.id.action_splashFragment_to_loginFragment)
                    }

                    is SplashNavigationState.GoToPatientHome -> {
                        binding.progressBar.visibility = View.GONE
                        navigateIfCurrent(R.id.action_splashFragment_to_patientHomeFragment)
                    }

                    is SplashNavigationState.GoToDoctorHome -> {
                        binding.progressBar.visibility = View.GONE
                        navigateIfCurrent(R.id.action_splashFragment_to_doctorHomeFragment)
                    }
                }
            }
        }
    }

    private fun navigateIfCurrent(actionId: Int) {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.splashFragment) {
            navController.navigate(actionId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
