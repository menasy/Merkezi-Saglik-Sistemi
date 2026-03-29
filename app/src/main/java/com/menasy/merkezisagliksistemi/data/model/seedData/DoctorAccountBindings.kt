package com.menasy.merkezisagliksistemi.data.model.seedData

/**
 * Firebase Auth hesabı olan doktorların bağlantı yapılandırması.
 *
 * Bu dosya, doktor profilleri (local seed) ile Firebase Authentication
 * hesapları arasındaki eşleştirmeleri yönetir.
 *
 * ## Kullanım
 * Yeni bir doktor hesabı açıldığında:
 * 1. Firebase Console'da Authentication > Users'dan hesap oluştur
 * 2. Firestore'da `users/{uid}` dokümanı oluştur (role = "doctor")
 * 3. Bu dosyadaki [doctorAccountBindings] map'ine eşleştirme ekle
 *
 * ## Örnek
 * ```
 * "dr_h01_branch_genel_dahiliye_1" to "abc123FirebaseUid"
 * ```
 *
 * ## Notlar
 * - doctorId: Local seed'deki Doctor.id değeri
 * - firebaseUid: Firebase Authentication'daki UID değeri
 * - Bir doktorun login yapabilmesi için bu map'te kaydı olmalı
 */
object DoctorAccountBindings {

    /**
     * Doktor ID -> Firebase Auth UID eşleştirmeleri.
     *
     * Key: Doctor.id (business doctor id)
     * Value: Firebase Auth UID
     */
    private val doctorAccountBindings: Map<String, String> = mapOf(
        
        // Genel Dahiliye
        "dr_h01_branch_genel_dahiliye_1" to "TTfX5Jl5oMfXdjpVUSIHfhP8vzh1",
        // "dr_h01_branch_genel_dahiliye_2" to "firebaseUid2",
    )

    /**
     * Verilen doktor ID için Firebase UID döndürür.
     * Eşleştirme yoksa null döner.
     */
    fun getFirebaseUidForDoctor(doctorId: String): String? {
        return doctorAccountBindings[doctorId]
    }

    /**
     * Verilen Firebase UID için doktor ID döndürür.
     * Eşleştirme yoksa null döner.
     */
    fun getDoctorIdForFirebaseUid(firebaseUid: String): String? {
        return doctorAccountBindings.entries
            .firstOrNull { it.value == firebaseUid }
            ?.key
    }

    /**
     * Login hesabı olan tüm doktor ID'lerini döndürür.
     */
    fun getAllBoundDoctorIds(): Set<String> {
        return doctorAccountBindings.keys
    }

    /**
     * Verilen doktor ID'nin login hesabı olup olmadığını kontrol eder.
     */
    fun hasLoginAccount(doctorId: String): Boolean {
        return doctorAccountBindings.containsKey(doctorId)
    }
}
