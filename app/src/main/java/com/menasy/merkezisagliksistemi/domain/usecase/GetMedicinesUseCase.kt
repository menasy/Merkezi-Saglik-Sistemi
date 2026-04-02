package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Medicine
import com.menasy.merkezisagliksistemi.data.repository.MedicineRepository

class GetMedicinesUseCase(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(): List<Medicine> {
        return medicineRepository.getMedicines()
    }
}
