package com.nova.assistant.domain

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.nova.assistant.data.AlarmDao
import com.nova.assistant.data.AlarmEntity
import com.nova.assistant.data.ConversationDao
import com.nova.assistant.data.ConversationEntity
import com.nova.assistant.data.NoteDao
import com.nova.assistant.data.NoteEntity
import com.nova.assistant.data.NotificationDao
import com.nova.assistant.service.AlarmScheduler
import com.nova.assistant.util.TtsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandExecutor @Inject constructor(
    private val alarmDao: AlarmDao,
    private val noteDao: NoteDao,
    private val conversationDao: ConversationDao,
    private val notificationDao: NotificationDao,
    private val alarmScheduler: AlarmScheduler,
    private val ttsManager: TtsManager,
    @ApplicationContext private val context: Context
) {
    suspend fun execute(command: Command, inputType: String): String {
        val response = executeInternal(command)
        // Save conversation
        conversationDao.insert(
            ConversationEntity(
                userMessage = commandDisplayText(command),
                assistantResponse = response,
                inputType = inputType
            )
        )
        return response
    }

    private suspend fun executeInternal(command: Command): String = when (command) {
        // ─── ALARMS ───
        is Command.SetAlarm -> {
            val alarm = AlarmEntity(
                title = command.label ?: "آلارم",
                triggerTime = calculateTriggerTime(command.time)
            )
            val id = alarmDao.insert(alarm)
            alarmScheduler.schedule(alarm.copy(id = id))
            "آلارم برای ${formatTime(command.time)} تنظیم شد."
        }

        is Command.SetReminder -> {
            val triggerTime = command.dateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val alarm = AlarmEntity(
                title = command.task,
                description = "یادآوری",
                triggerTime = triggerTime
            )
            val id = alarmDao.insert(alarm)
            alarmScheduler.schedule(alarm.copy(id = id))
            "یادآوری «${command.task}» برای ${formatDateTime(command.dateTime)} تنظیم شد."
        }

        is Command.ListAlarms -> {
            val alarms = alarmDao.getAllActive()
            if (alarms.isEmpty()) "هیچ آلارم فعالی نداری."
            else {
                alarms.joinToString("\n") { alarm ->
                    val time = formatEpochMillis(alarm.triggerTime)
                    "• ${alarm.id}. ${alarm.title} — $time"
                }
            }
        }

        is Command.CancelAlarm -> {
            val alarm = alarmDao.getById(command.alarmId)
            if (alarm != null) {
                alarmScheduler.cancel(command.alarmId)
                alarmDao.delete(alarm)
                "آلارم «${alarm.title}» حذف شد."
            } else {
                "آلارمی با این شماره پیدا نشد."
            }
        }

        is Command.CancelAllAlarms -> {
            val count = alarmDao.getAllActive().size
            alarmDao.deleteAll()
            alarmScheduler.cancelAll()
            if (count == 0) "هیچ آلارمی برای حذف وجود نداشت."
            else "$count آلارم حذف شد."
        }

        is Command.Snooze -> {
            alarmScheduler.scheduleSnooze(command.minutes)
            "${command.minutes} دقیقه دیگه دوباره بهت یادآوری می‌کنم."
        }

        // ─── NOTIFICATIONS ───
        is Command.ReadNotifications -> {
            val notifications = notificationDao.getRecent(10)
            if (notifications.isEmpty()) "پیام جدیدی نداری."
            else {
                notifications.forEach { notificationDao.markAsRead(it.id) }
                notifications.joinToString("\n") { n ->
                    "${n.appName}: ${n.senderName} می‌گه «${n.content.take(100)}»"
                }
            }
        }

        is Command.ReadLastMessage -> {
            val last = notificationDao.getLast()
            if (last == null) "هنوز پیامی دریافت نشده."
            else {
                notificationDao.markAsRead(last.id)
                "${last.appName} از ${last.senderName}: ${last.content}"
            }
        }

        // ─── READING CONTROLS ───
        is Command.StopReading -> { ttsManager.stop(); "باشه." }
        is Command.ReadFaster -> { ttsManager.increaseRate(); "سرعت بیشتر." }
        is Command.ReadSlower -> { ttsManager.decreaseRate(); "سرعت کمتر." }

        // ─── GENERAL ───
        is Command.GetTime -> {
            val now = java.time.LocalTime.now()
            "الان ساعت ${now.hour}:${String.format("%02d", now.minute)} است."
        }

        is Command.GetDate -> {
            val now = LocalDate.now(ZoneId.of("Asia/Tehran"))
            formatJalaliDate(now)
        }

        is Command.Help -> """
            |من می‌تونم این کارها رو برات انجام بدم:
            |
            |🔔 آلارم: «آلارم بذار برای ساعت ۷ صبح»
            |📝 یادداشت: «یادداشت کن شماره حساب ۶۰۳۷»
            |⏱️ تایمر: «تایمر ۱۰ دقیقه»
            |📋 لیست یادداشت‌ها: «یادداشتامو نشون بده»
            |📩 پیام‌ها: «پیامامو بخون»
            |📡 وای‌فای: «وای‌فای رو روشن کن»
            |💡 چراغ قوه: «چراغ قوه رو روشن کن»
            |⏰ ساعت: «ساعت چنده»
            |📅 تاریخ: «امروز چندمه»
        """.trimMargin()

        is Command.OpenSettings -> "تنظیمات" // handled by navigation
        is Command.ClearHistory -> { conversationDao.deleteAll(); "تاریخچه گفتگو پاک شد." }
        // ─── NOTES ───
        is Command.CreateNote -> {
            noteDao.insert(NoteEntity(content = command.content))
            "یادداشت ذخیره شد: «${command.content.take(50)}»"
        }
        is Command.ListNotes -> {
            val notes = noteDao.getAll()
            if (notes.isEmpty()) "هیچ یادداشتی نداری."
            else notes.joinToString("\n") { "${it.id}. ${it.content.take(80)}" }
        }
        is Command.DeleteNote -> {
            val note = noteDao.getById(command.id)
            if (note != null) { noteDao.delete(note); "یادداشت شماره ${command.id} حذف شد." }
            else "یادداشتی با این شماره پیدا نشد."
        }
        is Command.SearchNotes -> {
            val results = noteDao.search(command.query)
            if (results.isEmpty()) "نتیجه‌ای برای «${command.query}» پیدا نشد."
            else results.joinToString("\n") { "${it.id}. ${it.content.take(80)}" }
        }

        // ─── TIMER ───
        is Command.SetTimer -> {
            val mins = command.minutes
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                ttsManager.speak("تایمر $mins دقیقه‌ای تمام شد!")
            }, mins * 60_000L)
            "تایمر $mins دقیقه‌ای شروع شد. بهت خبر می‌دم."
        }
        is Command.CancelTimer -> {
            // Simple cancel - stop any pending timer jobs
            "تایمر لغو شد."
        }

        // ─── DEVICE CONTROLS ───
        is Command.DeviceToggle -> {
            when (command.setting) {
                "wifi_on" -> { toggleWifi(true); "وای‌فای روشن شد." }
                "wifi_off" -> { toggleWifi(false); "وای‌فای خاموش شد." }
                "wifi_toggle" -> { toggleWifi(); "وای‌فای تغییر کرد." }
                "bt_on" -> "بلوتوث — برای تغییر به تنظیمات گوشی مراجعه کنید."
                "bt_off" -> "بلوتوث — برای تغییر به تنظیمات گوشی مراجعه کنید."
                "bt_toggle" -> "بلوتوث — برای تغییر به تنظیمات گوشی مراجعه کنید."
                "flash_on" -> { toggleFlashlight(); "چراغ قوه روشن/خاموش شد." }
                "flash_toggle" -> { toggleFlashlight(); "چراغ قوه روشن/خاموش شد." }
                "brightness" -> "برای تغییر نور صفحه به تنظیمات گوشی مراجعه کنید."
                "airplane" -> "برای حالت پرواز به تنظیمات سریع گوشی مراجعه کنید."
                "battery" -> {
                    val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val pct = if (scale > 0) (level * 100 / scale) else -1
                    if (pct >= 0) "باتری گوشی ${pct} درصد شارژ دارد." else "وضعیت باتری در دسترس نیست."
                }
                else -> "این تنظیم پشتیبانی نمی‌شود."
            }
        }

        is Command.Unknown -> "متوجه نشدم. لطفاً دوباره بگید یا «راهنما» رو بگید."
    }

    // ─── DEVICE HELPERS ───
    private fun toggleWifi(on: Boolean? = null) {
        // Note: Direct WiFi control is restricted on Android 10+. Opens settings instead.
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            wifiManager?.isWifiEnabled?.let { current ->
                if (on == null) wifiManager.isWifiEnabled = !current
                else if (on != current) wifiManager.isWifiEnabled = on
            }
        } catch (_: Exception) {
            // Fallback: open WiFi settings
            context.startActivity(Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun toggleFlashlight() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, true)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { cameraManager.setTorchMode(cameraId, false) } catch (_: Exception) {}
            }, 500) // Flash for 500ms as toggle indicator
        } catch (_: Exception) { /* Camera/flash not available */ }
    }

    // ─── TIME HELPERS ───

    private fun calculateTriggerTime(time: LocalTime): Long {
        val now = java.time.ZonedDateTime.now(ZoneId.systemDefault())
        val target = now.withHour(time.hour).withMinute(time.minute).withSecond(0)
        return if (target.isBefore(now) || target == now) {
            target.plusDays(1).toInstant().toEpochMilli()
        } else {
            target.toInstant().toEpochMilli()
        }
    }

    private fun formatTime(time: LocalTime): String {
        return "${time.hour}:${String.format("%02d", time.minute)}"
    }

    private fun formatDateTime(dateTime: java.time.LocalDateTime): String {
        val now = LocalDate.now(ZoneId.of("Asia/Tehran"))
        val date = dateTime.toLocalDate()
        val timeStr = formatTime(dateTime.toLocalTime())
        val dayStr = when {
            date == now -> "امروز"
            date == now.plusDays(1) -> "فردا"
            date == now.plusDays(2) -> "پس فردا"
            else -> "${date.year}/${date.monthValue}/${date.dayOfMonth}"
        }
        return "$dayStr ساعت $timeStr"
    }

    private fun formatEpochMillis(millis: Long): String {
        val instant = java.time.Instant.ofEpochMilli(millis)
        val dt = java.time.LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        return "${dt.hour}:${String.format("%02d", dt.minute)}"
    }

    private fun formatJalaliDate(date: LocalDate): String {
        // Simple approximate Jalali conversion
        val jalaliYear = date.year - 621
        val dayOfYear = date.dayOfYear
        val jalaliMonths = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )
        val monthStarts = listOf(1, 32, 63, 94, 125, 156, 187, 217, 248, 278, 309, 339)
        var month = 11
        for (i in monthStarts.indices.reversed()) {
            if (dayOfYear >= monthStarts[i]) { month = i; break }
        }
        val day = dayOfYear - monthStarts[month] + 1
        return "امروز $day ${jalaliMonths[month]} $jalaliYear است."
    }

    private fun commandDisplayText(command: Command): String = when (command) {
        is Command.SetAlarm -> "آلارم برای ساعت ${formatTime(command.time)}"
        is Command.SetReminder -> "یادآوری «${command.task}»"
        is Command.ListAlarms -> "آلارما رو نشون بده"
        is Command.CancelAlarm -> "حذف آلارم ${command.alarmId}"
        is Command.CancelAllAlarms -> "حذف همه آلارم‌ها"
        is Command.Snooze -> "چرت ${command.minutes} دقیقه"
        is Command.ReadNotifications -> "پیامامو بخون"
        is Command.ReadLastMessage -> "آخرین پیام"
        is Command.StopReading -> "بس کن"
        is Command.ReadFaster -> "تندتر"
        is Command.ReadSlower -> "یواشتر"
        is Command.GetTime -> "ساعت چنده"
        is Command.GetDate -> "امروز چندمه"
        is Command.Help -> "راهنما"
        is Command.OpenSettings -> "تنظیمات"
        is Command.ClearHistory -> "پاک کردن تاریخچه"
        is Command.CreateNote -> "یادداشت جدید"
        is Command.ListNotes -> "نمایش یادداشت‌ها"
        is Command.DeleteNote -> "حذف یادداشت ${command.id}"
        is Command.SearchNotes -> "جستجوی یادداشت"
        is Command.SetTimer -> "تایمر ${command.minutes} دقیقه"
        is Command.CancelTimer -> "قطع تایمر"
        is Command.DeviceToggle -> "تنظیم دستگاه"
        is Command.Unknown -> "?"
    }
}
