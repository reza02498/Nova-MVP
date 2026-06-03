# Nova — نیازمندی‌های اپ

## هدف

دستیار صوتی/متنی فارسی — آفلاین و بدون نیاز به اینترنت.

## زبان

- **فارسی PRIMARY** — همه UI و پاسخ‌ها به فارسی
- انگلیسی fallback (ثانویه)
- RTL اجباری
- تاریخ شمسی (Jalali)
- TTS فارسی (زنجیره fallback: fa-IR → fa → ar → default)

## قابلیت‌های MVP (نسخه فعلی)

### آلارم و یادآوری
- تنظیم آلارم با ساعت ("آلارم بذار برای ۷ صبح")
- یادآوری با تاریخ ("یادآوری کن نان بخرم فردا ساعت ۱۰")
- لیست، حذف، حذف همه، چرت (snooze)
- AlarmManager با `setExactAndAllowWhileIdle`
- بازیابی بعد از reboot (BootReceiver)

### یادداشت
- ایجاد ("یادداشت کن شماره ۶۰۳۷")
- لیست، حذف، جستجو
- ذخیره در Room

### تایمر
- تنظیم ("تایمر ۱۰ دقیقه")
- **⚠️ باگ: CancelTimer عمل نمی‌کند** — Handler ذخیره نشده
- **⚠️ باگ: تایمر با kill اپ از بین می‌رود** — پایدار نیست

### کنترل گوشی
- WiFi: روشن/خاموش (WifiManager + Settings.Panel fallback)
- Bluetooth: فقط پیام راهنما
- Flashlight: toggle (CameraManager, 500ms)
- Brightness, Airplane, Battery: فقط پیام

### خواندن پیام‌ها
- خواندن ۱۰ اعلان آخر
- خواندن آخرین پیام
- NotificationListenerService (نیاز به فعال‌سازی دستی)

### عمومی
- ساعت، تاریخ، راهنما، تنظیمات، پاک کردن تاریخچه
- کنترل خواندن (Stop, Faster, Slower)

## محدودیت‌های فعلی (Known Issues)

1. تایمر با kill اپ از بین می‌رود
2. `CancelTimer` عمل نمی‌کند
3. `cancelAll()` فقط IDهای ۱-۱۰۰ را کنسل می‌کند
4. Bluetooth, Brightness, Airplane فقط متن برمی‌گردانند
5. `fallbackToDestructiveMigration()` داده‌ها را در migration نابود می‌کند
6. `BootReceiver` و `NotificationListenerService` از Hilt استفاده نمی‌کنند
7. تست وجود ندارد
8. چراغ قوه فقط ۵۰۰ms روشن می‌ماند

## امنیت و ایمنی

### Safety Guardrails (برنامه‌ریزی شده، پیاده‌سازی نشده)
- پاسخ ندادن به سوالات پزشکی/تشخیصی
- پاسخ ندادن به سوالات خودآزاری
- پاسخ ندادن به توصیه‌های مالی
- پاسخ ندادن به فعالیت‌های غیرقانونی

### حذف امن داده‌ها (برنامه‌ریزی شده، پیاده‌سازی نشده)
- ۴ سطح تأیید حذف (تکی → گروهی → مخرب → ریست کامل)
- سطل زباله ۷ روزه
- حذف فایل فقط با تأیید کاربر

### احراز هویت صوتی (برنامه‌ریزی شده، پیاده‌سازی نشده)
- ثبت voiceprint
- تأیید گوینده
- چند کاربر با سطح دسترسی مختلف

## نیازمندی‌های آینده (Roadmap)

### کوتاه‌مدت (MVP فعلی)
- رفع باگ CancelTimer
- رفع باگ cancelAll
- رفع fallbackToDestructiveMigration
- تست واحد برای CommandParser

### میان‌مدت
- Safety guardrails
- احراز هویت صوتی
- حذف امن داده‌ها
- بهبود UI/UX

### بلندمدت
- Embedding-based Intent Classification
- Local AI (آفلاین)
- Context-aware conversations
- Multi-turn dialogues
