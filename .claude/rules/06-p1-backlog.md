# Nova MVP — P1 Backlog

## P1-01: Word Boundary Matching — COMPLETED ✅

**Fix:** `IntentClassifier.kt` — `matchesWord()` function added. All keyword/synonym/negative matching now uses word boundary checks. "نت" no longer matches inside "نتونستم".

---

## P1-02: Voice Diagnostic System — COMPLETED ✅

**Fix:** `SpeechRecognizerManager.kt` — All error messages now include:
- Emoji prefix for visual distinction
- Specific guidance for each error type
- Download path for Persian language pack
- Clear distinction between "no speech" and "no language pack"

---

## P1-03: RHVoice Investigation — Decision

### KEEP_AS_OPTIONAL_FALLBACK

**دلیل:**
- Google TTS روی اکثر گوشیهای ایرانی (Samsung/Xiaomi) از پیش نصب است
- RHVoice نیاز به sideload APK دارد که برای کاربران غیرفنی پیچیده است
- کیفیت RHVoice برای MVP قابل قبول است ولی برای Production ایدهآل نیست
- در MVP فعلی، کاربرانی که TTS فارسی ندارند از طریق SettingsScreen راهنمایی میشوند

**Action for future:** اگر feedback کاربران نشان داد که TTS مشکل اساسی است، RHVoice integration در فاز بعد.

---

## P1-04: Real Persian Validation Suite — Test Design

### Target Metrics
- Accuracy >= 90%
- False Positive Rate < 5%

### Test Categories & Sample Phrases (150 total)

#### Time (15 phrases)
1. ساعت چنده — GetTime
2. ساعت رو بگو — GetTime
3. زمان رو بگو — GetTime
4. وقت چنده — GetTime
5. الان چند وقته — GetTime
6. ساعت الان — GetTime
7. ساعت چند شده — GetTime
8. الان ساعت چنده — GetTime
9. تایم چنده — GetTime
10. ساعت — GetTime
11. ببین ساعت چنده — GetTime
12. میشه ساعت رو بگی — GetTime
13. ساعت چنده الان — GetTime
14. ساعتو بگو — GetTime
15. زمان فعلی — GetTime (unsupported currently)

#### Date (10 phrases)
16. امروز چندمه — GetDate
17. تاریخ رو بگو — GetDate
18. امروز چه روزیه — GetDate
19. تاریخ امروز چیه — GetDate
20. امروز چندم — GetDate
21. چندمه — GetDate
22. امروز چندمیم — GetDate
23. بگو امروز چندمه — GetDate
24. چه روزی هستیم — GetDate
25. تاریخ — GetDate (unsupported — bare word)

#### WiFi (15 phrases)
26. وای فای رو روشن کن — TOGGLE_WIFI
27. وایفای رو خاموش کن — TOGGLE_WIFI
28. اینترنت رو وصل کن — TOGGLE_WIFI
29. نت رو قطع کن — TOGGLE_WIFI
30. وای فای — TOGGLE_WIFI (bare word — borderline)
31. وایفای — TOGGLE_WIFI (bare word)
32. وایرلس رو روشن کن — TOGGLE_WIFI
33. شبکه رو فعال کن — TOGGLE_WIFI
34. اینترنت رو خاموش کن — TOGGLE_WIFI
35. wifi on — TOGGLE_WIFI
36. نتونستم وصل بشم — UNKNOWN (critical — must NOT false positive)
37. پیام نتونست برسه — UNKNOWN (must NOT false positive)
38. وای فای موبایل — TOGGLE_WIFI
39. اینترنت وای فای — TOGGLE_WIFI
40. قطع کن اینترنتو — TOGGLE_WIFI

#### Alarm (15 phrases)
41. آلارم بذار ۷ صبح — SET_ALARM
42. زنگ بزن ۸ — SET_ALARM
43. ساعت ۶ بیدارم کن — SET_ALARM
44. فردا ۸ بیدارم کن — SET_ALARM
45. منو ۷ صبح بیدار کن — SET_ALARM
46. برای ۹ آلارم تنظیم کن — SET_ALARM
47. آلارما رو نشون بده — LIST_ALARMS
48. چه آلارمایی داری — LIST_ALARMS
49. زنگا رو بگو — LIST_ALARMS
50. آلارم ۳ رو حذف کن — CANCEL_ALARM
51. آلارم ۲ کنسل — CANCEL_ALARM
52. همه آلارما رو پاک کن — CANCEL_ALL_ALARMS
53. چرت ۱۰ دقیقه — SNOOZE
54. اسنوز ۵ — SNOOZE
55. آلارم — UNKNOWN (bare word, no time)

#### Timer (10 phrases)
56. تایمر ۵ دقیقه — SET_TIMER
57. ۱۰ دقیقه تایمر بذار — SET_TIMER
58. یه تایمر ۲۰ دقیقه ای — SET_TIMER
59. تایمر رو قطع کن — CANCEL_TIMER
60. تایمر — UNKNOWN (bare word)
61. تایمز ۱۵ — SET_TIMER
62. زمانسنج ۳۰ دقیقه — SET_TIMER
63. stop timer — CANCEL_TIMER
64. ۵ دقیقه دیگه — UNKNOWN (natural but no "تایمر" keyword)
65. تایمر ۱ ساعته — SET_TIMER (needs "60" not "1")

#### Notes (15 phrases)
66. یادداشت کن خرید نان — CREATE_NOTE
67. بنویس فردا دکتر دارم — CREATE_NOTE
68. یادداشت جدید بذار — CREATE_NOTE
69. ثبت کن جلسه ساعت ۳ — CREATE_NOTE
70. یادداشتامو نشون بده — LIST_NOTES
71. چه یادداشتایی دارم — LIST_NOTES
72. یادداشتها رو بیار — LIST_NOTES
73. حذف یادداشت ۳ — DELETE_NOTE
74. یادداشت ۲ رو پاک کن — DELETE_NOTE
75. پاک کن یادداشت ۴ — DELETE_NOTE
76. تو یادداشتا بگرد حساب — SEARCH_NOTES
77. جستجوی یادداشت دکتر — SEARCH_NOTES
78. سرچ یادداشت — SEARCH_NOTES (bare word after search)
79. یه نوت جدید — CREATE_NOTE
80. نوشته‌هام رو بگو — LIST_NOTES

#### Notifications (10 phrases)
81. پیامامو بخون — READ_NOTIFICATIONS
82. اعلان ها رو نشون بده — READ_NOTIFICATIONS
83. چه پیامایی دارم — READ_NOTIFICATIONS
84. کی پیام داده — READ_NOTIFICATIONS
85. آخرین پیام رو بخون — READ_LAST_MESSAGE
86. پیام آخر — READ_LAST_MESSAGE
87. پیامک — UNKNOWN (bare word — not supported yet)
88. چک کن کی پیام داده — READ_NOTIFICATIONS
89. پیام جدید — UNKNOWN (too vague)
90. نوتیفیکیشن ها رو بخون — READ_NOTIFICATIONS

#### Device (15 phrases)
91. چراغ قوه رو روشن کن — TOGGLE_FLASHLIGHT
92. فلش رو باز کن — TOGGLE_FLASHLIGHT
93. بلوتوث رو روشن کن — TOGGLE_BLUETOOTH
94. بلوتوث رو خاموش کن — TOGGLE_BLUETOOTH
95. باتری چند درصده — DeviceToggle(battery)
96. شارژ باتری چقدره — DeviceToggle(battery)
97. نور صفحه رو ببر بالا — DeviceToggle(brightness)
98. حالت پرواز — DeviceToggle(airplane)
99. flashlight on — TOGGLE_FLASHLIGHT
100. bt off — TOGGLE_BLUETOOTH

#### General (10 phrases)
101. راهنما — Help
102. کمک — Help
103. چه کارایی میتونی بکنی — Help
104. تنظیمات — OpenSettings
105. برو تنظیمات — OpenSettings
106. بس کن — StopReading
107. قطع کن — StopReading
108. تندتر بخون — ReadFaster
109. یواشتر — ReadSlower
110. پاک کردن تاریخچه — ClearHistory

#### Colloquial/Typo (20 phrases)
111. وایفای رو روشن کن — TOGGLE_WIFI
112. الارم بذار ۷ — SET_ALARM
113. یاداشت کن شیر — CREATE_NOTE
114. ببینم ساعت چنده — GetTime
115. خب یه تایمر بذار ۱۰ — SET_TIMER
116. میخوام وای فای رو روشن کنم — TOGGLE_WIFI
117. چراغ قوه رو ببینم روشن میشه — TOGGLE_FLASHLIGHT
118. بی‌زحمت ساعت رو بگو — GetTime
119. لطفا آلارما رو نشون بده — LIST_ALARMS
120. یه زنگ بذار واسه ۷ — SET_ALARM
121. آها — UNKNOWN (noise word)
122. باشه — UNKNOWN (acknowledgment)
123. اوکی — UNKNOWN
124. چشم — UNKNOWN
125. بذار ببینم — UNKNOWN (filler)
126. راستی ساعت چنده — GetTime
127. ضمنا یادداشت کن — CREATE_NOTE
128. دیگه بسه — StopReading
129. آخه چرا — UNKNOWN
130. اصلا نمیشه — UNKNOWN

#### Speech-To-Text Real Outputs (20 phrases)
131. ساعت چند شده — GetTime
132. وای فای روشنه — UNKNOWN (statement, not command)
133. الارم برا ۷ صبح — SET_ALARM
134. تایمر ۵ دقه — SET_TIMER
135. یادداشت جدید — CREATE_NOTE
136. چراق قوه — TOGGLE_FLASHLIGHT
137. بلوتوس — TOGGLE_BLUETOOTH
138. پیاما — READ_NOTIFICATIONS
139. یاداشتا — LIST_NOTES
140. الیوم چنده — GetDate
141. تنضیمات — OpenSettings
142. باتری چن درصده — DeviceToggle(battery)
143. تایمز ۱۰ — SET_TIMER
144. وایفا — TOGGLE_WIFI
145. الارما — LIST_ALARMS
146. یاداشت — UNKNOWN (too vague)
147. تایمرُ قطع کن — CANCEL_TIMER
148. پیام آخرُ بخون — READ_LAST_MESSAGE
149. چراغ قوه رو خاموش — TOGGLE_FLASHLIGHT
150. اسنوز ۱۰ دقیقه — SNOOZE

### Expected Results (based on code analysis)
- **Accuracy:** ~85-88% (based on new classifier + word boundary + Persian phrases)
- **False Positives:** 0 (word boundary prevents "نتونستم" → TOGGLE_WIFI)
- **False Negatives:** ~15 (bare words, unsupported phrases like "۵ دقیقه دیگه")
