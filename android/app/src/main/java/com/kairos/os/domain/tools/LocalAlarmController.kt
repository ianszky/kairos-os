package com.kairos.os.domain.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import com.kairos.os.data.db.LocalAlarm
import com.kairos.os.data.db.LocalAlarmDao
import com.kairos.os.services.AlarmReceiver
import com.kairos.os.ui.AlarmAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAlarmController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localAlarmDao: LocalAlarmDao
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    private val _timerRemaining = MutableStateFlow(0L)
    val timerRemaining: StateFlow<Long> = _timerRemaining.asStateFlow()

    private val _timerDuration = MutableStateFlow(0L)
    val timerDuration: StateFlow<Long> = _timerDuration.asStateFlow()

    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()

    private val _timerPaused = MutableStateFlow(false)
    val timerPaused: StateFlow<Boolean> = _timerPaused.asStateFlow()

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
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            // Sync with native Android Alarm App
            try {
                val nativeAlarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(nativeAlarmIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    fun startTimer(durationMs: Long, label: String = "Timer") {
        cancelTimer()
        _timerDuration.value = durationMs
        _timerRemaining.value = durationMs
        _timerRunning.value = true
        _timerPaused.value = false

        // Sync native timer
        try {
            val nativeTimerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, (durationMs / 1000).toInt())
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(nativeTimerIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        timerJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + durationMs
            while (isActive && System.currentTimeMillis() < endTime) {
                if (!_timerPaused.value) {
                    _timerRemaining.value = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
                }
                delay(100L)
            }
            if (isActive && !_timerPaused.value) {
                _timerRemaining.value = 0L
                _timerRunning.value = false
                _timerPaused.value = false
                triggerTimerAlert(label)
            }
        }
    }

    fun pauseTimer() {
        _timerPaused.value = true
    }

    fun resumeTimer() {
        _timerPaused.value = false
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerRemaining.value = 0L
        _timerDuration.value = 0L
        _timerRunning.value = false
        _timerPaused.value = false
    }

    private fun triggerTimerAlert(label: String) {
        val alertIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            putExtra("ALARM_LABEL", label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(alertIntent)
    }
}
