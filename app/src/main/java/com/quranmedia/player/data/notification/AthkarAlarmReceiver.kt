package com.quranmedia.player.data.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quranmedia.player.R
import com.quranmedia.player.data.database.dao.AthkarDao
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class AthkarAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var athkarDao: AthkarDao
    @Inject lateinit var settingsRepository: SettingsRepository

    companion object {
        const val ACTION_ATHKAR_ALARM = "com.quranmedia.player.ATHKAR_ALARM"
        const val EXTRA_ATHKAR_TYPE = "athkar_type" // "morning" or "evening"
        private const val CHANNEL_ID = "athkar_notifications"
        private const val NOTIFICATION_ID_MORNING = 5000
        private const val NOTIFICATION_ID_EVENING = 5001
        private const val REQUEST_CODE_MORNING = 4000
        private const val REQUEST_CODE_EVENING = 4001

        fun scheduleMorningAthkar(context: Context, hour: Int, minute: Int) {
            scheduleAthkar(context, "morning", hour, minute, REQUEST_CODE_MORNING)
        }

        fun scheduleEveningAthkar(context: Context, hour: Int, minute: Int) {
            scheduleAthkar(context, "evening", hour, minute, REQUEST_CODE_EVENING)
        }

        fun cancelMorningAthkar(context: Context) {
            cancelAthkar(context, "morning", REQUEST_CODE_MORNING)
        }

        fun cancelEveningAthkar(context: Context) {
            cancelAthkar(context, "evening", REQUEST_CODE_EVENING)
        }

        fun scheduleFromSettings(context: Context, settings: com.quranmedia.player.data.repository.UserSettings) {
            if (settings.morningAthkarNotificationEnabled) {
                scheduleMorningAthkar(context, settings.morningAthkarNotificationHour, settings.morningAthkarNotificationMinute)
            } else {
                cancelMorningAthkar(context)
            }
            if (settings.eveningAthkarNotificationEnabled) {
                scheduleEveningAthkar(context, settings.eveningAthkarNotificationHour, settings.eveningAthkarNotificationMinute)
            } else {
                cancelEveningAthkar(context)
            }
        }

        private fun scheduleAthkar(context: Context, type: String, hour: Int, minute: Int, requestCode: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, AthkarAlarmReceiver::class.java).apply {
                action = ACTION_ATHKAR_ALARM
                putExtra(EXTRA_ATHKAR_TYPE, type)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // If the time has already passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                    )
                }
                Timber.d("Scheduled $type athkar notification at $hour:$minute")
            } catch (e: Exception) {
                Timber.e(e, "Failed to schedule $type athkar alarm")
            }
        }

        private fun cancelAthkar(context: Context, type: String, requestCode: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AthkarAlarmReceiver::class.java).apply {
                action = ACTION_ATHKAR_ALARM
                putExtra(EXTRA_ATHKAR_TYPE, type)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Timber.d("Cancelled $type athkar notification")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ATHKAR_ALARM) return
        val type = intent.getStringExtra(EXTRA_ATHKAR_TYPE) ?: return

        Timber.d("Athkar alarm received: $type")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val athkarList = athkarDao.getAthkarByCategorySync(type)
                if (athkarList.isNotEmpty()) {
                    val randomThikr = athkarList.random()
                    val settings = settingsRepository.getCurrentSettings()
                    val isArabic = settings.appLanguage == AppLanguage.ARABIC

                    val title = when (type) {
                        "morning" -> if (isArabic) "أذكار الصباح" else "Morning Athkar"
                        "evening" -> if (isArabic) "أذكار المساء" else "Evening Athkar"
                        else -> if (isArabic) "أذكار" else "Athkar"
                    }

                    showNotification(context, type, title, randomThikr.textArabic)

                    // Reschedule for tomorrow
                    scheduleFromSettings(context, settings)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to show athkar notification")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, type: String, title: String, text: String) {
        // Create notification channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Athkar Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily morning and evening athkar reminders"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        // Content intent - opens the app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val notificationId = if (type == "morning") NOTIFICATION_ID_MORNING else NOTIFICATION_ID_EVENING

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Timber.w("POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        Timber.d("Showed $type athkar notification")
    }
}
