# AppointmentApp

## SİSTEM ANALİZİ, TASARIM VE TEKNİK GELİŞTİRME DOKÜMANI
### Android Tabanlı Merkezi Hastane Randevu, Hasta Takip ve E-Reçete Sistemi

---

## 1. Proje Tanımı

Bu proje, üniversite dersi kapsamında geliştirilecek Android tabanlı bir Merkezi Hastane Randevu, Hasta Takip ve E-Reçete Sistemi'dir. Uygulamanın amacı, hasta ve doktor arasındaki temel sağlık hizmeti süreçlerini tek bir mobil uygulama üzerinden düzenli, güvenli ve sürdürülebilir biçimde yönetmektir.

Sistem iki ana kullanıcı rolü üzerine kuruludur:

- Hasta
- Doktor

Bu doküman yalnızca genel bir tanıtım metni değildir. Aynı zamanda projenin geliştirme sürecinde izlenecek teknik yaklaşımı, veri modelini, mimari yapıyı, klasör organizasyonunu, iş kurallarını ve uygulama akışlarını tanımlayan resmi bir geliştirme rehberidir. Proje geliştirilirken temel başvuru kaynağı olarak kullanılmalıdır.

---

## 2. Projenin Amacı ve Hedefleri

Projenin temel amacı, hastane randevu sürecini ve muayene sonrası reçete takibini mobil ortama taşıyarak hem hasta hem doktor açısından düzenli bir kullanım akışı oluşturmaktır.

Bu kapsamda hedeflenen başlıca işlevler şunlardır:

- Hastanın şehir, hastane ve doktor seçimi yapabilmesi
- Doktorun uygun gün ve saatlerine göre randevu alınabilmesi
- Aynı zaman aralığına birden fazla randevu yazılmasının engellenmesi
- Hastanın aktif ve geçmiş randevularını görüntüleyebilmesi
- Doktorun kendi randevu listesini görebilmesi
- Muayene sonrası doktor tarafından reçete oluşturulabilmesi
- Hastanın kendisine yazılan reçeteleri sistem üzerinden görüntüleyebilmesi
- Tüm verilerin merkezi, tutarlı ve güvenli bir yapıda tutulması
- Modern Android geliştirme ilkelerine uygun bir mimariyle sürdürülebilir bir yazılım ortaya konulması

---

## 3. Problem Tanımı

Geleneksel randevu ve takip süreçlerinde aşağıdaki problemler yaşanabilmektedir:

- Randevu alma işlemlerinin yavaş ilerlemesi
- Uygun doktor ve saat bilgisinin dağınık biçimde sunulması
- Aynı zaman dilimi için çakışan kayıtların oluşabilmesi
- Hasta ve doktor bilgilerinin manuel takiple yürütülmesi
- Reçete bilgisinin kullanıcı tarafından düzenli şekilde takip edilememesi

Bu proje, söz konusu problemleri mobil odaklı, merkezi ve kullanıcı rolleriyle sınırlandırılmış bir sistem yapısı ile çözmeyi amaçlamaktadır.

---

## 4. Sistem Kapsamı

### 4.1 Hasta İşlevleri

- Kayıt olma
- Giriş yapma ve çıkış yapma
- Profil bilgilerini görüntüleme
- Şehir seçme
- Hastane seçme
- Doktor seçme
- Uygun tarih ve saatleri görüntüleme
- Randevu alma
- Randevu iptal etme
- Aktif ve geçmiş randevuları görüntüleme
- Kendisine yazılan reçeteleri görüntüleme
- Reçete kodu ve reçete detaylarını görüntüleme

### 4.2 Doktor İşlevleri

- Sisteme giriş yapma ve çıkış yapma
- Kendi randevu listesini görüntüleme
- Randevu detayını görüntüleme
- Hastaya ait temel bilgileri görüntüleme
- Randevu durumunu güncelleme
- Muayene tamamlandıktan sonra reçete oluşturma
- Oluşturulan reçete kodunu görüntüleme
- Yazdığı reçeteleri görüntüleme

### 4.3 Önceden Tanımlı Sistem Verileri

Sistem içinde bazı veriler uygulama çalışmadan önce tanımlanmış olacaktır:

- Şehir listesi
- Hastane listesi
- Doktor hesapları
- Doktorların hastane bilgileri
- Doktorların branş bilgileri
- Doktor çalışma günleri
- Slot başlangıç ve bitiş saatleri
- Slot süreleri

Doktor hesapları son kullanıcı tarafından oluşturulmaz. Bu hesaplar geliştirme aşamasında Firebase Authentication ve Firestore üzerinde tanımlanır.

---

## 5. Teknolojik Yapı ve Mimari Tercihler

Projede aşağıdaki teknoloji ve mimari kararlar esas alınacaktır:

- Kotlin
- XML tabanlı arayüz geliştirme
- ViewBinding
- MVVM mimarisi
- Repository katmanı
- Use Case katmanı
- Navigation Component
- Safe Args
- Coroutines
- Flow
- StateFlow
- Firebase Authentication
- Cloud Firestore
- Material Design 3

Genel mimari yapı şu katmanlardan oluşacaktır:

```text
View (Fragment)
    ↓
ViewModel
    ↓
UseCase
    ↓
Repository
    ↓
Firebase DataSource
```

Bu yapı sayesinde iş mantığı arayüz katmanından ayrılır, test edilebilirlik artar ve proje düzenli biçimde büyütülebilir hale gelir.

---

## 6. Tek Activity ve Çoklu Fragment Yapısı

Projede tek bir `MainActivity` ve çoklu `Fragment` yapısı kullanılacaktır. Bu yaklaşımın tercih edilme nedenleri şunlardır:

- Ekran geçişlerini merkezi biçimde yönetmek
- Navigation yapısını sadeleştirmek
- Ortak bileşenleri tek Activity üzerinden kontrol etmek
- ViewModel paylaşımını kolaylaştırmak
- Back stack yönetimini düzenli hale getirmek

Uygulama içi tüm ana akış `NavHostFragment` üzerinden yönetilecektir.

---

## 7. MVVM Yapısının Görev Dağılımı

### 7.1 View Katmanı

View katmanı `Fragment` sınıflarından ve XML layout dosyalarından oluşur. Kullanıcı etkileşimlerini alır, ViewModel'den gelen UI durumlarını gözlemler ve ekrana yansıtır.

### 7.2 ViewModel Katmanı

ViewModel, ekranın iş mantığını taşır. Kullanıcıdan gelen işlemleri alır, ilgili Use Case'i çalıştırır ve sonucu `StateFlow` ile View katmanına aktarır.

### 7.3 Use Case Katmanı

Use Case katmanı, tek bir iş kuralını temsil eder. Her Use Case belirli bir eylemden sorumludur. Örneğin giriş yapma, randevu oluşturma, reçete oluşturma veya randevu iptal etme gibi işlemler ayrı Use Case'lerle ele alınır.

### 7.4 Repository Katmanı

Repository, veri erişim katmanıdır. Firestore veya Authentication işlemlerine doğrudan ViewModel'in erişmesini engeller. Veriyi uygun formatta üst katmanlara iletir.

### 7.5 DataSource Katmanı

Firebase ile doğrudan iletişim kuran katmandır. Auth ve Firestore sorguları burada gerçekleştirilir.

---

## 8. UI State Yönetimi

Projede `LiveData` yerine `StateFlow` kullanılacaktır. Bunun başlıca nedenleri şunlardır:

- Kotlin Coroutines ile doğal uyum sağlaması
- Tek yönlü ve öngörülebilir veri akışı sunması
- Yükleniyor, başarılı, hata ve boş durumlarının açıkça modellenebilmesi
- Test edilebilir ve sürdürülebilir bir yapı sunması

Temel UI durumu aşağıdaki gibi temsil edilir:

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}
```

---

## 9. Cloud Firestore Kullanımı

Projede tek veritabanı çözümü olarak Cloud Firestore kullanılacaktır. Firestore hem veri saklama hem sorgulama hem de gerçek zamanlı veri yansıtma ihtiyacını karşılayacaktır.

Bu projede Firebase Realtime Database kullanılmayacaktır. Çünkü ihtiyaç duyulan temel işlemler Firestore ile karşılanabilmektedir ve öğrenci projesi kapsamında iki ayrı veritabanı teknolojisi kullanmak gereksiz karmaşıklık oluşturur.

Firestore'un projedeki kullanım alanları şunlardır:

- Kullanıcı verilerinin saklanması
- Hasta bilgilerinin saklanması
- Şehir, hastane ve doktor verilerinin tutulması
- Randevu kayıtlarının tutulması
- Reçete verilerinin tutulması
- Slot doluluk kontrolünün yapılması
- Ekranlarda veri değişimlerinin anlık yansıtılması

---

## 10. Firestore ile Gerçek Zamanlı Veri Yansıtma

Bu projede "gerçek zamanlı kullanım" ifadesi telefon bildirimini değil, veri değiştiğinde ekranın otomatik güncellenmesini ifade eder.

Örnek senaryolar:

- Doktor reçete oluşturduğunda, hasta reçete ekranı açıksa yeni reçete otomatik görünür
- Randevu durumu güncellendiğinde ilgili liste ekranı yeniden manuel yenileme yapılmadan güncellenir
- Doktorun günlük randevu listesi, veri değiştiğinde tekrar giriş yapmaya gerek kalmadan yenilenebilir

Bu yapı Firestore'un gerçek zamanlı dinleme mekanizması ile sağlanır. Teknik olarak bu işlem `addSnapshotListener` veya ilgili dinleme yapıları ile gerçekleştirilir.

Örnek kullanım mantığı:

```kotlin
firestore.collection("prescriptions")
    .whereEqualTo("patientId", patientId)
    .addSnapshotListener { snapshot, error ->
        if (error != null) return@addSnapshotListener

        val prescriptions = snapshot?.toObjects(Prescription::class.java) ?: emptyList()
        // Güncel veriyi UI state'e aktar
    }
```

Buradaki amaç, veriyi sadece bir kez almak değil, veri değiştikçe ekranın yeni durumu otomatik olarak almasını sağlamaktır.

---

## 11. Slot Çakışmasının Transaction ile Önlenmesi

Randevu sistemlerinde en kritik problemlerden biri aynı doktora, aynı gün ve aynı saat için birden fazla hastanın eşzamanlı olarak randevu almaya çalışmasıdır. Bu durum slot çakışması olarak tanımlanır.

Bu projede slot çakışması Firestore transaction yapısı ile önlenecektir.

Transaction mantığı şu şekilde çalışır:

1. Kullanıcı belirli bir slot için randevu almak ister
2. Sistem transaction başlatır
3. İlgili doktor, tarih ve saat kombinasyonu için slotun dolu olup olmadığını kontrol eder
4. Slot boşsa randevu oluşturulur
5. Slot doluysa işlem iptal edilir

Bu yaklaşım sayesinde aynı anda birden fazla kullanıcı aynı slota istek gönderse bile yalnızca ilk başarılı işlem veritabanına yazılır. Böylece çifte rezervasyon engellenmiş olur.

Temel örnek mantık:

```kotlin
suspend fun createAppointmentSafely(appointment: Appointment): Result<Unit> {
    return try {
        firestore.runTransaction { transaction ->
            val lockRef = firestore.collection("appointmentLocks")
                .document("${appointment.doctorId}_${appointment.appointmentDate}_${appointment.appointmentTime}")

            val lockSnapshot = transaction.get(lockRef)

            if (lockSnapshot.exists()) {
                throw FirebaseFirestoreException(
                    "Seçilen saat doludur",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }

            transaction.set(lockRef, mapOf("locked" to true))
            transaction.set(
                firestore.collection("appointments").document(appointment.id),
                appointment
            )
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

Bu yapı gerçek zamanlı veri yansıtma ile karıştırılmamalıdır.

- Gerçek zamanlı veri yansıtma: verinin ekrana otomatik gelmesi
- Transaction: verinin tutarlı ve çakışmasız biçimde kaydedilmesi

Bu iki yapı farklı problemleri çözer ve projede birlikte kullanılır.

---

## 12. Kimlik Doğrulama ve Rol Yönetimi

Projede kullanıcı kimlik doğrulama işlemleri Firebase Authentication ile yapılacaktır.

Rol bilgisi doğrudan Authentication içinde değil, Firestore üzerindeki `users` koleksiyonunda tutulacaktır.

Örnek kullanıcı alanları:

- id
- fullName
- email
- role
- phone
- createdAt

Giriş akışı şu şekilde ilerler:

1. Kullanıcı e-posta ve şifre ile giriş yapar
2. Authentication başarılı olursa `users/{uid}` dökümanı okunur
3. Kullanıcının rolü belirlenir
4. Rol bilgisine göre hasta veya doktor ekranına yönlendirme yapılır

---

## 13. Veritabanı Tasarımı

### 13.1 Koleksiyonlar

```text
cities/
  {cityId}
    id: String
    name: String

hospitals/
  {hospitalId}
    id: String
    name: String
    cityId: String
    district: String

users/
  {userId}
    id: String
    fullName: String
    email: String
    role: String
    phone: String
    createdAt: Long

patients/
  {patientId}
    userId: String
    tcNo: String
    birthDate: String
    gender: String

doctors/
  {doctorId}
    id: String
    userId: String
    fullName: String
    branch: String
    hospitalId: String
    roomInfo: String
    workingDays: List<String>
    slotStartHour: Int
    slotEndHour: Int
    slotDurationMinutes: Int

appointments/
  {appointmentId}
    id: String
    patientId: String
    doctorId: String
    hospitalId: String
    appointmentDate: String
    appointmentTime: String
    status: String
    createdAt: Long

prescriptions/
  {prescriptionId}
    id: String
    patientId: String
    doctorId: String
    appointmentId: String
    prescriptionCode: String
    diagnosis: String
    notes: String
    status: String
    createdAt: Long
    medicines: [
      {
        medicineName: String
        dosage: String
        usage: String
      }
    ]

appointmentLocks/
  {lockId}
    locked: Boolean
```

### 13.2 Slot Üretim Yaklaşımı

Randevu slotları veritabanında tek tek hazır tutulmayacaktır. Bunun yerine doktorun çalışma bilgileri baz alınarak ihtiyaç anında dinamik şekilde üretilecektir.

İşleyiş:

1. Doktorun çalışma günü kontrol edilir
2. Başlangıç ve bitiş saati arasında slot listesi üretilir
3. İlgili tarih için alınmış randevular sorgulanır
4. Dolu slotlar listeden işaretlenir
5. Kullanıcıya yalnızca uygun durumdaki saatler gösterilir

Bu yaklaşım veri tekrarını azaltır ve yönetimi kolaylaştırır.

---

## 14. Temel İş Kuralları

### 14.1 Randevu Kuralları

- Hasta yalnızca uygun slotlardan randevu alabilir
- Aynı doktor, tarih ve saat için birden fazla aktif randevu oluşturulamaz
- Yalnızca `SCHEDULED` durumundaki randevu iptal edilebilir
- `COMPLETED`, `MISSED` veya `CANCELLED` durumundaki kayıtlar yeni duruma göre işlenir

### 14.2 Reçete Kuralları

- Reçete yalnızca `COMPLETED` durumundaki randevu için oluşturulabilir
- Bir randevu için en fazla bir reçete yazılabilir
- Her reçete benzersiz bir reçete koduna sahip olmalıdır
- Tanı bilgisi boş bırakılamaz
- Reçete en az bir ilaç bilgisi içermelidir

### 14.3 Doktor Verisi Kuralları

- Doktor hesabı sistemde önceden tanımlanır
- Doktor belirli bir hastaneye bağlıdır
- Doktorun branşı ve çalışma bilgileri önceden saklanır
- Doktor yalnızca kendi randevu ve reçete verilerine erişebilir

---

## 15. Temel Veri Modelleri

```kotlin
data class City(
    val id: String = "",
    val name: String = ""
)

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "",
    val phone: String = "",
    val createdAt: Long = 0L
)

data class Patient(
    val userId: String = "",
    val tcNo: String = "",
    val birthDate: String = "",
    val gender: String = ""
)

data class Hospital(
    val id: String = "",
    val name: String = "",
    val cityId: String = "",
    val district: String = ""
)

data class Doctor(
    val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val branch: String = "",
    val hospitalId: String = "",
    val roomInfo: String = "",
    val workingDays: List<String> = emptyList(),
    val slotStartHour: Int = 9,
    val slotEndHour: Int = 17,
    val slotDurationMinutes: Int = 20
)

data class TimeSlot(
    val time: String = "",
    val isAvailable: Boolean = true
)

enum class AppointmentStatus { SCHEDULED, COMPLETED, CANCELLED, MISSED }

data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val hospitalId: String = "",
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val createdAt: Long = 0L
)

data class PrescriptionItem(
    val medicineName: String = "",
    val dosage: String = "",
    val usage: String = ""
)

enum class PrescriptionStatus { ACTIVE, CANCELLED }

data class Prescription(
    val id: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val appointmentId: String = "",
    val prescriptionCode: String = "",
    val diagnosis: String = "",
    val notes: String = "",
    val medicines: List<PrescriptionItem> = emptyList(),
    val createdAt: Long = 0L,
    val status: PrescriptionStatus = PrescriptionStatus.ACTIVE
)
```

---

## 16. Önerilen Proje Klasör ve Dosya Yapısı

```text
AppointmentApp/ // Proje kök klasörü; tüm Android modülleri, Gradle ayarları ve dokümantasyon burada tutulur
├── app/ // Uygulamanın Android application modülü; çalıştırılabilir APK bu modülden üretilir
│   ├── src/main/ // Production kaynakları; kod, manifest ve kaynak dosyaları burada bulunur
│   │   ├── java/com/example/appointmentapp/ // Kotlin kodlarının ana paket kökü; MVVM katmanları burada organize edilir
│   │   │   ├── data/ // Veri katmanı; model, uzak veri kaynakları ve repository implementasyonları burada yer alır
│   │   │   │   ├── model/ // Firestore ve uygulama içi veri temsilleri (data class) burada tanımlanır
│   │   │   │   │   ├── User.kt // model klasörünün ilk dosyası; temel kullanıcı veri yapısını tanımlar
│   │   │   │   │   ├── Patient.kt
│   │   │   │   │   ├── Doctor.kt
│   │   │   │   │   ├── Hospital.kt
│   │   │   │   │   ├── City.kt
│   │   │   │   │   ├── Appointment.kt
│   │   │   │   │   ├── Prescription.kt
│   │   │   │   │   └── TimeSlot.kt
│   │   │   │   ├── remote/ // Uzak veri erişim katmanı; API/Firebase gibi dış kaynak bağlantıları burada konumlanır
│   │   │   │   │   └── firebase/ // Firestore ve Firebase Auth ile doğrudan konuşan DataSource sınıfları bu klasörde olmalıdır
│   │   │   │   │       ├── AuthDataSource.kt // firebase klasörünün ilk dosyası; kimlik doğrulama erişimini yönetir
│   │   │   │   │       ├── UserDataSource.kt
│   │   │   │   │       ├── CityDataSource.kt
│   │   │   │   │       ├── HospitalDataSource.kt
│   │   │   │   │       ├── DoctorDataSource.kt
│   │   │   │   │       ├── AppointmentDataSource.kt
│   │   │   │   │       └── PrescriptionDataSource.kt
│   │   │   │   └── repository/ // DataSource sonuçlarını domain'e uygun veri akışına dönüştüren repository sınıfları burada olmalıdır
│   │   │   │       ├── AuthRepository.kt // repository klasörünün ilk dosyası; auth akışını domain'e taşır
│   │   │   │       ├── UserRepository.kt
│   │   │   │       ├── CityRepository.kt
│   │   │   │       ├── HospitalRepository.kt
│   │   │   │       ├── DoctorRepository.kt
│   │   │   │       ├── AppointmentRepository.kt
│   │   │   │       └── PrescriptionRepository.kt
│   │   │   ├── domain/ // İş kuralları katmanı; UI ve data katmanından bağımsız use-case yapıları burada tutulur
│   │   │   │   └── usecase/ // Her biri tek sorumluluk taşıyan iş akışı sınıfları bu klasörde bulunmalıdır
│   │   │   │       ├── LoginUserUseCase.kt // usecase klasörünün ilk dosyası; giriş iş kuralını çalıştırır
│   │   │   │       ├── RegisterPatientUseCase.kt
│   │   │   │       ├── LogoutUserUseCase.kt
│   │   │   │       ├── GetCurrentUserUseCase.kt
│   │   │   │       ├── GetCitiesUseCase.kt
│   │   │   │       ├── GetHospitalsByCityUseCase.kt
│   │   │   │       ├── GetDoctorsByHospitalUseCase.kt
│   │   │   │       ├── GetAvailableSlotsUseCase.kt
│   │   │   │       ├── CreateAppointmentUseCase.kt
│   │   │   │       ├── CancelAppointmentUseCase.kt
│   │   │   │       ├── GetPatientAppointmentsUseCase.kt
│   │   │   │       ├── GetDoctorAppointmentsUseCase.kt
│   │   │   │       ├── UpdateAppointmentStatusUseCase.kt
│   │   │   │       ├── CreatePrescriptionUseCase.kt
│   │   │   │       ├── GetPatientPrescriptionsUseCase.kt
│   │   │   │       └── GetDoctorPrescriptionsUseCase.kt
│   │   │   ├── ui/ // Sunum katmanı; Fragment, ViewModel, adapter ve UI state bileşenleri burada bulunur
│   │   │   │   ├── main/ // Tek-activity mimarisi için ana activity ve host bileşenleri burada tutulur
│   │   │   │   │   └── MainActivity.kt // main klasörünün ilk dosyası; tek activity host yapısını taşır
│   │   │   │   ├── splash/ // Uygulama açılış yönlendirme ekranı ile ilgili sınıflar burada bulunur
│   │   │   │   │   └── SplashFragment.kt // splash klasörünün ilk dosyası; başlangıç yönlendirmesini yapar
│   │   │   │   ├── auth/ // Kimlik doğrulama akışı; giriş ve kayıt ekranları burada organize edilir
│   │   │   │   │   ├── login/ // Giriş ekranı UI ve state yönetimi bu alt klasörde olmalıdır
│   │   │   │   │   │   ├── LoginFragment.kt // login klasörünün ilk dosyası; giriş ekranı UI katmanıdır
│   │   │   │   │   │   └── LoginViewModel.kt
│   │   │   │   │   └── register/ // Hasta kayıt akışı UI ve state yönetimi burada bulunmalıdır
│   │   │   │   │       ├── RegisterFragment.kt // register klasörünün ilk dosyası; kayıt ekranı UI katmanıdır
│   │   │   │   │       └── RegisterViewModel.kt
│   │   │   │   ├── common/ // Uygulama genelinde ortak kullanılan UI yardımcı bileşenleri burada tutulur
│   │   │   │   │   ├── adapter/ // RecyclerView adapter sınıfları; listeleme ekranlarında yeniden kullanılmalıdır
│   │   │   │   │   │   ├── CityAdapter.kt // adapter klasörünün ilk dosyası; şehir listesi item bağlamasını yapar
│   │   │   │   │   │   ├── HospitalAdapter.kt
│   │   │   │   │   │   ├── DoctorAdapter.kt
│   │   │   │   │   │   ├── TimeSlotAdapter.kt
│   │   │   │   │   │   ├── AppointmentAdapter.kt
│   │   │   │   │   │   └── PrescriptionAdapter.kt
│   │   │   │   │   ├── state/ // UI durum sınıfları; loading-success-error gibi ortak state tanımları burada olmalıdır
│   │   │   │   │   │   └── UiState.kt // state klasörünün ilk dosyası; ortak UI durum tipini tanımlar
│   │   │   │   │   └── component/ // Ortak dialog, custom view gibi tekrar kullanılabilir UI parçaları bulunmalıdır
│   │   │   │   │       └── LoadingDialog.kt // component klasörünün ilk dosyası; ortak yükleniyor diyaloğunu sağlar
│   │   │   │   ├── patient/ // Hasta rolüne ait ekranlar ve ViewModel sınıfları bu klasörde yer almalıdır
│   │   │   │   │   ├── home/ // Hasta ana ekranı ve hasta akışına geçiş noktası burada bulunmalıdır
│   │   │   │   │   │   ├── PatientHomeFragment.kt // home klasörünün ilk dosyası; hasta panel giriş ekranıdır
│   │   │   │   │   │   └── PatientHomeViewModel.kt
│   │   │   │   │   ├── cities/ // Şehir seçimi ekranına ait dosyalar bu alt klasörde bulunmalıdır
│   │   │   │   │   │   ├── CityListFragment.kt // cities klasörünün ilk dosyası; şehir seçim listesini gösterir
│   │   │   │   │   │   └── CityListViewModel.kt
│   │   │   │   │   ├── hospitals/ // Hastane seçimi ekranı dosyaları bu alt klasörde bulunmalıdır
│   │   │   │   │   │   ├── HospitalListFragment.kt // hospitals klasörünün ilk dosyası; hastane listesini gösterir
│   │   │   │   │   │   └── HospitalListViewModel.kt
│   │   │   │   │   ├── doctors/ // Doktor seçimi ekranı dosyaları bu alt klasörde bulunmalıdır
│   │   │   │   │   │   ├── DoctorListFragment.kt // doctors klasörünün ilk dosyası; doktor listesini gösterir
│   │   │   │   │   │   └── DoctorListViewModel.kt
│   │   │   │   │   ├── slots/ // Slot seçimi ekranı dosyaları burada konumlandırılmalıdır
│   │   │   │   │   │   ├── TimeSlotFragment.kt // slots klasörünün ilk dosyası; uygun saat slotlarını listeler
│   │   │   │   │   │   └── TimeSlotViewModel.kt
│   │   │   │   │   ├── appointments/ // Hastanın randevu liste/detay ekranları burada tutulmalıdır
│   │   │   │   │   │   ├── MyAppointmentsFragment.kt // appointments klasörünün ilk dosyası; hasta randevu listesini gösterir
│   │   │   │   │   │   ├── AppointmentDetailFragment.kt
│   │   │   │   │   │   └── AppointmentViewModel.kt
│   │   │   │   │   └── prescriptions/ // Hastanın reçete liste/detay ekranları bu klasörde bulunmalıdır
│   │   │   │   │       ├── MyPrescriptionsFragment.kt // prescriptions klasörünün ilk dosyası; hasta reçete listesini gösterir
│   │   │   │   │       ├── PrescriptionDetailFragment.kt
│   │   │   │   │       └── PrescriptionViewModel.kt
│   │   │   │   └── doctor/ // Doktor rolüne ait ekranlar ve ViewModel sınıfları bu klasörde yer almalıdır
│   │   │   │       ├── home/ // Doktor ana paneli dosyaları burada bulunmalıdır
│   │   │   │       │   ├── DoctorHomeFragment.kt // home klasörünün ilk dosyası; doktor panel giriş ekranıdır
│   │   │   │       │   └── DoctorHomeViewModel.kt
│   │   │   │       ├── appointments/ // Doktor randevu liste ve detay yönetimi bu klasörde olmalıdır
│   │   │   │       │   ├── DoctorAppointmentsFragment.kt // appointments klasörünün ilk dosyası; doktor randevu listesini gösterir
│   │   │   │       │   ├── DoctorAppointmentDetailFragment.kt
│   │   │   │       │   └── DoctorAppointmentsViewModel.kt
│   │   │   │       └── prescription/ // Doktorun reçete oluşturma ekranı ve state yönetimi burada olmalıdır
│   │   │   │           ├── CreatePrescriptionFragment.kt // prescription klasörünün ilk dosyası; reçete oluşturma ekranıdır
│   │   │   │           └── CreatePrescriptionViewModel.kt
│   │   │   ├── navigation/ // Navigation tanımları; ekran yönlendirme kuralları merkezi olarak burada tutulur
│   │   │   │   ├── AppNavGraph.kt // navigation klasörünün ilk dosyası; ekran geçiş grafiğini tanımlar
│   │   │   │   └── Route.kt
│   │   │   ├── utils/ // Yardımcı fonksiyonlar ve tekrar kullanılabilir utility sınıfları burada bulunmalıdır
│   │   │   │   ├── Constants.kt // utils klasörünün ilk dosyası; ortak sabit değerleri tutar
│   │   │   │   ├── DateUtils.kt
│   │   │   │   ├── Extensions.kt
│   │   │   │   ├── ValidationUtils.kt
│   │   │   │   └── PrescriptionCodeGenerator.kt
│   │   │   └── di/ // Bağımlılıkların oluşturulduğu ve katmanlara enjekte edildiği yapı burada yer almalıdır
│   │   │       └── ServiceLocator.kt // di klasörünün ilk dosyası; bağımlılık üretimini merkezileştirir
│   │   ├── res/ // XML tabanlı UI ve görsel kaynaklar; tüm Android resource dosyaları burada bulunur
│   │   │   ├── layout/ // Activity, Fragment ve RecyclerView item XML tasarımları bu klasörde olmalıdır
│   │   │   │   ├── activity_main.xml // layout klasörünün ilk dosyası; ana activity container düzenini içerir
│   │   │   │   ├── fragment_splash.xml
│   │   │   │   ├── fragment_login.xml
│   │   │   │   ├── fragment_register.xml
│   │   │   │   ├── fragment_patient_home.xml
│   │   │   │   ├── fragment_city_list.xml
│   │   │   │   ├── fragment_hospital_list.xml
│   │   │   │   ├── fragment_doctor_list.xml
│   │   │   │   ├── fragment_time_slot.xml
│   │   │   │   ├── fragment_my_appointments.xml
│   │   │   │   ├── fragment_appointment_detail.xml
│   │   │   │   ├── fragment_my_prescriptions.xml
│   │   │   │   ├── fragment_prescription_detail.xml
│   │   │   │   ├── fragment_doctor_home.xml
│   │   │   │   ├── fragment_doctor_appointments.xml
│   │   │   │   ├── fragment_doctor_appointment_detail.xml
│   │   │   │   ├── fragment_create_prescription.xml
│   │   │   │   ├── item_city.xml
│   │   │   │   ├── item_hospital.xml
│   │   │   │   ├── item_doctor.xml
│   │   │   │   ├── item_time_slot.xml
│   │   │   │   ├── item_appointment.xml
│   │   │   │   └── item_prescription.xml
│   │   │   ├── drawable/ // Shape, selector, vector icon ve arkaplan gibi çizimsel kaynaklar burada tutulmalıdır
│   │   │   │   └── ... // Buton arkaplanı, durum etiketleri ve dekoratif çizim dosyaları bu klasörde bulunmalıdır
│   │   │   ├── mipmap/ // Launcher icon yoğunluk varyantları burada bulunmalıdır
│   │   │   │   └── ... // Uygulama ikonunun mdpi-hdpi-xhdpi vb. boyut dosyaları burada yer almalıdır
│   │   │   ├── menu/ // Toolbar, bottom navigation veya overflow menü XML tanımları burada tutulmalıdır
│   │   │   │   └── ... // Menü öğelerinin id, icon ve title tanımlarını içeren dosyalar burada olmalıdır
│   │   │   ├── navigation/ // Navigation Component graph XML dosyaları burada konumlandırılmalıdır
│   │   │   │   └── nav_graph.xml // navigation klasörünün ilk dosyası; fragment geçişlerini tanımlar
│   │   │   └── values/ // Tema, renk, metin ve ölçü gibi global kaynaklar burada tutulmalıdır
│   │   │       ├── colors.xml // values klasörünün ilk dosyası; uygulamanın renk paletini tanımlar
│   │   │       ├── strings.xml
│   │   │       ├── themes.xml
│   │   │       └── dimens.xml
│   │   └── AndroidManifest.xml // src/main içindeki ilk dosya; uygulama bileşen ve izin tanımlarını içerir
├── build.gradle.kts // proje kökündeki ilk dosya; üst seviye Gradle yapılandırmasını içerir
├── settings.gradle.kts
└── README.md
```

---

## 17. Firebase Security Rules Yaklaşımı

Firestore kuralları aşağıdaki prensiplere göre hazırlanmalıdır:

- Kimlik doğrulaması yapılmamış kullanıcı veri okuyamaz
- Hasta yalnızca kendi bilgilerini ve kendi randevu ile reçetelerini okuyabilir
- Doktor yalnızca kendisine ait randevu ve reçeteleri okuyabilir
- Şehir, hastane ve doktor verileri yalnızca okunabilir statik veri olarak tutulur
- Reçete oluşturma yetkisi yalnızca doktordadır
- Randevu oluşturma işlemi rol ve sahiplik kontrolü ile sınırlandırılır

Örnek kural yapısı:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return request.auth.uid == userId;
    }

    function hasRole(role) {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == role;
    }

    match /cities/{cityId} {
      allow read: if isAuthenticated();
      allow write: if false;
    }

    match /hospitals/{hospitalId} {
      allow read: if isAuthenticated();
      allow write: if false;
    }

    match /doctors/{doctorId} {
      allow read: if isAuthenticated();
      allow write: if false;
    }

    match /users/{userId} {
      allow read, create, update: if isAuthenticated() && isOwner(userId);
    }

    match /patients/{patientId} {
      allow read, write: if isAuthenticated() && isOwner(patientId);
    }

    match /appointments/{appointmentId} {
      allow read: if isAuthenticated() &&
        (resource.data.patientId == request.auth.uid ||
         resource.data.doctorId == request.auth.uid);

      allow create: if isAuthenticated() &&
        hasRole('patient') &&
        request.resource.data.patientId == request.auth.uid;

      allow update: if isAuthenticated() &&
        (isOwner(resource.data.patientId) || isOwner(resource.data.doctorId));
    }

    match /prescriptions/{prescriptionId} {
      allow read: if isAuthenticated() &&
        (resource.data.patientId == request.auth.uid ||
         resource.data.doctorId == request.auth.uid);

      allow create: if isAuthenticated() &&
        hasRole('doctor') &&
        request.resource.data.doctorId == request.auth.uid;
    }
  }
}
```

---

## 18. Uygulama Akışları

### 18.1 Oturum ve Yönlendirme Akışı

```text
Uygulama açılır
    → Splash ekranı çalışır
    → Mevcut oturum kontrol edilir
        → Oturum yoksa giriş ekranı açılır
        → Oturum varsa kullanıcı rolü Firestore'dan okunur
            → Hasta ise hasta ana ekranı açılır
            → Doktor ise doktor ana ekranı açılır
```

### 18.2 Hasta Randevu Alma Akışı

```text
Hasta ana ekranı
    → Şehir seçimi
    → Hastane seçimi
    → Doktor seçimi
    → Tarih ve uygun saatlerin görüntülenmesi
    → Slot seçimi
    → Transaction ile randevu oluşturma
    → Randevularım ekranında görüntüleme
```

### 18.3 Doktor Randevu ve Reçete Akışı

```text
Doktor ana ekranı
    → Randevu listesi
    → Randevu detayı
    → Muayene sonucu durum güncelleme
        → COMPLETED ise reçete oluşturma ekranı açılabilir
        → Reçete bilgileri kaydedilir
        → Hasta reçete ekranında veriyi anlık görebilir
```

### 18.4 Hasta Reçete Görüntüleme Akışı

```text
Hasta reçetelerim ekranı
    → Firestore listener ile veri dinlenir
    → Yeni reçete oluşursa liste otomatik güncellenir
    → Kullanıcı reçete detay ekranına geçer
```

---

## 19. Geliştirme Sırası

### Aşama 1: Proje Kurulumu

- Android Studio projesinin oluşturulması
- Firebase bağlantısının yapılması
- Gerekli Gradle bağımlılıklarının eklenmesi
- Navigation altyapısının kurulması
- ViewBinding ve tema yapısının hazırlanması

### Aşama 2: Kimlik Doğrulama ve Temel Kullanıcı Yapısı

- Splash ekranı
- Giriş ekranı
- Hasta kayıt ekranı
- Firebase Authentication entegrasyonu
- Firestore `users` ve `patients` kayıt akışı
- Rol bazlı yönlendirme

### Aşama 3: Statik Verilerin Hazırlanması

- Şehir verilerinin eklenmesi
- Hastane verilerinin eklenmesi
- Doktor verilerinin eklenmesi
- Şehir, hastane ve doktor liste ekranlarının hazırlanması

### Aşama 4: Randevu Yönetimi

- Doktor çalışma bilgilerine göre slot üretimi
- Doluluk kontrolü
- Transaction ile güvenli randevu oluşturma
- Randevularım ekranı
- Randevu iptal akışı

### Aşama 5: Doktor Süreci

- Doktor randevu liste ekranı
- Randevu detay ekranı
- Durum güncelleme işlemleri

### Aşama 6: Reçete Yönetimi

- Reçete oluşturma ekranı
- Reçete kodu üretimi
- Reçetelerim ekranı
- Reçete detay ekranı
- Firestore listener ile gerçek zamanlı veri güncelleme

### Aşama 7: Güvenlik ve İyileştirme

- Security Rules yazımı
- Form validasyonları
- Hata yönetimi
- Loading ve empty state ekranları
- Görsel iyileştirmeler

---

## 20. Sonuç

Bu dokümanda tanımlanan yapı, üniversite projesi kapsamında geliştirilecek Android tabanlı Merkezi Hastane Randevu, Hasta Takip ve E-Reçete Sistemi için teknik temel oluşturmaktadır.

Belirlenen yaklaşımın temel özellikleri şunlardır:

- Düzenli katmanlı mimari
- Firestore tabanlı merkezi veri yönetimi
- Transaction ile güvenli randevu oluşturma
- Firestore listener ile gerçek zamanlı veri yansıtma
- Hasta ve doktor odaklı yalın ama genişletilebilir sistem yapısı
- Android geliştirme sürecinde doğrudan uygulanabilecek net klasör organizasyonu

Bu yapı, proje geliştirme sürecinde teknik referans ve uygulama planı olarak kullanılmalıdır.
