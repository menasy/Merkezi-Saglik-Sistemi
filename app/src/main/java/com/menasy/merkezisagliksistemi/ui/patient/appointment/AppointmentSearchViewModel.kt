package com.menasy.merkezisagliksistemi.ui.patient.appointment

import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val errorMessage: String? = null
)

class AppointmentSearchViewModel(
    private val getCitiesUseCase: GetCitiesUseCase,
    private val getDistrictsByCityUseCase: GetDistrictsByCityUseCase,
    private val getHospitalsByDistrictUseCase: GetHospitalsByDistrictUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val getDoctorsUseCase: GetDoctorsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentSearchUiState())
    val uiState: StateFlow<AppointmentSearchUiState> = _uiState.asStateFlow()

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val citiesResult = getCitiesUseCase()
            val branchesResult = getBranchesUseCase()

            val cities = citiesResult.getOrElse {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = it.message ?: "Iller yuklenemedi"
                )
                return@launch
            }

            val branches = branchesResult.getOrElse {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = it.message ?: "Poliklinikler yuklenemedi"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                cities = cities,
                branches = branches
            )
        }
    }

    fun onCitySelected(cityId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedCityId = cityId,
                selectedDistrictId = null,
                selectedHospitalId = null,
                selectedDoctorId = null,
                districts = emptyList(),
                hospitals = emptyList(),
                doctors = emptyList(),
                errorMessage = null
            )

            val districtsResult = getDistrictsByCityUseCase(cityId)
            val districts = districtsResult.getOrElse {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = it.message ?: "Ilceler yuklenemedi"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                districts = districts
            )

            refreshHospitalsAndDoctors()
        }
    }

    fun onDistrictSelected(districtId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedDistrictId = districtId,
                selectedHospitalId = null,
                selectedDoctorId = null,
                hospitals = emptyList(),
                doctors = emptyList(),
                errorMessage = null
            )
            refreshHospitalsAndDoctors()
        }
    }

    fun onBranchSelected(branchId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedBranchId = branchId,
                selectedHospitalId = null,
                selectedDoctorId = null,
                doctors = emptyList(),
                errorMessage = null
            )
            refreshHospitalsAndDoctors()
        }
    }

    fun onHospitalSelected(hospitalId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedHospitalId = hospitalId,
                selectedDoctorId = null,
                doctors = emptyList(),
                errorMessage = null
            )
            refreshDoctorsOnly()
        }
    }

    fun onDoctorSelected(doctorId: String?) {
        _uiState.value = _uiState.value.copy(selectedDoctorId = doctorId)
    }

    fun onDateRangeSelected(
        startDateMillis: Long,
        endDateMillis: Long
    ): Result<Unit> {
        if (endDateMillis < startDateMillis) {
            return Result.failure(Exception("Bitis tarihi baslangic tarihinden once olamaz"))
        }

        val selectedDays = TimeUnit.MILLISECONDS.toDays(endDateMillis - startDateMillis) + 1
        if (selectedDays > MAX_RANGE_DAYS) {
            return Result.failure(Exception("Tarih araligi en fazla 15 gun olabilir"))
        }

        _uiState.value = _uiState.value.copy(
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            errorMessage = null
        )
        return Result.success(Unit)
    }

    fun buildSearchCriteria(): Result<AppointmentSearchCriteria> {
        val state = _uiState.value
        val cityId = state.selectedCityId
            ?: return Result.failure(Exception("Il secimi zorunludur"))
        val branchId = state.selectedBranchId
            ?: return Result.failure(Exception("Poliklinik secimi zorunludur"))
        val startDate = state.startDateMillis
            ?: return Result.failure(Exception("Baslangic tarihi secilmelidir"))
        val endDate = state.endDateMillis
            ?: return Result.failure(Exception("Bitis tarihi secilmelidir"))

        val selectedDays = TimeUnit.MILLISECONDS.toDays(endDate - startDate) + 1
        if (selectedDays > MAX_RANGE_DAYS) {
            return Result.failure(Exception("Tarih araligi en fazla 15 gun olabilir"))
        }

        return Result.success(
            AppointmentSearchCriteria(
                startDateMillis = startDate,
                endDateMillis = endDate,
                cityId = cityId,
                districtId = state.selectedDistrictId,
                branchId = branchId,
                hospitalId = state.selectedHospitalId,
                doctorId = state.selectedDoctorId
            )
        )
    }

    private suspend fun refreshHospitalsAndDoctors() {
        val currentState = _uiState.value
        val cityId = currentState.selectedCityId ?: return

        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

        val hospitalsResult = getHospitalsByDistrictUseCase(
            cityId = cityId,
            districtId = currentState.selectedDistrictId,
            branchId = currentState.selectedBranchId
        )

        val hospitals = hospitalsResult.getOrElse {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = it.message ?: "Hastaneler yuklenemedi"
            )
            return
        }

        val updatedHospitalSelection =
            if (currentState.selectedHospitalId in hospitals.map { it.id }) {
                currentState.selectedHospitalId
            } else {
                null
            }

        _uiState.value = _uiState.value.copy(
            hospitals = hospitals,
            selectedHospitalId = updatedHospitalSelection,
            doctors = emptyList(),
            selectedDoctorId = null
        )

        refreshDoctorsOnly()
    }

    private suspend fun refreshDoctorsOnly() {
        val state = _uiState.value
        val cityId = state.selectedCityId ?: run {
            _uiState.value = state.copy(isLoading = false)
            return
        }
        val branchId = state.selectedBranchId ?: run {
            _uiState.value = state.copy(
                doctors = emptyList(),
                selectedDoctorId = null,
                isLoading = false
            )
            return
        }

        val doctorsResult = getDoctorsUseCase(
            cityId = cityId,
            branchId = branchId,
            districtId = state.selectedDistrictId,
            hospitalId = state.selectedHospitalId
        )

        _uiState.value = doctorsResult.fold(
            onSuccess = { doctors ->
                state.copy(
                    isLoading = false,
                    doctors = doctors,
                    selectedDoctorId = state.selectedDoctorId?.takeIf { selectedId ->
                        doctors.any { doctor -> doctor.id == selectedId }
                    }
                )
            },
            onFailure = { exception ->
                state.copy(
                    isLoading = false,
                    doctors = emptyList(),
                    selectedDoctorId = null,
                    errorMessage = exception.message ?: "Hekimler yuklenemedi"
                )
            }
        )
    }

    private companion object {
        const val MAX_RANGE_DAYS = 15L
    }
}
