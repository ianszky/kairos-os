package com.kairos.os.domain.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kairos.os.data.db.LocalAlarm
import com.kairos.os.data.db.LocalAlarmDao
import com.kairos.os.services.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAlarmController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localAlarmDao: LocalAlarmDao
) {
    suspend fun setAlarm(hour: Int, minute: Int, label: String): LocalAlarm = withContext(Dispatchers.IO) {
        val alarm = LocalAlarm(hour = hour, minute = minute, label = label, isActive = true)
        val id = localAlarmDao.insert(alarm)
        val savedAlarm = alarm.copy(id = id.toInt())

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", savedAlarm.id)
            putExtra("ALARM_LABEL", label)
            putExtra("ALARM_HOUR", hour)
            putExtra("ALARM_MINUTE", minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            savedAlarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        withContext(Dispatchers.Main) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        savedAlarm
    }

    suspend fun getAllAlarms(): List<LocalAlarm> = withContext(Dispatchers.IO) {
        localAlarmDao.getAllAlarms()
    }

    suspend fun cancelAlarm(id: Int): Boolean = withContext(Dispatchers.IO) {
        val alarm = localAlarmDao.getAlarmById(id)
        if (alarm != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_MUTABLE
            )
            if (pendingIntent != null) {
                withContext(Dispatchers.Main) {
                    alarmManager.cancel(pendingIntent)
                }
                pendingIntent.cancel()
            }
            localAlarmDao.delete(alarm)
            true
        } else {
            false
        }
    }
}
