package com.menasy.merkezisagliksistemi.data.model.seedData

import com.menasy.merkezisagliksistemi.data.model.Doctor

/**
 * Doktor seed verilerinin doğrulama işlemleri.
 */
internal object DoctorSeedValidator {

    /**
     * Doktor seed listesini doğrular.
     *
     * @param doctors Doğrulanacak doktor listesi
     * @param validHospitalIds Geçerli hastane ID'leri
     * @param validBranchIds Geçerli branş ID'leri
     * @param hospitals Hastane listesi (branş başına doktor sayısı kontrolü için)
     *
     * @throws IllegalArgumentException Doğrulama hatası durumunda
     */
    fun validate(
        doctors: List<Doctor>,
        validHospitalIds: Set<String>,
        validBranchIds: Set<String>,
        hospitals: List<com.menasy.merkezisagliksistemi.data.model.Hospital>
    ) {
        validateNotEmpty(doctors)
        validateNoDuplicateDoctorIds(doctors)
        validateNoDuplicateUserIds(doctors)
        validateHospitalReferences(doctors, validHospitalIds)
        validateBranchReferences(doctors, validBranchIds)
        validateRequiredFields(doctors)
        validateDoctorCountPerBranch(doctors, hospitals)
    }

    private fun validateNotEmpty(doctors: List<Doctor>) {
        require(doctors.isNotEmpty()) {
            "Doctor seed list cannot be empty."
        }
    }

    private fun validateNoDuplicateDoctorIds(doctors: List<Doctor>) {
        val duplicateDoctorId = doctors
            .groupingBy { it.id }
            .eachCount()
            .entries
            .firstOrNull { it.value > 1 }
            ?.key

        require(duplicateDoctorId == null) {
            "Duplicate doctor id detected: $duplicateDoctorId"
        }
    }

    private fun validateNoDuplicateUserIds(doctors: List<Doctor>) {
        // Sadece userId'si olan doktorları kontrol et
        val doctorsWithUserId = doctors.filter { !it.userId.isNullOrBlank() }

        val duplicateUserId = doctorsWithUserId
            .groupingBy { it.userId }
            .eachCount()
            .entries
            .firstOrNull { it.value > 1 }
            ?.key

        require(duplicateUserId == null) {
            "Duplicate userId detected: $duplicateUserId"
        }
    }

    private fun validateHospitalReferences(doctors: List<Doctor>, validHospitalIds: Set<String>) {
        val invalidHospitalRef = doctors.firstOrNull { it.hospitalId !in validHospitalIds }

        require(invalidHospitalRef == null) {
            "Orphan doctor hospitalId detected: ${invalidHospitalRef?.hospitalId}"
        }
    }

    private fun validateBranchReferences(doctors: List<Doctor>, validBranchIds: Set<String>) {
        val invalidBranchRef = doctors.firstOrNull { it.branchId !in validBranchIds }

        require(invalidBranchRef == null) {
            "Orphan doctor branchId detected: ${invalidBranchRef?.branchId}"
        }
    }

    private fun validateRequiredFields(doctors: List<Doctor>) {
        // userId artık zorunlu değil - sadece gerçek zorunlu alanları kontrol et
        val invalidFieldDoctor = doctors.firstOrNull {
            it.id.isBlank() ||
                    it.fullName.isBlank() ||
                    it.hospitalId.isBlank() ||
                    it.branchId.isBlank() ||
                    it.roomInfo.isBlank()
        }

        require(invalidFieldDoctor == null) {
            "A doctor record contains blank required fields: ${invalidFieldDoctor?.id}"
        }
    }

    private fun validateDoctorCountPerBranch(
        doctors: List<Doctor>,
        hospitals: List<com.menasy.merkezisagliksistemi.data.model.Hospital>
    ) {
        val hospitalsById = hospitals.associateBy { it.id }
        val countsByHospitalBranch = doctors.groupingBy { it.hospitalId to it.branchId }.eachCount()
        val requiredCount = DoctorSeedGenerator.getDoctorsPerBranch()

        countsByHospitalBranch.forEach { (hospitalBranch, count) ->
            val hospital = hospitalsById[hospitalBranch.first]
                ?: error("Hospital id ${hospitalBranch.first} is not available in seed.")

            require(count == requiredCount) {
                "Hospital ${hospital.id}, branch ${hospitalBranch.second} has $count doctor(s), " +
                        "required count is $requiredCount."
            }
        }
    }
}
