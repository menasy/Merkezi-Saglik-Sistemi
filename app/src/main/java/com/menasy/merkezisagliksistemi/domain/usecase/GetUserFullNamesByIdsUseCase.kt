package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository

class GetUserFullNamesByIdsUseCase(
    private val appointmentRepository: AppointmentRepository
) {
    suspend operator fun invoke(userIds: Set<String>): Result<Map<String, String>> {
        return appointmentRepository.getUserFullNamesByIds(userIds)
    }
}
