package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.model.Medicine
import com.menasy.merkezisagliksistemi.data.remote.local.MedicineDataSource

class MedicineRepository(
    private val medicineDataSource: MedicineDataSource
) {
    fun getMedicines(): List<Medicine> {
        return medicineDataSource.getMedicines()
    }
}
