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
    val errorMessage: String? = null,
    val isDoctorFieldEnabled: Boolean = false
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val citiesResult = getCitiesUseCase()
            val branchesResult = getBranchesUseCase()

            val cities = citiesResult.getOrElse { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "İller yüklenemedi")
                }
                return@launch
            }

            val branches = branchesResult.getOrElse { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Poliklinikler yüklenemedi")
                }
                return@launch
            }

            _uiState.update {
                it.copy(isLoading = false, cities = cities, branches = branches)
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
                    isDoctorFieldEnabled = false,
                    errorMessage = null
                )
            }

            val districtsResult = getDistrictsByCityUseCase(cityId)
            val districts = districtsResult.getOrElse { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "İlçeler yüklenemedi")
                }
                return@launch
            }

            _uiState.update { it.copy(districts = districts) }
            refreshHospitals()
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
                    isDoctorFieldEnabled = false,
                    errorMessage = null
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
                    isDoctorFieldEnabled = false,
                    errorMessage = null
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
                    isDoctorFieldEnabled = isSpecificHospitalSelected,
                    errorMessage = null
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
    ): Result<Unit> {
        val todayStartMillis = getTodayStartMillis()

        if (startDateMillis < todayStartMillis) {
            return Result.failure(Exception("Geçmiş tarihte randevu araması yapılamaz"))
        }

        if (endDateMillis < startDateMillis) {
            return Result.failure(Exception("Bitiş tarihi başlangıç tarihinden önce olamaz"))
        }

        val selectedDays = TimeUnit.MILLISECONDS.toDays(endDateMillis - startDateMillis) + 1
        if (selectedDays > MAX_RANGE_DAYS) {
            return Result.failure(Exception("Tarih aralığı en fazla 15 gün olabilir"))
        }

        _uiState.update {
            it.copy(
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
                errorMessage = null
            )
        }
        return Result.success(Unit)
    }

    fun buildSearchCriteria(): Result<AppointmentSearchCriteria> {
        val state = _uiState.value
        val cityId = state.selectedCityId
            ?: return Result.failure(Exception("İl seçimi zorunludur"))
        val branchId = state.selectedBranchId
            ?: return Result.failure(Exception("Poliklinik seçimi zorunludur"))
        val startDate = state.startDateMillis
            ?: return Result.failure(Exception("Başlangıç tarihi seçilmelidir"))
        val endDate = state.endDateMillis
            ?: return Result.failure(Exception("Bitiş tarihi seçilmelidir"))

        val todayStartMillis = getTodayStartMillis()
        if (startDate < todayStartMillis) {
            return Result.failure(Exception("Geçmiş tarihte randevu araması yapılamaz"))
        }

        val selectedDays = TimeUnit.MILLISECONDS.toDays(endDate - startDate) + 1
        if (selectedDays > MAX_RANGE_DAYS) {
            return Result.failure(Exception("Tarih aralığı en fazla 15 gün olabilir"))
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

    private suspend fun refreshHospitals() {
        val currentState = _uiState.value
        val cityId = currentState.selectedCityId ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val hospitalsResult = getHospitalsByDistrictUseCase(
            cityId = cityId,
            districtId = currentState.selectedDistrictId
        )

        hospitalsResult.fold(
            onSuccess = { hospitals ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hospitals = hospitals,
                        selectedHospitalId = null,
                        selectedDoctorId = null,
                        doctors = emptyList(),
                        isDoctorFieldEnabled = false
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Hastaneler yüklenemedi"
                    )
                }
            }
        )
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

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val doctorsResult = getDoctorsUseCase(
            hospitalId = hospitalId,
            branchId = currentState.selectedBranchId
        )

        doctorsResult.fold(
            onSuccess = { doctors ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        doctors = doctors,
                        selectedDoctorId = null
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        doctors = emptyList(),
                        selectedDoctorId = null,
                        errorMessage = error.message ?: "Hekimler yüklenemedi"
                    )
                }
            }
        )
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
