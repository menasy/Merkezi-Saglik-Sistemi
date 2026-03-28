package com.menasy.merkezisagliksistemi.ui.auth.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentLoginBinding
import com.menasy.merkezisagliksistemi.ui.common.base.BaseFragment
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import com.menasy.merkezisagliksistemi.ui.common.state.UiState
import kotlinx.coroutines.launch

class LoginFragment : BaseFragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeUiEvents(viewModel.uiEvents)
        observeLoginState()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.login(email, password)
        }

        binding.tvGoRegister.setOnClickListener {
            navigateIfCurrent(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun observeLoginState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is UiState.Empty -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE

                        when (state.data.role) {
                            "patient" -> {
                                navigateIfCurrent(R.id.action_loginFragment_to_patientHomeFragment)
                            }

                            "doctor" -> {
                                navigateIfCurrent(R.id.action_loginFragment_to_doctorHomeFragment)
                            }

                            else -> {
                                showError(AppErrorReason.INVALID_USER_ROLE)
                            }
                        }

                        viewModel.clearState()
                    }

                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        showError(Throwable(state.message), OperationType.LOGIN)
                        viewModel.clearState()
                    }
                }
            }
        }
    }

    private fun navigateIfCurrent(actionId: Int) {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.loginFragment) {
            navController.navigate(actionId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
