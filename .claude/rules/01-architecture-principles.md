# Nova — اصول معماری

## اولویت‌ها (به ترتیب)

1. **سادگی (KISS)** — ساده‌ترین طراحی قابل نگهداری را انتخاب کن
2. **YAGNI** — فقط نیازهای واقعی فاز فعلی را پیاده‌سازی کن
3. **تست‌پذیری** — هر بخش باید مستقل قابل تست باشد
4. **حداقل وابستگی** — هر کلاس کمترین dependency ممکن را داشته باشد
5. **قابلیت توسعه آینده** — بدون Over-Engineering

## قوانین YAGNI

### Interface نساز مگر اینکه:
- حداقل **دو** پیاده‌سازی واقعی در فاز فعلی وجود داشته باشد
- "ممکن است در آینده لازم شود" دلیل کافی نیست
- سوال طلایی: "اگر این abstraction را حذف کنم چه چیزی می‌شکند؟" → پاسخ "هیچ چیز" = حذف

### Class جداگانه نساز وقتی:
- یک `private function` کافی است
- یک `when` ساده کافی است
- کلاس Stateless است و فقط یک متد public دارد

### از اینها استفاده نکن:
- ❌ Factory Pattern (مگر واقعاً نیاز باشد)
- ❌ Strategy Pattern (مگر ۳+ استراتژی واقعی وجود داشته باشد)
- ❌ Plugin System (YAGNI کلاسیک)
- ❌ Generic Extension Points
- ❌ Rule Engine
- ❌ Specification Pattern

## قوانین KISS

- اگر یک `when` ساده کافی است ← Registry نساز
- اگر یک `private function` کافی است ← Class جداگانه نساز
- اگر یک `mutable field` کافی است ← Provider/Repository نساز
- اگر `@Inject constructor` کافی است ← Factory/Provider نساز

## Dependency Injection

- همه کلاس‌های جدید از `@Inject constructor` استفاده کنند
- `@Binds` فقط وقتی استفاده شود که Interface وجود دارد
- از manual instantiation پرهیز شود (مگر در BroadcastReceiver/Service که Hilt اجباری نیست)
