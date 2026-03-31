Tamam, tüm son kararlarını dikkate alarak metni baştan sona yeniden düzenledim. Aşağıdaki sürüm artık doktor modülü için daha net, eksiksiz, tutarlı ve uygulanabilir son akış metnidir. Yapı; doktorun yalnızca kendi randevu ve reçete verilerine erişmesi, randevu durumunu yönetmesi ve muayene ile reçete sürecini birlikte yürütmesi açısından mevcut proje dokümanındaki genel kurallarla uyumludur. 

# Doktor Modülü Sistem Akışı

Doktor modülü, sistemde doktor rolüne ait işlemlerin yönetildiği ana bölümdür. Doktor uygulamaya giriş yaptıktan sonra yalnızca kendisine ait randevuları, muayene süreçlerini ve oluşturduğu reçeteleri görüntüleyebilir. Bu modülün temel amacı; doktorun günlük randevu akışını düzenli şekilde yönetmesi, zamanı gelmiş hastaları muayene etmesi, gerekli durumlarda reçete oluşturması ve geçmiş işlemlerini takip edebilmesidir.

Doktor tarafında alt menü yapısı şu dört bölümden oluşacaktır:

* Anasayfa
* Randevular
* Reçeteler
* Hesabım

Burada muayene işlemi ayrı bir bottom navigation sekmesi olmayacaktır. Muayene, bağımsız bir bölüm değil, randevuya bağlı bir işlem olduğu için Randevular ekranı içinden başlatılan bir aksiyon olarak çalışacaktır. Bu yapı hem daha temiz hem de kullanıcı açısından daha doğal bir akış sunar.

---

# 1. Doktor Anasayfa Ekranı

Doktor giriş yaptıktan sonra açılacak ilk ekran Doktor Anasayfa ekranıdır. Bu ekran, doktorun güncel durumunu özetleyen ana panel olarak çalışacaktır.

Bu ekranda şu bilgiler yer almalıdır:

* Karşılama mesajı
  Örnek: “Hoş geldiniz, Dr. Ahmet Yılmaz”
* Doktorun adı soyadı
* Branş bilgisi
* Bekleyen randevu sayısı
* Bugün muayene edilmiş hasta sayısı

## Bekleyen randevu sayısı

Bekleyen randevu sayısı, doktora ait olup durumu hâlâ `SCHEDULED` kalan ve randevu tarihi-saati geçmiş olmasına rağmen henüz sonuçlandırılmamış kayıtlar üzerinden hesaplanmalıdır.

Yani bu sayı:

* doktora ait olan
* `status = SCHEDULED` olan
* randevu tarihi ve saati geçmiş olan

randevuların toplamını göstermelidir.

Bu bilgi sayesinde doktor, sisteme girdiği anda işlem bekleyen gecikmiş randevularını görebilir.

## Bugün muayene edilmiş hasta sayısı

Bu alanda doktora ait ve bugünün tarihinde `COMPLETED` durumuna getirilmiş randevuların sayısı gösterilmelidir. Böylece doktor o gün kaç hastanın muayenesini tamamladığını görebilir.

---

# 2. Randevular Ekranı

Randevular ekranı, doktor tarafının ana işlem ekranıdır. Tasarım dili hasta tarafındaki randevu ekranı ile uyumlu olabilir. Böylece uygulama içinde görsel bütünlük korunur. Ancak doktor tarafında içerik ve aksiyonlar doktor rolüne göre özelleştirilmelidir. Doktor burada yalnızca kendisine ait randevuları görecektir. Listeleme `doctorId` alanı üzerinden yapılmalıdır. Bu yaklaşım mevcut sistemin rol bazlı erişim yapısıyla da uyumludur. 

Randevular ekranı üç ana bölümden oluşmalıdır:

* Bekleyen Randevular
* Gelecek Randevular
* Geçmiş Randevular

Her bölümün uygun bir yerinde ilgili listedeki toplam kayıt sayısı da gösterilmelidir. Böylece doktor her sekmede kaç randevu bulunduğunu hızlıca anlayabilir.

---

## 2.1 Bekleyen Randevular

Bu bölümde zamanı geçmiş ama henüz doktor tarafından sonuçlandırılmamış randevular listelenmelidir.

Burada şu kayıtlar gösterilmelidir:

* doktora ait olan
* `status = SCHEDULED` olan
* randevu tarihi ve saati geçmiş olan

Bu sekme, doktorun aktif olarak işlem yapması gereken kayıtları içerir. Buradaki her randevu kartında `Muayene Et` butonu bulunmalıdır. Doktor bu buton üzerinden muayene sürecini başlatacaktır.

Bu bölümün üst kısmında ya da uygun bir alanında örneğin şu şekilde sayı bilgisi gösterilebilir:

* Bekleyen Randevu Sayısı: 4

Bu sayı, listelenen kayıtlarla birebir aynı olmalıdır.

---

## 2.2 Gelecek Randevular

Bu bölümde henüz zamanı gelmemiş planlı randevular listelenmelidir.

Burada şu kayıtlar yer almalıdır:

* doktora ait olan
* `status = SCHEDULED` olan
* randevu tarihi ve saati henüz gelmemiş olan

Bu bölümde tarih seçimi yapılabilmelidir. Doktor belirli bir gün için planlı randevularını görebilmelidir. Sayfanın uygun bir yerinde seçili tarihe ait toplam randevu sayısı da gösterilmelidir.

Varsayılan olarak ekran açıldığında **bugünün gelecek randevuları** listelenmelidir. Yani ilk açılışta seçili tarih bugünün tarihi olmalı ve bugüne ait, saati henüz gelmemiş randevular gösterilmelidir.

Örneğin ekranda şu mantık yer alabilir:

* Seçili Tarih: 31 Mart 2026
* Toplam Gelecek Randevu: 6

Bu bölüm yalnızca planlı randevuları göstermelidir. Burada muayene başlatma butonu bulunmamalıdır. Detay ya da ek aksiyon da olmayacaktır. Bu kartlar yalnızca bilgi amaçlı listelenecektir.

---

## 2.3 Geçmiş Randevular

Bu bölümde sonucu kesinleşmiş tüm randevular listelenmelidir.

Burada şu durumdaki kayıtlar bulunabilir:

* `COMPLETED`
* `MISSED`
* `CANCELLED`

Yani geçmiş randevular yalnızca muayene edilmiş kayıtları değil, sonucu netleşmiş tüm eski kayıtları içerir. Bu yapı sistem açısından daha kapsamlı ve daha doğrudur.

Geçmiş randevular bölümünde de toplam kayıt sayısı gösterilmelidir.

Örneğin:

* Geçmiş Randevu Sayısı: 18

Bu kartlarda detay ekranı, ekstra aksiyon ya da ayrı bir detay görüntüleme ihtiyacı olmayacaktır. Kartlar bilgi amaçlı gösterilecektir. Ancak burada önemli bir istisna vardır:

Eğer ilgili geçmiş randevuya ait bir reçete varsa kart üzerinde `Reçeteyi Görüntüle` butonu bulunmalıdır. Bu butona basıldığında reçete ayrı bir sayfaya gitmeden, ekran üzerinde açılan responsive bir pencere ya da modal window yapısında gösterilmelidir.

Bu pencere:

* mobil ekrana uyumlu olmalı
* taşma yapmamalı
* küçük ekranlarda alt taraftan açılan sheet mantığında çalışabilmeli
* büyük ekranlarda ortalanmış dialog yapısında görünebilmelidir

Bu pencere içinde şu bilgiler gösterilebilir:

* reçete kodu
* hasta adı
* oluşturulma tarihi
* ilaç listesi
* doktor açıklamaları / notları

Eğer randevuya ait reçete yoksa bu buton hiç görünmemelidir.

---

# 3. Randevu Kartı Yapısı

Doktor tarafındaki randevu kartları, hasta tarafındaki kart yapısıyla uyumlu olabilir; ancak doktorun ihtiyaçlarına göre düzenlenmelidir.

Kartlarda şu bilgiler bulunmalıdır:

* Hasta adı soyadı
* Randevu tarihi
* Randevu saati
* Hastane adı
* Branş adı
* Durum etiketi

Duruma göre kart davranışı şu şekilde olmalıdır:

* Bekleyen Randevular kartlarında `Muayene Et` butonu bulunmalıdır
* Gelecek Randevular kartlarında buton bulunmamalıdır
* Geçmiş Randevular kartlarında yalnızca reçete varsa `Reçeteyi Görüntüle` butonu bulunmalıdır

Bunun dışında geçmiş ve gelecek kartlarda ekstra detay butonu, düzenleme butonu ya da başka bir aksiyon yer almayacaktır.

---

# 4. Muayene Başlatma Akışı

Muayene işlemi `DoctorExaminationFragment` ekranında yürütülecektir. Bu ekran, doktor tarafında muayene ve reçete sürecinin birlikte yönetildiği ana ekrandır.

Doktor, Bekleyen Randevular sekmesindeki bir kart üzerinde bulunan `Muayene Et` butonuna bastığında bu ekrana yönlendirilmelidir.

Bu ekran yalnızca şu koşullardaki randevular için açılmalıdır:

* randevu doktora ait olmalı
* randevu durumu `SCHEDULED` olmalı
* randevu tarihi ve saati gelmiş ya da geçmiş olmalı

Bu kuralla zamanı henüz gelmemiş randevuların yanlışlıkla sonuçlandırılması önlenmiş olur.

---

# 5. DoctorExaminationFragment İçeriği

Bu ekran muayene sürecinin merkezidir. Doktor burada randevuyu sonuçlandırır, hastanın gelip gelmediğini belirtir ve gerekiyorsa reçete oluşturur.

Bu ekranda şu alanlar bulunmalıdır:

* Hasta adı soyadı
* Randevu tarihi
* Randevu saati
* Hastane bilgisi
* Branş bilgisi
* Doktor bilgisi
* Muayene notu alanı
* Reçete gerekli mi? seçimi
* İlaç seçimi bölümü
* Doktor açıklama alanı
* Hasta Gelmedi aksiyonu
* Muayeneyi Tamamla butonu

## Muayene notu alanı

Muayene notu alanı bulunmalıdır ancak zorunlu olmamalıdır. Doktor isterse not ekleyebilir, isterse boş bırakabilir.

---

# 6. Reçete Gerekli mi? Kararı
    
Muayene ekranında doktorun net şekilde karar vermesi gereken alanlardan biri “Reçete gerekli mi?” seçimidir. Bu seçim boş bırakılamamalıdır. Doktor şu iki seçenekten birini mutlaka seçmelidir:

* Evet, reçete gerekli
* Hayır, reçete gerekli değil

## Reçete gerekli değilse

Bu durumda:

* ilaç seçme alanı açılmaz
* doktor isterse muayene notu ekler
* `Muayeneyi Tamamla` butonuna basar
* randevu `COMPLETED` olur
* reçete kaydı oluşmaz

## Reçete gerekliyse

Bu durumda:

* ilaç seçme alanı görünür hale gelir
* doktor ilaçları seed data listesinden seçer
* seçilen ilacın standart bilgileri otomatik doldurulur
* doktor sadece gerekli açıklama alanlarını tamamlar
* `Muayeneyi Tamamla` butonuna bastığında hem reçete oluşturulur hem randevu tamamlanır

Bu karar, reçete sürecini belirsizlikten çıkarır ve akışı zorunlu seçim temeline oturtur.

---

# 7. Reçete Yazımı UI/UX Yapısı

Reçete yazım alanı, muayene ekranının içinde açılan düzenli ve hızlı kullanılabilir bir yapı olmalıdır. Amaç, doktorun her şeyi manuel yazmak zorunda kalmaması ve reçeteyi kısa sürede oluşturabilmesidir.

## İlaç seçimi

Doktor ilaçları serbest metinle yazmamalıdır. Bunun yerine uygulama içinde önceden tanımlanmış ilaç seed data listesinden seçim yapmalıdır.

Akış şöyle olmalıdır:

* doktor `İlaç Ekle` butonuna basar
* seed data listesinden ilaç seçer
* seçilen ilaç reçete listesine eklenir
* ilaca ait standart alanlar otomatik doldurulur

## Her ilaç satırında gösterilecek alanlar

Reçete içinde her ilaç satırında şu bilgiler yer almalıdır:

* İlaç adı
* Doz
* Kullanım sıklığı
* Kullanım açıklaması

Ancak bu alanların mantığı şöyledir:

* İlaç adı seed data’dan gelir
* Doz seed data’dan gelir
* Kullanım sıklığı seed data’dan gelir
* Kullanım açıklaması seed data’dan gelir
* Doktor sadece gerekiyorsa ek açıklama ya da özel not girer

Bu yapı sayesinde veri standardizasyonu sağlanır ve doktorun manuel veri girişi azaltılır.

## Reçete bölümü tasarım mantığı

Reçete alanı çok kalabalık olmamalıdır. Şu yapı uygun olur:

* Üstte “Reçete Bilgileri” başlığı
* Altında seçilen ilaçların kart ya da satır listesi
* Her ilaç için otomatik gelen alanlar
* Doktor notu / ek açıklama alanı
* Yeni ilaç ekleme butonu
* İstenirse ilaç silme butonu

Bu yapı hem okunabilir hem de mobil ekranda rahat kullanılabilir olur.

---

# 8. Muayene ve Reçete Tamamlama Mantığı

Buradaki en önemli iş kuralı şudur:

**Muayene tamamlandığında, reçete gerekiyorsa reçete de aynı işlem içinde oluşturulmuş olmalıdır.**

Bu nedenle ayrı bir `Reçeteyi Kaydet` butonu olmayacaktır. Tek ana işlem butonu `Muayeneyi Tamamla` olacaktır.

Bu butonun davranışı seçime göre değişir:

## Reçete gerekmiyorsa

`Muayeneyi Tamamla` butonu:

* randevuyu `COMPLETED` yapar
* reçete oluşturmaz

## Reçete gerekiyorsa

`Muayeneyi Tamamla` butonu:

* önce reçete kaydını oluşturur
* ardından randevuyu `COMPLETED` yapar

Yani reçete kaydı ve muayene tamamlama işlemi aynı akış içinde gerçekleşmelidir. Bu yaklaşım yarım veri oluşmasını önler, doktor deneyimini sadeleştirir ve veri tutarlılığını artırır.

---

# 9. Hasta Gelmedi Akışı

Muayene ekranında ayrıca `Hasta Gelmedi` aksiyonu bulunmalıdır.

Doktor bu seçeneği kullandığında:

* bir onay penceresi açılmalıdır
* işlem teyit edilmelidir
* onay sonrası randevu `MISSED` yapılmalıdır
* reçete alanı pasif hale gelmelidir
* reçete oluşturulamaz

Bu sayede doktor, zamanı geçmiş fakat gerçekleşmemiş randevuları hızlıca sonuçlandırabilir.

---

# 10. Randevu Durum Kuralları

Doktor tarafında kullanılacak randevu durumları şunlardır:

```kotlin id="k4rxtb"
enum class AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    MISSED,
    CANCELLED
}
```

Doktor yalnızca şu geçişleri yapabilmelidir:

* `SCHEDULED -> COMPLETED`
* `SCHEDULED -> MISSED`

Doktorun yapamaması gereken durum geçişleri:

* `COMPLETED -> SCHEDULED`
* `MISSED -> SCHEDULED`
* `CANCELLED -> COMPLETED`
* `COMPLETED -> CANCELLED`

Durumların anlamı şöyledir:

* `SCHEDULED`: planlanmış ancak henüz sonuçlandırılmamış randevu
* `COMPLETED`: muayenesi tamamlanmış randevu
* `MISSED`: hasta gelmediği için sonuçlandırılmış randevu
* `CANCELLED`: iptal edilmiş randevu

Bu sınırlama sistemde kontrolsüz veri değişimini önler.

---

# 11. Reçeteler Ekranı

Reçeteler ekranında doktorun yazdığı reçeteler listelenmelidir. Doktor yalnızca kendisine ait reçeteleri görebilmelidir. Bu davranış mevcut rol bazlı sistem yaklaşımıyla uyumludur. 

## Varsayılan görünüm

Ekran açıldığında varsayılan olarak bugünün reçeteleri listelenmelidir.

## Filtreleme

Bu ekranda tarih filtreleme bulunmalıdır. Ayrıca tarih aralığı filtreleme de desteklenmelidir.

Desteklenebilecek filtre türleri:

* Bugün
* Belirli tarih seçimi
* Tarih aralığı seçimi

## Reçete kartı içeriği

Her reçete kartında şu bilgiler bulunabilir:

* Hasta adı soyadı
* Reçete kodu
* Oluşturulma tarihi
* İlaç sayısı
* Kısa not özeti

Kart seçildiğinde reçete detay bilgisi gösterilebilir.

---

# 12. Hesabım Ekranı

Hesabım ekranı, hasta tarafındaki profil ekranıyla benzer mantıkta çalışabilir. Burada doktorun temel bilgileri ve çıkış işlemi yer almalıdır.

Bu ekranda şu bilgiler bulunmalıdır:

* Ad soyad
* E-posta
* Branş
* Hastane
* Oda bilgisi
* Çıkış Yap butonu

İstenirse çalışma günleri ve çalışma saatleri de burada gösterilebilir.

---

# 13. Teknik Veri Modelleri ve İlişkiler

Doktor modülünün temelinde randevu, reçete ve ilaç verileri yer alır. Sistemde randevu ve reçete verilerinin merkezi şekilde Firestore’da tutulduğu ve doktorun yalnızca kendi kayıtlarına eriştiği yapı tanımlanmıştır. 

## Appointment

```kotlin id="cd8pc4"
data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val hospitalId: String = "",
    val branchId: String = "",
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val createdAt: Long = 0L
)
```

Bu model doktorun hangi hastayla, hangi tarih ve saatte randevusu olduğunu ve randevunun hangi durumda bulunduğunu temsil eder.

## Prescription

Yeni kararlara göre reçete yapısı not ve ilaçlar üzerinden ilerlemelidir.

```kotlin id="68713j"
data class Prescription(
    val id: String = "",
    val appointmentId: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val prescriptionCode: String = "",
    val note: String = "",
    val medicines: List<PrescriptionItem> = emptyList(),
    val createdAt: Long = 0L,
    val status: PrescriptionStatus = PrescriptionStatus.ACTIVE
)
```

## PrescriptionItem

```kotlin id="uk1cgg"
data class PrescriptionItem(
    val medicineId: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val usageDescription: String = "",
    val doctorNote: String = ""
)
```

Burada:

* `appointmentId` reçetenin hangi randevuya ait olduğunu gösterir
* `patientId` reçetenin hangi hastaya ait olduğunu gösterir
* `doctorId` reçeteyi hangi doktorun oluşturduğunu gösterir

Bu ilişki yapısı sayesinde bir reçetenin hangi muayene sonucunda oluştuğu açık şekilde takip edilebilir.

## Medicine Seed Data

İlaçlar uygulama içinde seed data olarak tanımlanmalıdır. Doktor reçete oluştururken ilaçları bu veri kaynağından seçmelidir.

Örnek yapı:

```kotlin id="40ye24"
data class MedicineSeed(
    val id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val usageDescription: String
)
```

Bu yapı sayesinde seçilen ilaç sisteme standart formatta eklenir.

---

# 14. İşlem Akışları

## Muayene reçetesiz tamamlanırsa

1. Doktor bekleyen randevuyu açar
2. Reçete gerekli değil seçer
3. İsterse muayene notu ekler
4. Muayeneyi Tamamla butonuna basar
5. Randevu `COMPLETED` olur
6. Reçete kaydı oluşmaz

## Muayene reçeteli tamamlanırsa

1. Doktor bekleyen randevuyu açar
2. Reçete gerekli seçer
3. Seed data listesinden ilaç seçer
4. Gerekirse ilaçlara özel açıklama ekler
5. Muayeneyi Tamamla butonuna basar
6. Sistem reçete kaydını oluşturur
7. Aynı işlem akışında randevu `COMPLETED` yapılır

## Hasta gelmediyse

1. Doktor bekleyen randevuyu açar
2. Hasta Gelmedi aksiyonunu seçer
3. Onay verir
4. Randevu `MISSED` olur
5. Reçete oluşturulmaz

---

# 15. Sonuç

Bu yapıyla doktor modülü sade, anlaşılır ve güçlü bir akışa sahip olur. Doktor anasayfada kendi iş yoğunluğunu görür, Randevular ekranında bekleyen, gelecek ve geçmiş randevularını ayrı ayrı takip eder, zamanı gelmiş kayıtlar için `DoctorExaminationFragment` üzerinden muayene sürecini yürütür ve gerekiyorsa reçeteyi aynı akış içinde oluşturur. Geçmiş randevularda reçetesi olan kayıtlar için reçete doğrudan responsive bir pencere içinde görüntülenebilir. Gelecek randevular tarih seçimiyle incelenebilir ve her bölümde ilgili randevu sayıları gösterilir. Bu yapı hem kullanıcı deneyimi açısından düzenlidir hem de projenin teknik mantığıyla uyumlu, uygulanabilir ve sürdürülebilir bir doktor modülü ortaya çıkarır.
