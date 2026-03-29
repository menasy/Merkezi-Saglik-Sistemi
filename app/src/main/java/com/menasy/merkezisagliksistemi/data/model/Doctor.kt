package com.menasy.merkezisagliksistemi.data.model

/**
 * Doktor profil modeli.
 *
 * @property id Business doctor ID - randevu ve reçete işlemlerinde kullanılır
 * @property userId Firebase Auth UID - sadece login olabilen doktorlar için dolu olur.
 *                  Tüm doktorların login hesabı olmak zorunda değildir.
 * @property fullName Doktor tam adı (unvan dahil)
 * @property branchId Doktorun branş/poliklinik ID'si
 * @property hospitalId Doktorun çalıştığı hastane ID'si
 * @property roomInfo Oda/muayenehane bilgisi
 * @property slotStartHour Günlük mesai başlangıç saati
 * @property slotEndHour Günlük mesai bitiş saati
 * @property slotDurationMinutes Randevu slot süresi (dakika)
 */
data class Doctor(
    val id: String = "",
    val userId: String? = null,
    val fullName: String = "",
    val branchId: String = "",
    val hospitalId: String = "",
    val roomInfo: String = "",
    val slotStartHour: Int = 9,
    val slotEndHour: Int = 17,
    val slotDurationMinutes: Int = 20
) {
    /**
     * Bu doktorun Firebase Auth hesabı ile giriş yapıp yapamayacağını belirtir.
     */
    val canLogin: Boolean
        get() = !userId.isNullOrBlank()
    //Doktorların db şeması build/repoorts/
}
