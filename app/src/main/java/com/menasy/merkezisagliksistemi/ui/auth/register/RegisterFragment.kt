package com.menasy.merkezisagliksistemi.ui.auth.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.menasy.merkezisagliksistemi.databinding.FragmentRegisterBinding
import com.menasy.merkezisagliksistemi.ui.common.state.UiState
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGenderDropdown()
        setupClickListeners()
        observeRegisterState()
    }

    private fun setupGenderDropdown() {
        val genderList = listOf("Erkek", "Kadın")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            genderList
        )
        binding.actvGender.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val tcNo = binding.etTcNo.text.toString().trim()
            val birthDate = binding.etBirthDate.text.toString().trim()
            val gender = binding.actvGender.text.toString().trim()

            viewModel.registerPatient(
                fullName = fullName,
                email = email,
                password = password,
                tcNo = tcNo,
                birthDate = birthDate,
                gender = gender
            )
        }

        binding.tvGoLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeRegisterState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registerState.collect { state ->
                when (state) {
                    is UiState.Empty -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Kayıt başarılı. Giriş yapabilirsiniz.",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.clearState()
                        findNavController().navigateUp()
                    }

                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}