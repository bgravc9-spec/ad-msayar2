# ADIM SAYAR — Gizlilik Politikası Taslağı (V1)

Tarih: 1 Kasım 2026

**ÖNEMLİ:** Bu belge yalnızca dağıtıma hazırlık amaçlı bir taslaktır. Uygulamanın
kamuya açık yayınından önce geliştiricinin/işletmenin gerçek kimlik ve iletişim
bilgileri eklenmeli, KVKK ve ilgili mağaza yükümlülükleri açısından kontrol edilmelidir.

## Uygulamanın işlediği veriler

- Telefonun hareket sensöründen alınan adım sayaçları
- Günlük adım toplamı ve geçmiş günlerin toplamları
- Kullanıcının ayarladığı günlük hedef, adım uzunluğu ve kilo

## Verilerin kullanımı

Veriler adım toplamını, mesafe ve yaklaşık kalori tahminini ve geçmişi göstermek
amacıyla **yalnızca cihazda** tutulur. V1 kaynak kodunda sunucuya veri yollama,
analitik veya reklam SDK'sı bulunmaz. Uygulama internet veya konum izni istemez.

## İzinler

- `ACTIVITY_RECOGNITION`: adım sensörü erişimi.
- `FOREGROUND_SERVICE` ve `FOREGROUND_SERVICE_HEALTH`: kullanıcı başlatmışken
  bildirimli takip işlemi.
- `POST_NOTIFICATIONS`: Android 13+ cihazlarda takip bildirimini göstermek.

## Silme ve kontrol

Adım takibi uygulama içinden durdurulabilir. Android Ayarları üzerinden
uygulama verilerini silmek, bu sürümdeki yerel kayıtları kaldırır. Uygulama
kaldırıldığında yerel depolanan veriler de normal şartlarda silinir.

## İletişim ve sorumluluk

Kamuya açık yayın öncesinde veri sorumlusunun iletişim bilgileri, saklama süreleri,
veri sahibi hakları ve ilgili diğer zorunlu açıklamalar eklenmelidir.
