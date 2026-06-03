package com.nova.assistant.domain.normalizer

import javax.inject.Inject

class PersianNormalizer @Inject constructor() {

    fun normalize(input: String): String {
        var text = input.trim()
        if (text.isEmpty()) return ""

        // 1. Arabic → Persian character mapping
        text = text
            .replace("ي", "ی").replace("ك", "ک").replace("ة", "ه")
            .replace("ؤ", "و").replace("أ", "ا").replace("إ", "ا")
            .replace("ۀ", "ه")

        // 2. Persian/Arabic digits → ASCII
        text = text
            .replace("٠", "0").replace("١", "1").replace("٢", "2")
            .replace("٣", "3").replace("٤", "4").replace("٥", "5")
            .replace("٦", "6").replace("٧", "7").replace("٨", "8").replace("٩", "9")
            .replace("۰", "0").replace("۱", "1").replace("۲", "2")
            .replace("۳", "3").replace("۴", "4").replace("۵", "5")
            .replace("۶", "6").replace("۷", "7").replace("۸", "8").replace("۹", "9")

        // 3. Common typos
        text = text
            .replace("الارم", "آلارم")
            .replace("یاداشت", "یادداشت")
            .replace("تایمز", "تایمر")
            .replace("وایفای", "وای فای")
            .replace("وای‌فای", "وای فای")
            .replace("بلوتوس", "بلوتوث")
            .replace("بلوتوت", "بلوتوث")
            .replace("چراق قوه", "چراغ قوه")
            .replace("چراغقوه", "چراغ قوه")
            .replace("فلش لایت", "چراغ قوه")
            .replace("باطری", "باتری")

        // 4. Remove filler words
        text = text
            .replace(Regex("(^|\\s)خب(\\s|$)"), " ")
            .replace(Regex("(^|\\s)ببین(م)?(\\s|$)"), " ")
            .replace(Regex("(^|\\s)بی‌زحمت(\\s|$)"), " ")
            .replace(Regex("(^|\\s)لطفاً?(\\s|$)"), " ")
            .replace(Regex("(^|\\s)لطفا(\\s|$)"), " ")
            .replace(Regex("(^|\\s)میشه(\\s|$)"), " ")
            .replace(Regex("(^|\\s)می‌شه(\\s|$)"), " ")
            .replace(Regex("(^|\\s)میخوام(\\s|$)"), " ")
            .replace(Regex("(^|\\s)می‌خوام(\\s|$)"), " ")
            .replace(Regex("(^|\\s)میخواستم(\\s|$)"), " ")
            .replace(Regex("(^|\\s)می‌خواستم(\\s|$)"), " ")
            .replace(Regex("(^|\\s)یه(\\s|$)"), " ")
            .replace(Regex("(^|\\s)یکی(\\s|$)"), " ")
            .replace(Regex("(^|\\s)برام(\\s|$)"), " ")
            .replace(Regex("(^|\\s)واسم(\\s|$)"), " ")
            .replace(Regex("(^|\\s)اینکه(\\s|$)"), " ")
            .replace(Regex("(^|\\s)اونکه(\\s|$)"), " ")
            .replace(Regex("(^|\\s)درواقع(\\s|$)"), " ")
            .replace(Regex("(^|\\s)اصلا(\\s|$)"), " ")
            .replace(Regex("(^|\\s)اصلاً(\\s|$)"), " ")
            .replace(Regex("(^|\\s)راستی(\\s|$)"), " ")
            .replace(Regex("(^|\\s)ضمنا(\\s|$)"), " ")
            .replace(Regex("(^|\\s)آخه(\\s|$)"), " ")
            .replace(Regex("(^|\\s)دیگه(\\s|$)"), " ")

        // 5. Colloquial → standard verb normalization
        text = text
            .replace("بذارم", "بذار").replace("بکن", "کن").replace("بکنم", "کن")
            .replace("بدم", "بده").replace("بگم", "بگو").replace("ببینم", "ببین")
            .replace("بخوام", "بخون").replace("بخونم", "بخون").replace("بزنم", "بزن")
            .replace("نداره", "ندارد").replace("داره", "دارد")
            .replace("میخواد", "می‌خواهد").replace("میاد", "میاید")
            .replace("بشه", "باشد").replace("هستش", "هست").replace("نیستش", "نیست")
            .replace("چیکار", "چه کار").replace("جطور", "چطور")

        // 6. Spelling variations
        text = text
            .replace("نصف", "نیم").replace("ربع", "15 دقیقه").replace("یک ربع", "15 دقیقه")

        // 7. Whitespace
        text = text.replace(Regex("\\s+"), " ").trim()

        return text
    }
}
