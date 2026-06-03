# Nova — معماری Command Understanding Layer

## وضعیت فعلی (بعد از Refactor)

```
MainViewModel
     │
     ▼
CommandParser.parse(text: String): Command   ← API بدون تغییر
  @Inject constructor(
      normalizer: PersianNormalizer,
      classifier: IntentClassifier,
      extractor: EntityExtractor
  )
     │
     ├── 1. normalizer.normalize(rawText)       ← ۶ مرحله نرمال‌سازی
     ├── 2. directMatch(normalized) → Command?   ← Tier 1: ۱۰ فرمان مستقیم
     ├── 3. classifier.classify(normalized)       ← Tier 2: ۱۸ Intent
     ├── 4. extractor.extract(text, intent, ctx)  ← استخراج entity
     ├── 5. mapToCommand(intent, entities)        ← private fun
     └── 6. lastContext = ConversationContext(...)
     │
     ▼
Command sealed class (بدون تغییر)
     │
     ▼
CommandExecutor (بدون تغییر)
```

## Tier 1 — Direct Commands (بدون classification)

این فرمان‌ها unambiguous هستند — بدون هیچ ابهامی تشخیص داده می‌شوند:

| فرمان | تشخیص |
|---|---|
| GetTime | "ساعت" + "چنده/چند/الان" |
| GetDate | "امروز چندمه/تاریخ امروز/چه روزی/چندمه" |
| Help | "راهنما/کمک/help/چه کارا/چیکار" |
| OpenSettings | "تنظیمات/setting" |
| ClearHistory | "تاریخچه/گفتگو" + "پاک/حذف" |
| StopReading | "بس کن/قطع/خاموش/ساکت/stop" |
| ReadFaster | "تندتر/سریعتر/سرعت بیشتر" |
| ReadSlower | "یواشتر/آرومتر/آهسته تر" |
| Brightness | "نور صفحه/روشنایی" + "زیاد/کم/ببر" |
| Battery | "باتری/شارژ" + "چند/چقدر/وضعیت" |

## Tier 2 — Intent Classification

۱۸ Intent در `IntentClassifier` با الگوریتم scoring:

```
confidence = (matchedPositive × 1.0 + matchedSynonym × 0.8 - negativePenalty × 1.5) / maxPossibleScore

if requiresNumber && !hasNumber → confidence = 0
if requiresTime && !hasTime → confidence = 0

پذیرش اگر: confidence >= threshold (پیش‌فرض 0.4)
```

## فایل‌ها

```
domain/
├── intent/
│   ├── Intent.kt               ← enum (18 مقدار)
│   ├── IntentResult.kt         ← data class
│   ├── IntentDefinition.kt     ← data class + SynonymGroup
│   ├── IntentDefinitions.kt    ← ۱۸ تعریف با ۵-۱۰ مثال
│   └── IntentClassifier.kt     ← concrete class — keyword+synonym scoring
├── entity/
│   ├── ExtractedEntities.kt    ← data class + DeviceTarget + DeviceAction
│   └── EntityExtractor.kt      ← extract(text, intent, context?)
├── context/
│   └── ConversationContext.kt  ← data class
└── normalizer/
    └── PersianNormalizer.kt    ← Pure Kotlin — ۶ مرحله
```

## اضافه کردن قابلیت جدید

برای اضافه کردن یک دستور جدید:

1. اگر unambiguous است → به `directMatch()` اضافه کن
2. اگر نیاز به classification دارد:
   - به `Intent` enum اضافه کن
   - یک `IntentDefinition` به `IntentDefinitions.all` اضافه کن
   - `EntityExtractor.extract()` را آپدیت کن
   - `mapToCommand()` را آپدیت کن

## آنچه حذف شد (YAGNI)

- ❌ `IntentClassifier` interface
- ❌ `ContextProvider` interface
- ❌ `SemanticMatcher` + پیاده‌سازی
- ❌ `CommandMapper` class
- ❌ `EntitySpec` data class
- ❌ `alternativeIntents` + `reasoning`
- ❌ `contextHints`
- ❌ Hybrid scoring (0.7+0.3)
