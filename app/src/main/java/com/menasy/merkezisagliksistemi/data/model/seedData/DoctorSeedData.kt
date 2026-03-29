package com.menasy.merkezisagliksistemi.data.model.seedData

import com.menasy.merkezisagliksistemi.data.model.Doctor

val doctors: List<Doctor> by lazy(LazyThreadSafetyMode.NONE) { buildDoctorSeeds() }

private fun buildDoctorSeeds(): List<Doctor> {
    val allBranchIds = branches.map { it.id }.distinct()
    val validBranchIds = allBranchIds.toSet()
    val hospitalIds = hospitals.map { it.id }.toSet()

    val generated = mutableListOf<Doctor>()

    hospitals.forEachIndexed { hospitalIndex, hospital ->
        val selectedBranches = resolveHospitalBranchIds(
            hospital = hospital,
            allBranchIds = allBranchIds
        )

        selectedBranches.forEachIndexed { branchIndex, branchId ->
            val doctorCount = doctorsPerBranch()

            repeat(doctorCount) { localDoctorIndex ->
                val doctorIndex = generated.size + 1
                val idSuffix = "${hospital.id}_${branchId}_${localDoctorIndex + 1}"

                generated += Doctor(
                    id = "dr_$idSuffix",
                    userId = "doctor_$idSuffix",
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

    validateDoctorSeed(
        doctors = generated,
        validHospitalIds = hospitalIds,
        validBranchIds = validBranchIds
    )

    return generated
}

private fun doctorsPerBranch(): Int {
    return DOCTORS_PER_BRANCH
}

private fun requiredDoctorsPerBranch(@Suppress("UNUSED_PARAMETER") bedCount: Int): Int {
    return DOCTORS_PER_BRANCH
}

private fun buildDoctorName(
    doctorIndex: Int,
    hospitalIndex: Int,
    branchIndex: Int,
    localDoctorIndex: Int,
    branchId: String
): String {
    val title = doctorTitleFor(branchId, doctorIndex)

    val firstNameIndex = positiveHash("$hospitalIndex:$branchIndex:$localDoctorIndex") % FIRST_NAMES.size
    val lastNameIndex = positiveHash("$branchId:$doctorIndex") % LAST_NAMES.size

    val firstName = FIRST_NAMES[firstNameIndex]
    val lastName = LAST_NAMES[lastNameIndex]

    return "$title $firstName $lastName"
}

private fun doctorTitleFor(branchId: String, seed: Int): String {
    return when {
        "cerrahi" in branchId || "dogum" in branchId -> SURGICAL_TITLES[seed % SURGICAL_TITLES.size]
        "cocuk" in branchId -> PEDIATRIC_TITLES[seed % PEDIATRIC_TITLES.size]
        else -> GENERAL_TITLES[seed % GENERAL_TITLES.size]
    }
}

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

private fun validateDoctorSeed(
    doctors: List<Doctor>,
    validHospitalIds: Set<String>,
    validBranchIds: Set<String>
) {
    require(doctors.isNotEmpty()) { "Doctor seed list cannot be empty." }

    val duplicateDoctorId = doctors.groupingBy { it.id }.eachCount().entries
        .firstOrNull { it.value > 1 }
        ?.key
    require(duplicateDoctorId == null) { "Duplicate doctor id detected: $duplicateDoctorId" }

    val duplicateUserId = doctors.groupingBy { it.userId }.eachCount().entries
        .firstOrNull { it.value > 1 }
        ?.key
    require(duplicateUserId == null) { "Duplicate userId detected: $duplicateUserId" }

    val invalidHospitalRef = doctors.firstOrNull { it.hospitalId !in validHospitalIds }
    require(invalidHospitalRef == null) {
        "Orphan doctor hospitalId detected: ${invalidHospitalRef?.hospitalId}"
    }

    val invalidBranchRef = doctors.firstOrNull { it.branchId !in validBranchIds }
    require(invalidBranchRef == null) {
        "Orphan doctor branchId detected: ${invalidBranchRef?.branchId}"
    }

    val invalidFieldDoctor = doctors.firstOrNull {
        it.id.isBlank() ||
            it.userId.isBlank() ||
            it.fullName.isBlank() ||
            it.hospitalId.isBlank() ||
            it.branchId.isBlank() ||
            it.roomInfo.isBlank()
    }
    require(invalidFieldDoctor == null) { "A doctor record contains blank required fields." }

    val hospitalsById = hospitals.associateBy { it.id }
    val countsByHospitalBranch = doctors.groupingBy { it.hospitalId to it.branchId }.eachCount()

    countsByHospitalBranch.forEach { (hospitalBranch, count) ->
        val hospital = hospitalsById[hospitalBranch.first]
            ?: error("Hospital id ${hospitalBranch.first} is not available in seed.")
        val required = requiredDoctorsPerBranch(hospital.bedCount)
        require(count == required) {
            "Hospital ${hospital.id}, branch ${hospitalBranch.second} has $count doctor(s), required count is $required."
        }
    }
}

private fun positiveHash(value: String): Int {
    return value.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
}

private const val DOCTORS_PER_BRANCH = 2

private val GENERAL_TITLES = listOf("Dr.", "Uzm. Dr.", "Doc. Dr.")
private val SURGICAL_TITLES = listOf("Op. Dr.", "Uzm. Dr.", "Doc. Dr.")
private val PEDIATRIC_TITLES = listOf("Uzm. Dr.", "Dr.", "Doc. Dr.")

private val FIRST_NAMES = listOf(
    // Erkek isimleri
    "Ahmet", "Mehmet", "Mustafa", "Ali", "Yusuf", "Ömer", "Hasan", "Hüseyin", "İbrahim", "İsmail",
    "Murat", "Emre", "Can", "Burak", "Onur", "Serkan", "Tolga", "Hakan", "Kaan", "Eren",
    "Kemal", "Cem", "Barış", "Levent", "Sinan", "Volkan", "Kadir", "Cihan", "Ferhat", "Bekir",
    "Nihat", "Halil", "Ramazan", "Taha", "Harun", "Rauf", "Taner", "Adem", "Rasim", "Süleyman",
    "Okan", "Umut", "Serdar", "Özgür", "Fatih", "Selim", "Yasin", "Yavuz", "Cengiz", "Koray",
    "Alper", "Engin", "Gökhan", "İlker", "Cemil", "Cüneyt", "Faruk", "Gökmen", "Haydar", "İlhan",
    "Metin", "Necati", "Oğuz", "Orhan", "Polat", "Recep", "Şahin", "Tayfun", "Ufuk", "Vedat",
    "Yunus", "Zafer", "Alparslan", "Barbaros", "Cenk", "Deniz", "Erdem", "Ferit", "Görkem", "Halis",
    "İrfan", "Kerem", "Latif", "Mahir", "Nazım", "Osman", "Poyraz", "Rıza", "Sarp", "Taylan",
    "Uğur", "Vahit", "Yiğit", "Zeki", "Anıl", "Bora", "Doğan", "Efe", "Fırat", "Güven",
    "Bülent", "Çağrı", "Doruk", "Ertuğrul", "Gürkan", "Hüsnü", "Kağan", "Melih", "Necip", "Özcan",
    "Rüştü", "Selçuk", "Şenol", "Turgay", "Ümit", "Yakup", "Ziya", "Atilla", "Baran", "Coşkun",
    // Kadın isimleri
    "Ayşe", "Fatma", "Emine", "Hatice", "Zeynep", "Elif", "Merve", "Seda", "Esra", "Derya",
    "Selin", "Nazlı", "Aslı", "Buse", "Gizem", "Damla", "Deniz", "Sinem", "Ece", "Yasemin",
    "Aylin", "Başak", "Cansu", "Dilara", "Ebru", "Feyza", "Gülşen", "Hazal", "İrem", "Jale",
    "Kübra", "Leyla", "Melek", "Nilay", "Özlem", "Pelin", "Rana", "Serap", "Tuğba", "Ülkü",
    "Vildan", "Yıldız", "Ayten", "Berna", "Ceyda", "Didem", "Elçin", "Fulya", "Gonca", "Hilal",
    "İpek", "Kiraz", "Lale", "Meral", "Neslihan", "Özge", "Pervin", "Rüya", "Sevgi", "Tülay",
    "Umay", "Vuslat", "Yazgül", "Zühal", "Ayla", "Belgin", "Canan", "Defne", "Eylem", "Figen",
    "Gülşah", "Hande", "Işıl", "Kadriye", "Meltem", "Nurcan", "Öykü", "Pınar", "Reyhan", "Şule",
    "Gülay", "Hülya", "İnci", "Jülide", "Müge", "Necla", "Oya", "Perihan", "Sabiha", "Şebnem",
    "Tülay", "Ümmühan", "Vahide", "Yüksel", "Zübeyde", "Açelya", "Begüm", "Çiğdem", "Deniz", "Eylül"
)

private val LAST_NAMES = listOf(
    // Yaygın soyadları
    "Yılmaz", "Kaya", "Demir", "Çelik", "Şahin", "Yıldız", "Yıldırım", "Öztan", "Aydın", "Özdemir",
    "Arslan", "Doğan", "Kılıç", "Aslan", "Çetin", "Kara", "Koç", "Kurt", "Acar", "Tekin",
    "İnce", "Polat", "Kaplan", "Keskin", "Taş", "Aksoy", "Tunç", "Ateş", "Şimşek", "Bozkurt",
    "Duman", "Uslu", "Toprak", "Güneş", "Karaca", "Avcı", "Esen", "Baş", "Akçay", "Turgut",
    "Çoban", "Erdem", "Bulut", "İnal", "Altun", "Dinç", "Bayrak", "Karataş", "Tan", "Gül",
    "Çakır", "İpek", "Akın", "Sarı", "Güler", "Yüce", "Çam", "Sezer", "Köse", "Taşdemir",
    // Ek soyadları
    "Aktaş", "Bakır", "Canlı", "Denizli", "Ercan", "Fidan", "Gökçe", "Hacıoğlu", "Ilgın", "Karadağ",
    "Lale", "Mutlu", "Narin", "Ocak", "Peker", "Reis", "Soylu", "Turan", "Ünal", "Vural",
    "Yaşar", "Zengin", "Akbaş", "Bahar", "Candan", "Dağlı", "Ergül", "Fırat", "Görgün", "Hazar",
    "Işık", "Kalkan", "Laçin", "Metin", "Nalbant", "Önder", "Pala", "Raşit", "Şeker", "Toker",
    "Uysal", "Vardar", "Yaman", "Zorlu", "Akgün", "Balcı", "Ceylan", "Durak", "Erkan", "Ferdi",
    "Günay", "Halıcı", "Irmak", "Koçak", "Lider", "Maraş", "Nalcı", "Özel", "Parlak", "Reyhan",
    "Sağlam", "Tok", "Uzun", "Varol", "Yalçın", "Zaman", "Akan", "Bayar", "Çiçek", "Dursun",
    "Erol", "Fidaner", "Güven", "Harman", "İnan", "Kahraman", "Levent", "Mavzer", "Nazlı", "Okur",
    "Poyraz", "Reşit", "Sönmez", "Turhan", "Ülker", "Vatan", "Yakut", "Zeytin", "Altan", "Bircan",
    "Özkan", "Şen", "Güneyli", "Tekelioğlu", "Yörük", "Çınar", "Özbek", "Şenel", "Gümüş", "Toraman",
    "Özçelik", "Şanlı", "Güngör", "Türeli", "Yücel", "Çetiner", "Özdoğan", "Şimşir", "Gülay", "Tunçyılmaz",
    "Özgür", "Şekerci", "Güneşli", "Topkan", "Yüksek", "Çetinkaya", "Özyurt", "Şirin", "Gündoğdu", "Tokeri"
)
