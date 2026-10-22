# Masapay Sans

Brendin öz şrifti: həndəsi, monolayn, bütün ucları yumrudur. Hərflər dairə, düz
xətt və qövs skeletlərindən qurulur, xəttin qalınlığı çəkini təyin edir.

- Çəkilər: 400 (`sw` 80), 500 (96), 600 (112), 700 (128). Bax `glyphs.py` →
  `WEIGHTS`.
- Ölçülər (UPM 1000): x-height 520, cap 700, ascender 740, descender -200.
- Simvollar: A–Z, a–z, Ə ə Ğ ğ İ ı Ö ö Ş ş Ç ç Ü ü, 0–9 (tabular), əsas durğu
  işarələri, `₼ • × ± → … ·`, copyright işarəsi. Olmayan simvollar saytda Helvetica Neue ilə
  göstərilir.
- Kerning: `build.py` → `KERN` (T, V, W, Y, A, L, F, P cütləri).
- Rəqəmlər default tabulardır (qiymət sütunları üçün). İri başlıq rəqəmlərində
  `pnum` xüsusiyyəti var: Tailwind `proportional-nums`.

## Yenidən yığmaq

Tələblər: Python 3 + `fonttools`, `skia-pathops`, `brotli`; Windows-da Adobe
Illustrator (COM).

1. `python make_svg.py <iş-qovluğu>`: hər çəki üçün `skeleton-<w>.svg`,
   `metrics-<w>.json` və brauzerdə baxmaq üçün `specimen.html` yaradır.
2. Illustrator-da konturlama: `outline.jsx`-dən əvvəl `var JOBS = [{ src, out }]`
   əlavə edib `DoJavaScriptFile` ilə işə sal (`src` = `skeleton-<w>.svg`,
   `out` = `outlined-<w>.svg`). Skript hər qlifi Outline Stroke
   (`OffsetPath v22`) və Pathfinder → Unite edir.
3. `python build.py <iş-qovluğu> <çıxış-qovluğu>`: OTF və WOFF2 faylları.
   Illustrator SVG idxalında kətanı 8192-yə kiçildir; `build.py` miqyası
   skelet `viewBox`-u ilə bərpa edir.
4. WOFF2-ləri `frontend/public/fonts/`-a, OTF-ləri bu qovluğa köçür.
