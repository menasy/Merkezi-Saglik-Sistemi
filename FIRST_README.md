# Merkezi Saglik Sistemi

<table>
  <tr>
    <td valign="top" width="70%">
      Bu dokuman, projenin 29 Mart 2026 itibariyla guncel durumunu anlatir.
    </td>
    <td align="right" valign="top" width="30%">
      <img src="./merkezi-saglik-sistemi-logo.png" alt="Merkezi Saglik Sistemi Logo" width="170" />
    </td>
  </tr>
</table>

## 1) Proje Ozeti

`MerkeziSaglikSistemi`, Kotlin + XML tabanli, MVVM mimarisi kullanan bir Android uygulamasidir.

Aktif ana akislar:
1. Splash + oturum kontrolu
2. Hasta kayit
3. Giris (patient/doctor rol yonlendirmesi)
4. Hasta randevu arama (il, ilce, poliklinik, hastane, doktor, tarih araligi)
5. Randevu sonuclari listeleme
6. Doktor uygun saat secimi
7. Randevu onay ekrani (confirm islemi su an bilincli olarak TODO)

## 2) Refactor Sonucu (Tamamlanan Mimari)

Refactor hedefi uygulanmistir:

1. Firestore referans veri kaynagi olmaktan cikarildi.
2. Referans/statik veriler artik sadece APK icindeki seed data dosyalarindan okunuyor.
3. Firestore yalnizca gercek kullanici/veri islemleri icin kullaniliyor.
4. Splash acilisinda reference initialization kaldirildi.
5. SessionCache eklendi ve login/splash/logout akislarina baglandi.

## 3) Firestore vs Local Veri Ayrimi

### Firestore tarafi
Kodda aktif kullanilan koleksiyonlar:
1. `users`
2. `patients`

Mimari olarak ayrilan ama bu asamada aktif CRUD olmayan koleksiyonlar:
1. `appointments`
2. `prescriptions`

### Local (seed) tarafi
Asagidaki referans veriler seed dosyalarindan okunur:
1. `cities` -> `CitySeedData.kt`
2. `districts` -> `DistrictSeedData.kt`
3. `hospitals` -> `HospitalSeedData.kt`
4. `branches` -> `BranchSeedData.kt`
5. `doctors` -> `DoctorSeedData.kt`

## 4) Data Source Katmani

Guncel paket yapisi:
1. `data/remote/firebase`
   - `AuthDataSource.kt` (Firebase Auth + Firestore `users/patients`)
2. `data/remote/local`
   - `CityDataSource.kt`
   - `DistrictDataSource.kt`
   - `BranchDataSource.kt`
   - `HospitalDataSource.kt`
   - `DoctorDataSource.kt`

Local data source davranislari:
1. City: tum sehirler + `id` ile sehir bulma
2. District: `cityId` ile ilce listeleme + `id` ile ilce bulma
3. Branch: tum branslar + `id` ile brans bulma
4. Hospital: `cityId` ve opsiyonel `districtId` ile filtreleme, `id` listesiyle toplu bulma
5. Doctor: `hospitalId`, `branchId` ve `hospitalIds` ile lokal filtreleme

## 5) Repository ve UseCase Katmani

Referans veri repository/use case zinciri local calisir:
1. `CityRepository` + `GetCitiesUseCase`
2. `DistrictRepository` + `GetDistrictsByCityUseCase`
3. `BranchRepository` + `GetBranchesUseCase`
4. `HospitalRepository` + `GetHospitalsByDistrictUseCase`
5. `DoctorRepository` + `GetDoctorsUseCase`

Bu use case'lerde referans veri icin gereksiz `suspend` ve `Result` katmani kaldirilmistir; liste donusleri dogrudan yapilir.

## 6) Splash ve SessionCache

### Splash akisi
`SplashViewModel` artik su islemleri yapar:
1. Aktif Firebase oturumu var mi kontrol eder.
2. SessionCache doluysa (ve user ayniysa) rol ile dogrudan yonlendirir.
3. Cache yoksa Firestore `users` kaydindan rol/fullName okuyup cache'i doldurur.
4. Role gore patient/doctor home veya login'e yonlendirir.

### SessionCache
`di/SessionCache.kt` icinde tutulur ve su alanlari kapsar:
1. `userId`
2. `role`
3. `fullName`

Kullanim:
1. Login basarili -> cache populate
2. Splash -> cache once kontrol
3. Logout -> cache clear

## 7) Kaldirilan Yapi ve Dosyalar

Asagidaki referans-init dosyalari kaldirilmistir:
1. `ReferenceDataSource.kt`
2. `ReferenceDataRepository.kt`
3. `InitializeReferenceDataUseCase.kt`

Ayrica referans veriyi Firestore'a yazan seed/sync akisi kaldirilmistir.

## 8) Randevu Arama/Result Akisi

`AppointmentSearchViewModel` ve `AppointmentResultsViewModel` referans verileri tamamen local use case zincirinden alir.

Bu nedenle:
1. Il/ilce/brans/hastane/doktor filtreleri Firestore query yapmaz.
2. Lokal filtreleme seed veriler uzerinden uygulanir.
3. Firestore baglantisi sadece kimlik/oturum tarafinda kalir.

## 9) Bilerek Dokunulmayan Alan

`AppointmentConfirmationViewModel.confirm()` mevcut kapsam geregi TODO olarak birakilmistir.

## 10) Mimari Ozet

1. UI: Fragment + ViewModel + StateFlow
2. Domain: UseCase
3. Data: Repository
4. DataSource:
   - Firebase: sadece auth/kullanici
   - Local: tum referans veriler

Bu yapi ile hedeflenen kazanimin tamami saglanmistir:
1. Daha az Firestore okuma
2. Daha sade dependency zinciri
3. Splash acilisinda gereksiz initialization yok
4. Referans veride deterministik ve hizli local akislari

## 11) Build ve Test

Yerelde kullanilan dogrulama komutlari:
1. `./gradlew :app:testDebugUnitTest`
2. `./gradlew :app:assembleDebug`

Her iki komut da basarili gecmektedir.

## 12) Uygulama Kullanimi

### Hasta kullanimi (normal akiş)
1. Uygulamayi ac.
2. `Kayit Ol` ekranindan yeni hasta hesabi olustur.
3. Kayit sonrasi `Giris Yap` ekranindan e-posta/sifre ile login ol.
4. Patient tarafinda randevu arama, olusturma ve listeleme akislarini test et.

### Doktor kullanimi (test hesabi)
Doktor hesaplari sistemde seed + Firestore `users` kaydi ile tanimlidir.  
Testte doktor tarafini denemek icin asagidaki hesaplarla login olabilirsin:

| Doktor | Hastane | Email | Sifre |
|---|---|---|---|
| Prof. Dr. Yunus Yıldız | Istanbul Mehmet Akif Ersoy Gogus Kalp ve Damar Cerrahisi EAH | `doctor2@gmail.com` | `User123` |
| Prof. Dr. Mehmet Nasim Yılmaz | Adana Sehir Hastanesi | `doctor3@gmail.com` | `User123` |
| Prof. Dr. Mahsum Turgut | Basaksehir Cam ve Sakura Sehir Hastanesi | `doctor1@gmail.com` | `User123` |

Notlar:
1. Doktor login'i icin Firestore tarafinda `users/{uid}` dokumani olmalidir.
2. `users/{uid}` icinde en az su alanlar bulunmalidir: `id`, `fullName`, `email`, `role = "doctor"`, `createdAt`.
3. Doktorun `DoctorSeedData.kt` icindeki `userId` degeri, Firebase Auth UID ile birebir ayni olmalidir.
