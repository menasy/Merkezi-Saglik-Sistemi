# Merkezi Saglik Sistemi

Bu dokuman, projenin **29 Mart 2026** itibariyla mevcut durumunu anlatir.
Hedefi: Projede su an calisan ozellikleri, teknik mimariyi ve UI -> Firestore -> UI veri akislarini dosya bazinda netlestirmek.

## 1) Proje Ozeti

`MerkeziSaglikSistemi`, Kotlin + XML tabanli, Firebase destekli bir Android uygulamasidir.

Su anda aktif olarak calisan cekirdek akislar:
1. Splash + oturum kontrolu
2. Hasta kayit
3. Giris (hasta/doktor rol ayrimi)
4. Hasta tarafinda randevu arama (il/ilce/poliklinik/hastane/hekim + tarih araligi)
5. Randevu sonucu listeleme
6. Doktor uygun saat secimi
7. Randevu onay ekrani (UI tamam, backend kayit asamasi henuz TODO)

## 2) Mevcut Ozellikler

### 2.1 Kimlik ve Oturum
1. Firebase Authentication ile e-posta/sifre kayit
2. Firebase Authentication ile e-posta/sifre giris
3. Giriste Firestore `users` dokumanindan rol (`patient`/`doctor`) okunmasi
4. Acilis ekraninda aktif oturum + rol kontrolu ile ilgili ana ekrana yonlendirme

### 2.2 Referans Veri Hazirlama
1. Uygulama acilisinda `cities`, `districts`, `hospitals`, `branches`, `doctors` koleksiyonlarinin varligi kontrol edilir.
2. Eksik koleksiyon varsa seed datadan Firestore'a batch write ile doldurulur.

### 2.3 Randevu Arama Akisi (Hasta)
1. Filtreler: Il, ilce (opsiyonel), poliklinik, hastane (opsiyonel), hekim (opsiyonel)
2. Tarih araligi secimi (en fazla 15 gun)
3. Dinamik bagimli dropdown akisi:
- Il secilince ilceler yuklenir
- Il/ilceye gore hastaneler yuklenir
- Hastane secildiginde ilgili hekimler yuklenir
4. Sonuc ekraninda uygun doktor randevu kartlari listelenir

### 2.4 Doktor Uygunluk ve Onay Akisi
1. Sonuc ekranindan secilen doktora gidilir
2. Gun ve saat bloklari olusturulur
3. Saat secimi yapilir
4. Onay ekraninda doktor/hastane/poliklinik/tarih/saat + hasta adi gosterilir
5. `Randevu Onayla` aksiyonu su an bilgi mesaji gosterir (Firestore'a randevu yazma henuz yok)

### 2.5 Ortak UI Altyapisi
1. Global mesaj sistemi (success/error/warning/info)
2. Custom hasta bottom menu
3. BaseFragment + BaseViewModel uzerinden ortak event/messaging

## 3) Henuz Tamamlanmamis Alanlar

1. Gercek randevu olusturma (`appointments` koleksiyonuna write) yok
2. Randevu iptal/tamamlama akisi yok
3. Reçete modulu su an placeholder ekran
4. Hasta hesap/profil modulu su an placeholder ekran
5. Doktor ana sayfa su an ozet/placeholder
6. Gercek zamanli Firestore listener (`addSnapshotListener`) kullanimi su an yok

## 4) Kullanilan Teknolojiler ve Kutuphaneler

1. Dil: Kotlin
2. UI: XML + ViewBinding
3. Mimari: MVVM + UseCase + Repository + DataSource
4. Navigation: AndroidX Navigation Fragment/UI KTX (Safe Args plugin yok, manuel `Bundle` args var)
5. Asenkron: Kotlin Coroutines + StateFlow/SharedFlow
6. Backend: Firebase Authentication + Cloud Firestore + Firebase Analytics
7. UI Kit: Material Design 3 (`com.google.android.material`)
8. Build: AGP 9.1.0, Java 11 source/target

## 5) Mimari Katmanlar ve Dosya Sorumluluklari

### 5.1 Giris Noktalari
1. `MainActivity`: Navigation host, global message host, bottom menu kontrolu
2. `nav_graph.xml`: Tum ekran gecisleri
3. `ServiceLocator`: Firebase instance + data source/repo/use case wiring

### 5.2 Data Katmani
1. `data/model/*`: Domain/data modeller
2. `data/model/seedData/*`: Referans ve uretilmis seed veriler
3. `data/remote/firebase/*`: Firebase islemlerinin dogrudan yapildigi katman
4. `data/repository/*`: Data source'lari use case katmanina acan adapter katman

### 5.3 Domain Katmani
1. `domain/usecase/*`: Tek sorumluluklu is akislari

### 5.4 UI Katmani
1. `ui/auth/*`: Login/Register
2. `ui/splash/*`: Acilis + oturum yonlendirme
3. `ui/patient/*`: Hasta ekranlari
4. `ui/patient/appointment/*`: Randevu akisi (arama/sonuc/uygunluk/onay)
5. `ui/common/*`: Base, state, hata mapleme, global mesaj altyapisi, bottom menu widget

## 6) UI -> Firestore -> UI Teknik Akislari (Dosya Bazinda)

### 6.1 Uygulama Acilisi ve Seed
1. UI: `SplashFragment` -> `SplashViewModel.initializeApp()`
2. UseCase: `InitializeReferenceDataUseCase`
3. Repository: `ReferenceDataRepository`
4. DataSource: `ReferenceDataSource.ensureReferenceDataInitialized()`
5. Firestore:
- `cities`, `districts`, `hospitals`, `branches`, `doctors` koleksiyonlarina `limit(1)` kontrol
- Eksikse `batch.set(...)` ile seed write
6. Geri Donus UI:
- Basarili ise `GetCurrentUserUseCase` ile session/rol kontrol
- Role gore login/patientHome/doctorHome yonlendirme

### 6.2 Hasta Kayit
1. UI: `RegisterFragment` form -> `RegisterViewModel.registerPatient(...)`
2. UseCase: `RegisterPatientUseCase`
3. Repository: `AuthRepository.registerPatient`
4. DataSource: `AuthDataSource.registerPatient`
5. Firebase islemleri:
- `FirebaseAuth.createUserWithEmailAndPassword`
- Firestore `users/{uid}` dokumanina `User` write
- Firestore `patients/{uid}` dokumanina `Patient` write
6. UI geri donus:
- Basari mesaji + login ekranina donus
- Hata durumunda `AppErrorMapper` ile anlamli mesaj

### 6.3 Giris ve Rol Cozumu
1. UI: `LoginFragment` -> `LoginViewModel.login(email,password)`
2. UseCase: `LoginUserUseCase`
3. Repository: `AuthRepository.login`
4. DataSource: `AuthDataSource.login`
5. Firebase islemleri:
- `FirebaseAuth.signInWithEmailAndPassword`
- Firestore `users/{uid}` okumasi
- `role` okunur
6. UI geri donus:
- `patient` -> patient home
- `doctor` -> doctor home

### 6.4 Randevu Arama Filtre Verisi
1. UI: `AppointmentSearchFragment` acilinca `loadInitialData()`
2. ViewModel: `AppointmentSearchViewModel`
3. UseCase zinciri:
- `GetCitiesUseCase` -> `CityDataSource.getCities()` -> Firestore `cities`
- `GetBranchesUseCase` -> `DoctorDataSource.getBranches()` -> Firestore `branches`
4. Il secimi:
- `GetDistrictsByCityUseCase` -> Firestore `districts.whereEqualTo(cityId)`
- Sonra `GetHospitalsByDistrictUseCase` -> Firestore `hospitals.whereEqualTo(cityId)` (+ opsiyonel district)
5. Hastane secimi:
- `GetDoctorsUseCase(hospitalId, branchId)` -> Firestore `doctors.whereEqualTo(hospitalId)`
- Branch filtreleme local alias map ile yapilir
6. UI geri donus:
- Dropdown'lar stateflow ile guncellenir

### 6.5 Randevu Sonuc Listesi
1. UI: `AppointmentSearchFragment` kriteri `AppointmentSearchArgs` olarak bundle'lar
2. UI: `AppointmentResultsFragment` args okur -> `AppointmentResultsViewModel.loadAppointments(...)`
3. Veri toplama:
- Hastaneler (`GetHospitalsByDistrictUseCase`)
- Poliklinikler (`GetBranchesUseCase`)
- Doktorlar:
  - Hastane seciliyse tek hastane query
  - Hastane secili degilse `whereIn(hospitalId)` batch query (limit 30, max 60 hastane)
4. Sonuc uretimi:
- Doktor + hastane + poliklinik adlari birlestirilir
- Tarih araligi icinde deterministic bir gun atanir (`hash` tabanli)
5. UI geri donus:
- RecyclerView kartlari

### 6.6 Doktor Uygunluk ve Saat Secimi
1. UI: Sonuc kartindan `DoctorAvailabilityArgs` bundle
2. ViewModel: `DoctorAvailabilityViewModel.load(...)`
3. Veri uretimi:
- Secilen tarih araligindan max 5 gun
- Saat bloklari `slotStartHour`, `slotEndHour`, `slotDurationMinutes`
- Her slot `doctorId|date|time` hash'i ile uygun/dolu simulasyonu
4. UI geri donus:
- Saat bloklari chip gruplari
- Secilen slot ozeti

### 6.7 Onay Ekrani
1. UI: `AppointmentConfirmationFragment`
2. ViewModel: `AppointmentConfirmationViewModel.load(args)`
3. Veri:
- Args'dan doktor/hastane/poliklinik/tarih/saat
- `GetCurrentUserUseCase.getCurrentUserFullName()` ile Firestore `users/{uid}.fullName`
4. Onay aksiyonu:
- `confirm()` su an sadece bilgilendirme mesaji basar
- Firestore write yok

## 7) Firestore Koleksiyonlari ve Alanlar

### 7.1 Aktif Kullanilan Koleksiyonlar
1. `users`
- `id`, `fullName`, `email`, `role`, `createdAt`
2. `patients`
- `userId`, `tcNo`, `birthDate`, `gender`
3. `cities`
- `id`, `name`
4. `districts`
- `id`, `cityId`, `name`
5. `hospitals`
- `id`, `cityId`, `districtId`, `name`, `detsisCode`, `bedCount`
6. `branches`
- `id`, `name`
7. `doctors`
- `id`, `userId`, `fullName`, `branchId`, `hospitalId`, `roomInfo`, `slotStartHour`, `slotEndHour`, `slotDurationMinutes`

### 7.2 Tanimli ama su an aktif write/read olmayan model
1. `Appointment` modeli var ama Firestore'da aktif CRUD yok

## 8) Query ve Islem Stratejileri

1. Hastane query: once `cityId`, opsiyonel `districtId`
2. Ilce filtreli hastane bos donerse fallback: sadece `cityId`
3. Doktor query: composite index zorlamamak icin once `hospitalId`; branch filtreleme uygulama tarafinda
4. Coklu hastane doktor aramasi: `whereIn(hospitalId)` batch (30 limit, max 60 hastane)
5. Branch normalizasyonu:
- Seed branch listesi canonical kaynak
- Normalized ad karsilastirmasi ve alias esleme
- Randevuya uygun olmayan branch keyword elemesi

## 9) Depolama ve Cache Mekanizmalari

### 9.1 Kalici Depolama
1. Cloud Firestore (uzak kalici veri)
2. Firebase Authentication (oturum ve kullanici kimligi)

### 9.2 Uygulama Ici/In-Memory Durum
1. ViewModel `StateFlow` state cache
2. `SharedFlow` UI event buffer (global mesaj eventleri)
3. `DoctorDataSource` icindeki companion object map/set cache'leri:
- `seedBranchById`, `seedBranchByNormalizedName`
- `nonAppointmentKeywords`
4. `DoctorSeedData` lazy initialization (`by lazy`) ile bellek ici uretilmis seed

### 9.3 Notlar
1. Room yok
2. DataStore/SharedPreferences tabanli custom cache yok
3. Manuel disk cache katmani yok
4. Firestore offline persistence acik/kapali olarak kodda ozellestirilmemis (SDK default davranisi kullaniliyor)

## 10) Hata Yonetimi ve Mesajlasma

1. `AppErrorMapper` FirebaseAuth/FirebaseFirestore/network hatalarini domain-uyumlu mesajlara cevirir
2. `BaseViewModel` icinden `publishSuccess/publishError/...` ile UI event uretilir
3. `BaseFragment` eventleri toplar ve `MessageHost` uzerinden `MainActivity`ye iletir
4. `GlobalMessageView` toast yerine custom animated top-banner mesaj gosterir

## 11) Seed Veri Boyutu (Koddan Turetilen)

1. Sehir: 81 (`CitySeedData.kt`)
2. Ilce: 976 (`DistrictSeedData.kt`)
3. Hastane: 961 (`HospitalSeedData.kt`)
4. Poliklinik: 110 (`BranchSeedData.kt`)
5. Doktor: Yaklasik 240240

Doktor sayisi, `DoctorSeedData.kt` icindeki mevcut mantiga gore hesaplanmistir:
1. Her hastane tum branch'leri alir
2. Her branch icin doktor sayisi yatak sayisina gore min 2 veya 3
3. Formul: `branchCount * (lowBedHospitals*2 + highBedHospitals*3)`

## 12) Test Durumu

1. Unit testler:
- `DoctorSeedDataTest.kt`: seed tutarlilik ve dagilim kontrolleri
- `ExampleUnitTest.kt`: ornek test
2. Instrumented test:
- `ExampleInstrumentedTest.kt`
3. Not:
- Bu ortamda `./gradlew testDebugUnitTest` calistirma denemesi `app/google-services.json` eksigi nedeniyle `processDebugGoogleServices` adiminda fail oldu.
- Root'ta `google-services.json` var, ancak plugin `app/google-services.json` bekliyor.

## 13) Derleme ve Calistirma Notlari

1. Gerekli dosya: `app/google-services.json`
2. Min SDK: 24
3. Compile/Target SDK: 36
4. Java source/target: 11

## 14) Guvenlik ve Veri Notlari

1. Repo icindeki Firebase anahtar/dosyalari gercek projede gizli tutulmali
2. Firestore guvenlik kurallari bu repoda dokumante edilmemis
3. Kimlik ve hasta verileri icin production tarafinda KVKK/GDPR uyumlu loglama/sifreleme/erisim kurallari ayrica ele alinmali

## 15) Dosya Agaci (Guncel)

Asagidaki agac, `.git`, `.gradle`, `build`, `app/build`, `.idea`, `.kotlin` klasorleri dislanarak alinmistir.

```text
.
├── .codex
├── .gitignore
├── LICENSE
├── README_guncel_rapor.md
├── app
│   ├── .gitignore
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src
│       ├── androidTest
│       │   └── java
│       │       └── com
│       │           └── menasy
│       │               └── merkezisagliksistemi
│       │                   └── ExampleInstrumentedTest.kt
│       ├── main
│       │   ├── AndroidManifest.xml
│       │   ├── java
│       │   │   └── com
│       │   │       └── menasy
│       │   │           └── merkezisagliksistemi
│       │   │               ├── MainActivity.kt
│       │   │               ├── data
│       │   │               │   ├── model
│       │   │               │   │   ├── Appointment.kt
│       │   │               │   │   ├── Branch.kt
│       │   │               │   │   ├── City.kt
│       │   │               │   │   ├── District.kt
│       │   │               │   │   ├── Doctor.kt
│       │   │               │   │   ├── Hospital.kt
│       │   │               │   │   ├── LoginResult.kt
│       │   │               │   │   ├── Patient.kt
│       │   │               │   │   ├── User.kt
│       │   │               │   │   └── seedData
│       │   │               │   │       ├── BranchSeedData.kt
│       │   │               │   │       ├── CitySeedData.kt
│       │   │               │   │       ├── DistrictSeedData.kt
│       │   │               │   │       ├── DoctorSeedData.kt
│       │   │               │   │       └── HospitalSeedData.kt
│       │   │               │   ├── remote
│       │   │               │   │   └── firebase
│       │   │               │   │       ├── AuthDataSource.kt
│       │   │               │   │       ├── CityDataSource.kt
│       │   │               │   │       ├── DoctorDataSource.kt
│       │   │               │   │       ├── HospitalDataSource.kt
│       │   │               │   │       └── ReferenceDataSource.kt
│       │   │               │   └── repository
│       │   │               │       ├── AuthRepository.kt
│       │   │               │       ├── CityRepository.kt
│       │   │               │       ├── DoctorRepository.kt
│       │   │               │       ├── HospitalRepository.kt
│       │   │               │       └── ReferenceDataRepository.kt
│       │   │               ├── di
│       │   │               │   └── ServiceLocator.kt
│       │   │               ├── domain
│       │   │               │   └── usecase
│       │   │               │       ├── .initial-folder
│       │   │               │       ├── GetBranchesUseCase.kt
│       │   │               │       ├── GetCitiesUseCase.kt
│       │   │               │       ├── GetCurrentUserUseCase.kt
│       │   │               │       ├── GetDistrictsByCityUseCase.kt
│       │   │               │       ├── GetDoctorsUseCase.kt
│       │   │               │       ├── GetHospitalsByDistrictUseCase.kt
│       │   │               │       ├── InitializeReferenceDataUseCase.kt
│       │   │               │       ├── LoginUserUseCase.kt
│       │   │               │       ├── LogoutUserUseCase.kt
│       │   │               │       └── RegisterPatientUseCase.kt
│       │   │               ├── navigation
│       │   │               │   └── .initial-folder
│       │   │               ├── ui
│       │   │               │   ├── auth
│       │   │               │   │   ├── login
│       │   │               │   │   │   ├── .initial-folder
│       │   │               │   │   │   ├── LoginFragment.kt
│       │   │               │   │   │   ├── LoginViewModel.kt
│       │   │               │   │   │   └── LoginViewModelFactory.kt
│       │   │               │   │   └── register
│       │   │               │   │       ├── .initial-folder
│       │   │               │   │       ├── RegisterFragment.kt
│       │   │               │   │       ├── RegisterViewModel.kt
│       │   │               │   │       └── RegisterViewModelFactory.kt
│       │   │               │   ├── common
│       │   │               │   │   ├── adapter
│       │   │               │   │   │   └── .initial-folder
│       │   │               │   │   ├── base
│       │   │               │   │   │   ├── BaseFragment.kt
│       │   │               │   │   │   └── BaseViewModel.kt
│       │   │               │   │   ├── component
│       │   │               │   │   │   └── .initial-folder
│       │   │               │   │   ├── error
│       │   │               │   │   │   ├── AppErrorMapper.kt
│       │   │               │   │   │   ├── AppErrorReason.kt
│       │   │               │   │   │   ├── AppException.kt
│       │   │               │   │   │   └── OperationType.kt
│       │   │               │   │   ├── message
│       │   │               │   │   │   ├── GlobalMessageController.kt
│       │   │               │   │   │   ├── GlobalMessageView.kt
│       │   │               │   │   │   ├── MessageHost.kt
│       │   │               │   │   │   ├── MessageType.kt
│       │   │               │   │   │   ├── UiEvent.kt
│       │   │               │   │   │   └── UiMessage.kt
│       │   │               │   │   ├── state
│       │   │               │   │   │   ├── .initial-folder
│       │   │               │   │   │   └── UiState.kt
│       │   │               │   │   └── widget
│       │   │               │   │       └── PatientBottomMenuView.kt
│       │   │               │   ├── doctor
│       │   │               │   │   ├── .initial-folder
│       │   │               │   │   └── DoctorHomeFragment.kt
│       │   │               │   ├── main
│       │   │               │   │   └── .initial-folder
│       │   │               │   ├── patient
│       │   │               │   │   ├── PatientFragment.kt
│       │   │               │   │   ├── account
│       │   │               │   │   │   └── PatientAccountFragment.kt
│       │   │               │   │   ├── appointment
│       │   │               │   │   │   ├── AppointmentConfirmationFragment.kt
│       │   │               │   │   │   ├── AppointmentConfirmationViewModel.kt
│       │   │               │   │   │   ├── AppointmentConfirmationViewModelFactory.kt
│       │   │               │   │   │   ├── AppointmentFlowArgs.kt
│       │   │               │   │   │   ├── AppointmentResultsAdapter.kt
│       │   │               │   │   │   ├── AppointmentResultsFragment.kt
│       │   │               │   │   │   ├── AppointmentResultsViewModel.kt
│       │   │               │   │   │   ├── AppointmentResultsViewModelFactory.kt
│       │   │               │   │   │   ├── AppointmentSearchFragment.kt
│       │   │               │   │   │   ├── AppointmentSearchViewModel.kt
│       │   │               │   │   │   ├── AppointmentSearchViewModelFactory.kt
│       │   │               │   │   │   ├── DoctorAvailabilityFragment.kt
│       │   │               │   │   │   ├── DoctorAvailabilityViewModel.kt
│       │   │               │   │   │   ├── DoctorAvailabilityViewModelFactory.kt
│       │   │               │   │   │   └── DoctorDaySlotsAdapter.kt
│       │   │               │   │   ├── appointments
│       │   │               │   │   │   └── PatientAppointmentsFragment.kt
│       │   │               │   │   └── prescriptions
│       │   │               │   │       └── PatientPrescriptionsFragment.kt
│       │   │               │   └── splash
│       │   │               │       ├── SplashFragment.kt
│       │   │               │       ├── SplashViewModel.kt
│       │   │               │       └── SplashViewModelFactory.kt
│       │   │               └── utils
│       │   │                   ├── DistrictIdNormalizer.kt
│       │   │                   └── TurkishTextNormalizer.kt
│       │   └── res
│       │       ├── drawable
│       │       │   ├── bg_bottom_menu_container.xml
│       │       │   ├── bg_bottom_menu_item_idle.xml
│       │       │   ├── bg_bottom_menu_item_selected.xml
│       │       │   ├── bg_pill_primary_light.xml
│       │       │   ├── bg_register_chip.xml
│       │       │   ├── bg_register_hero.xml
│       │       │   ├── ic_arrow_back_24.xml
│       │       │   ├── ic_bottom_account_24.xml
│       │       │   ├── ic_bottom_appointments_24.xml
│       │       │   ├── ic_bottom_home_24.xml
│       │       │   ├── ic_bottom_prescriptions_24.xml
│       │       │   ├── ic_branch_24.xml
│       │       │   ├── ic_city_24.xml
│       │       │   ├── ic_district_24.xml
│       │       │   ├── ic_doctor_24.xml
│       │       │   ├── ic_form_calendar_24.xml
│       │       │   ├── ic_form_gender_24.xml
│       │       │   ├── ic_form_id_card_24.xml
│       │       │   ├── ic_form_lock_24.xml
│       │       │   ├── ic_form_mail_24.xml
│       │       │   ├── ic_form_person_24.xml
│       │       │   ├── ic_hospital_24.xml
│       │       │   ├── ic_launcher_background.xml
│       │       │   ├── ic_launcher_foreground.webp
│       │       │   ├── ic_message_close_24.xml
│       │       │   ├── ic_message_error_24.xml
│       │       │   ├── ic_message_info_24.xml
│       │       │   ├── ic_message_success_24.xml
│       │       │   ├── ic_message_warning_24.xml
│       │       │   └── ic_message_wave.xml
│       │       ├── layout
│       │       │   ├── activity_main.xml
│       │       │   ├── fragment_appointment_confirmation.xml
│       │       │   ├── fragment_appointment_results.xml
│       │       │   ├── fragment_appointment_search.xml
│       │       │   ├── fragment_doctor_availability.xml
│       │       │   ├── fragment_doctor_home.xml
│       │       │   ├── fragment_login.xml
│       │       │   ├── fragment_patient_account.xml
│       │       │   ├── fragment_patient_appointments.xml
│       │       │   ├── fragment_patient_home.xml
│       │       │   ├── fragment_patient_prescriptions.xml
│       │       │   ├── fragment_register.xml
│       │       │   ├── fragment_splash.xml
│       │       │   ├── item_appointment_result.xml
│       │       │   ├── item_doctor_day_slots.xml
│       │       │   ├── item_dropdown_option.xml
│       │       │   ├── view_global_message.xml
│       │       │   └── view_patient_bottom_menu.xml
│       │       ├── mipmap-anydpi-v26
│       │       │   ├── ic_launcher.xml
│       │       │   └── ic_launcher_round.xml
│       │       ├── mipmap-hdpi
│       │       │   ├── ic_launcher.webp
│       │       │   └── ic_launcher_round.webp
│       │       ├── mipmap-mdpi
│       │       │   ├── ic_launcher.webp
│       │       │   └── ic_launcher_round.webp
│       │       ├── mipmap-xhdpi
│       │       │   ├── ic_launcher.webp
│       │       │   └── ic_launcher_round.webp
│       │       ├── mipmap-xxhdpi
│       │       │   ├── ic_launcher.webp
│       │       │   └── ic_launcher_round.webp
│       │       ├── mipmap-xxxhdpi
│       │       │   ├── ic_launcher.webp
│       │       │   └── ic_launcher_round.webp
│       │       ├── navigation
│       │       │   └── nav_graph.xml
│       │       ├── values
│       │       │   ├── colors.xml
│       │       │   ├── strings.xml
│       │       │   ├── styles.xml
│       │       │   └── themes.xml
│       │       └── xml
│       │           ├── backup_rules.xml
│       │           └── data_extraction_rules.xml
│       └── test
│           └── java
│               └── com
│                   └── menasy
│                       └── merkezisagliksistemi
│                           ├── DoctorSeedDataTest.kt
│                           └── ExampleUnitTest.kt
├── build.gradle
├── google-services.json
├── gradle
│   ├── gradle-daemon-jvm.properties
│   ├── libs.versions.toml
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties
└── settings.gradle
```

## 16) Kisa Durum Ozeti

1. Projede auth + referans veri + randevu arama/sonuc/uygunluk/onay UI akisi calisir durumda.
2. Firestore ile canli baglanti mevcut, ancak gercek randevu CRUD ve recete modulu henuz tamamlanmamis.
3. Mimaride katman ayrimi net ve genislemeye uygun.
