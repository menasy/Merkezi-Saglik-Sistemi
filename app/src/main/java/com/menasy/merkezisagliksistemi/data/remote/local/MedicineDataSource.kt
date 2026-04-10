package com.menasy.merkezisagliksistemi.data.remote.local

import com.menasy.merkezisagliksistemi.data.model.Medicine
import com.menasy.merkezisagliksistemi.data.model.seedData.MedicineSeedData

class MedicineDataSource {

    fun getMedicines(): List<Medicine> {
        return MedicineSeedData.allMedicines
            .distinctBy { it.medicineId }
            .sortedBy { it.medicineName }
    }
}
