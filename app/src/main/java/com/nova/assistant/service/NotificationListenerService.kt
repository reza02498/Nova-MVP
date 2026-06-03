package com.nova.assistant.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nova.assistant.data.NovaDatabase
import com.nova.assistant.data.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListenerService : NotificationListenerService() {

    private val allowedPackages = setOf(
        "com.android.mms",           // SMS
        "com.google.android.apps.messaging", // Google Messages
        "com.whatsapp",              // WhatsApp
        "org.telegram.messenger",    // Telegram
        "com.whatsapp.w4b"           // WhatsApp Business
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in allowedPackages) return
        if (!sbn.notification.flags.and(android.app.Notification.FLAG_ONGOING_EVENT).equals(0)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val content = text.ifEmpty { bigText }
        if (content.isEmpty()) return

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            sbn.packageName.substringAfterLast(".")
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = getDatabase()
            db.notificationDao().insert(
                NotificationEntity(
                    appName = appName,
                    senderName = title.ifEmpty { appName },
                    content = content
                )
            )
            // Auto-clean notifications older than 7 days
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            db.notificationDao().deleteOlderThan(sevenDaysAgo)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No action needed
    }

    private fun getDatabase(): NovaDatabase {
        return androidx.room.Room.databaseBuilder(
            applicationContext,
            NovaDatabase::class.java,
            NovaDatabase.NAME
        ).fallbackToDestructiveMigration().build()
    }
}
