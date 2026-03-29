package com.menasy.merkezisagliksistemi.data.model.seedData

import com.menasy.merkezisagliksistemi.data.model.Doctor

/**
 * Doktor seed üretimi için yardımcı fonksiyonlar.
 */
internal object DoctorSeedGenerator {

    private const val DOCTORS_PER_BRANCH = 2

    /**
     * Tüm hastaneler için doktor listesi üretir.
     */
    fun generateDoctors(
        hospitals: List<com.menasy.merkezisagliksistemi.data.model.Hospital>,
        allBranchIds: List<String>
    ): List<Doctor> {
        val generated = mutableListOf<Doctor>()

        hospitals.forEachIndexed { hospitalIndex, hospital ->
            val selectedBranches = resolveHospitalBranchIds(
                hospital = hospital,
                allBranchIds = allBranchIds
            )

            selectedBranches.forEachIndexed { branchIndex, branchId ->
                repeat(DOCTORS_PER_BRANCH) { localDoctorIndex ->
                    val doctorIndex = generated.size + 1
                    val doctorId = buildDoctorId(hospital.id, branchId, localDoctorIndex)

                    generated += Doctor(
                        id = doctorId,
                        userId = DoctorAccountBindings.getFirebaseUidForDoctor(doctorId),
                        fullName = buildDoctorName(
                            doctorIndex = doctorIndex,
                            hospitalIndex = hospitalIndex,
                            branchIndex = branchIndex,
                            localDoctorIndex = localDoctorIndex,
                            branchId = branchId
                        ),
                        branchId = branchId,
                        hospitalId = hospital.id,
                        roomInfo = buildRoomInfo(
                            branchId = branchId,
                            branchIndex = branchIndex,
                            localDoctorIndex = localDoctorIndex
                        )
                    )
                }
            }
        }

        return generated
    }

    /**
     * Doktor ID'si oluşturur.
     */
    private fun buildDoctorId(hospitalId: String, branchId: String, localIndex: Int): String {
        return "dr_${hospitalId}_${branchId}_${localIndex + 1}"
    }

    /**
     * Doktor tam adı (unvan + isim + soyisim) oluşturur.
     */
    private fun buildDoctorName(
        doctorIndex: Int,
        hospitalIndex: Int,
        branchIndex: Int,
        localDoctorIndex: Int,
        branchId: String
    ): String {
        val titles = DoctorSeedNames.getTitlesForBranch(branchId)
        val title = titles[doctorIndex % titles.size]

        val firstNameIndex = positiveHash("$hospitalIndex:$branchIndex:$localDoctorIndex") %
                DoctorSeedNames.FIRST_NAMES.size
        val lastNameIndex = positiveHash("$branchId:$doctorIndex") %
                DoctorSeedNames.LAST_NAMES.size

        val firstName = DoctorSeedNames.FIRST_NAMES[firstNameIndex]
        val lastName = DoctorSeedNames.LAST_NAMES[lastNameIndex]

        return "$title $firstName $lastName"
    }

    /**
     * Oda bilgisi oluşturur.
     */
    private fun buildRoomInfo(
        branchId: String,
        branchIndex: Int,
        localDoctorIndex: Int
    ): String {
        val wing = ('A'.code + (branchIndex % 8)).toChar()
        val floor = 1 + (branchIndex / 8)
        val baseRoom = 100 + (localDoctorIndex * 3) + (positiveHash(branchId) % 10)
        return "$wing$floor-$baseRoom"
    }

    /**
     * Branş başına doktor sayısını döndürür.
     */
    fun getDoctorsPerBranch(): Int = DOCTORS_PER_BRANCH

    /**
     * Negatif olmayan hash değeri üretir.
     */
    private fun positiveHash(value: String): Int {
        return value.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
    }
}
