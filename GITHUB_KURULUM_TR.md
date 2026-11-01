# ADIM SAYAR - GitHub ile APK yapma (V2)

## En kolay yol

1. Bu ZIP dosyasini bilgisayarinda cikar. **ZIP dosyasini oldugu gibi GitHub'a yukleme**.
2. GitHub'daki ADIM SAYAR reposuna gir. Kodlarin bulundugu ana klasore bu ZIP'in icindeki tum dosyalari yukle. `.github` klasoru gizli olabilir, mutlaka eklendigini kontrol et.
3. GitHub'da `.github/workflows/android-apk.yml` yolunun bulunduğunu kontrol et.
   Bu sürüm Html2Apk örneği gibi sadedir: ekstra `sdkmanager` / `setup-android` yok.
4. `Commit changes` islemini yap.
5. `Actions` > `ADIM SAYAR - APK Derle` > `Run workflow` (veya otomatik push is akisinin tamamlanmasi).
6. Yesil tik geldikten sonra is akisini ac, en alttan `Artifacts` > `ADIM-SAYAR-debug-APK` dosyasini indir.
7. Inen ZIP'i ac; `app-debug.apk` uygulamayi Android telefona kurmak icindir.

## Sadece tek dosyayi degistirmek istersen

GitHub repository > Code > `.github/workflows/android-apk.yml` > kalem (Edit)
> eski icerigi sil > `android-apk-DUZELTILMIS.yml` dosyasinin icerigini yapistir > `Commit changes`.

## Android Studio icin tam kaynak kod

Actions basariyla tamamlaninca, ikinci artifact olan
`ADIM-SAYAR-AndroidStudio-Kaynak` ZIP dosyasini indir.
Bu dosya `gradlew`, `gradlew.bat` ve `gradle-wrapper.jar` icerir.
Gonderilen bu on kaynak ZIP'inde wrapper .jar dosyasi yoktur;
GitHub Actions bunu olusturur. GitHub bu kaynagi derleyince olusan ZIP
Android Studio icin daha eksiksizdir.

## GitHub hata verirse

Kirmizi X > basarisiz asamayi ac > en sondaki **son 20-30 log satirini**
veya log dosyasini paylas. Yalnizca baslik veya ilk 10 satir gercek
hata nedenini gostermez.

NOT: Bu bir kaynak kod projesidir, onceden derlenmis APK degildir.
