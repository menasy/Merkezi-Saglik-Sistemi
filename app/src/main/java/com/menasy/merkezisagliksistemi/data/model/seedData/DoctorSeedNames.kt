package com.menasy.merkezisagliksistemi.data.model.seedData

/**
 * Doktor seed üretimi için isim ve unvan havuzları.
 */
internal object DoctorSeedNames {

    val GENERAL_TITLES = listOf("Dr.", "Uzm. Dr.", "Doç. Dr.")
    val SURGICAL_TITLES = listOf("Op. Dr.", "Uzm. Dr.", "Doç. Dr.")
    val PEDIATRIC_TITLES = listOf("Uzm. Dr.", "Dr.", "Doç. Dr.")

    val FIRST_NAMES = listOf(
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

    val LAST_NAMES = listOf(
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

    /**
     * Branş ID'sine göre uygun unvan listesini döndürür.
     */
    fun getTitlesForBranch(branchId: String): List<String> {
        return when {
            "cerrahi" in branchId || "dogum" in branchId -> SURGICAL_TITLES
            "cocuk" in branchId -> PEDIATRIC_TITLES
            else -> GENERAL_TITLES
        }
    }
}
