# masapay — Dizayn Sistemi

Bu sənəd masapay-ın vizual dizayn dilini müəyyən edir. Bütün UI işi (müştəri
PWA və admin panel) bu qaydalara tabedir. Emoji qadağası və shadcn/ui əsaslılıq
qaydaları [CLAUDE.md](../CLAUDE.md)-dəki kimi qüvvədə qalır.

## Fəlsəfə

Minimalist, **boş sahə (whitespace)** üzərində qurulmuş, məzmun-mərkəzli və
**etibar hissi** yaradan dizayn. Açıq və tünd bölmələrin növbələşməsi ilə ritm
yaradılır — isti bej/greige fon bölmələri ilə demək olar ki, qara (near-black)
bölmələr bir-birini əvəz edir.

## Rəng Palitrası

Layihədə istifadə olunan rənglər:

| Rol | Dəyər | İzah |
|---|---|---|
| Ağ | `#FFFFFF` | Səth / açıq mətn fonu |
| Qara | `#000000` | Saf qara mətn (açıq fonda) |
| Vurğu (yaşıl) | `#08CB00` | Bizim **imza rəngi** — yalnız seyrək, önəmli vurğular |
| Açıq fon (greige) | `#F6F5F6` | İsti, neytral açıq-boz/krem — hero və açıq bölmələr |
| Near-black | `#0A0A0A` | Tünd panellər fonu |

Qaydalar:

- **Fon:** hero və açıq bölmələr isti neytral açıq-boz/krem (`#F6F5F6`); tünd
  bölmələr demək olar qara (`#0A0A0A`) panellər.
- **Mətn:** açıq fonda saf qara, tünd fonda ağ — həmişə **yüksək kontrast**.
- **Yaşıl çox seyrək istifadə olunur.** O, bizim imza rəngimizdir və yalnız
  önəmli vurğu nöqtələrində görünür (loqo, bir neçə açar ikon, kiçik nişan).
  Səhifənin böyük hissəsi qara/ağ/greige-dir. **Yaşıl əsas düymələrdə,
  başlıqlarda və ya böyük sahələrdə istifadə OLUNMUR.** (Referans: Sunday öz
  çəhrayısını ~2000 elementdən yalnız ~8-ində işlədir.)
- **Yaşıl vurğu (`#08CB00`) üzərində mətn qara** olmalıdır (parlaq yaşıl fonda
  ağ mətn kontrast standartını keçmir).
- **Qradient rənglər və Bənövşəyi-Mavi qarışıqları qadağandır** (əvvəlki
  qaydadan qalır).

### Düymələr

- Forma: **pill** (`rounded-full`).
- Əsas (primary): **qara fon, ağ mətn**. Tünd səth üzərində tərsinə —
  **ağ fon, qara mətn**.
- İkincili (secondary): şəffaf fon, nazik border (outline), yenə pill.
- Düymələr yaşıl DEYİL — yaşıl yalnız imza vurğusudur.

### Tokenlər

Brand rəngləri CSS dəyişənləri kimi `src/index.css`-də təyin olunub və Tailwind
`brand.*` utiliti kimi əlçatandır:

```
--brand-white: #fff;        →  bg-brand-white / text-brand-white
--brand-black: #000;        →  text-brand-black
--brand-green: #08cb00;     →  bg-brand-green / text-brand-green
--brand-greige: #f6f5f6;    →  bg-brand-greige
--brand-near-black: #0a0a0a; → bg-brand-near-black
```

> shadcn semantik tokenlərinin (`--background`, `--primary`, `--success` və s.)
> tam bu palitraya köçürülməsi ayrıca issue kimi izlənilir.

## Tipografiya

- **Şrift:** Helvetica Neue / Arial (klassik qrotesk). Layihəyə self-hosted
  woff2 kimi inteqrasiya olunub (`/public/fonts`, bax [Font inteqrasiyası](#font-inteqrasiyası)).
- **Nəhəng başlıqlar:** çəki **400**, **çox sıx tracking** (`-0.045em`) və sıx
  sətir aralığı (leading ~0.98). İri, tam eninə yayılan başlıqlar səhifənin əsas
  vizual ağırlığını daşıyır.
- **Bütün mətn 400 çəkidədir** (başlıqlar da). Vurğu ölçü və boşluqla yaradılır,
  qalın çəki ilə yox.
- **Alt mətnlər:** kiçik (16px), boz (muted) və oxunaqlı — aydın iyerarxiya.

Nümunə iyerarxiya (referans design ilə uyğun):

| Element | Ölçü | Çəki | Tracking |
|---|---|---|---|
| Hero başlıq | ~48px (`clamp` 36–52px) | 400 | -0.045em |
| Bölmə başlığı | ~48px (`clamp` 32–48px) | 400 | -0.045em |
| Kart / addım başlığı | 24px | 400 | -0.01em |
| Gövdə / alt mətn | 16px | 400 | normal, boz |

## Bölmə Ritmi

Səhifələr açıq (`#F6F5F6`) və tünd (`#0A0A0A`) bölmələrin **növbələşməsi** ilə
qurulur. Hər bölmə geniş whitespace ilə nəfəs alır; məzmun mərkəzdə, maksimum en
məhdudlaşdırılır.

## Header Davranışı

Header **fixed**-dir, lakin scroll ilə ağıllı gizlənir:

- Aşağı scroll edildikdə header **gizlənir** — aşağıdan yuxarıya sürüşmə
  animasiyası ilə (yuxarı çıxıб yox olur).
- Yuxarı scroll edildikdə header **yenidən ortaya çıxır** — yuxarıdan aşağıya
  gəlmə animasiyası ilə.
- Səhifənin ən yuxarısında header həmişə görünür.

Tövsiyə olunan tətbiq: scroll istiqamətini izləyən hook + `framer-motion`
`translateY` keçidi (məs. gizli `-100%`, görünən `0`), yumşaq `ease` ilə.

## Hərəkət (Motion)

- **Bölmə animasiyaları:** `framer-motion` — bölmələr viewport-a daxil olduqda
  fade/slide reveal (`whileInView`), `once: true`.
- **Parallax:** scroll-a bağlı parallax effektləri (`useScroll` +
  `useTransform`).
- **3D (opsional):** hero və ya vurğu elementlərində `react-three-fiber` /
  `@react-three/drei` ilə yüngül 3D. Yalnız performansa ziyan vurmadıqda və
  mobil üçün degrade olunmaqla.
- Bütün animasiyalar `prefers-reduced-motion` seçimini nəzərə almalıdır.

## Font İnteqrasiyası

- Fontlar `frontend/public/fonts/helvetica-neue-{300,400,500,700}.woff2`
  ünvanında self-hosted-dir (OTF-dən woff2-yə çevrilib, brotli sıxılma ilə).
- `@font-face` təyinatları `frontend/src/styles/fonts.css`-də, `index.css`-ə
  import olunub; `font-display: swap`.
- Tailwind `fontFamily.sans` = `["Helvetica Neue", "Arial", "system-ui",
  "sans-serif"]`; `body` da eyni stack-i istifadə edir.
- Yalnız istifadə olunan 4 çəki daxil edilib (bundle ölçüsünü saxlamaq üçün);
  əlavə çəki (Thin, Medium Italic və s.) lazım olduqda eyni qayda ilə əlavə
  olunur.
