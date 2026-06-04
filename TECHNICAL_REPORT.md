# Nova MVP — گزارش فنی کامل

## ۱. اطلاعات کلی پروژه

| فیلد | مقدار |
|---|---|
| **نام پروژه** | NovaMVP |
| **Application ID** | `com.nova.assistant` |
| **نوع** | دستیار صوتی فارسی — Android App |
| **هدف** | MVP آزمایشی برای تشخیص فرمان‌های صوتی/متنی فارسی و اجرای آن‌ها (آلارم، یادداشت، تایمر، کنترل گوشی) |
| **زبان** | Kotlin 100% |
| **UI Toolkit** | Jetpack Compose + Material 3 |
| **Minimum SDK** | 26 (Android 8.0) |
| **Target SDK** | 35 (Android 15) |
| **Gradle** | Kotlin DSL |
| **نسخه** | 1.0-mvp |

### وابستگی‌های اصلی

| کتابخانه | نسخه | کاربرد |
|---|---|---|
| Compose BOM | 2024.10.00 | UI toolkit |
| Room | 2.6.1 | دیتابیس محلی |
| Hilt | 2.52 | Dependency Injection |
| Navigation Compose | 2.8.4 | مسیریابی بین صفحات |
| DataStore | 1.1.1 | ذخیره تنظیمات |
| Lifecycle | 2.8.6 | ViewModel + Compose lifecycle |
| Coroutines | 1.9.0 | برنامه‌نویسی async |

### مجوزها (AndroidManifest)

`RECORD_AUDIO`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`

---

## ۲. ساختار ماژول‌ها

### ۲.۱ پکیج `com.nova.assistant` (root)

| فایل | کلاس | مسئولیت |
|---|---|---|
| `NovaApp.kt` | `NovaApp : Application` | نقطه ورود اپ — `@HiltAndroidApp` |
| `MainActivity.kt` | `MainActivity : ComponentActivity` | Single Activity — میزبان NavHost برای main و settings |

### ۲.۲ پکیج `data`

| فایل | محتوا | مسئولیت |
|---|---|---|
| `Entities.kt` | `NoteEntity`, `AlarmEntity`, `ConversationEntity`, `NotificationEntity` | ۴ جدول Room |
| `Daos.kt` | `NoteDao`, `AlarmDao`, `ConversationDao`, `NotificationDao` | دسترسی به دیتابیس |
| `NovaDatabase.kt` | `NovaDatabase : RoomDatabase` | تعریف دیتابیس — version=2, fallbackToDestructiveMigration |
| `Preferences.kt` | `AppPreferences`, `PreferencesManager` | DataStore — نرخ گفتار، زبان، first-launch |

### ۲.۳ پکیج `di`

| فایل | محتوا | مسئولیت |
|---|---|---|
| `AppModule.kt` | `AppModule` (Hilt) | تامین Database, DAOها, TtsManager |

### ۲.۴ پکیج `domain`

| فایل | محتوا | مسئولیت |
|---|---|---|
| `Command.kt` | `Command` sealed class | ۲۴ نوع فرمان + Unknown |
| `CommandParser.kt` | `CommandParser` | پردازش متن به Command — **Rule-based خالص، بدون AI** |
| `CommandExecutor.kt` | `CommandExecutor` | اجرای Command — دسترسی به DB, AlarmManager, TTS |

### ۲.۵ پکیج `service`

| فایل | محتوا | مسئولیت |
|---|---|---|
| `AlarmScheduler.kt` | `AlarmScheduler` | زمان‌بندی آلارم با `AlarmManager.setExactAndAllowWhileIdle` |
| `AlarmReceiver.kt` | `AlarmReceiver : BroadcastReceiver` | دریافت broadcast آلارم — شروع AlarmScreen |
| `BootReceiver.kt` | `BootReceiver : BroadcastReceiver` | بازیابی آلارم‌ها بعد از ریبوت — **Hilt را دور می‌زند** |
| `NotificationListenerService.kt` | `NotificationListenerService` | شنود اعلان‌های SMS/واتساپ/تلگرام — **Hilt را دور می‌زند** |

### ۲.۶ پکیج `ui`

| فایل | محتوا | مسئولیت |
|---|---|---|
| `MainScreen.kt` | ۱۰ composable function | رابط کاربری اصلی — چت، هدر، input bar |
| `MainViewModel.kt` | `MainViewModel`, `ChatMessage`, `MainUiState` | مدیریت state چت و voice/text pipeline |
| `AlarmScreen.kt` | `AlarmScreen : ComponentActivity` | صفحه تمام‌صفحه آلارم با snooze/dismiss |
| `SettingsScreen.kt` | `SettingsViewModel`, `SettingsScreen` | تنظیمات نرخ گفتار و زبان |

### ۲.۷ پکیج `ui.theme`

| فایل | محتوا | مسئولیت |
|---|---|---|
| `Theme.kt` | `NovaTheme`, پالت رنگی | تم دارک، RTL اجباری، تایپوگرافی |

### ۲.۸ پکیج `util`

| فایل | محتوا | مسئولیت |
|---|---|---|
| `TtsManager.kt` | `TtsManager` | خروجی صوتی فارسی — fallback زنجیره‌ای fa-IR→fa→ar→default |
| `SpeechRecognizerManager.kt` | `SpeechRecognizerManager` | تشخیص گفتار — فارسی اول، انگلیسی fallback، ۳ ثانیه timeout سکوت |
| `PhoneLanguage.kt` | `PhoneLanguage` | تشخیص زبان گوشی برای راهنمایی دو زبانه |

---

## ۳. معماری نرم‌افزار

### Hybrid MVVM + Clean Architecture (تلاش شده، ناقص)

```
┌──────────────────────────────────────────────────────┐
│                    UI LAYER                          │
│  MainScreen (Compose)    AlarmScreen (Activity)      │
│  SettingsScreen (Compose)                            │
│  NovaTheme (Material 3, RTL)                         │
├──────────────────────────────────────────────────────┤
│                 VIEWMODEL LAYER                      │
│  MainViewModel (processCommand, voice/text pipeline) │
│  SettingsViewModel (preferences)                     │
├──────────────────────────────────────────────────────┤
│                  DOMAIN LAYER                        │
│  Command (sealed class — 24 variants)                │
│  CommandParser (rule-based NLP, pure Kotlin)  ✓      │
│  CommandExecutor (⚠️ imports Context, Android APIs)  │
├──────────────────────────────────────────────────────┤
│                   DATA LAYER                         │
│  NovaDatabase (Room — 4 tables)                      │
│  Daos (NoteDao, AlarmDao, ConversationDao, NotifDao) │
│  PreferencesManager (DataStore)                      │
├──────────────────────────────────────────────────────┤
│                 ANDROID SERVICES                     │
│  AlarmScheduler (AlarmManager)                       │
│  TtsManager (TextToSpeech)                           │
│  SpeechRecognizerManager (SpeechRecognizer)          │
│  NotificationListenerService, BootReceiver, etc.     │
└──────────────────────────────────────────────────────┘
```

### Data Flow

```
کاربر (صدا/متن)
    │
    ▼
MainViewModel.processCommand()
    │
    ├─ checkGreeting() ← ۵ الگوی احوالپرسی
    │
    ├─ commandParser.parse(text)
    │     ├─ normalize() ← نرمال‌سازی ۶ مرحله‌ای
    │     └─ parseCommand() ← درخت تصمیم ۱۲ مرحله‌ای
    │
    ├─ commandExecutor.execute(command)
    │     ├─ when(command) ← ۲۴ شاخه
    │     ├─ DB read/write
    │     └─ AlarmManager / WifiManager / ...
    │
    ├─ conversationDao.insert() ← ذخیره تاریخچه
    │
    └─ ttsManager.speak(response) ← خروجی صوتی
```

### State Management

- `MainUiState` data class — single source of truth
- `MutableStateFlow<MainUiState>` — reactive updates
- `_state.update { it.copy(...) }` — immutable state transitions
- `collectAsStateWithLifecycle()` — Compose collects state

---

## ۴. سیستم Voice Command

### Pipeline کامل

```
┌─────────────────────────────────────────────────────────────────┐
│ ۱. کاربر دکمه میکروفن را می‌زند                                   │
│    MainScreen.onMicClick → viewModel.startVoiceInput(context)    │
├─────────────────────────────────────────────────────────────────┤
│ ۲. SpeechRecognizerManager ایجاد می‌شود (دستی، بدون DI)          │
│    • startListening() با fa-IR و EXTRA_PREFER_OFFLINE=true      │
│    • ۳ ثانیه سکوت = اتمام خودکار                                  │
│    • onPartialResult → نمایش زنده متن                             │
│    • onError → فارسی fail, انگلیسی retry                         │
├─────────────────────────────────────────────────────────────────┤
│ ۳. متن خام به processCommand(text, "VOICE") می‌رود               │
├─────────────────────────────────────────────────────────────────┤
│ ۴. checkGreeting() چک می‌شود — اگه سلام/خداحافظ بود،             │
│    مستقیم پاسخ داده می‌شود (بدون پارسر)                           │
├─────────────────────────────────────────────────────────────────┤
│ ۵. commandParser.parse(text)                                     │
│    ├─ normalize():                                               │
│    │  ۱. کاراکتر عربی → فارسی (ي→ی, ك→ک, ...)                    │
│    │  ۲. ارقام فارسی/عربی → ASCII (۰→0, ۱→1, ...)               │
│    │  ۳. اصلاح تایپو (الارم→آلارم, یاداشت→یادداشت, ...)         │
│    │  ۴. حذف کلمات پرکننده (خب, ببین, لطفاً, میشه, یه, ...)     │
│    │  ۵. نرمال‌سازی فعل (بذارم→بذار, بکن→کن, ...)               │
│    │  ۶. نرمال‌سازی املایی (نصف→نیم, ربع→۱۵)                    │
│    │  ۷. نرمال‌سازی فاصله‌ها                                     │
│    └─ parseCommand():                                            │
│        اولویت بررسی:                                             │
│        ۱. کنترل گوشی (WiFi, BT, Flashlight, ...)                │
│        ۲. تایمر (Set/Cancel)                                     │
│        ۳. آلارم (Set/Reminder)                                   │
│        ۴. لیست/حذف/چرت آلارم                                     │
│        ۵. یادداشت (Create/List/Search/Delete)                    │
│        ۶. اعلان‌ها (Read)                                        │
│        ۷. کنترل خواندن (Stop/Faster/Slower)                     │
│        ۸. ساعت/تاریخ                                             │
│        ۹. راهنما/تنظیمات/پاک کردن                                │
│       ۱۰. حذف عمومی با شماره                                     │
│       ۱۱. Unknown                                                │
├─────────────────────────────────────────────────────────────────┤
│ ۶. commandExecutor.execute(command, inputType)                   │
│    • اجرای when روی ۲۴ نوع Command                              │
│    • ذخیره در ConversationDao                                    │
│    • برگرداندن متن پاسخ فارسی                                    │
├─────────────────────────────────────────────────────────────────┤
│ ۷. ttsManager.speak(response)                                    │
│    • تنظیم locale فارسی (fa-IR → fa → ar → default)             │
│    • پخش صدا                                                     │
│    • onDone → به‌روزرسانی state.isSpeaking                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## ۵. سیستم Command

### ۲۴ فرمان (sealed class)

#### آلارم‌ها (۶):

| # | Command | فیلدها | عملکرد |
|---|---|---|---|
| 1 | `SetAlarm` | `time: LocalTime, label: String?` | درج AlarmEntity + زمان‌بندی AlarmManager |
| 2 | `SetReminder` | `task: String, dateTime: LocalDateTime` | مشابه آلارم با description |
| 3 | `ListAlarms` | — | خواندن آلارم‌های فعال از DB |
| 4 | `CancelAlarm` | `alarmId: Long` | حذف + cancel AlarmManager |
| 5 | `CancelAllAlarms` | — | حذف همه + cancel ۱-۱۰۰ |
| 6 | `Snooze` | `minutes: Int` | زمان‌بندی snooze |

#### اعلان‌ها (۲):

| 7 | `ReadNotifications` | — | ۱۰ اعلان آخر |
| 8 | `ReadLastMessage` | — | آخرین اعلان |

#### کنترل خواندن (۳):

| 9 | `StopReading` | — | ttsManager.stop() |
| 10 | `ReadFaster` | — | +۰.۱ نرخ |
| 11 | `ReadSlower` | — | -۰.۱ نرخ |

#### عمومی (۵):

| 12 | `GetTime` | — | ساعت فعلی به فارسی |
| 13 | `GetDate` | — | تاریخ شمسی |
| 14 | `Help` | — | لیست قابلیت‌ها |
| 15 | `OpenSettings` | — | navigation |
| 16 | `ClearHistory` | — | حذف کل conversations |

#### یادداشت (۴):

| 17 | `CreateNote` | `content: String` | درج NoteEntity |
| 18 | `ListNotes` | — | همه یادداشت‌ها |
| 19 | `DeleteNote` | `id: Long` | حذف با ID |
| 20 | `SearchNotes` | `query: String` | LIKE search |

#### تایمر (۲):

| 21 | `SetTimer` | `minutes: Int` | Handler.postDelayed — **پایدار نیست** |
| 22 | `CancelTimer` | — | **عمل نمی‌کند!** — فقط متن برمی‌گرداند |

#### کنترل گوشی (۱):

| 23 | `DeviceToggle` | `setting: String` | ۱۰+ sub-command با پارامتر رشته‌ای |

#### سایر (۱):

| 24 | `Unknown` | — | "متوجه نشدم" |

### نحوه Match شدن

- **Rule-based** — بدون هیچ AI
- `containsAny(text, "کلمه۱", "کلمه۲", ...)` — تطبیق کلیدواژه
- `text.contains(Regex("pattern"))` — تطبیق regex
- `extractNumber(text)` — استخراج عدد با تبدیل کلمات (یک→۱) + regex
- `extractTime(text)` — استخراج زمان با regex + تنظیم AM/PM فارسی

### نحوه اضافه کردن Command جدید

1. اضافه کردن variant به `Command` sealed class
2. اضافه کردن الگوی تشخیص در `CommandParser.parseCommand()`
3. اضافه کردن شاخه اجرا در `CommandExecutor.executeInternal()`
4. اضافه کردن متن نمایش در `commandDisplayText()`

---

## ۶. NLP و پردازش زبان

### وضعیت: **هیچ AI یا ML وجود ندارد**

- **Rule-based**: ۱۰۰٪ تشخیص‌ها بر اساس `text.contains()` و `Regex`
- **Regex-based**: استخراج زمان، عدد، تاریخ
- **Semantic**: وجود ندارد — درکی از معنی جمله نیست
- **AI/ML**: وجود ندارد — بدون LLM، بدون ML Kit، بدون Embedding، بدون Intent Classification

### تکنیک‌های پردازش زبان موجود:

۱. **نرمال‌سازی کاراکتر**: ۱۴ نگاشت عربی→فارسی
۲. **نرمال‌سازی اعداد**: ارقام فارسی/عربی → ASCII
۳. **اصلاح تایپو**: ۲۰+ جایگزینی (الارم→آلارم، یاداشت→یادداشت، وایفای→وای فای)
۴. **حذف کلمات پرکننده**: ۳۰+ الگو (خب، ببین، لطفاً، میشه، میخوام، یه، راستی، ...)
۵. **نرمال‌سازی فعل**: ۴۰+ نگاشت محاوره→رسمی (بذارم→بذار، بکن→کن)
۶. **نگاشت عددی**: یک→۱, دو→۲, ... ده→۱۰, نیم→۳۰
۷. **تشخیص زمان**: regex با تنظیم صبح/ظهر/عصر/شب
۸. **تشخیص تاریخ نسبی**: امروز/فردا/پس‌فردا/روزهای هفته

---

## ۷. Persistence Layer

| تکنولوژی | کاربرد | جداول/کلیدها |
|---|---|---|
| **Room** | ذخیره اصلی | `notes`, `alarms`, `conversations`, `notifications` |
| **DataStore** | تنظیمات کاربر | `speech_rate`, `voice_language`, `is_first_launch` |
| **SQLite** | Room از SQLite استفاده می‌کند | — |
| **SharedPreferences** | استفاده نشده | — |
| **فایل محلی** | استفاده نشده | — |

---

## ۸. Dependency Injection

| روش | کجا استفاده شده |
|---|---|
| **Hilt (`@Inject constructor`)** | `CommandParser`, `CommandExecutor`, `AlarmScheduler`, `PreferencesManager` |
| **Hilt (`@HiltViewModel`)** | `MainViewModel`, `SettingsViewModel` |
| **Hilt (`@Provides @Singleton`)** | `NovaDatabase`, DAOها, `TtsManager` |
| **Hilt (`@AndroidEntryPoint`)** | `MainActivity` |
| **Hilt (`@HiltAndroidApp`)** | `NovaApp` |
| **دستی (Bypass Hilt)** | `BootReceiver` — خودش Room می‌سازد! `NotificationListenerService` — خودش Room می‌سازد! |
| **دستی (new)** | `SpeechRecognizerManager` — در ViewModel با `new` ساخته می‌شود |

---

## ۹. تست‌ها

**تعداد تست: ۰ (صفر)**

- فایل تست وجود ندارد
- وابستگی تست (JUnit, Mockito, ...) در build.gradle تعریف نشده

---

## ۱۰. نقاط ضعف فنی (۲۰ مورد)

### بحرانی:

1. **`CancelTimer` کار نمی‌کند** — Handler ذخیره نشده، قابل کنسل نیست
2. **`cancelAll()` فقط IDهای ۱-۱۰۰ را کنسل می‌کند** — ID > 100 کنسل نمی‌شود
3. **Hilt در `BootReceiver` و `NotificationListenerService` دور زده شده** — Room مستقیماً ساخته می‌شود
4. **تایمر پایدار نیست** — Handler با kill اپ از بین می‌رود
5. **لرزش دوبل** — AlarmReceiver و AlarmScreen هر دو ویبره می‌کنند

### مهم:

6. **Bluetooth/Brightness/Airplane فقط متن برمی‌گردانند** — عمل نمی‌کنند
7. **`fallbackToDestructiveMigration()`** — آپدیت دیتابیس = حذف همه داده‌ها
8. **`NoteDao.getAll()` از Flow استفاده نمی‌کند** — reactive نیست
9. **`isFirstLaunch` ذخیره می‌شود ولی هرگز خوانده نمی‌شود** — کد مرده
10. **`CommandExecutor` در domain ولی Context import کرده** — نقض Clean Architecture
11. **`SpeechRecognizerManager` بدون DI** — در ViewModel با new ساخته می‌شود

### کم‌اهمیت:

12. **عدم استفاده از `AlarmScreen` در navigation** — `onNavigateToAlarm` نادیده گرفته شده
13. **`AlarmScreen` بدون `@AndroidEntryPoint`** — Hilt injection ممکن نیست
14. **چراغ قوه فقط ۵۰۰ms روشن می‌ماند** — به عنوان indicator، کاربر را گیج می‌کند
15. **`scheduleSnooze` از `Int.MAX_VALUE` به عنوان requestCode استفاده می‌کند**
16. **تایمر Handler قابل track نیست** — کاربر نمی‌تواند بپرسد "چقدر از تایمر مونده"
17. **`NovaApp.instance` یک singleton public است** — ضدالگوی Hilt
18. **بدون retry/recovery** — خطا فقط نمایش داده می‌شود
19. **`exportSchema = false`** — تاریخچه migration ثبت نمی‌شود
20. **درخت تصمیم `parseCommand` ترتیب حساس است** — "تایمر ۵" قبل از "آلارم ۵" بررسی می‌شود

---

## ۱۱. آمادگی برای AI

| نیازمندی | وضعیت فعلی | فاصله |
|---|---|---|
| **Pluggable NLP** | یک `CommandParser` hardcoded | نیاز به interface برای تعویض rule/AI |
| **پردازش async** | `suspend execute()` ✓ | آماده |
| **تاریخچه مکالمه** | `ConversationDao` با Flow ✓ | آماده — ولی flat ذخیره می‌شود |
| **Context Memory** | ندارد ✗ | باید session grouping اضافه شود |
| **Prompt template** | ندارد ✗ | باید از صفر ساخته شود |
| **Streaming** | ندارد ✗ | TTS blocking است، streaming نیاز به refactor |
| **Function Calling** | ساختار `Command` sealed class ✓ | ایده‌آل — خروجی structured دارد |
| **Error handling** | یک `catch (Exception)` ✗ | نیاز به retry/fallback/degradation |
| **Token management** | ندارد ✗ | باید از صفر ساخته شود |
| **Plugin architecture** | ندارد ✗ | هر Command hardcoded است |

### بهترین نقطه تزریق AI:

```kotlin
// در MainViewModel.processCommand():
if (aiEnabled && !isSimpleCommand(text)) {
    response = aiService.generate(text, conversationHistory)  // مسیر AI
} else {
    command = commandParser.parse(text)                       // مسیر rule-based
    response = commandExecutor.execute(command, inputType)
}
```

`Command` sealed class می‌تواند به عنوان **Function Calling schema** استفاده شود — مدل AI خروجی structured بدهد به جای متن آزاد.

---

## ۱۲. Roadmap پیشنهادی

### فاز ۰: پایدارسازی (۲-۱ هفته)

- رفع `CancelTimer` — ذخیره Handler reference
- رفع `cancelAll()` — استفاده از IDهای واقعی
- رفع لرزش دوبل AlarmReceiver/AlarmScreen
- اضافه کردن Hilt به BootReceiver و NotificationListenerService
- حذف `fallbackToDestructiveMigration()` — پیاده‌سازی migration واقعی
- تست واحد برای `CommandParser`

### فاز ۱: لایه AI Adapter (۳-۲ هفته)

- Interface سازی `CommandParser` ← `RuleCommandParser` + `AiCommandParser`
- اضافه کردن `AiService` interface
- DI provider برای تعویض runtime
- اضافه کردن HTTP client (Retrofit/Ktor)

### فاز ۲: NLP پایه با AI (۴-۳ هفته)

- یکپارچه‌سازی با API مدل (مثل Gemini)
- استفاده از `Command` به عنوان function-calling schema
- Streaming response در UI
- Context window با `ConversationDao`
- Fallback rule-based در صورت خطا

### فاز ۳: قابلیت‌های Agent (۶-۴ هفته)

- اجرای پویای Command توسط AI
- مکالمه چند مرحله‌ای stateful
- پیشنهاد proactive
- خلاصه‌سازی اعلان‌ها

### فاز ۴: قابلیت‌های پیشرفته (۸-۶ هفته)

- ML روی دستگاه (TensorFlow Lite)
- تشخیص wake word سفارشی
- Fallback آفلاین با مدل distilled
- معماری plugin برای Commandهای third-party

---

## پیوست: فهرست کامل فایل‌ها

| # | مسیر | کلاس |
|---|---|---|
| 1 | `com/nova/assistant/NovaApp.kt` | `NovaApp` |
| 2 | `com/nova/assistant/MainActivity.kt` | `MainActivity` |
| 3 | `com/nova/assistant/data/Entities.kt` | `NoteEntity`, `AlarmEntity`, `ConversationEntity`, `NotificationEntity` |
| 4 | `com/nova/assistant/data/Daos.kt` | `NoteDao`, `AlarmDao`, `ConversationDao`, `NotificationDao` |
| 5 | `com/nova/assistant/data/NovaDatabase.kt` | `NovaDatabase` |
| 6 | `com/nova/assistant/data/Preferences.kt` | `AppPreferences`, `PreferencesManager` |
| 7 | `com/nova/assistant/di/AppModule.kt` | `AppModule` |
| 8 | `com/nova/assistant/domain/Command.kt` | `Command` (sealed) |
| 9 | `com/nova/assistant/domain/CommandParser.kt` | `CommandParser` |
| 10 | `com/nova/assistant/domain/CommandExecutor.kt` | `CommandExecutor` |
| 11 | `com/nova/assistant/service/AlarmScheduler.kt` | `AlarmScheduler` |
| 12 | `com/nova/assistant/service/AlarmReceiver.kt` | `AlarmReceiver` |
| 13 | `com/nova/assistant/service/BootReceiver.kt` | `BootReceiver` |
| 14 | `com/nova/assistant/service/NotificationListenerService.kt` | `NotificationListenerService` |
| 15 | `com/nova/assistant/ui/MainScreen.kt` | (composableها) |
| 16 | `com/nova/assistant/ui/MainViewModel.kt` | `MainViewModel` |
| 17 | `com/nova/assistant/ui/AlarmScreen.kt` | `AlarmScreen` |
| 18 | `com/nova/assistant/ui/SettingsScreen.kt` | `SettingsViewModel`, `SettingsScreen` |
| 19 | `com/nova/assistant/ui/theme/Theme.kt` | `NovaTheme` |
| 20 | `com/nova/assistant/util/TtsManager.kt` | `TtsManager` |
| 21 | `com/nova/assistant/util/SpeechRecognizerManager.kt` | `SpeechRecognizerManager` |
| 22 | `com/nova/assistant/util/PhoneLanguage.kt` | `PhoneLanguage` |
