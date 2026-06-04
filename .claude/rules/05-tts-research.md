# TTS تحقیق — فارسی

## نتیجه: RHVoice بهترین گزینه برای ایران است

| معیار | Google TTS | RHVoice | eSpeak NG |
|--------|-----------|---------|-----------|
| کیفیت فارسی | عالی | خوب | ضعیف (رباتیک) |
| آفلاین | بله | بله | بله |
| منبع دانلود | Google servers (فیلتر) | GitHub/F-Droid (غیرفیلتر) | GitHub (غیرفیلتر) |
| نصب | Play Store | APK sideload | APK sideload |
| حجم | ~15MB | ~10MB | ~5MB |
| لایسنس | Proprietary | GPL | GPL |

## استراتژی:
1. پیش‌فرض: Google TTS (روی اکثر گوشی‌های Samsung/Xiaomi موجود در ایران نصب است)
2. Fallback: RHVoice — کاربر از داخل Nova راهنمایی می‌شود تا APK را دانلود و نصب کند
3. Last resort: eSpeak NG

## پیاده‌سازی در Nova:
- TtsManager فعلی با زنجیره fa-IR → fa → ar → default بماند
- اگر locale نهایی fa نباشد ← پیام به کاربر: "صدای فارسی نصب نیست"
- دکمه در تنظیمات: "دانلود RHVoice" ← لینک به GitHub Release
