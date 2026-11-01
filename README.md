**V2 NOT:** GitHub Actions eski setup-android@v3 adimi yerine hazir SDK'yi kullanir. Yukleme adimlari icin `GITHUB_KURULUM_TR.md` dosyasina bak. Kaynak ZIP ilk halinde wrapper JAR barindirmaz; ilk basarili Actions calismasi tam Android Studio kaynak ZIP'ini olusturur.

# ADIM SAYAR — Android Studio + GitHub Actions

**Sürüm:** 1.0.0 (ilk geliştirici sürümü; derleme ve cihaz testi bekliyor)  
**Platform:** Android 8.0 (API 26) ve üstü  
**Dil:** Kotlin (tamamen yerel Android uygulaması; HTML/WebView değil)  
**İnternet:** Uygulama çalışırken gerekmiyor  
**Derleme:** AGP 8.11.1 + Kotlin 2.2.21 + Gradle 8.13 + JDK 17

## Uygulamada çalışan özellikler

- Telefonun `TYPE_STEP_COUNTER` sensörünü okur.
- Telefon bunu sunmazsa `TYPE_STEP_DETECTOR` sensörünü dener.
- Android 10 ve sonrasında `ACTIVITY_RECOGNITION` izni ister.
- Kullanıcı başlattığında bildirim gösteren bir `health` foreground service başlatır.
- Kullanıcı bildirimden veya ana ekrandan takibi durdurabilir.
- Günlük adım toplamları cihazda saklanır, 14 günlük geçmişi ve haftalık grafiği gösterir.
- Günlük hedef, adım uzunluğu, kilo, tahmini mesafe ve kalori hesaplaması bulunur.
- GPS, hesap açma, veri aktarımı veya internet bağlantısı gerekmez.

## 1) GitHub kullanarak APK üretme (önerilen kolay yol)

1. GitHub'da yeni ve boş bir depo aç (örneğin `adim-sayar`).
2. Bu ZIP dosyasını **bilgisayarda klasöre çıkar**. GitHub'a ZIP dosyasının kendisini değil,
   içindeki bütün proje dosyalarını ve `.github/workflows/android-apk.yml` klasörünü yükle.
   Windows'ta gizli `.github` klasörünün de yüklendiğini kontrol et.
3. Depoda **Add file > Upload files** üzerinden içerikleri yükleyip `Commit changes` yap.
   Daha rahat yöntem: GitHub Desktop ile depoyu bilgisayara klonlamak ve klasördeki dosyaları kopyalayıp `Commit` + `Push` yapmak.
4. GitHub deposunda **Actions** sekmesine git.
5. **ADIM SAYAR - APK Derle** iş akışını aç. İstersen `Run workflow` düğmesiyle çalıştır.
   `main` veya `master` dalına push yapınca da tetiklenir.
6. İşlem başarıyla tamamlanırsa en son iş akışına gir, sayfanın altındaki **Artifacts** bölümünden:
   - `ADIM-SAYAR-debug-APK` (APK içeren ZIP artefaktı)
   - `ADIM-SAYAR-AndroidStudio-Kaynak` (Gradle Wrapper dahil, Android Studio'ya hazır kaynak ZIP'i)
   indirilebilir.
7. APK artefaktından çıkan `app-debug.apk` dosyasını Android telefonuna aktar ve kur.
   Android cihazından üçüncü taraf APK kurulumuna izin verilmesi gerekebilir.

> GitHub Actions APK'yı otomatik DERLER ama kullanıcının hesabına yükleme veya Play Store yayını yapmaz.
> İş akışı `Debug APK` oluşturur; Play Store için imzalı sürüm/AAB, uygulama beyanları ve ek testler gerekir.

## 2) Android Studio'da açma

Bu ilk ZIP, GitHub Actions üzerinde doğrudan derlenebilecek bütün kaynak kodlarını içerir.
Gradle Wrapper **binary jar** dosyası GitHub iş akışında resmi Gradle ile üretilir.
Android Studio ile yerelde en sorunsuz açılış için Actions artefaktındaki
`ADIM_SAYAR_ANDROID_STUDIO_TAM.zip` dosyasını indirip açman önerilir.

Android Studio > **File > Open** > `AdimSayar` proje klasörünü seç > **Sync Project** >
**Run**. JDK 17 ve Android SDK Platform 35 kurulu olmalı.

Bu ilk ZIP'i Android Studio ile hemen açmak istersen sisteminde Gradle 8.13 kuruluysa
proje klasöründe `gradle wrapper --gradle-version 8.13` komutunu çalıştırabilirsin;
Android Studio'da ardından Sync işlemini yap.

## 3) Proje klasörleri

```
ADIM_SAYAR_ANDROID_STUDIO_GITHUB/
├── .github/workflows/android-apk.yml   # GitHub'da otomatik APK derlemesi
├── app/build.gradle.kts                # Android uygulama ayarları
├── app/src/main/AndroidManifest.xml    # Sensör ve servis izinleri
├── app/src/main/java/com/adimsayar/app/
│   ├── MainActivity.kt                 # Ekranlar, gezinme, ayarlar, izinler
│   ├── StepTrackingService.kt          # Arka planda açık takip ve bildirim
│   ├── StepRepository.kt               # Günlük veriler ve matematik
│   └── Visuals.kt                      # Adım halkası ve haftalık grafik
├── app/src/main/res/                  # Tema, icon, metinler
├── build.gradle.kts                   # Android Gradle ve Kotlin sürümleri
├── settings.gradle.kts
├── gradle.properties
├── PRIVACY.md
└── README.md
```

## 4) Bilmen gereken sınırlar

1. **Gerçek sensör şarttır:** Donanım adım sensörü olmayan Android telefonda bu ilk sürüm adım sayamaz.
   Emülatörde gerçek fiziksel adım görmek çoğunlukla mümkün değildir.
2. **İlk sensör okuması baz alınır:** Uygulama kurulmadan veya takip başlamadan önce atılmış adımları geçmişten geriye doğru hesaplamaz.
3. **Kilitli ekran:** Foreground service ve sensörün çalışması hedeflenmiştir; marka/model pil yönetimi,
   sensör gönderim sıklığı veya kullanıcı tarafından zorla durdurulma bunu etkileyebilir.
4. **Gece yarısı:** Gün değişirken servis hiç sensör olayı almazsa, geçmişte birikmiş adımların
   hangi güne ait olduğunu kesin olarak tespit edemez. Bu adımlar otomatik tahmin edilerek eklenmez.
5. **Yeniden başlatma:** Cihaz yeniden başlatıldığında otomatik servis başlatılmıyor; kullanıcı uygulamayı
   açıp takibi yeniden başlatmalıdır. Bu sırada kaçan adımlar geriye dönük sayılamaz.
6. **Health Connect:** Bu ilk kaynak ZIP'inde Health Connect veya Google Fit senkronizasyonu bulunmaz.
   Gelişmiş geçmiş senkronizasyonu sonraki sürümün özelliğidir.
7. **Kalori:** Sadece basit tahmin (`mesafe(km) × kilo(kg) × 0,7`) gösterilir, tıbbi doğruluk iddia edilmez.
8. **Test:** Bu projeyi kaynak dosyalarından oluşturdum; burada Android SDK ve Gradle derleme ortamı
   bulunmadığı için APK derlemesini çalıştırarak doğrulayamadım. GitHub Actions hata verirse
   `Actions > build > Debug APK derle` günlüklerini paylaş; hatayı düzeltebiliriz.

## 5) Google Play'e geçmeden önce

- Gerçek cihazlarda Android 8–15+ üzerinde fonksiyon ve pil tüketimi testleri yap.
- Google Play foreground service türünü ve sağlık uygulaması içerik beyanlarını kontrol et.
- Gizlilik politikasını erişilebilir bir URL'de yayınla (örnek taslak `PRIVACY.md`).
- Yayın için kendi paket adını seç, uygun imzalı release/AAB oluştur ve mağaza kurallarını karşıla.

### Resmî bağlantılar
- https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://developer.android.com/build/releases/about-agp
- https://docs.github.com/en/actions
