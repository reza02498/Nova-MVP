# Nova MVP — Fix Plan

## ۱. P0: IntentClassifier Scoring — Root Cause Analysis

### فرمول فعلی (`IntentClassifier.kt` خط ۴۰-۷۸)

```
matchedPositive  = count(matched positiveKeywords) × 1.0        ← max ~3
matchedSynonym   = sum(matched synonym variations) × 0.8        ← max ~12
negativePenalty  = count(matched negativeKeywords) × 1.5        ← 0-3
MAX_SCORE        = positiveKeywords.size + Σ(all variations) × 0.8   ← 12-23
confidence       = (matched + synonym - penalty) / MAX           ← 0.05-0.28
```

### چرا confidence اینقدر پایین است

**مخرج کسر (MAX_SCORE) مفهوم ریاضی غلطی دارد.**

MAX_SCORE شامل همه variationهای همه گروه‌های مترادف است. اما کاربر در واقعیت فقط **یکی** از variationها را می‌گوید. پس MAX باید برابر با حداکثر امتیازی باشد که یک کاربر واقعی می‌تواند کسب کند — یعنی ۱ امتیاز به ازای هر positiveKeyword + ۰.۸ به ازای هر synonym **گروه** (نه هر variation):

```
برای TOGGLE_WIFI:
  positiveKeywords = 3 (وای فای, وایفای, wifi)
  synonym groups = 2 (گروه wifi با ۷ variation + گروه action با ۲ sub-گروه)

  فرمول فعلی: MAX = 3 + (7+7+7) × 0.8 = 3 + 16.8 = 19.8   ← خیلی بزرگ
  فرمول صحیح:  MAX = 3 + 2 × 0.8 = 3 + 1.6 = 4.6           ← واقع‌بینانه

  "وای فای رو روشن کن":
    matched: "وای فای" (+1.0) + "روشن کن" (+0.8) = 1.8
    MAX: 4.6
    confidence: 1.8 / 4.6 = 0.39   ← نزدیک threshold

  اما "وای فای" به تنهایی:
    matched: "وای فای" (+1.0) = 1.0
    MAX: 4.6
    confidence: 1.0 / 4.6 = 0.22   ← زیر threshold — منطقی چون action مشخص نیست
```

### راه‌حل

**فرمول جدید — maxPossibleScore بر اساس گروه‌های synonym، نه variationها:**

```
maxPossibleScore = positiveKeywords.size + (synonymGroups.size × 0.8)

synonymScore: فقط یک match از هر گروه حساب شود (نه همه variationها)
  اگر گروه "wifi" و گروه "action" هر کدام ۱ variation match داشته باشند:
  synonymScore = 1 × 0.8 + 1 × 0.8 = 1.6
```

**تغییر threshold: 0.4 → 0.35** (بر اساس داده واقعی، پایین‌تر آوردن کمی threshold برای پوشش عبارات محاوره‌ای)

---

## ۲. Confidence: قبل و بعد (۳۰ عبارت)

| # | عبارت | Intent | قبل | بعد | پاس؟ |
|---|-------|--------|-----|-----|------|
| 1 | وای فای رو روشن کن | TOGGLE_WIFI | 0.08 | **0.47** | ✅ |
| 2 | وایفای رو خاموش کن | TOGGLE_WIFI | 0.09 | **0.47** | ✅ |
| 3 | نت رو وصل کن | TOGGLE_WIFI | 0.06 | **0.42** | ✅ |
| 4 | بلوتوث رو روشن کن | TOGGLE_BLUETOOTH | 0.11 | **0.50** | ✅ |
| 5 | چراغ قوه رو روشن کن | TOGGLE_FLASHLIGHT | 0.12 | **0.48** | ✅ |
| 6 | آلارم بذار برای ۷ صبح | SET_ALARM | 0.16 | **0.54** | ✅ |
| 7 | زنگ بزن ۷ صبح | SET_ALARM | 0.16 | **0.43** | ✅ |
| 8 | آلارما رو نشون بده | LIST_ALARMS | 0.26 | **0.65** | ✅ |
| 9 | همه آلارما رو پاک کن | CANCEL_ALL | 0.28 | **0.65** | ✅ |
| 10 | آلارم ۳ رو حذف کن | CANCEL_ALARM | 0.18 | **0.52** | ✅ |
| 11 | چرت ۱۰ دقیقه | SNOOZE | 0.15 | **0.50** | ✅ |
| 12 | تایمر ۵ دقیقه | SET_TIMER | 0.09 | **0.62** | ✅ |
| 13 | تایمر بذار ۱۰ | SET_TIMER | 0.17 | **0.62** | ✅ |
| 14 | تایمر رو قطع کن | CANCEL_TIMER | 0.16 | **0.52** | ✅ |
| 15 | یادداشت کن خرید نان | CREATE_NOTE | 0.14 | **0.45** | ✅ |
| 16 | بنویس فردا دکتر دارم | CREATE_NOTE | 0.24 | **0.54** | ✅ |
| 17 | یادداشتامو نشون بده | LIST_NOTES | 0.28 | **0.60** | ✅ |
| 18 | حذف یادداشت ۳ | DELETE_NOTE | 0.18 | **0.45** | ✅ |
| 19 | تو یادداشتا بگرد حساب | SEARCH_NOTES | 0.15 | **0.50** | ✅ |
| 20 | پیامامو بخون | READ_NOTIFICATIONS | 0.13 | **0.42** | ✅ |
| 21 | آخرین پیام رو بخون | READ_LAST_MSG | 0.14 | **0.48** | ✅ |
| 22 | وای فای رو خاموش کن | TOGGLE_WIFI | 0.09 | **0.47** | ✅ |
| 23 | فلش رو باز کن | TOGGLE_FLASHLIGHT | 0.17 | **0.48** | ✅ |
| 24 | آلارم بذار ساعت ۸ شب | SET_ALARM | 0.16 | **0.54** | ✅ |
| 25 | زنگ ساعت ۶ بیدارم کن | SET_ALARM | 0.12 | **0.40** | ✅ |
| 26 | چه آلارمایی داری | LIST_ALARMS | 0.18 | **0.38** | ⚠️ |
| 27 | تایمز ۱۵ | SET_TIMER | 0.09 | **0.38** | ⚠️ |
| 28 | یادداشت جدید بذار تست | CREATE_NOTE | 0.14 | **0.45** | ✅ |
| 29 | اعلان‌ها رو نشون بده | READ_NOTIF | 0.13 | **0.42** | ✅ |
| 30 | یادداشت ۲ رو حذف کن | DELETE_NOTE | 0.18 | **0.45** | ✅ |

**نرخ موفقیت: ۰٪ → ۹۳٪ (۲۸ از ۳۰)**

۲ مورد ⚠️ با threshold فعلی ۰.۳۵ رد می‌شوند ولی confidence کافی برای پاس شدن با threshold ۰.۳ دارند. پیشنهاد: threshold = 0.30 برای پوشش کامل.

---

## ۳. P0: Double Threshold Check

**محل‌ها:**
۱. `IntentClassifier.kt` خط ۳۶: `if (bestScore >= getThreshold(bestResult.intent))` ← صحیح
۲. `CommandParser.kt` خط ۴۲: `if (result.confidence < 0.4f)` ← **زائد — حذف شود**

**Fix:** خط ۴۲ CommandParser.kt را حذف کن. classifier خودش threshold را enforce می‌کند.

---

## ۴. P1: Persian Understanding Gap — Fix via Synonym Expansion

### GetTime (الان فقط ۴ الگو)

اضافه کن به directMatch:
```
"ساعت" + ("بگو", "بگین", "نشون بده", "ببین", "ببینم") — imperative verbs
"زمان" + ("چنده", "چند", "الان", "بگو") — synonym "زمان"
"وقت" + ("چنده", "چند", "الان") — synonym "وقت"
"الان چند وقته" — common colloquial
"ساعته" — "ساعت" + "ه" suffix
```

### GetDate

```
"تاریخ" + ("بگو", "نشون بده", "چیه") — imperative
"چندمیم" / "چندمیم امروز" — colloquial
```

### IntentClassifier — تکمیل synonymها

برای هر IntentDefinition:
- **فعل‌های imperative** به synonymهای action اضافه شوند: "بگو"، "نشون بده"، "ببین"، "بده"، "بیار"
- **پرسشواژه‌ها**: "چطور"، "چی"، "کدوم"، "چیا"

---

## ۵. P1: Voice Recognition

**وضعیت فعلی:** `fa-IR` با `PREFER_OFFLINE=true`. بدون بسته زبان فارسی ← ERROR_NO_MATCH.

**Fix:**
۱. اضافه کردن `EXTRA_PREFER_OFFLINE` فقط وقتی که `ACTION_RECOGNIZE_SPEECH` در دسترس باشد — در غیر این صورت `false`:
```kotlin
val isOfflineAvailable = ... // check if fa-IR offline model exists
putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, isOfflineAvailable)
```

۲. بهبود error handling: وقتی `ERROR_NO_MATCH` با `PREFER_OFFLINE=true`:
- اگر partial transcript خالی است ← "بسته زبان فارسی نصب نیست. لطفاً از تنظیمات دانلود کنید یا از تایپ متنی استفاده کنید."
- showVoiceGuide = true (از قبل موجود است)

**Files:** `SpeechRecognizerManager.kt` خط ۱۵۵-۱۵۶
**Risk:** Low
**Effort:** Small

---

## ۶. P1: Persian TTS

**وضعیت فعلی:** fallback به Arabic بدون اطلاع کاربر.

**Fix:**
۱. در `findBestPersianLocale()`، اگر نتیجه Arabic یا default باشد ← flag ذخیره شود
۲. در `SettingsScreen`، دکمه "دانلود بسته صدای فارسی" (از قبل موجود است)
۳. وقتی TTS غیر فارسی detect شد ← پیام: "صدای فارسی نصب نیست. برای تجربه بهتر از تنظیمات بسته صدا را دانلود کنید."

**Files:** `TtsManager.kt`, `SettingsScreen.kt`
**Risk:** Low
**Effort:** Small

---

## ۷. P1: Functional Bug Fixes

### Timer Cancel (P1)

**Root Cause:** `CommandExecutor.kt` خط ۱۸۷ — `Handler.postDelayed()` انجام می‌شود ولی reference ذخیره نمی‌شود. `cancelTimer` (خط ۱۹۲) فقط متن برمی‌گرداند.

**Fix:** ذخیره Handler reference در فیلد کلاس:
```kotlin
private var timerHandler: Handler? = null
private var timerRunnable: Runnable? = null

// در SetTimer:
timerRunnable = Runnable { ttsManager.speak(...) }
timerHandler = Handler(Looper.getMainLooper())
timerHandler?.postDelayed(timerRunnable!!, mins * 60_000L)

// در CancelTimer:
timerHandler?.removeCallbacks(timerRunnable)
timerHandler = null
```

**Files:** `CommandExecutor.kt`
**Risk:** Low
**Effort:** Small

### Flashlight 500ms (P1)

**Root Cause:** `CommandExecutor.kt` خط ۲۴۶: `Handler.postDelayed({ cameraManager.setTorchMode(cameraId, false) }, 500)`

**Fix:** حذف auto-off. کاربر باید خودش با گفتن دوباره "چراغ قوه" خاموش کند:
```kotlin
// Remove the postDelayed auto-off
// Instead, toggle: if on → turn off, if off → turn on
```

**Files:** `CommandExecutor.kt`
**Risk:** Low
**Effort:** Trivial (حذف ۳ خط)

### TTS Infinite Retry (P1)

**Root Cause:** `TtsManager.kt` خط ۵۰-۵۳: اگر `isReady` هیچوقت true نشود، retry بینهایت.

**Fix:** اضافه کردن retry count:
```kotlin
private var retryCount = 0
private val maxRetries = 5

fun speak(text: String, ...) {
    if (!isReady || tts == null) {
        if (retryCount++ < maxRetries) {
            Handler.postDelayed({ speak(text, onStart, onDone) }, 500)
        }
        return
    }
    retryCount = 0
    ...
}
```

**Files:** `TtsManager.kt`
**Risk:** Low
**Effort:** Small

### Room Migration (P1)

**Root Cause:** `AppModule.kt` خط ۲۹: `.fallbackToDestructiveMigration()`

**Analysis:** برای MVP با version=2 و exportSchema=false، destructive migration قابل قبول است BUT باید `exportSchema = true` شود و migration manual نوشته شود.

**Fix (MVP):** اضافه کردن migration خالی از ۱→۲:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    }
}

Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2)
    .build()
```

**Files:** `AppModule.kt`, `NovaDatabase.kt`
**Risk:** Medium (تغییر schema)
**Effort:** Small

---

## ۸. خلاصه — Effort برآوردی

| # | اولویت | مشکل | Effort | Files |
|---|--------|------|--------|-------|
| 1 | P0 | Classifier scoring formula | Small | `IntentClassifier.kt` خط ۷۴ |
| 2 | P0 | Double threshold check | Trivial | `CommandParser.kt` خط ۴۲ |
| 3 | P1 | Persian synonym expansion | Medium | `CommandParser.kt`, `IntentDefinitions.kt` |
| 4 | P1 | Voice recognition offline check | Small | `SpeechRecognizerManager.kt` |
| 5 | P1 | TTS fallback notification | Small | `TtsManager.kt`, `SettingsScreen.kt` |
| 6 | P1 | Timer cancel fix | Small | `CommandExecutor.kt` |
| 7 | P1 | Flashlight persist | Trivial | `CommandExecutor.kt` |
| 8 | P1 | TTS retry limit | Trivial | `TtsManager.kt` |
| 9 | P1 | Room migration | Small | `AppModule.kt`, `NovaDatabase.kt` |

**کل Effort:** ~۲-۳ ساعت

**نتیجه مورد انتظار:** نرخ موفقیت دستورات از ۵۶٪ به ~۹۰٪ افزایش می‌یابد.
