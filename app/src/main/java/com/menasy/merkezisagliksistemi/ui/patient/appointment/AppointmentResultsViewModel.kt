package com.menasy.merkezisagliksistemi.ui.patient.appointment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.data.model.Doctor
import com.menasy.merkezisagliksistemi.domain.usecase.GetBranchesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetHospitalsByDistrictUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

data class AppointmentResultUiModel(
    val doctorId: String,
    val doctorName: String,
    val hospitalId: String,
    val hospitalName: String,
    val branchId: String,
    val branchName: String,
    val appointmentDateMillis: Long,
    val appointmentDateLabel: String,
    val daysLeftText: String,
    val slotStartHour: Int,
    val slotEndHour: Int,
    val slotDurationMinutes: Int
)

data class AppointmentResultsUiState(
    val isLoading: Boolean = false,
    val resultSummary: String = "",
    val appointments: List<AppointmentResultUiModel> = emptyList(),
    val emptyMessage: String? = null,
    val errorMessage: String? = null
)

class AppointmentResultsViewModel(
    private val getDoctorsUseCase: GetDoctorsUseCase,
    private val getHospitalsByDistrictUseCase: GetHospitalsByDistrictUseCase,
    private val getBranchesUseCase: GetBranchesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentResultsUiState())
    val uiState: StateFlow<AppointmentResultsUiState> = _uiState.asStateFlow()

    private var isLoaded = false

    fun loadAppointments(args: AppointmentSearchArgs) {
        if (isLoaded) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, emptyMessage = null)
            }

            val hospitalsResult = getHospitalsByDistrictUseCase(
                cityId = args.cityId,
                districtId = args.districtId
            )

            val hospitals = hospitalsResult.getOrElse { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Hastaneler listelenemedi")
                }
                return@launch
            }

            val branchesResult = getBranchesUseCase()
            val branches = branchesResult.getOrElse { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Poliklinik bilgisi alınamadı")
                }
                return@launch
            }

            val doctorsResult = if (!args.hospitalId.isNullOrBlank()) {
                getDoctorsUseCase(
                    hospitalId = args.hospitalId,
                    branchId = args.branchId
                )
            } else {
                val hospitalIds = hospitals.map { it.id }
                getDoctorsUseCase.byHospitalIds(
                    hospitalIds = hospitalIds,
                    branchId = args.branchId
                )
            }

            val doctors = doctorsResult.getOrElse { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Hekimler listelenemedi")
                }
                return@launch
            }

            val filteredDoctors = doctors
                .filter { doctor -> args.doctorId == null || doctor.id == args.doctorId }
                .distinctBy { doctor -> doctor.id }

            if (filteredDoctors.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        appointments = emptyList(),
                        emptyMessage = "Seçilen filtrelere uygun randevu bulunamadı.",
                        resultSummary = "0 uygun randevu bulundu"
                    )
                }
                isLoaded = true
                return@launch
            }

            val hospitalNameMap = hospitals.associateBy({ it.id }, { it.name })
            val branchNameMap = branches.associateBy({ it.id }, { it.name })
            val startDate = millisToLocalDate(args.startDateMillis)
            val endDate = millisToLocalDate(args.endDateMillis)

            val appointmentItems = filteredDoctors
                .map { doctor ->
                    doctor.toAppointmentResult(
                        startDate = startDate,
                        endDate = endDate,
                        hospitalName = hospitalNameMap[doctor.hospitalId] ?: "Hastane Bilgisi Yok",
                        branchName = branchNameMap[doctor.branchId] ?: "Poliklinik Bilgisi Yok"
                    )
                }
                .sortedBy { item -> item.appointmentDateMillis }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    appointments = appointmentItems,
                    resultSummary = "${appointmentItems.size} uygun randevu bulundu",
                    emptyMessage = null
                )
            }
            isLoaded = true
        }
    }

    private fun Doctor.toAppointmentResult(
        startDate: LocalDate,
        endDate: LocalDate,
        hospitalName: String,
        branchName: String
    ): AppointmentResultUiModel {
        val appointmentDate = generateNearestDate(doctorId = id, startDate = startDate, endDate = endDate)
        val appointmentDateMillis = appointmentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            .toEpochMilli()
        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), appointmentDate).toInt().coerceAtLeast(0)

        return AppointmentResultUiModel(
            doctorId = id,
            doctorName = fullName,
            hospitalId = hospitalId,
            hospitalName = hospitalName,
            branchId = branchId,
            branchName = branchName,
            appointmentDateMillis = appointmentDateMillis,
            appointmentDateLabel = DISPLAY_DATE_FORMATTER.format(appointmentDate),
            daysLeftText = if (daysLeft <= 1) "1 gün kaldı" else "$daysLeft gün kaldı",
            slotStartHour = slotStartHour,
            slotEndHour = slotEndHour,
            slotDurationMinutes = slotDurationMinutes
        )
    }

    private fun generateNearestDate(
        doctorId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): LocalDate {
        val dayCount = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val normalizedDayCount = dayCount.coerceAtLeast(1)
        val offset = abs(doctorId.hashCode()) % normalizedDayCount
        return startDate.plusDays(offset.toLong())
    }

    private fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    private companion object {
        val DISPLAY_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR"))
    }
}
