package com.menasy.merkezisagliksistemi.ui.patient.appointment

import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.data.model.Branch
import com.menasy.merkezisagliksistemi.data.model.City
import com.menasy.merkezisagliksistemi.data.model.District
import com.menasy.merkezisagliksistemi.data.model.Doctor
import com.menasy.merkezisagliksistemi.data.model.Hospital
import com.menasy.merkezisagliksistemi.domain.usecase.GetBranchesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetCitiesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDistrictsByCityUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetHospitalsByDistrictUseCase
import com.menasy.merkezisagliksistemi.ui.common.base.BaseViewModel
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class AppointmentSearchCriteria(
    val startDateMillis: Long,
    val endDateMillis: Long,
    val cityId: String,
    val districtId: String?,
    val branchId: String,
    val hospitalId: String?,
    val doctorId: String?
)

data class AppointmentSearchUiState(
    val isLoading: Boolean = false,
    val cities: List<City> = emptyList(),
    val districts: List<District> = emptyList(),
    val branches: List<Branch> = emptyList(),
    val hospitals: List<Hospital> = emptyList(),
    val doctors: List<Doctor> = emptyList(),
    val selectedCityId: String? = null,
    val selectedDistrictId: String? = null,
    val selectedBranchId: String? = null,
    val selectedHospitalId: String? = null,
    val selectedDoctorId: String? = null,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val isDoctorFieldEnabled: Boolean = false
)

class AppointmentSearchViewModel(
    private val getCitiesUseCase: GetCitiesUseCase,
    private val getDistrictsByCityUseCase: GetDistrictsByCityUseCase,
    private val getHospitalsByDistrictUseCase: GetHospitalsByDistrictUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val getDoctorsUseCase: GetDoctorsUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AppointmentSearchUiState())
    val uiState: StateFlow<AppointmentSearchUiState> = _uiState.asStateFlow()

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val cities = getCitiesUseCase()
                val branches = getBranchesUseCase()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cities = cities,
                        branches = branches
                    )
                }
            } catch (error: Exception) {
                onDataLoadFailure(error)
            }
        }
    }

    fun onCitySelected(cityId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedCityId = cityId,
                    selectedDistrictId = null,
                    selectedHospitalId = null,
                    selectedDoctorId = null,
                    districts = emptyList(),
                    hospitals = emptyList(),
                    doctors = emptyList(),
                    isDoctorFieldEnabled = false
                )
            }

            try {
                val districts = getDistrictsByCityUseCase(cityId)
                _uiState.update { it.copy(districts = districts) }
                refreshHospitals()
            } catch (error: Exception) {
                onDataLoadFailure(error)
            }
        }
    }

    fun onDistrictSelected(districtId: String?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedDistrictId = districtId,
                    selectedHospitalId = null,
                    selectedDoctorId = null,
                    hospitals = emptyList(),
                    doctors = emptyList(),
                    isDoctorFieldEnabled = false
                )
            }
            refreshHospitals()
        }
    }

    fun onBranchSelected(branchId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedBranchId = branchId,
                    selectedHospitalId = null,
                    selectedDoctorId = null,
                    hospitals = emptyList(),
                    doctors = emptyList(),
                    isDoctorFieldEnabled = false
                )
            }
            refreshHospitals()
        }
    }

    fun onHospitalSelected(hospitalId: String?) {
        viewModelScope.launch {
            val isSpecificHospitalSelected = !hospitalId.isNullOrBlank()

            _uiState.update {
                it.copy(
                    selectedHospitalId = hospitalId,
                    selectedDoctorId = null,
                    doctors = emptyList(),
                    isDoctorFieldEnabled = isSpecificHospitalSelected
                )
            }

            if (isSpecificHospitalSelected) {
                refreshDoctors()
            }
        }
    }

    fun onDoctorSelected(doctorId: String?) {
        _uiState.update { it.copy(selectedDoctorId = doctorId) }
    }

    fun onDateRangeSelected(
        startDateMillis: Long,
        endDateMillis: Long
    ): Boolean {
        val validationError = validateDateRange(startDateMillis, endDateMillis)
        if (validationError != null) {
            publishError(validationError)
            return false
        }

        _uiState.update {
            it.copy(
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis
            )
        }
        return true
    }

    fun buildSearchCriteria(): AppointmentSearchCriteria? {
        val state = _uiState.value
        val cityId = state.selectedCityId ?: return validationFailure(AppErrorReason.CITY_SELECTION_REQUIRED)
        val branchId = state.selectedBranchId ?: return validationFailure(AppErrorReason.BRANCH_SELECTION_REQUIRED)
        val startDate = state.startDateMillis ?: return validationFailure(AppErrorReason.START_DATE_REQUIRED)
        val endDate = state.endDateMillis ?: return validationFailure(AppErrorReason.END_DATE_REQUIRED)

        val validationError = validateDateRange(startDate, endDate)
        if (validationError != null) {
            return validationFailure(validationError)
        }

        return AppointmentSearchCriteria(
            startDateMillis = startDate,
            endDateMillis = endDate,
            cityId = cityId,
            districtId = state.selectedDistrictId,
            branchId = branchId,
            hospitalId = state.selectedHospitalId,
            doctorId = state.selectedDoctorId
        )
    }

    private suspend fun refreshHospitals() {
        val currentState = _uiState.value
        val cityId = currentState.selectedCityId ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        try {
            val hospitals = getHospitalsByDistrictUseCase(
                cityId = cityId,
                districtId = currentState.selectedDistrictId
            )

            val selectedBranchId = currentState.selectedBranchId
            val filteredHospitals = if (selectedBranchId.isNullOrBlank()) {
                hospitals
            } else {
                hospitals.filter { hospital ->
                    hospital.branchIds.isEmpty() || selectedBranchId in hospital.branchIds
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    hospitals = filteredHospitals,
                    selectedHospitalId = null,
                    selectedDoctorId = null,
                    doctors = emptyList(),
                    isDoctorFieldEnabled = false
                )
            }
        } catch (error: Exception) {
            onDataLoadFailure(error)
        }
    }

    private suspend fun refreshDoctors() {
        val currentState = _uiState.value
        val hospitalId = currentState.selectedHospitalId

        if (hospitalId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    doctors = emptyList(),
                    selectedDoctorId = null,
                    isDoctorFieldEnabled = false
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        try {
            val doctors = getDoctorsUseCase(
                hospitalId = hospitalId,
                branchId = currentState.selectedBranchId
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    doctors = doctors,
                    selectedDoctorId = null
                )
            }
        } catch (error: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    doctors = emptyList(),
                    selectedDoctorId = null
                )
            }
            publishError(error, OperationType.FETCH_DATA)
        }
    }

    private fun validateDateRange(startDateMillis: Long, endDateMillis: Long): AppErrorReason? {
        val todayStartMillis = getTodayStartMillis()

        if (startDateMillis < todayStartMillis) {
            return AppErrorReason.PAST_DATE_NOT_ALLOWED
        }

        if (endDateMillis < startDateMillis) {
            return AppErrorReason.END_DATE_BEFORE_START
        }

        val selectedDays = TimeUnit.MILLISECONDS.toDays(endDateMillis - startDateMillis) + 1
        if (selectedDays > MAX_RANGE_DAYS) {
            return AppErrorReason.DATE_RANGE_TOO_LONG
        }

        return null
    }

    private fun validationFailure(reason: AppErrorReason): AppointmentSearchCriteria? {
        publishError(reason)
        return null
    }

    private fun onDataLoadFailure(error: Throwable) {
        _uiState.update { it.copy(isLoading = false) }
        publishError(error, OperationType.FETCH_DATA)
    }

    private fun getTodayStartMillis(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private companion object {
        const val MAX_RANGE_DAYS = 15L
    }
}
