package com.menasy.merkezisagliksistemi.data.model.seedData

import com.menasy.merkezisagliksistemi.data.model.Doctor

/**
 * Doktor seed verileri.
 *
 * Bu dosya, doktor seed üretimi için ana giriş noktasıdır.
 * Üretim mantığı [DoctorSeedGenerator], doğrulama [DoctorSeedValidator],
 * isim havuzları [DoctorSeedNames] ve Firebase UID bağlantıları
 * [DoctorAccountBindings] ayrı dosyalarda yönetilir.
 *
 * ## Doktor Kimlik Yapısı
 * - `Doctor.id`: Business doctor ID - randevu ve reçete işlemlerinde kullanılır
 * - `Doctor.userId`: Firebase Auth UID - sadece login olabilen doktorlar için dolu
 *
 * ## Yeni Doktor Hesabı Ekleme
 * 1. Firebase Console'da Authentication hesabı oluştur
 * 2. Firestore'da `users/{uid}` dokümanı oluştur (role = "doctor")
 * 3. [DoctorAccountBindings] dosyasına doctorId -> firebaseUid eşleştirmesi ekle
 */
val doctors: List<Doctor> by lazy(LazyThreadSafetyMode.NONE) { buildDoctorSeeds() }

private fun buildDoctorSeeds(): List<Doctor> {
    val allBranchIds = branches.map { it.id }.distinct()
    val validBranchIds = allBranchIds.toSet()
    val hospitalIds = hospitals.map { it.id }.toSet()

    val generated = DoctorSeedGenerator.generateDoctors(
        hospitals = hospitals,
        allBranchIds = allBranchIds
    )

    DoctorSeedValidator.validate(
        doctors = generated,
        validHospitalIds = hospitalIds,
        validBranchIds = validBranchIds,
        hospitals = hospitals
    )

    return generated
}
