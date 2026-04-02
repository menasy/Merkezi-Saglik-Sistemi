# Merkezi Sağlık Sistemi

<table>
  <tr>
    <td valign="top" width="70%">
      <p><strong>Merkezi Sağlık Sistemi</strong>, Kotlin + XML ile geliştirilmiş, rol tabanlı çalışan bir Android mobil sağlık uygulamasıdır.</p>
      <p>Uygulama; <strong>hasta</strong> ve <strong>doktor</strong> akışlarını tek platformda birleştirir, kimlik doğrulama için <strong>Firebase Authentication</strong>, operasyonel kayıt yönetimi için <strong>Cloud Firestore</strong> kullanır. Şehir, ilçe, hastane, branş, doktor ve ilaç gibi referans veriler ise performans ve deterministik davranış için uygulama içi seed veri setinden okunur.</p>
      <p>Sonuç olarak sistem; merkezi randevu planlama, muayene takibi ve reçete yönetimini aynı mimari çatı altında, sürdürülebilir bir katmanlı yapı ile sunar.</p>
    </td>
    <td align="right" valign="top" width="30%">
      <img src="./merkezi-saglik-sistemi-logo.png" alt="Merkezi Sağlık Sistemi Logo" width="180" />
    </td>
  </tr>
</table>

## 1. Giriş
Merkezi Sağlık Sistemi, sağlık randevu süreçlerini uçtan uca dijitalleştiren bir Android uygulamasıdır. Sistem iki farklı kullanıcı tipini tek istemcide yönetir:
- Hasta: randevu arama, slot seçimi, randevu oluşturma/iptal, reçete görüntüleme
- Doktor: randevu yönetimi, muayene sonuçlandırma, reçete oluşturma ve izleme

Temel tasarım ilkesi, veriyi iki ayrı katmanda yönetmektir:
- Operasyonel veri (kullanıcı, randevu, reçete): Firebase Authentication + Cloud Firestore
- Referans veri (şehir, ilçe, hastane, branş, doktor, ilaç): Local seed data

Bu ayrım sayesinde yüksek okuma trafiği oluşturan sabit veriler hızlı ve düşük maliyetli çalışırken, operasyonel veriler gerçek zamanlı ve tutarlı biçimde yönetilir.

## 2. Proje Amacı
Projenin teknik hedefleri:
- Randevu alma sürecini kural tabanlı ve güvenilir hale getirmek
- Aynı doktora aynı tarih/saat için çift rezervasyonu transaction + lock ile engellemek
- Rol bazlı ekran ve veri erişimini net ayrıştırmak
- Hasta ve doktor akışlarını aynı uygulamada sade ve tutarlı bir UX ile sunmak
- Mimariyi sürdürülebilir, genişletilebilir ve bakımı kolay bir yapıda tutmak

## 3. Sistem Özeti
Sistem akışı özetle şu şekilde çalışır:
1. Splash, aktif oturumu kontrol eder ve kullanıcıyı role göre yönlendirir.
2. Hasta tarafında randevu filtreleme local seed veriler üzerinden yapılır.
3. Slot seçimi ekranında uygunluk bilgisi Firestore üzerinden gerçek zamanlı izlenir.
4. Randevu oluşturma transaction ile gerçekleştirilir; lock mekanizmasıyla çakışma önlenir.
5. Doktor, zamanı gelmiş randevuyu muayene eder; gerekiyorsa reçete oluşturur.
6. Hasta ve doktor listeleri snapshot listener ile canlı güncellenir.

## 4. Öne Çıkan Özellikler
- MVVM tabanlı katmanlı mimari
- ViewModel + StateFlow ile reaktif UI state yönetimi
- UseCase katmanında ayrıştırılmış iş kuralları
- Repository üzerinden veri kaynağı soyutlama
- Firestore transaction ile slot çakışma önleme
- Firestore snapshot listener ile gerçek zamanlı güncelleme
- Local seed veri ile hızlı referans sorguları
- Global hata/mesaj altyapısı (`AppErrorMapper`, `GlobalMessageView`)
- Role göre dinamik alt menü ve navigasyon

## 5. Kullanıcı Rolleri
### Hasta (`patient`)
- Uygulama içinden kayıt olabilir
- Standart e-posta/şifre girişi yapar
- Randevu arar, oluşturur, iptal eder
- Aktif/geçmiş randevularını ve reçetelerini görüntüler

### Doktor (`doctor`)
- Uygulama içinden kayıt olmaz
- Önceden tanımlı hesapla giriş yapar
- Sadece kendi randevu ve reçete verilerini görüntüler
- Muayene sonucu girer, reçete oluşturur ve süreci tamamlar

## 6. Uygulama Kullanımı
### 6.1 Hasta Kullanımı
1. Uygulama açılır.
2. Gerekirse `Kayıt Ol` ekranından hasta hesabı oluşturulur.
3. `Giriş Yap` ekranından oturum açılır.
4. Hasta ana ekrana yönlenilir.
5. Randevu arama akışına geçilir.
6. İl, ilçe (opsiyonel), branş, hastane (opsiyonel), doktor (opsiyonel) seçilir.
7. Tarih aralığı belirlenir.
8. Uygun sonuçlar listelenir.
9. Doktor ve saat seçilir.
10. Randevu onaylanır.
11. `Randevularım` ekranında aktif/geçmiş kayıtlar izlenir.
12. Kurala uygunsa aktif randevu iptal edilir.
13. `Reçetelerim` ekranında reçeteler filtrelenerek görüntülenir.

### 6.2 Doktor Kullanımı
1. Doktor, ön tanımlı hesabıyla giriş yapar.
2. Doktor ana ekrana yönlenir.
3. Günlük özet ve randevu durumlarını görüntüler.
4. `Randevular` ekranında bekleyen/gelecek/geçmiş kayıtları yönetir.
5. Bekleyen randevuda `Muayene Et` ile muayene sürecini başlatır.
6. Muayene notu, reçete kararı ve ilaç seçimlerini tamamlar.
7. Muayeneyi `COMPLETED` veya `MISSED` olarak sonuçlandırır.
8. `Reçeteler` ekranında oluşturduğu reçeteleri izler.
9. Hesap ekranından çıkış yapar.

### 6.3 Test Doktor Hesapları
| Doktor | Hastane | Email | Şifre |
|---|---|---|---|
| Prof. Dr. Yunus Yıldız | Istanbul Mehmet Akif Ersoy Gogus Kalp ve Damar Cerrahisi EAH | `doctor2@gmail.com` | `User123` |
| Prof. Dr. Mehmet Nasim Yılmaz | Adana Sehir Hastanesi | `doctor3@gmail.com` | `User123` |
| Prof. Dr. Mahsum Turgut | Basaksehir Cam ve Sakura Sehir Hastanesi | `doctor1@gmail.com` | `User123` |

Notlar:
- Hasta kullanıcılar uygulama içinden kayıt olabilir.
- Doktor kullanıcılar uygulama içinden kayıt olmaz.
- Doktor hesapları Firebase Auth + Firestore tarafında ön tanımlı olmalıdır.

## 7. Mimari Yapı
Uygulama, tek activity + çok fragment yaklaşımıyla Navigation Component üzerinde çalışır.

Mimari kararlar:
- MVVM
- Tek yönlü veri akışı
- StateFlow tabanlı ekran durumu
- UseCase ile iş kuralı izolasyonu
- Repository ile veri erişim soyutlaması
- DataSource katmanında Firebase/local ayrımı
- `ServiceLocator` ile bağımlılık yönetimi
- `SessionCache` ile oturum içi hızlı erişim

Temsili klasör/dosya yerleşimi:
```text
app/src/main/java/com/menasy/merkezisagliksistemi
├── MainActivity.kt                            # Tek activity, nav host ve global message host
├── data/                                      # Veri katmanı
│   ├── model/                                 # Entity/data class modelleri
│   │   ├── Appointment.kt
│   │   ├── Prescription.kt
│   │   ├── User.kt
│   │   └── seedData/                          # Local referans veri setleri
│   │       ├── CitySeedData.kt
│   │       ├── DistrictSeedData.kt
│   │       ├── HospitalSeedData.kt
│   │       ├── DoctorSeedData.kt
│   │       └── MedicineSeedData.kt
│   ├── remote/                                # Veri kaynakları
│   │   ├── firebase/                          # Auth + Firestore operasyonel kaynakları
│   │   │   ├── AuthDataSource.kt
│   │   │   └── AppointmentDataSource.kt
│   │   └── local/                             # Seed tabanlı referans veri kaynakları
│   │       ├── CityDataSource.kt
│   │       ├── HospitalDataSource.kt
│   │       └── DoctorDataSource.kt
│   └── repository/                            # DataSource soyutlama katmanı
│       ├── AuthRepository.kt
│       ├── AppointmentRepository.kt
│       └── DoctorRepository.kt
├── domain/                                    # İş kuralı katmanı
│   └── usecase/                               # Uygulama senaryoları (login, create/cancel appointment vb.)
├── ui/                                        # Sunum katmanı
│   ├── auth/                                  # Login/Register ekranları
│   ├── splash/                                # Oturum kontrolü ve role göre yönlendirme
│   ├── patient/                               # Hasta modülü ekranları
│   │   ├── appointmentflow/
│   │   ├── appointmentlist/
│   │   ├── prescriptions/
│   │   └── account/
│   ├── doctor/                                # Doktor modülü ekranları
│   │   ├── home/
│   │   ├── appointments/
│   │   ├── examination/
│   │   ├── prescriptions/
│   │   └── account/
│   └── common/                                # Ortak base, error, message, widget bileşenleri
├── di/                                        # Servis sağlama ve session yönetimi
│   ├── ServiceLocator.kt
│   └── SessionCache.kt
└── utils/                                     # Yardımcı fonksiyonlar (tarih/saat vb.)
    └── DateTimeUtils.kt
```

## 8. Uygulama Pipeline’ı ve Katmanlar
```text
View (Fragment)
  -> ViewModel
    -> UseCase
      -> Repository
        -> DataSource (Firebase / Local)
```

Katman sorumlulukları:
- View: kullanıcı etkileşimi ve render
- ViewModel: state + UI event yönetimi
- UseCase: iş kuralları
- Repository: veri kaynağı soyutlama
- DataSource: doğrudan veri erişimi

## 9. Teknoloji Yığını
| Teknoloji | Kullanım Amacı |
|---|---|
| Kotlin | Uygulama geliştirme dili |
| XML | Ekran ve bileşen tanımı |
| Android Studio | Geliştirme, debug, build |
| Firebase Authentication | Kimlik doğrulama |
| Cloud Firestore | Operasyonel veri depolama + realtime |
| Navigation Component | Fragment geçiş yönetimi |
| ViewBinding | Tip güvenli view erişimi |
| Coroutines | Asenkron programlama |
| Flow / StateFlow | Reaktif veri ve state akışı |
| Material Components | Modern Android UI bileşenleri |

Teknik temel:
- `minSdk 24`
- `targetSdk 36`
- `compileSdk 36`
- Java 11 uyumluluğu

## 10. Firebase Authentication Yapısı
Auth akışı:
- Kayıt (hasta): `createUserWithEmailAndPassword`
- Giriş: `signInWithEmailAndPassword`
- Oturum kontrolü: `currentUser`
- Çıkış: `signOut`

Giriş sonrası:
1. UID alınır.
2. `users/{uid}` belgesi okunur.
3. `role` alanına göre yönlendirme yapılır.
4. Doktor rolünde ek olarak seed doktor profili UID eşleşmesi doğrulanır.

## 11. Firestore Veri Yapısı
Operasyonel koleksiyonlar:
- `users`
- `patients`
- `appointments`
- `prescriptions`
- `appointmentLocks`

Koleksiyonların görevi:
- `users`: kullanıcı üst profili ve rol
- `patients`: hasta ek bilgileri
- `appointments`: randevu yaşam döngüsü
- `prescriptions`: reçete kayıtları
- `appointmentLocks`: slot kilit dokümanları

## 12. Veritabanı Şeması / Koleksiyon Mantığı
| Koleksiyon | Doküman ID | Alanlar |
|---|---|---|
| `users` | `uid` | `id`, `fullName`, `email`, `role`, `createdAt` |
| `patients` | `uid` | `userId`, `tcNo`, `birthDate`, `gender` |
| `appointments` | `appointmentId` | `id`, `patientId`, `doctorId`, `hospitalId`, `branchId`, `appointmentDate`, `appointmentTime`, `status`, `examinationNote`, `createdAt`, `completedAt`, `completedDate` |
| `prescriptions` | `appointmentId` | `id`, `appointmentId`, `prescriptionCode`, `createdAtMillis`, `note`, `medicines` |
| `appointmentLocks` | `doctorId_yyyy-MM-dd_HH:mm` | `doctorId`, `date`, `time`, `createdAt` |

> Not: Doktor referans profilleri Firestore `doctors` koleksiyonundan okunmaz; local seed (`DoctorSeedData`) kullanılır.

## 13. Local Seed / Referans Veri Yapısı
Seed kaynakları:
- `CitySeedData.kt`
- `DistrictSeedData.kt`
- `HospitalSeedData.kt`
- `BranchSeedData.kt`
- `DoctorSeedData.kt`
- `MedicineSeedData.kt`

Bu yapı uygulamada şu avantajları sağlar:
- Hızlı filtreleme
- Düşük ağ bağımlılığı
- Tutarlı sonuç üretimi

## 14. Veri Modelleri ve Data Class Yapıları
Temel modeller:
- `User`
- `Patient`
- `Doctor`
- `City`
- `District`
- `Hospital`
- `Branch`
- `Appointment`
- `Prescription`
- `Medicine`
- `AppointmentStatus`

Model ilişkisi:
- `User (doctor/patient)` kimlik katmanıdır.
- `Patient`, `User` ile `uid` üzerinden ilişkilidir.
- `Appointment`, `patientId` + `doctorId` + kurum bilgileriyle süreci temsil eder.
- `Prescription`, doğrudan bir `appointmentId` ile ilişkilidir.

Not:
- Kod tabanında ayrı bir `PrescriptionStatus` enum’u bulunmaz.
- `PrescriptionItem` ayrı bir data class olarak değil, `Prescription.medicines` listesi üzerinden temsil edilir.

## 15. Veri Akışı
### 15.1 Kimlik ve Session
1. Auth giriş/kayıt tamamlanır.
2. Firestore `users` belgesi okunur.
3. SessionCache doldurulur.
4. Splash ve ekran geçişleri cache + role göre yönetilir.

### 15.2 Randevu
1. Kullanıcı filtreleri seçer (local seed).
2. Uygun doktor/tarih/saat seçenekleri üretilir.
3. Slot seçimi realtime dolulukla doğrulanır.
4. Randevu transaction ile yazılır.
5. Liste ekranları realtime güncellenir.

### 15.3 Muayene ve Reçete
1. Doktor zamanı gelmiş randevuyu açar.
2. Sonuç `COMPLETED` veya `MISSED` olarak işlenir.
3. Gerekli ise reçete aynı işlemde oluşturulur.
4. Hasta/doktor reçete ekranlarında kayıt görünür.

## 16. Randevu Sistemi Teknik Mantığı
Ana kurallar:
- Tarih aralığı en fazla 15 gün
- Geçmiş saat/tarih için yeni randevu oluşturulamaz
- Aynı hasta, aynı doktor için ikinci aktif randevu alamaz
- Hasta aynı anda en fazla 5 aktif randevuya sahip olabilir

Randevu akışı:
- Arama -> Sonuç -> Uygunluk -> Slot -> Onay -> Oluşturma
- Aktif/geçmiş ayrımı `status` ve zaman bilgisinden türetilir
- İptal işlemi uygun koşullarda lock kaldırarak tamamlanır

## 17. Gerçek Zamanlı Veri Güncelleme Mantığı
Realtime güncelleme Flow + snapshot listener üzerinden çalışır:
- Doktor gün/saat doluluk takibi
- Hastanın randevu listesinin canlı güncellenmesi
- Doktorun randevu listesinin canlı güncellenmesi

Reçete listeleri için yaklaşım:
- Önce ilgili randevular stream olarak alınır
- Ardından gerekli appointment ID’leri için reçete önizlemeleri batch sorgularla çekilir

## 18. Slot Çakışmasını Önleme Mantığı
Slot çakışması Firestore transaction ile engellenir:
1. Randevu oluşturma öncesi iş kuralları doğrulanır.
2. Slot için lock ID üretilir.
3. Transaction içinde lock belgesi kontrol edilir.
4. Lock varsa işlem reddedilir (`SLOT_ALREADY_TAKEN`).
5. Lock yoksa lock + appointment atomik olarak yazılır.

Lock temizliği:
- Randevu iptal edildiğinde lock silinir.
- Randevu muayene ile sonuçlandığında lock silinir.

## 19. Hasta Modülü
Hasta tarafındaki ekranlar:
- Splash
- Register
- Login
- Hasta anasayfa
- Randevu arama
- Sonuçlar
- Doktor uygunluğu ve slot seçimi
- Randevu onayı
- Randevularım (aktif/geçmiş)
- Reçetelerim
- Hesabım

## 20. Doktor Modülü
Doktor tarafındaki ekranlar:
- Doktor anasayfa
- Randevular (bekleyen/gelecek/geçmiş)
- Muayene ekranı
- Reçeteler
- Hesabım

Doktor akışında erişim kuralı:
- Doktor yalnızca kendi `doctorId` verilerine erişir.

## 21. Reçete Sistemi
Reçete sistemi randevuya bağlıdır:
- Bağlantı: `doctor -> appointment -> prescription -> patient`
- Reçete kararı muayene sırasında verilir.
- Reçete gerekli ise en az bir ilaç seçimi zorunludur.
- Reçete kaydı ve randevu sonuçlandırma aynı işlem bütünlüğünde yürütülür.
- Reçete kodu sistem tarafından üretilir.

## 22. Kurulum
1. Repo’yu klonlayın:
```bash
git clone https://github.com/menasy/Merkezi-Saglik-Sistemi.git
cd Merkezi-Saglik-Sistemi
```
2. Projeyi Android Studio ile açın.
3. `app/google-services.json` dosyasını Firebase projenize göre ekleyin.
4. Gradle senkronizasyonunu tamamlayın.
5. Build alın:
```bash
./gradlew :app:assembleDebug
```

## 23. Çalıştırma
- Android Studio üzerinden emülatör veya fiziksel cihazda `Run` ile başlatın.
- Terminalden doğrulama komutları:
```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## 24. Gerekli İzinler
`AndroidManifest.xml` içinde kullanılan temel izin:
- `android.permission.INTERNET`

## 25. Güvenlik Yaklaşımı
- Rol bazlı yönlendirme ve ekran erişimi
- Randevu sahipliği doğrulaması (hasta iptal işlemi)
- Doktor sahipliği doğrulaması (muayene sonuçlandırma)
- Transaction tabanlı veri tutarlılığı
- SessionCache temizlenerek güvenli çıkış

Öneri:
- Üretim ortamında Firestore Security Rules, rol ve sahiplik temelli erişimi zorunlu kılacak şekilde yapılandırılmalıdır.

## 26. UI/UX Yaklaşımı
- XML tabanlı, sade ve okunabilir ekran kurgusu
- Kart tabanlı bilgi sunumu
- Aşamalı filtreleme ile düşük hata oranı
- Slot seçiminde hızlı chip tabanlı etkileşim
- Role göre değişen alt menü ve navigasyon
- Reçete önizlemede mobil uyumlu dialog/bottom sheet yaklaşımı
- Merkezi ve tutarlı hata/başarı mesajları

## 27. Sonuç
Merkezi Sağlık Sistemi; randevu, muayene ve reçete süreçlerini tek mobil uygulamada birleştiren, teknik olarak tutarlı ve genişletilebilir bir sağlık platformudur.

Proje; katmanlı mimarisi, gerçek zamanlı veri akışı, transaction tabanlı tutarlılık yaklaşımı ve local seed referans veri tasarımı ile hem geliştirme hem operasyon perspektifinde güçlü bir temel sunar.
