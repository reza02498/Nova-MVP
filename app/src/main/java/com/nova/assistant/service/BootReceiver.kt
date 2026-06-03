package com.nova.assistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nova.assistant.data.AlarmEntity
import com.nova.assistant.data.NovaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = NovaDatabase.getInstance(context)
            val alarms = db.alarmDao().getAllActive()
            val scheduler = AlarmScheduler(context)

            for (alarm in alarms) {
                if (alarm.triggerTime > System.currentTimeMillis()) {
                    scheduler.schedule(alarm)
                }
            }
        }
    }
}

// Helper extension for NovaDatabase in BootReceiver
fun NovaDatabase.Companion.getInstance(context: Context): NovaDatabase {
    return androidx.room.Room.databaseBuilder(
        context.applicationContext,
        NovaDatabase::class.java,
        NovaDatabase.NAME
    ).fallbackToDestructiveMigration().build()
}
