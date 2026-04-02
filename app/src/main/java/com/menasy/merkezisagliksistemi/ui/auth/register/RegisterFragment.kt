package com.menasy.merkezisagliksistemi.ui.auth.register

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.FragmentRegisterBinding
import com.menasy.merkezisagliksistemi.ui.common.base.BaseFragment
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import com.menasy.merkezisagliksistemi.ui.common.state.UiState
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class RegisterFragment : BaseFragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory()
    }

    private val birthDateFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("tr-TR"))

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

        setupKeyboardAwareForm()
        setupGenderDropdown()
        setupBirthDatePicker()
        setupClickListeners()
        observeUiEvents(viewModel.uiEvents)
        observeRegisterState()
    }

    private fun setupGenderDropdown() {
        val genderList = listOf("Erkek", "Kadın")
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown_option,
            android.R.id.text1,
            genderList
        )
        binding.actvGender.setAdapter(adapter)
        binding.actvGender.setDropDownAnchor(binding.tilGender.id)
        binding.actvGender.post {
            val anchorWidth = binding.tilGender.width
            if (anchorWidth > 0) {
                binding.actvGender.dropDownWidth = anchorWidth
            }
        }
    }

    private fun setupBirthDatePicker() {
        binding.etBirthDate.setOnClickListener {
            scrollFieldIntoView(binding.tilBirthDate)
            showBirthDatePicker()
        }
        binding.tilBirthDate.setEndIconOnClickListener {
            scrollFieldIntoView(binding.tilBirthDate)
            showBirthDatePicker()
        }
    }

    private fun setupKeyboardAwareForm() {
        val baseBottomPadding = binding.nsvRegister.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.nsvRegister) { view, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val bottomInset = maxOf(imeBottom, systemBottom)
            view.updatePadding(bottom = baseBottomPadding + bottomInset + dp(6))
            insets
        }
        ViewCompat.requestApplyInsets(binding.nsvRegister)

        setupInputFocusScrolling()
    }

    private fun setupInputFocusScrolling() {
        val fields = listOf(
            binding.etFullName to binding.tilFullName,
            binding.etEmail to binding.tilEmail,
            binding.etPassword to binding.tilPassword,
            binding.etTcNo to binding.tilTcNo
        )

        fields.forEach { (input, container) ->
            input.setOnFocusChangeListener { focusedView, hasFocus ->
                if (hasFocus) {
                    scrollFieldIntoView(container)
                }
            }
        }

        binding.actvGender.setOnClickListener {
            scrollFieldIntoView(binding.tilGender)
            binding.actvGender.showDropDown()
        }
    }

    private fun scrollFieldIntoView(target: View) {
        binding.nsvRegister.post {
            val rect = Rect()
            target.getDrawingRect(rect)
            binding.nsvRegister.offsetDescendantRectToMyCoords(target, rect)

            val currentScrollY = binding.nsvRegister.scrollY
            val visibleTop = currentScrollY + dp(12)
            val visibleBottom = currentScrollY +
                binding.nsvRegister.height -
                binding.nsvRegister.paddingBottom -
                dp(56)

            val delta = when {
                rect.bottom > visibleBottom -> rect.bottom - visibleBottom
                rect.top < visibleTop -> rect.top - visibleTop
                else -> 0
            }

            if (delta != 0) {
                binding.nsvRegister.smoothScrollBy(0, delta)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
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
            navigateBackToLogin()
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
                        viewModel.clearState()
                        navigateBackToLogin()
                    }

                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        showError(Throwable(state.message), OperationType.REGISTER)
                        viewModel.clearState()
                    }
                }
            }
        }
    }

    private fun navigateBackToLogin() {
        val navController = findNavController()
        if (!navController.navigateUp() &&
            navController.currentDestination?.id == R.id.registerFragment
        ) {
            navController.navigate(R.id.loginFragment)
        }
    }

    private fun showBirthDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val selectedDate = selectedBirthDateMillis() ?: MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Doğum tarihi seçin")
            .setSelection(selectedDate)
            .setCalendarConstraints(constraints)
            .setTheme(R.style.ThemeOverlay_MerkeziSaglik_DateRangePicker)
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            binding.etBirthDate.setText(formatBirthDate(millis))
        }

        picker.show(parentFragmentManager, BIRTH_DATE_PICKER_TAG)
    }

    private fun selectedBirthDateMillis(): Long? {
        val text = binding.etBirthDate.text?.toString().orEmpty().trim()
        if (text.isBlank()) return null

        return runCatching {
            val localDate = LocalDate.parse(text, birthDateFormatter)
            localDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    private fun formatBirthDate(millis: Long): String {
        val date = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return birthDateFormatter.format(date)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val BIRTH_DATE_PICKER_TAG = "birth_date_picker"
    }
}
