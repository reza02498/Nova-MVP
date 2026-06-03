# Nova — توسعه قوانین و محدودیت‌ها

## غیرقابل تغییر (Non-Negotiables)

### اینها هرگز تغییر نمی‌کنند:
1. **`Command` sealed class** — بدون تغییر در ساختار
2. **`CommandExecutor`** — بدون تغییر در منطق اجرا
3. **`MainViewModel` public API** — `parse(String): Command` بدون تغییر
4. **Room Database schema** — migration امن، بدون `fallbackToDestructiveMigration` در Production
5. **UI Layer** — Compose + Material 3 + RTL

### اینها هرگز اضافه نمی‌شوند (در فاز فعلی):
1. ❌ LLM / AI / ML / Embedding
2. ❌ API خارجی (Retrofit, Ktor)
3. ❌ SemanticMatcher / RuleBasedSemanticMatcher
4. ❌ ContextProvider (interface)
5. ❌ IntentClassifier (interface)
6. ❌ Wake Word Detection (Vosk)
7. ❌ Pack Download System

## قوانین کدنویسی

### Pure Kotlin در لایه Domain
- بدون `Context`
- بدون `Activity`
- بدون `Service`
- بدون Android SDK APIs
- فقط: `kotlin.*`, `java.time.*`, `javax.inject.*`

### کلاس‌های جدید
- `@Inject constructor` برای همه
- Stateless تا حد امکان
- Single Responsibility
- Public API حداقلی

## قوانین Push

1. **هرگز بدون اجازه کاربر push نکن**
2. قبل از push، فایل‌های تغییر یافته را بررسی کن
3. مطمئن شو تغییرات روی فایل‌های نامرتبط تأثیر نگذاشته باشند

## قوانین commit

1. یک commit = یک تغییر منطقی
2. پیام commit به انگلیسی
3. توضیح بده **چرا** تغییر داده شد، نه **چه چیزی** تغییر کرد
