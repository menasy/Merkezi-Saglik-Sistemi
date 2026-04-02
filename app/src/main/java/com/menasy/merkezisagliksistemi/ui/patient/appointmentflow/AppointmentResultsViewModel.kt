package com.menasy.merkezisagliksistemi.ui.patient.appointmentflow

import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.data.model.Doctor
import com.menasy.merkezisagliksistemi.domain.usecase.GetBranchesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetHospitalsByDistrictUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetNearestAvailableDateUseCase
import com.menasy.merkezisagliksistemi.ui.common.base.BaseViewModel
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import com.menasy.merkezisagliksistemi.utils.DateTimeUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class AppointmentResultUiModel(
    val doctorId: String,
    val doctorName: String,
    val hospitalId: String,
    val hospitalName: String,
    val branchId: String,
    val branchName: String,
    val nearestAvailableDateMillis: Long,
    val nearestAvailableDateLabel: String,
    val nearestAvailableRelativeText: String,
    val slotStartHour: Int,
    val slotEndHour: Int,
    val slotDurationMinutes: Int
)

data class AppointmentResultsUiState(
    val isLoading: Boolean = false,
    val resultSummary: String = "",
    val appointments: List<AppointmentResultUiModel> = emptyList(),
    val emptyMessage: String? = null
)

class AppointmentResultsViewModel(
    private val getDoctorsUseCase: GetDoctorsUseCase,
    private val getHospitalsByDistrictUseCase: GetHospitalsByDistrictUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val getNearestAvailableDateUseCase: GetNearestAvailableDateUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AppointmentResultsUiState())
    val uiState: StateFlow<AppointmentResultsUiState> = _uiState.asStateFlow()

    private var isLoaded = false

    fun loadAppointments(args: AppointmentSearchArgs) {
        if (isLoaded) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, emptyMessage = null)
            }

            try {
                val hospitalsRaw = getHospitalsByDistrictUseCase(
                    cityId = args.cityId,
                    districtId = args.districtId
                )
                val hospitals = if (args.branchId.isNullOrBlank()) {
                    hospitalsRaw
                } else {
                    hospitalsRaw.filter { hospital ->
                        hospital.branchIds.isEmpty() || args.branchId in hospital.branchIds
                    }
                }

                if (hospitals.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            appointments = emptyList(),
                            emptyMessage = "Seçilen filtrelere uygun hastane bulunamadı.",
                            resultSummary = "0 uygun randevu bulundu"
                        )
                    }
                    publishInfo(
                        title = "Sonuç Bulunamadı",
                        description = "Seçilen branş için uygun hastane bulunamadı."
                    )
                    isLoaded = true
                    return@launch
                }

                val branches = getBranchesUseCase()

                val doctors = if (!args.hospitalId.isNullOrBlank()) {
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
                    publishInfo(
                        title = "Randevu Bulunamadı",
                        description = "Seçilen filtrelere uygun randevu bulunamadı."
                    )
                    isLoaded = true
                    return@launch
                }

                val hospitalNameMap = hospitals.associateBy({ it.id }, { it.name })
                val branchNameMap = branches.associateBy({ it.id }, { it.name })
                val startDate = DateTimeUtils.millisToLocalDate(args.startDateMillis)
                val endDate = DateTimeUtils.millisToLocalDate(args.endDateMillis)

                val semaphore = Semaphore(NEAREST_DATE_LOOKUP_PARALLELISM)
                val appointmentItems = coroutineScope {
                    filteredDoctors.map { doctor ->
                        async {
                            semaphore.withPermit {
                                doctor.toAppointmentResultOrNull(
                                    startDate = startDate,
                                    endDate = endDate,
                                    hospitalName = hospitalNameMap[doctor.hospitalId]
                                        ?: "Hastane Bilgisi Yok",
                                    branchName = branchNameMap[doctor.branchId]
                                        ?: "Poliklinik Bilgisi Yok"
                                )
                            }
                        }
                    }.awaitAll().filterNotNull()
                }.sortedBy { item -> item.nearestAvailableDateMillis }

                if (appointmentItems.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            appointments = emptyList(),
                            resultSummary = "0 uygun randevu bulundu",
                            emptyMessage = "Seçilen tarih aralığında uygun seans bulunamadı."
                        )
                    }
                    publishInfo(
                        title = "Uygun Seans Bulunamadı",
                        description = "Seçilen tarih aralığında hekimler için boş seans bulunamadı."
                    )
                    isLoaded = true
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        appointments = appointmentItems,
                        resultSummary = "${appointmentItems.size} uygun randevu bulundu",
                        emptyMessage = null
                    )
                }
                isLoaded = true
            } catch (error: Exception) {
                onDataLoadFailure(error)
            }
        }
    }

    private fun onDataLoadFailure(error: Throwable) {
        _uiState.update { it.copy(isLoading = false) }
        publishError(error, OperationType.FETCH_DATA)
    }

    private suspend fun Doctor.toAppointmentResultOrNull(
        startDate: LocalDate,
        endDate: LocalDate,
        hospitalName: String,
        branchName: String
    ): AppointmentResultUiModel? {
        val appointmentDate = getNearestAvailableDateUseCase(
            doctorId = id,
            startDate = startDate,
            endDate = endDate,
            slotStartHour = slotStartHour,
            slotEndHour = slotEndHour,
            slotDurationMinutes = slotDurationMinutes
        ) ?: return null

        val appointmentDateMillis = DateTimeUtils.localDateToStartOfDayMillis(appointmentDate)

        return AppointmentResultUiModel(
            doctorId = id,
            doctorName = fullName,
            hospitalId = hospitalId,
            hospitalName = hospitalName,
            branchId = branchId,
            branchName = branchName,
            nearestAvailableDateMillis = appointmentDateMillis,
            nearestAvailableDateLabel = EXACT_DATE_FORMATTER.format(appointmentDate),
            nearestAvailableRelativeText = buildRelativeDateLabel(appointmentDate),
            slotStartHour = slotStartHour,
            slotEndHour = slotEndHour,
            slotDurationMinutes = slotDurationMinutes
        )
    }

    private fun buildRelativeDateLabel(appointmentDate: LocalDate): String {
        val daysLeft = ChronoUnit.DAYS.between(DateTimeUtils.currentLocalDate(), appointmentDate)
            .toInt()
            .coerceAtLeast(0)

        return when (daysLeft) {
            0 -> "Bugün"
            1 -> "Yarın"
            else -> "$daysLeft gün sonra"
        }
    }

    private companion object {
        const val NEAREST_DATE_LOOKUP_PARALLELISM = 8
        val EXACT_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("tr-TR"))
    }
}
