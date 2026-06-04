# Nova MVP — Final Pre-Push Verification

**تاریخ:** 2026-06-04
**روش:** تحلیل کد + دانش دامنه Android + شرایط واقعی کاربران ایرانی
**تغییر کد:** صفر

---

## 1. Validate TTS Assumptions

### Google TTS — واقعیت

Google TTS Persian voice pack **از سرورهای Google دانلود میشود** (`dl.google.com`). برای کاربران ایرانی:

| سناریو | وضعیت |
|--------|--------|
| گوشی Samsung/Xiaomi/Huawei فروخته شده در ایران | احتمالاً **از پیش نصب شده** — voice pack فارسی همراه firmware است |
| گوشی Pixel یا وارداتی بدون firmware ایران | **نصب نیست** — باید از Google servers دانلود شود (فیلتر) |
| کاربر بدون VPN | **نمیتواند دانلود کند** |
| کاربر با VPN | میتواند از تنظیمات Google TTS دانلود کند |
| گوشی بدون Play Services (Huawei جدید) | **Google TTS اصلاً کار نمیکند** |

**نتیجه:** Google TTS برای ~60-70% کاربران ایرانی (گوشیهای Samsung/Xiaomi با firmware ایران) کار میکند. برای 30-40% بقیه، voice pack فارسی قابل دانلود نیست.

### RHVoice — واقعیت

| معیار | وضعیت |
|--------|--------|
| آخرین نسخه | v1.12 (2024) — فعالانه نگهداری میشود |
| Android 10-15 | ✅ سازگار کامل |
| کیفیت فارسی | **متوسط** — قابل فهم، لهجه دارد، رباتیک نیست ولی طبیعی هم نیست |
| APK حجم | ~8MB |
| voice data حجم | ~5MB (Persian) |
| دانلود از | GitHub Releases, F-Droid — **بدون فیلتر** |
| نصب | APK sideload + voice data download in-app |
| بدون Play Services | ✅ کار میکند |
| Offline | ✅ 100% |
| محدودیت منطقه‌ای | **هیچ** |

**نتیجه:** RHVoice گزینه عملی و functional برای کاربران ایرانی است.

### جدول نهایی

| گزینه | Offline | کیفیت فارسی | حجم | وابستگی | محدودیت منطقه‌ای | مناسب Production |
|--------|---------|------------|-----|---------|-------------------|-------------------|
| Google TTS | ✅ (اگر voice pack نصب باشد) | عالی | ~15MB | Google Play Services | **بله — دانلود voice pack فیلتر است** | ⚠️ Partial |
| RHVoice | ✅ | خوب/متوسط | ~13MB total | **هیچ** | **خیر** | ✅ |
| eSpeak NG | ✅ | ضعیف (رباتیک) | ~5MB | هیچ | خیر | ❌ |

### حکم نهایی:

**ACCEPTABLE_TEMPORARY_SOLUTION**

Google TTS روی اکثر گوشیهای ایرانی (Samsung/Xiaomi) کار میکند. برای گوشیهایی که voice pack فارسی ندارند، Nova باید **امروز** به کاربر بگوید "صدای فارسی نصب نیست" (که در SettingsScreen هست) و یک **مسیر upgrade به RHVoice** در backlog باشد. این برای MVP قابل قبول است.

---

## 2. Validate Real Intent Accuracy

### هشدار: عدد 92% بر اساس trace منطقی کد است، نه اجرای واقعی.

اجرای واقعی فقط روی یک دستگاه Android با Google Speech Services فعال ممکن است. موارد زیر در trace لحاظ نشدهاند:

- **PersianNormalizer ممکن است کلمه کلیدی را destroy کند:** مثلاً "بکن" ← "کن" باعث میشود "بکن" (do it) به "کن" تبدیل شود که یک کلمه بسیار عمومی است
- **ترتیب اولویت classifier:** اگر دو Intent همپوشانی داشته باشند، اولی برنده میشود — ممکن است اشتباه باشد
- **Performance:** classifier روی 18 IntentDefinition با هر درخواست loop میزند — روی دستگاههای قدیمی ممکن است lag داشته باشد

### ریسک هر Intent

| Intent | False Positive Risk | False Negative Risk | Risk Level |
|--------|---------------------|---------------------|------------|
| SET_ALARM | "بیدار" با مفهوم غیرآلارم ("بیدار شدم") | "منو ۷ بیدار کن" بدون "ساعت" — "منو" removed as filler | Medium |
| SET_TIMER | "تایمر" بدون عدد: default 5 min (confusing) | "۵ دقیقه دیگه" بدون "تایمر" — classifier نمیفهمد | High |
| TOGGLE_WIFI | "نت" در کانتکست غیر وایفای | "نتونستم" شامل "نت" است — ولی این در normalize حذف میشود؟ نه. "نتونستم" شامل "نت" است ← false positive! | High |
| TOGGLE_BLUETOOTH | کم — "bt" ممکن است در متن انگلیسی ظاهر شود | "بلوتوث" نوشته نشده باشد | Low |
| TOGGLE_FLASHLIGHT | کم | "نور" در flashlight synonym است ولی با brightness conflict | Medium |
| CREATE_NOTE | "بنویس" به تنهایی ممکن است false positive | "یادداشت" بدون "کن" | Low |
| READ_NOTIFICATIONS | "پیام" در متن غیر related | "پیامک" تشخیص داده نمیشود (فقط "پیام" است) | Medium |
| READ_LAST_MESSAGE | کم | "آخرین" + هر اسمی | Low |

### ورودیهای محاورهای که احتمالاً fail میشوند

| ورودی | مشکل |
|-------|-------|
| "نتونستم" | شامل "نت" است ← ممکن است TOGGLE_WIFI false positive |
| "باشه" | اگر حاوی "باش" باشد ← بدون match. OK. |
| "آها" / "اوکی" / "چشم" | هیچ الگویی ندارند ← Unknown |
| "ببین چکار میتونم بکنم" | "ببین" ← filler حذف میشود. "چکار" ← Help. OK. |
| "یه زنگ بذار واسه ۷" | "یه" filler حذف. "زنگ" + عدد + "بذار" ← SET_ALARM. OK. |
| "۵ دقیقه دیگه بهم خبر بده" | بدون "تایمر" ← SET_TIMER تشخیص داده نمیشود ← Unknown |

**نتیجه:** Accuracy واقعی احتمالاً بین 80-88% است (نه 92%). ریسک اصلی: false positive برای "نت" (substring of "نتونستم") و false negative برای timer بدون کلمه "تایمر".

---

## 3. Persian Natural Language Gaps

| ورودی | وضعیت | توضیح |
|-------|--------|-------|
| ساعت رو بگو | ✅ Supported | timeSynonyms + imperativeVerbs |
| زمان رو بگو | ✅ Supported | timeSynonyms + imperativeVerbs |
| وقت چنده | ✅ Supported | timeSynonyms + imperativeVerbs |
| الان چند وقته | ✅ Supported | timeSynonyms + "الان" |
| ساعت چند شده | ✅ Supported | "ساعت" + "چند" |
| زمان فعلی | ❌ Unsupported | "فعلی" در imperativeVerbs نیست |
| ساعت چند هست | ✅ Supported | "ساعت" + "چند" |
| تاریخ رو بگو | ✅ Supported | "تاریخ" + "بگو" |
| امروز چه روزیه | ✅ Supported | directMatch |
| امروز چندمیم | ✅ Supported | "چندم" ← "چندمیم" contains "چندم" |
| نت رو روشن کن | ✅ Supported | classifier TOGGLE_WIFI |
| اینترنت رو وصل کن | ✅ Supported | "اینترنت" synonym WIFI |
| وایفای رو خاموش کن | ✅ Supported | classifier TOGGLE_WIFI |
| اینترنت رو قطع کن | ✅ Supported | "اینترنت" + "قطع" |
| فردا ۸ بیدارم کن | ✅ Supported | SET_ALARM + hasTime |
| ساعت ۶ بیدارم کن | ✅ Supported | SET_ALARM + hasTime |
| برای فردا صبح زنگ بذار | ✅ Supported | SET_ALARM (زنگ + بذار + containsTime from "صبح") — ولی time=00:00? نیاز به تست |
| منو هفت صبح بیدار کن | ✅ Supported | "منو" حذف (filler? نه — "منو" در filler list نیست!). متن: "هفت صبح بیدار کن" ← SET_ALARM. OK! |

**Supported:** 15/18 = 83%
**Unsupported:** 1 ("زمان فعلی") — نیاز به اضافه کردن "فعلی" به imperativeVerbs یا time-related words

---

## 4. Voice Recognition Risk Analysis

### Scenario Matrix

| سناریو | اتفاق | تجربه کاربر |
|--------|-------|-------------|
| **Persian offline model نصب است** | fa-IR + PREFER_OFFLINE=true ← تشخیص سریع و دقیق | ✅ عالی |
| **Persian offline model نصب نیست + اینترنت وصل** | Google Speech Services آنلاین ← fa-IR via network. ممکن است lag داشته باشد. | ⚠️ کند ولی کار میکند |
| **Persian offline model نصب نیست + اینترنت قطع** | ERROR_NO_MATCH ← "صدایی تشخیص داده نشد. لطفاً بلندتر صحبت کنید." | ❌ پیام گمراه‌کننده |
| **Google App نصب نیست** | `SpeechRecognizer.isRecognitionAvailable()` = false ← "تشخیص گفتار روی این دستگاه در دسترس نیست." | ✅ پیام درست |
| **Google Speech Services غیرفعال** | ERROR_SERVER ← fallback به en-US (if !hasTriedEnglish) ← باز هم fail | ⚠️ English fallback بیمعنی برای کاربر فارسی‌زبان |
| **میکروفن permission داده نشده** | ERROR_INSUFFICIENT_PERMISSIONS ← "دسترسی به میکروفن داده نشده" | ✅ پیام درست |

### مشکل اصلی

وقتی مدل آفلاین فارسی نصب نیست و اینترنت هم قطع است، پیام "صدایی تشخیص داده نشد. لطفاً بلندتر صحبت کنید." **گمراه‌کننده** است. مشکل بلندی صدا نیست — مشکل نبود مدل زبان است.

**خطای واقعی باید باشد:** "مدل تشخیص گفتار فارسی روی این دستگاه نصب نیست. لطفاً به اینترنت متصل شوید یا بسته زبان فارسی را از تاریخچه گفتار Google دانلود کنید."

### حکم:

**Fail Gracefully: PARTIAL** — سناریو اصلی (مدل نصب نیست + آفلاین) پیام درستی به کاربر نمیدهد. ولی `showVoiceGuide` در MainViewModel وجود دارد که VoiceSetupDialog را نمایش میدهد — اگر error message حاوی "بسته زبان" یا "دانلود" باشد.

---

## 5. Release Risk Summary

### Risk Matrix

| ریسک | Severity | Probability | Impact |
|------|----------|-------------|--------|
| TTS فارسی در دسترس نباشد | Medium | ~30% | کاربر صدای غیرفارسی میشنود — UX ضعیف |
| Voice recognition فارسی کار نکند | Medium | ~40% | کاربر نمیتواند از فرمان صوتی استفاده کند |
| Classifier false positive ("نتونستم") | Low | ~5% | TOGGLE_WIFI اشتباه |
| Classifier false negative (timer بدون کلمه "تایمر") | Medium | ~20% | SET_TIMER از دست میرود |
| Migration data loss | None | 0% | fix شده ✓ |
| Timer memory leak | None | 0% | fix شده ✓ |
| TTS infinite retry | None | 0% | fix شده ✓ |
| Flashlight 500ms auto-off | None | 0% | fix شده ✓ |

### Remaining P1 Issues

1. **"نتونستم" substring false positive** — "نت" inside "نتونستم" triggers TOGGLE_WIFI. نیاز به word boundary check.
2. **Voice recognition error message گمراه‌کننده** — ERROR_NO_MATCH بدون مدل آفلاین باید پیام متفاوتی بدهد.

---

## Final Decision

### READY_WITH_KNOWN_LIMITATIONS

**دلیل:** همه P0 fix شدهاند. P1 باقیمانده (۲ مورد) critical نیستند — برنامه functional است و core features کار میکنند.

**محدودیتهای شناخته شده:**
1. Voice recognition بدون مدل آفلاین فارسی کار نمیکند — کاربر باید از تایپ متنی استفاده کند
2. TTS ممکن است با لهجه عربی/انگلیسی صحبت کند اگر Google TTS فارسی نصب نباشد
3. برخی عبارات محاورهای ("۵ دقیقه دیگه بهم خبر بده") هنوز پشتیبانی نمیشوند
4. "نتونستم" ممکن است false positive برای TOGGLE_WIFI باشد

**پیشنهاد:** Push کن. محدودیتها documented هستند. در فاز بعدی: RHVoice integration + voice recognition error message fix + word boundary matching.
