# MERKEZI SAGLIK SISTEMI
## Teknik Sistem Analizi ve Gelistirme Raporu

**Hazirlama Tarihi:** 31 Mart 2026  
**Proje Tipi:** Android mobil uygulama (tek modul, Kotlin + XML)  
**Rapor Kapsami:** Mevcut calisan yapi (ozellikle patient tarafi) ve planlanan doctor + recete sisteminin teknik olarak ayrik analizi

---

## 1. Proje Tanitimi

### 1.1 Projenin Adi
Merkezi Saglik Sistemi

### 1.2 Projenin Amaci
Merkezi Saglik Sistemi, hasta ve doktor rollerinin tek bir mobil platform uzerinden yonetildigi, randevu merkezli bir saglik hizmeti koordinasyon uygulamasidir. Projenin temel amaci, hasta tarafinda randevu alma ve randevu yonetim surecini dijital, olceklendirilebilir ve hata toleransli bir yapida sunmaktir.

### 1.3 Cozum Sunulan Problem
Proje, geleneksel randevu sureclerindeki su problemlere odaklanir:

- Uygun hastane/branş/doktor seciminde daginik veri ve manuel surec
- Ayni saat dilimi icin cakisma riski (double booking)
- Randevunun olusturma, guncel takip ve iptal asamalarinin tek bir akista olmamasi
- Rol bazli ekran ayriminin zayif olmasi
- Hasta ve doktor tarafinin ayni uygulamada kontrollu sekilde ayrilamamasi

### 1.4 Uygulamanin Genel Hedefi
Sistem, hasta tarafinda uctan uca randevu akisini (arama -> sonuc -> uygun saat -> onay -> listeleme/iptal) calisir duruma getirmeyi; doktor tarafinda ise once temel paneli devreye alip, sonraki asamada muayene ve recete surecini dogru durum gecisleri ile entegre etmeyi hedeflemektedir.

### 1.5 Kullanici Rolleri
Projede iki temel rol vardir:

- `patient`: Kayit, giris, randevu arama/olusturma, aktif-gecmis randevu takibi, hesap yonetimi
- `doctor`: Giris, doktor anasayfa ozeti, hesap yonetimi (mevcut); randevu ve recete operasyonlari (planlanan)

### 1.6 Sistemin Temel Mantigi
Sistem, rol bazli yonlendirme ve katmanli mimari ustune kuruludur:

- Kimlik dogrulama Firebase Authentication ile yapilir
- Rol ve profil ust verisi Firestore `users` koleksiyonundan okunur
- Patient operasyonel verisi Firestore'da tutulur
- Referans veriler (il, ilce, hastane, branş, doktor) local seed yapisinda tutulur
- Randevu kayitlarinda transaction + lock mekanizmasi ile cakisma engellenir
- UI, `StateFlow` ve tek yonlu veri akis mantigi ile guncellenir

---

## 2. Projenin Kapsami

Bu raporda en kritik ilke, **mevcut calisan sistem** ile **planlanan/eklenecek sistem** ayrimini net tutmaktir.

### 2.1 Su Anda Calisan Kismlar

#### Patient tarafi (aktif ve calisir)

- Splash ve rol bazli yonlendirme
- Login / Register akisi
- Patient home
- Il / ilce / hastane / branş / doktor secimli randevu arama
- Tarih araligi secimi ve is kurali dogrulamalari
- Randevu sonuclari ve en yakin uygun tarih hesaplama
- Doktor gun/saat uygunluk ekrani
- Randevu onayi ve randevu olusturma
- Gercek zamanli randevu listeleme
- Aktif/gecmis sekmeli randevu ekrani
- Aktif randevu iptali
- Hasta hesap ekrani ve cikis

#### Doctor tarafi (kismen aktif)

- Doctor login yonlendirmesi
- Doctor session cache (business `doctorId` dahil)
- Doctor home (ozet paneli)
- Doctor account (profil bilgisi + cikis)

### 2.2 Henuz Gelistirilmekte Olan / Planlanan Kismlar

#### Patient tarafi

- Patient prescriptions ekrani halen placeholder/TODO durumundadir (UI var, operasyonel akis yok)

#### Doctor tarafi

- Doctor appointments ekrani (bekleyen/gelecek/gecmis operasyonel liste)
- Doctor prescriptions ekrani (doktorun yazdigi recetelerin liste/filtre yapisi)
- Muayene et akisinin tamamlanmasi (`DoctorExaminationFragment`)
- `SCHEDULED -> COMPLETED/MISSED` durum gecis operasyonlari

#### Recete sistemi

- Prescription veri modeli ve Firestore koleksiyonu
- Seed medicine secimi
- Muayene tamamlanirken recete olusturma
- Gecmis randevuda recete goruntuleme
- Patient recete listeleme/detay akisi

### 2.3 Patient Tarafinda Tamamlanan Moduller

- Kimlik dogrulama
- Randevu arama/olusturma/iptal
- Gercek zamanli takip
- Hesap yonetimi

### 2.4 Doctor Tarafi ve Recete Sistemi Icin Planlanan Alanlar

- Randevu yonetimi (sekmeli)
- Muayene sonuclandirma
- Recete zorunlu karar akisi
- Ilac secim altyapisi
- Recete raporlama ve goruntuleme

---

## 3. Teknoloji Yigini ve Secim Gerekceleri

Bu bolumde kullanilan teknoloji bileşenleri "ne kullanildi" seviyesinde degil, "neden kullanildi" seviyesinde ele alinmaktadir.

### 3.1 Kotlin

Kotlin, Android tarafinda null-safety, extension mekanizmasi, sealed class yapisi ve coroutine entegrasyonu nedeniyle tercih edilmistir. Projede:

- Domain/UI state tanimlari daha guvenli ve okunur yazilmistir
- `Result` tabanli operasyon donusleri sadeleştirilmistir
- Coroutine + Flow kullaniminda boilerplate azaltilmistir

### 3.2 XML (View tabani)

UI katmani XML ile tanimlanmis, Fragment tabanli ekran yaklasimi benimsenmistir. Bu tercih:

- Tasarim hizalamalarinin deterministic olmasi
- Farkli kaynak klasorleri ile rol/ekran bazli duzenin ayrilmasi
- Material componentlerinin olgun XML destegi

nedenleriyle pragmatik bulunmustur.

### 3.3 Android Studio

Derleme, debug, profiler, layout inspect ve Gradle entegrasyonu ile proje yasam dongusu Android Studio uzerinden yonetilmektedir.

### 3.4 Firebase Authentication

E-posta/sifre tabanli kimlik dogrulama icin secilmistir. Projede:

- Patient kaydi (`createUserWithEmailAndPassword`)
- Login (`signInWithEmailAndPassword`)
- Session varligi kontrolu (`currentUser`)
- Logout (`signOut`)

islemleri bu katmanda yurutulur.

### 3.5 Cloud Firestore

Operasyonel veri deposu olarak kullanilmaktadir. Mevcut yapida:

- `users`
- `patients`
- `appointments`
- `appointmentLocks`

koleksiyonlari uzerinden calisilir. Firestore seciminin kritik nedeni, transaction destegi + snapshot listener ile gercek zamanli akis sunmasidir.

### 3.6 MVVM

UI ile is mantigi ayrimi icin temel mimari desendir.

- Fragment = View
- ViewModel = ekran durumu + UI event
- UseCase = tek sorumluluklu is kurali

Bu sayede ekranlar test edilebilir ve buyutulebilir hale getirilmistir.

### 3.7 Repository Katmani

Veri kaynagi detaylarini UI/domain katmanindan gizler. Firestore/local seed gibi farkli kaynaklar repository uzerinden standartlasir.

### 3.8 Use Case Katmani

Her operasyon tek bir is birimi olarak modellenmistir (`CreateAppointmentUseCase`, `LoginUserUseCase`, vb.). Bu secim:

- Is kurallarinin fragment/viewmodel icine dagilmasini engeller
- Gelecekte servis degisikligi veya test yazimini kolaylastirir

### 3.9 ViewBinding

`findViewById` kaynakli runtime hata riskini azaltir, tip guvenli gorunum erisimi saglar. Tum fragmentlerde sistematik sekilde kullanilmistir.

### 3.10 Navigation Component

Tek activity + cok fragment yapisinda gecisleri merkezi yonetir. Role gore hedef fragmente gitme, back stack kontrolu ve akisin netlesmesi icin secilmistir.

### 3.11 StateFlow / UI State

Ekran durumlari `StateFlow` ile izlenir. Asenkron ve tekrar cizime dayali UI modelinde kararlilik saglar. `UiState` sealed class deseninin kullanimiyla loading/success/error/empty halleri acikca modellenmistir.

### 3.12 Diger Yardimci Yapi ve Bilesenler

- `ServiceLocator`: Manuel dependency wiring
- `SessionCache`: Bellek ici session bilgisi
- `AppErrorMapper`: Firebase/app hatalarini kullaniciya uygun mesaja donusturme
- `GlobalMessageView`: Uygulama genelinde tek tip mesaj gosterimi
- `DateTimeUtils`: Randevu zamani parse/gelecek kontrolu
- `HospitalBranchAllocator`: Hastane kapasitesine gore branş dagitim algoritmasi

---

## 4. Proje Mimarisi

### 4.1 Katmanli Yapi

Uygulama asagidaki veri akis mimarisi ile calisir:

```text
View (Fragment)
   -> ViewModel
      -> UseCase
         -> Repository
            -> DataSource (Firestore veya Local Seed)
```

### 4.2 Mimari Tercih Nedenleri

Bu mimari secimin temel teknik gerekceleri:

- Is mantigi ile UI'nin ayrismasi
- Veri kaynagi degisiminde ust katmanlarin etkilenmemesi
- Unit test kapsamini artirma
- Ekipli gelistirmede sorumluluk alanlarini netlestirme
- Kod tekrarini azaltma

### 4.3 Kodun Modulerligi ve Surdurulebilirligi

- Her ekran icin ayri ViewModel
- Her operasyon icin ayri UseCase
- Model/repository/datasource adlandirmasi tutarli
- Seed veri ve operasyonel veri ayrik

Bu yapi orta ve uzun vadede kapsam buyurken etkilenim alanini sinirlar.

### 4.4 DRY ve Okunabilirlik

DRY prensibi, ozellikle su alanlarda uygulanmistir:

- Ortak mesaj-hata yonetimi (`BaseViewModel`, `BaseFragment`)
- Tarih/saat donusumleri (`DateTimeUtils`)
- Argument tasima yapilari (`AppointmentFlowArgs`)
- UI style/theme token standardizasyonu

---

## 5. Klasor ve Dosya Yapisi

### 5.1 Genel Paketleme Stratejisi

`app/src/main/java/com/menasy/merkezisagliksistemi` altinda katman bazli paketleme vardir:

- `data`
- `domain`
- `ui`
- `di`
- `utils`

### 5.2 `data` Katmani

- `model`: Domain varliklari (`Appointment`, `Doctor`, `User`, vb.)
- `model/seedData`: Local referans veri listeleri
- `remote/firebase`: Firestore/Auth data source
- `remote/local`: Seed tabanli data source
- `repository`: Veri erisimi abstraction katmani

### 5.3 `domain` Katmani

- Her is adimi icin use case siniflari
- Ornek: `CreateAppointmentUseCase`, `ObservePatientAppointmentsUseCase`, `GetDoctorHomeSummaryUseCase`

### 5.4 `ui` Katmani

- `auth` (login/register)
- `splash`
- `patient` (home, appointmentflow, appointmentlist, account, prescriptions)
- `doctor` (home, appointments, prescriptions, account)
- `common` (base, error, message, state, widget)

### 5.5 `di` Katmani

- `ServiceLocator`: Turetim ve bagimlilik saglama
- `SessionCache`: Kullanici oturum bilgisi

### 5.6 `utils` Katmani

- Tarih/saat, metin normalize, ilce id normalize gibi yardimci fonksiyonlar

### 5.7 Kaynak Klasoru Organizasyonu

`sourceSets` ile kaynaklar anlamsal olarak ayrilmistir:

- `res`
- `res_shared`
- `res_auth`
- `res_appointment`
- `res_doctor`
- `res_patient`

Bu duzen UI tarafinda baglamsal dosya bulmayi kolaylastirir, buyuyen projede kaynak kaosunu azaltir.

---

## 6. Kimlik Dogrulama ve Kullanici Yonetimi

### 6.1 Giris Sistemi

Giris sureci iki katmanda ilerler:

1. Firebase Auth ile kullanici dogrulama
2. Firestore `users/{uid}` uzerinden rol ve profil ust bilgisini alma

### 6.2 Patient Kayit/Giris Akisi

Kayit (`registerPatient`) sirasinda:

- Auth uzerinden UID uretilir
- `users/{uid}` dokumani yazilir (`role = patient`)
- `patients/{uid}` dokumani yazilir (TC, dogum tarihi, cinsiyet)

Giris sirasinda:

- Auth sign-in
- `users` dokumani okunur
- `LoginResult(uid, role, fullName)` olusturulur

### 6.3 Firebase Authentication Kullanimi

Auth sadece kimlik dogrulama icin kullanilir. Rol bilgisi Auth custom claim ile degil, Firestore `users` koleksiyonunda tutulur. Bu tasarim, rol bilgisini uygulama verisi ile birlikte yonetmeyi kolaylastirir.

### 6.4 `users` ve `patients` Iliskisi

- `users`: Ortak kimlik ve rol bilgisi
- `patients`: Hasta role ozel ek alanlar

Bu ayirim, role ozel genislemeyi destekler (doktor/yonetici gibi rollerde farkli dokuman yapisi kurulabilir).

### 6.5 Rol Bazli Yonlendirme Mantigi

- Splash asamasinda aktif session kontrol edilir
- Rol "patient" ise patient home
- Rol "doctor" ise doctor home
- Doktor icin ek olarak seed'de UID -> `doctorId` eslesmesi zorunludur
- Eslesme veya yetki yoksa hata mesaji + login'e donus

---

## 7. Patient Tarafinda Mevcut Calisan Sistem

Bu bolum, raporun en kritik kismidir ve su anda uretilmis gercek calisan akis teknik olarak parcalanmistir.

### 7.1 Splash Ekrani

`SplashViewModel` uygulama acilisinda:

- Auth `currentUser` varligini kontrol eder
- SessionCache doluysa Firestore cagrisini atlayabilir
- Cache bossa rol ve ad bilgisini Firestore'dan okur
- Role gore navigasyonu tetikler

Doktor rolunde ek kontrol vardir: Firebase UID local doctor seed verisiyle eslesmiyorsa giris engellenir.

### 7.2 Login Ekrani

`LoginViewModel`:

- E-posta/sifre bosluk validasyonu
- `LoginUserUseCase` calistirma
- Basarida role gore aninda yonlendirme (`patientHome` / `doctorHome`)

Doktor girisinde login sonucu sadece "role=doctor" olmasi yetmez; seed profil ve login yetkisi de kontrol edilir.

### 7.3 Register Ekrani

Kayit formunda:

- Zorunlu alan kontrolu
- TC uzunluk kontrolu (11 hane)
- Dogum tarihi secimi (gelecek tarih secimine kapali)
- Cinsiyet secimi dropdown

Basarili kayittan sonra login ekranina donulur.

### 7.4 Patient Home Ekrani

Hasta role giris yaptiginda acilan ana ekran:

- Session cache'den kullanici adiyla karsilama
- "Randevu Al" butonu ile arama akisina gecis

### 7.5 Il / Ilce / Hastane / Branş / Doktor Secim Akisi

`AppointmentSearchViewModel` tarafinda state tabanli alan yonetimi vardir:

- Il secilmeden ilce/branş/hastane/doktor alanlari aktif olmaz
- Il secimi sonrasinda ilce ve branş aktif olur
- Branş secimiyle hastane listesi branşa gore filtrelenir
- Hastane secimi yapilirsa doktor listesi yuklenir

Opsiyonel alanlar:

- Ilce: `Fark etmez`
- Hastane: `Fark etmez`
- Doktor: `Fark etmez`

Zorunlu alanlar:

- Il
- Branş
- Baslangic ve bitis tarihi

### 7.6 Tarih Araligi Is Kurallari

Arama tarih araligi dogrulama kurallari:

- Gecmis tarih secilemez
- Bitis tarihi baslangictan once olamaz
- Maksimum aralik 15 gun

Bu kurallar UI'de anlik mesaja donusturulerek kullaniciya aktarilir.

### 7.7 Appointment Results Ekrani

Sonuc ekraninda su teknik akis izlenir:

1. Konum (il/ilce) bazli hastaneler cekilir
2. Branş secimi varsa hastaneler `hospital.branchIds` ile filtrelenir
3. Doktorlar secili hastane veya hastane listesi bazinda toplanir
4. Her doktor icin secilen aralikta **en yakin uygun tarih** hesaplanir
5. Sonuclar uygun tarih artan sirada listelenir

Performans notu: En yakin tarih hesaplari paralel (Semaphore = 8) yurur.

### 7.8 Slot Secimi (Doctor Availability)

Doktor uygunluk ekraninda:

- Aralik icindeki gunlerden en fazla ilk 5 gun gosterilir (`MAX_DAY_COUNT = 5`)
- Saat bloklari doktorun `slotStartHour`, `slotEndHour`, `slotDurationMinutes` alanlarindan uretilir
- Her gun icin Firestore `SCHEDULED` slotlar realtime dinlenir

Kritik davranis: Kullanici bir saat secmisken baska bir hasta ayni saati alirsa secim otomatik sifirlanir ve uyari gosterilir.

### 7.9 Randevu Onayi ve Olusturma

Onay ekraninda hasta su bilgileri gorur:

- Doktor
- Hastane
- Branş
- Tarih-saat
- Randevu sahibi

Onayla butonu:

- Session'dan patientId alir
- `Appointment` modeli olusturur
- `CreateAppointmentUseCase` ile transaction tabanli kayit yapar

Basarili islemde hasta randevular ekranina yonlendirilir.

### 7.10 Randevu Listeleme

`PatientAppointmentsViewModel` gercek zamanli akis kullanir:

- `observePatientAppointments(patientId)` ile snapshot listener tabanli liste
- `AppointmentMapper` ile doctor/hospital/branş ID -> isim mapleme
- Liste iki sekmeye bolunur: `ACTIVE` ve `PAST`

Ayrim kurali:

- `ACTIVE`: `status == SCHEDULED` VE tarih-saat gelecekte
- `PAST`: diger tum durumlar (gecmis scheduled, cancelled, completed, missed)

### 7.11 Randevu Iptal Etme

Sadece aktif randevularda "Iptal Et" butonu gorunur.

Iptal operasyonu:

- Appointment status `CANCELLED`
- Ilgili lock dokumani silinir
- Boylce ayni slot tekrar alinabilir hale gelir

Bu operasyon transaction icinde atomik yurutulur.

### 7.12 Profil / Hesabim

Patient account ekrani:

- Ad soyad / rol / kullanici id gosterimi
- Session eksikse Firestore'dan role ve ad tazeleme
- Logout ile SessionCache temizleme + Auth signOut

### 7.13 Veri Cekme ve Gosterme Mantigi

Hasta akisinda veri iki kaynaktan gelir:

- Referans veri: local seed data source
- Operasyonel veri: Firestore

Bu hibrit yapi sayesinde sorgu maliyeti dusuk tutulurken, randevu gibi dinamik veriler merkezi kalir.

### 7.14 UI/UX Mantigi (Patient)

- Arama alanlarinda bagimli etkinlestirme ile hatali secim azaltilir
- "Fark etmez" secenekleri ile kullaniciya kademeli filtreleme esnekligi sunulur
- Slot secimi chip tabanli, durum odakli gorsellestirme ile yapilir
- Aktif/gecmis randevu ayrimi islemsel netlik saglar

### 7.15 Firestore'da Verinin Tutulma Pratigi (Patient ile Ilgili)

- `appointments`: randevu ana kaydi
- `appointmentLocks`: doktor+tarih+saat tekilligini garanti eden lock kaydi
- `users` / `patients`: kimlik ve hasta profili

---

## 8. Randevu Sistemi Teknik Yapisi

### 8.1 Appointment Veri Modeli

Mevcut model:

```kotlin
data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val hospitalId: String = "",
    val branchId: String = "",
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val status: String = AppointmentStatus.SCHEDULED.name,
    val createdAt: Long = System.currentTimeMillis()
)
```

Durum enumu:

```kotlin
enum class AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    MISSED
}
```

### 8.2 Randevu Nasil Olusturuluyor

`createAppointmentWithLock` akisinda:

1. Hastanin aktif randevulari sorgulanir (`status = SCHEDULED`)
2. Ayni doktordan aktif randevu varsa islem engellenir
3. Toplam aktif randevu sayisi 5 ise islem engellenir
4. Lock ID uretilir: `doctorId_date_time`
5. Transaction icinde lock varligi kontrol edilir
6. Lock yoksa lock + appointment ayni transaction'da yazilir

### 8.3 Slot Mantigi

Slot dolulugu, ilgili doktora ait ve `SCHEDULED` durumundaki randevularin `appointmentTime` alanindan uretilir. UI tarafi bos slotlari secilebilir, dolu slotlari pasif gosterir.

### 8.4 Cakisma Onleme Yontemi

Cakisma Firestore transaction + lock dokumaniyla onlenir.

- Iki kullanici ayni anda ayni slota istek gonderse bile
- Sadece lock'u once alan islem basarili olur
- Diger islem `SLOT_ALREADY_TAKEN` hatasi alir

### 8.5 Firestore Transaction Davranisi

Randevu olusturma ve randevu iptal etme islemleri transaction seviyesinde atomiktir. Bu, lock ve appointment kaydinin tutarsiz kalma riskini dusurur.

### 8.6 Hastanin Randevu Ekraninda Listeleme Mantigi

- Snapshot listener ile gercek zamanli veri akisi
- UI mapper ile seed uzerinden isim cozumu
- Aktif/gecmis partition
- Tarih siralama:
  - Aktif: en yakin ilk
  - Gecmis: en yeni ilk

### 8.7 Durumlarin Islevsel Mantigi

- `SCHEDULED`: Planlanmis randevu (patient tarafinda aktif operasyon)
- `CANCELLED`: Hasta tarafindan iptal edilmis
- `COMPLETED`: Muayene tamamlandi (doctor akisinda aktiflesecek)
- `MISSED`: Hasta gelmedi (doctor akisinda aktiflesecek)

### 8.8 Patient Tarafinda Aktif Kullanilan Durumlar

Mevcut uygulamada fiilen uretilen durumlar:

- `SCHEDULED` (olusturma)
- `CANCELLED` (iptal)

`COMPLETED` ve `MISSED` enumde tanimli olup doktor akisinin tamamlanmasiyla operasyonel olarak aktiflesecektir.

---

## 9. UI/UX Yaklasimi

### 9.1 XML Tasarim Kurgusu

Ekranlar Material tabanli kart + alan + buton kompozisyonu ile tasarlanmistir. Ortak style tokenlari sayesinde ekranlar arasi tutarlilik korunmustur.

### 9.2 Responsive Dusunce

- `NestedScrollView` ve ConstraintLayout kombinasyonu
- Kenar bosluklari ve kart genislik sinirlari
- Klavye acilisina duyarlı register formu
- Global message ve bottom menu inset uyarlamalari

### 9.3 Modern ve Sade Tasarim Dili

- Acik zemin + turkuaz/mavi vurgu
- Kart tabanli bilgi sunumu
- Kritik durumlar icin renk kodlama (aktif/gecmis/iptal)
- Chip tabanli saat secimi ile hizli etkileşim

### 9.4 Patient Ekranlarinda Gorsel Tutarlilik

- Ortak buton stilleri
- Ortak page title text appearance
- Benzer header/back button yapisi
- Ortak iconografi

### 9.5 Kart, Secim ve Bilgi Gosterim Yaklasimi

- Sonuc kartlari: hizli karar vermeye yonelik ozet
- Uygunluk ekrani: gun -> saat -> slot hiyerarsisi
- Onay karti: olusturulacak kaydin tam ozeti
- Randevu kartlari: tarih/saat + kurum + durum + aksiyon birarada

### 9.6 Global Mesaj Deneyimi

`GlobalMessageView` ile success/info/warning/error bildirimleri uygulama genelinde tek stilde sunulur. Bu, fragment bazli Toast daginikligini azaltir ve UX standartlastirir.

---

## 10. Veri Yonetimi ve Firebase Yapisi

### 10.1 Firestore Koleksiyon Yapisi (Mevcut)

#### `users`

- `id`
- `fullName`
- `email`
- `role`
- `createdAt`

#### `patients`

- `userId`
- `tcNo`
- `birthDate`
- `gender`

#### `appointments`

- `id`
- `patientId`
- `doctorId`
- `hospitalId`
- `branchId`
- `appointmentDate`
- `appointmentTime`
- `status`
- `createdAt`

#### `appointmentLocks`

- `doctorId`
- `date`
- `time`
- `createdAt`

### 10.2 Veri Okuma/Yazma Mantigi

- Register: Auth + `users` + `patients`
- Login: Auth + `users`
- Appointment create/cancel: transaction odakli
- Appointment listing/occupied slots: snapshot listener tabanli

### 10.3 Gercek Zamanli Guncelleme

Su iki alanda aktif kullanilir:

- Secili doktor/tarih icin dolu saatler
- Hastanin randevu listesi

Bu sayede manuel yenileme olmadan UI kendini gunceller.

### 10.4 Guvenlik Yaklasimi

Uygulama katmaninda:

- Rol bazli yonlendirme
- Doktor UID -> seed profil eslesmesi
- Session cache kontrolu

Not: Uretim seviyesinde bu kontrollerin Firestore security rules tarafinda da ayni ilkelere gore zorunlu kilinmasi gerekir.

### 10.5 Rol Bazli Veri Erisimi

- Patient: kendi randevu kayitlari
- Doctor: kendi `doctorId` alanina bagli kayitlar (doctor home tarafinda aktif)

Bu model, cok kullanicili yapida veri sinirlarini netlestirir.

---

## 11. Su Ana Kadar Gelistirilen Ozellikler (Aciklamali)

### 11.1 Auth altyapisi

Hasta kayit/giris/cikis senaryolari Firebase tabaninda tamamlanmistir. Rol bilgisi Firestore ile eslenerek yalnizca kimlik degil, islevsel yonlendirme de saglanmistir.

### 11.2 Session cache

Uygulama acilisinda gereksiz Firestore okumalarini azaltmak icin bellek ici cache modeli uygulanmistir. Doktorlar icin business `doctorId` de cache'e alinmistir.

### 11.3 Local referans veri migrasyonu

Il, ilce, hastane, branş, doktor verileri local seed'e alinmis; UI filtreleme performansi ve maliyet kontrolu saglanmistir.

### 11.4 Hastane-branş dagitim algoritmasi

Hastane yatak kapasitesine gore belirli branş sayisi atayan deterministic algoritma eklenmistir; testlerle dogrulanmistir.

### 11.5 Randevu arama modulu

Kademeli filtreleme ve tarih araligi kurallariyla hasta tarafi arama deneyimi tamamlanmistir.

### 11.6 Sonuc ve en yakin tarih hesaplama

Her doktor icin secilen aralikta uygun ilk gun hesaplanarak kullaniciya karar destekli sonuc listesi sunulmustur.

### 11.7 Gercek zamanli slot izleme

Slot secimi ekraninda Firestore listener ile anlik doluluk yansitilarak gecikmeli cakisma riski azaltilmistir.

### 11.8 Transaction tabanli randevu olusturma

Lock + appointment atomik yazimi ile double booking problemi teknik olarak cozulmustur.

### 11.9 Is kurali: aktif randevu limitleri

- Ayni doktorla ikinci aktif randevu engeli
- Toplam aktif randevu sayisinin 5 ile sinirlanmasi

kurallari eklenmistir.

### 11.10 Randevu listeleme ve iptal modulu

Patient tarafinda aktif/gecmis sekmeli, gercek zamanli listeleme ve aktif randevu iptal operasyonu devreye alinmistir.

### 11.11 Error/message standartlastirmasi

AppErrorMapper + global message sistemi ile hata/uyari/basarili islem bildirimleri standardize edilmistir.

### 11.12 Role duyarli bottom menu

Tek bilesen uzerinden patient ve doctor sekmeleri role gore degistirilebilir hale getirilmistir.

### 11.13 Doctor home dashboard

Doktor icin ozet panel (profil, branş, hastane, planlanan randevu sayisi, bugun tamamlanan sayi) implement edilmistir.

---

## 12. Gelistirme Sureci

### 12.1 Baslangic Asamasi

Proje, Mart 2026 sonunda temel Android proje kurulumu ve ViewBinding entegrasyonu ile baslamistir. Ilk asamada hedef, mimari omurgayi erken sabitleyip feature eklemelerini bu omurga uzerinden ilerletmek olmustur.

### 12.2 Mimari Kararlarin Verilmesi

Erken donemde secilen kritik kararlar:

- MVVM + use case + repository
- Tek activity, Navigation Component
- Firebase Auth + Firestore
- Referans veriyi yerelde tutma

Bu kararlar, ileride doktor ve recete akisinin eklenmesini kolaylastiran bir temel olusturmustur.

### 12.3 Hasta Tarafi Gelisim Sirasi

Gelistirme pratiği hasta akisinda su sirayla ilerlemistir:

1. Auth ve rol yonlendirme
2. Referans veri altyapisi
3. Arama filtreleme
4. Sonuc/uygunluk ekranlari
5. Onay ve transaction tabanli randevu olusturma
6. Listeleme ve iptal
7. UI/UX iyilestirmeleri

### 12.4 Surecte Dikkat Edilen Prensipler

- Is kurallarini use case veya data source seviyesinde merkezileştirme
- UI state'i deterministik tutma
- Firestore transaction kullanimiyla veri tutarliligi
- Role gore net navigasyon
- Ekranlar arasi argument sozlesmesini standartlastirma

### 12.5 Kod Duzeni ve Surdurulebilirlik

Kod tabani, ozellikle su yonde gelistirilmistir:

- Sinif sorumluluklari tekil tutulmustur
- Paketleme ve adlandirma tutarlidir
- Ortak davranislar ayrik katmanlarda tekrar kullanilmistir

### 12.6 Gelisim Zaman Cizelgesi (Git Geçmisi Ozet)

- **27 Mart 2026:** Proje init, mimari kurulum, auth pipeline baslangici
- **28 Mart 2026:** Navigation ve patient randevu akisinin omurgasi
- **29 Mart 2026:** Seed migrasyonu, session cache, UI standardizasyonu
- **30 Mart 2026:** Transaction lock, aktif randevu kurallari, realtime list/iptal
- **31 Mart 2026:** Doktor alt menu entegrasyonu ve doctor home dashboard implementasyonu

---

## 13. Mevcut Durum Degerlendirmesi

### 13.1 Patient Tarafinda Su Anda Calisan Moduller

- Auth (register/login/logout)
- Arama ve filtreleme
- Sonuc ve uygunluk
- Randevu olusturma
- Realtime randevu listeleme
- Randevu iptal
- Hesap ekrani

### 13.2 Tamamlanan Ozellikler

Hasta tarafinda randevu yonetimi acisindan cekirdek islevler tamamlanmis durumdadir. Sistem uctan uca operasyonel bir hasta deneyimi sunmaktadir.

### 13.3 Sistemin Guclu Yonleri

- Transaction tabanli cakisma onleme
- Realtime slot/list guncelleme
- Rol bazli net navigasyon
- Local seed + Firestore hibrit veri stratejisi
- Mimari olarak genislemeye uygun kurgu

### 13.4 Hazir Durumdaki Alanlar

- Doktor login routing altyapisi
- Doktor anasayfa ozet paneli
- Doktor hesap yonetimi

### 13.5 Teknik Borc / Acik Alanlar

- Patient prescriptions modulu henüz operasyonel degil
- Doctor appointments/prescriptions ekranlari placeholder
- `COMPLETED` / `MISSED` status gecisleri doctor akisinda aktiflestirilmeli
- Firestore rule seti ve audit izleri uretim seviyesi icin genisletilmeli

---

## 14. Eklenecek Sistemler (Doctor Tarafi ve Recete Sistemi)

Bu bolumde anlatilanlar **mevcutta calisan kisim degil**, planlanan/eklenecek sistem tasarimidir.

### 14.1 Doctor Tarafinin Genel Mantigi

Doktor modulu, doktorun sadece kendi verilerini gorecegi ve sonuclandiracagi bir operasyon paneli olarak kurgulanir:

- Anasayfa (mevcut)
- Randevular (planlanan)
- Receteler (planlanan)
- Hesabim (mevcut)

### 14.2 Doctor Home Ekrani (Mevcut + Genisleme Potansiyeli)

Mevcut olarak:

- Doktor adi
- Branş
- Hastane
- Planlanan randevu sayisi
- Bugun tamamlanan muayene sayisi

gosterilmektedir.

Planlanan genisleme:

- Bekleyen/gecikmis randevu metriklerinin ayrik gostergeleri
- Gunluk performans kartlarinin detaylandirilmasi

### 14.3 Doctor Randevular Ekrani (Planlanan)

Hedeflenmis yapi:

- Bekleyen randevular
- Gelecek randevular
- Gecmis randevular

Bekleyen sekmede "Muayene Et" aksiyonu; gecmis sekmede recete varsa "Receteyi Goruntule" aksiyonu yer alacaktir.

### 14.4 Bekleyen / Gelecek / Gecmis Mantigi

Planlanan kurallar:

- Bekleyen: zamani gecmis + `SCHEDULED`
- Gelecek: zamani gelmemis + `SCHEDULED`
- Gecmis: `COMPLETED`, `MISSED`, `CANCELLED`

Bu ayrim doktor operasyonunu zaman boyutunda netlestirir.

### 14.5 Muayene Et Akisi (`DoctorExaminationFragment`) - Planlanan

Hedef ekran, randevu sonuclandirma merkezidir. Doktor:

- Hasta geldi/gelmedi karari verecek
- Muayene notu girecek
- Recete gereklilik kararini zorunlu sececek

### 14.6 "Recete Gerekli mi" Karari - Planlanan

Muayene ekraninda zorunlu iki secenek:

- Evet (recete olusturma adimi acilir)
- Hayir (sadece muayene tamamlama)

Bu karar, akis belirsizligini azaltir ve yarim veri riskini onler.

### 14.7 Seed Data ile Ilac Secimi - Planlanan

Recete tarafinda ilaclarin serbest metin yerine seed listeden secilmesi hedeflenmistir. Beklenen faydalar:

- Veri standardizasyonu
- Yazim hatasi azalmasi
- Reçete satirlarinda tutarli doz/frekans bilgisi

### 14.8 Recete Olusturma Mantigi - Planlanan

`Muayeneyi Tamamla` aksiyonu tek komut gibi calisacaktir:

- Recete gerekmiyorsa: sadece `SCHEDULED -> COMPLETED`
- Recete gerekiyorsa: once recete kaydi, sonra `COMPLETED`

### 14.9 Receteler Ekrani - Planlanan

Doktorun kendi recetelerini tarih filtreleri ile listeleyen ekran hedeflenmektedir:

- Bugun varsayilan filtre
- Tarih / tarih araligi filtreleri
- Kart bazli recete ozetleri

### 14.10 Gecmis Randevuda Recete Goruntuleme - Planlanan

Gecmis randevu kartinda recete varsa modal/bottom-sheet benzeri responsive pencere ile recete detaylari acilacaktir.

### 14.11 Hesabim Ekrani - Mevcut Durum ve Plan

Mevcut:

- Kullanici bilgisi
- Rol
- User id
- Logout

Plan:

- Branş/hastane/oda bilgilerini genisletme
- Doktor odakli profil detaylarini zenginlestirme

### 14.12 Hasta Tarafiyla Entegrasyon

Doctor + recete sistemi devreye girdiginde:

- Doctor tarafinda tamamlanan muayene `COMPLETED` olarak patient gecmisine dusecek
- `MISSED` kayitlari patient gecmisinde gorunecek
- Yazilan recete, patient prescriptions ekranina yansitilacak

Bu entegrasyon, hasta ve doktor akislarini tek bir tutarli yasam dongusunde birlestirecektir.

---

## 15. Sonuc ve Genel Degerlendirme

Merkezi Saglik Sistemi, 31 Mart 2026 itibariyla hasta tarafinda cekirdek operasyonlari calisir hale getirmis, transaction tabanli guvenilir randevu yonetimi saglayan ve mimari olarak buyumeye hazir bir seviyeye ulasmistir.

### 15.1 Gelinen Nokta

- Patient tarafi uctan uca randevu sureci bakimindan olgunlasmistir
- Doctor tarafi icin giris, session ve dashboard zemini hazirdir
- Recete sistemi icin mimari ve akis kararlari netlestirilmistir

### 15.2 Neden Patient Tarafi Onemli Olcude Tamamlanmistir

Cunku proje omurgasini olusturan en kritik operasyonel hat (randevu olusturma, slot cakisma onleme, gercek zamanli takip, iptal) teknik olarak devrededir. Bu hat, doktor ve recete modullerinin ustune oturacagi ana veri omurgasidir.

### 15.3 Doctor ve Recete Tarafi Eklendiginde Beklenen Butunlesme

Doctor randevu durum gecisleri ve recete olusturma mekanizmasi eklendiginde sistem:

- planla (SCHEDULED)
- sonuclandir (COMPLETED/MISSED)
- tedavi kaydet (Prescription)
- hasta tarafina yansit

dongusunu tamamlamis olacaktir.

### 15.4 Akademik ve Teknik Deger

Proje, bitirme projesi/sistem analizi perspektifinde su teknik degeri uretmektedir:

- Net katmanli mimari
- Is kurali odakli use case modelleme
- Firebase tabanli transaction + realtime kombinasyonu
- Rol bazli mobil is akisi
- Yerel referans veri ile performans/maliyet dengesi

Sonuc olarak Merkezi Saglik Sistemi, hasta odakli cekirdek fonksiyonlari tamamlamis, doktor ve recete modullerinin entegrasyonuna teknik olarak hazir, surdurulebilir ve kurumsal olcege evrilebilir bir yazilim altyapisina sahiptir.

---

## Ek A - Mevcut Referans Veri Boyutu (Kod Taramasi)

- Sehir (local seed): **81**
- Ilce (local seed): **976**
- Branş (local seed): **17**
- Hastane (local seed): **961**
- Doktor (local seed): **~6592+** (dosya icinde 6593 `Doctor(...)` gecisi)
- Login yetkili doktor: **2** (test ile dogrulanmis)

## Ek B - Referans Veri Kaynaklari

Bu projede local seed veri olarak kullanilan branş ve hastane listeleri, asagidaki resmi kaynaklar referans alinarak derlenmistir:

- Branş/klinik kodlari (T.C. Saglik Bakanligi Teletip):  
  `https://www.teletip.saglik.gov.tr/docs/ClinicCode.pdf`
- Kamu hastaneleri guncel listesi (T.C. Saglik Bakanligi KHGMS):  
  `https://khgmsaglikhizmetleridb.saglik.gov.tr/TR-87343/kamu-hastaneleri-genel-mudurlugune-bagli-2-ve-3-basamak-kamu-saglik-tesisleri-guncel-listesi.html`

Not: Uygulamadaki `seedData` yapisi, bu resmi kaynaklardan alinan verilerin proje ihtiyacina gore normalize edilmis ve id yapisina uygun hale getirilmis surumunu kullanir.

## Ek C - Build ve Platform Ozeti

- `minSdk`: 24
- `targetSdk`: 36
- `compileSdk`: 36
- Java uyumluluk: 11
- Material: 1.12.0
- Firebase BoM: 34.11.0
- Navigation: 2.8.9
