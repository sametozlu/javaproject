# ShopFlow — Render Deploy

## Hızlı kurulum (Blueprint)

1. Repo GitHub’da: https://github.com/sametozlu/javaproject  
2. [Render Dashboard](https://dashboard.render.com) → **New** → **Blueprint**  
3. `sametozlu/javaproject` reposunu bağla — `render.yaml` otomatik okunur.  
4. Deploy bitince web servis URL’i: **`https://shopflow-api.onrender.com`** (servis adı `shopflow-api` ise).

## Ortam değişkenleri (otomatik + kontrol)

| Değişken | Kaynak | Açıklama |
|----------|--------|----------|
| `SPRING_PROFILES_ACTIVE` | `prod,render` | PostgreSQL + Redis yok (simple cache) |
| `JWT_SECRET` | Render generate | Otomatik |
| `DB_*` | `shopflow-db` | Blueprint PostgreSQL |
| `APP_MAIL_ENABLED` | `false` | SMTP olmadan deploy |
| `CORS_ALLOWED_ORIGINS` | Blueprint | Deploy sonrası gerçek URL ile güncelle |

Deploy sonrası servis URL’in farklıysa Render’da **Environment** → `CORS_ALLOWED_ORIGINS` = `https://SENIN-SERVIS.onrender.com` yap.

`RENDER_EXTERNAL_URL` Render tarafından set edilir; şifre sıfırlama linkleri için `app.app-url` buna düşer.

## İlk açılış

- Ücretsiz planda soğuk start **~1–2 dk** sürebilir.  
- Health: `https://shopflow-api.onrender.com/actuator/health`  
- Mağaza: `https://shopflow-api.onrender.com`  
- Admin (seed): `admin@shop.com` / `admin123`

## Yerel prod testi

```bash
docker compose up -d postgres redis mailhog
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

## Notlar

- Ürün görselleri `picsum.photos` üzerinden gelir (internet gerekir).  
- Admin görsel yükleme Render diskinde kalıcı değildir (ephemeral FS).  
- Canlı SMTP için `APP_MAIL_ENABLED=true` + `MAIL_HOST` / `MAIL_PORT` / kullanıcı-şifre ekle.
