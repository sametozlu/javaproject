# ShopFlow — Senin yapman gerekenler

## Yerelde çalıştır (ayar yok)

1. **`BASLA.bat`** dosyasına çift tıkla  
   veya PowerShell’de: `.\baslat.ps1`

2. Tarayıcı: **http://localhost:8080**

3. Admin: `admin@shop.com` / `admin123`

Bu kadar. Veritabanı (H2), ürünler ve demo ödeme hazır gelir.

---

## İsteğe bağlı (şimdilik atlayabilirsin)

| Ne | Neden | Ne zaman |
|----|--------|----------|
| **Stripe** | Gerçek sandbox ödeme sekmesi | [Stripe test keys](https://dashboard.stripe.com/test/apikeys) alıp `.env` içine `STRIPE_SECRET_KEY` ve `STRIPE_PUBLISHABLE_KEY` yaz, uygulamayı yeniden başlat |
| **Render canlı site** | İnternetten demo link | [render.com](https://render.com) → Blueprint → `javaproject` repo → `docs/DEPLOY.md` |
| **E-posta** | Kargo maili görmek | Docker varsa `baslat.ps1` MailHog’u açar → http://localhost:8025 |

---

## Sorun çıkarsa

- Port 8080 dolu → eski Java penceresini kapat veya `BASLA.bat` tekrar dene  
- Sayfa eski görünüyor → **Ctrl+F5**  
- `mvn test` hatası → `.\mvnw.cmd clean test`

Proje GitHub: https://github.com/sametozlu/javaproject
