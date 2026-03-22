package com.quranmedia.player.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quranmedia.player.R
import com.quranmedia.player.data.repository.PrayerNotificationMode
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.model.PrayerType
import com.quranmedia.player.media.service.AthanService
import com.quranmedia.player.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

/**
 * BroadcastReceiver for exact prayer time alarms.
 * This ensures notifications/athan trigger at the exact scheduled time,
 * even when the device is in Doze mode.
 */
@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var prayerTimesRepository: com.quranmedia.player.domain.repository.PrayerTimesRepository

    @Inject
    lateinit var prayerNotificationScheduler: PrayerNotificationScheduler

    companion object {
        const val ACTION_PRAYER_ALARM = "com.quranmedia.player.PRAYER_ALARM"
        const val ACTION_STOP_ATHAN = "com.quranmedia.player.STOP_ATHAN"
        const val EXTRA_PRAYER_TYPE = "prayer_type"
        private const val CHANNEL_ID = "prayer_notifications"
        private const val NOTIFICATION_ID_BASE = 2000
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Handle stop athan action
        if (intent.action == ACTION_STOP_ATHAN) {
            Timber.d("Stop athan action received")
            AthanService.stopAthan(context)
            return
        }

        if (intent.action != ACTION_PRAYER_ALARM) return

        val prayerTypeName = intent.getStringExtra(EXTRA_PRAYER_TYPE) ?: return
        val prayerType = try {
            PrayerType.valueOf(prayerTypeName)
        } catch (e: Exception) {
            Timber.e("Invalid prayer type: $prayerTypeName")
            return
        }

        Timber.d("Prayer alarm received for: $prayerType")

        val settings = settingsRepository.getCurrentSettings()

        // Guard: bail out if prayer notifications are globally disabled
        if (!settings.prayerNotificationEnabled) {
            Timber.d("$prayerType: Prayer notifications disabled globally, skipping")
            return
        }

        // Guard: bail out if this specific prayer is disabled
        val isPrayerEnabled = when (prayerType) {
            PrayerType.FAJR -> settings.notifyFajr
            PrayerType.DHUHR -> settings.notifyDhuhr
            PrayerType.ASR -> settings.notifyAsr
            PrayerType.MAGHRIB -> settings.notifyMaghrib
            PrayerType.ISHA -> settings.notifyIsha
            else -> true
        }
        if (!isPrayerEnabled) {
            Timber.d("$prayerType: Notification disabled for this prayer, skipping")
            return
        }

        // Get notification mode for this prayer
        val mode = settingsRepository.getPrayerNotificationMode(prayerType.name)

        when (mode) {
            PrayerNotificationMode.ATHAN -> {
                // Check if phone is in silent/vibrate mode
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val ringerMode = audioManager.ringerMode
                val isPhoneSilent = ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE

                if (isPhoneSilent && !settings.athanInSilentMode) {
                    // Phone is silent and user hasn't enabled "play in silent mode" - show notification only
                    Timber.d("$prayerType: Phone is silent and athanInSilentMode is OFF, showing notification instead")
                    showNotification(context, prayerType, false, ringerMode == AudioManager.RINGER_MODE_VIBRATE)
                } else {
                    // Play athan normally
                    val athanId = settingsRepository.getPrayerAthanId(prayerType.name)
                    showAthanNotification(context, prayerType, athanId, settings.athanMaxVolume)
                }
            }
            PrayerNotificationMode.NOTIFICATION -> {
                // Show notification only
                showNotification(context, prayerType, settings.prayerNotificationSound, settings.prayerNotificationVibrate)
            }
            PrayerNotificationMode.SILENT -> {
                // Do nothing
                Timber.d("$prayerType is set to silent, skipping")
            }
        }

        // Reschedule all prayer notifications (this will schedule for tomorrow if time has passed)
        rescheduleNotifications()
    }

    /**
     * Reschedule all prayer notifications after an alarm fires.
     * This ensures the next day's alarms are scheduled.
     */
    private fun rescheduleNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val location = prayerTimesRepository.getSavedLocationSync()
                if (location == null) {
                    Timber.d("No saved location, cannot reschedule prayer alarms")
                    return@launch
                }

                val method = prayerTimesRepository.getSavedCalculationMethod()
                val settings = settingsRepository.getCurrentSettings()
                val asrMethod = com.quranmedia.player.domain.model.AsrJuristicMethod.fromId(settings.asrJuristicMethod)

                val result = prayerTimesRepository.getPrayerTimes(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    date = LocalDate.now(),
                    method = method,
                    asrMethod = asrMethod
                )

                when (result) {
                    is com.quranmedia.player.domain.util.Resource.Success -> {
                        result.data?.let { prayerTimes ->
                            prayerNotificationScheduler.schedulePrayerNotifications(prayerTimes)
                            Timber.d("Prayer alarms rescheduled after alarm fired")
                        }
                    }
                    is com.quranmedia.player.domain.util.Resource.Error -> {
                        Timber.e("Failed to reschedule prayer alarms: ${result.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error rescheduling prayer alarms")
            }
        }
    }

    /**
     * Start AthanService to play athan audio.
     * AthanService handles all playback, volume control, and dismissal.
     */
    private fun showAthanNotification(context: Context, prayerType: PrayerType, athanId: String, maxVolume: Boolean) {
        Timber.d("PrayerAlarmReceiver: Starting AthanService for $prayerType with athanId: $athanId")
        try {
            AthanService.startAthan(context, athanId, prayerType)
        } catch (e: Exception) {
            Timber.e(e, "PrayerAlarmReceiver: Failed to start AthanService for $prayerType")
        }
    }

    private fun showNotification(context: Context, prayerType: PrayerType, sound: Boolean, vibrate: Boolean, isAthanPlaying: Boolean = false) {
        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("Notification permission not granted")
                return
            }
        }

        createNotificationChannel(context)

        val settings = settingsRepository.getCurrentSettings()
        val isArabic = settings.appLanguage.code == "ar"

        val (title, text) = getPrayerNotificationContent(prayerType, isArabic)

        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setOngoing(isAthanPlaying)  // Make ongoing while athan plays so user can't accidentally dismiss

        // Add stop action if athan is playing
        if (isAthanPlaying) {
            val stopIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_STOP_ATHAN
                putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                prayerType.ordinal + 100,  // Different request code
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_pause,
                if (isArabic) "إيقاف" else "Stop",
                stopPendingIntent
            )
            // Set delete intent to stop athan when notification is dismissed
            builder.setDeleteIntent(stopPendingIntent)
        }

        if (!sound) {
            builder.setSilent(true)
        }

        if (!vibrate) {
            builder.setVibrate(null)
        }

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(NOTIFICATION_ID_BASE + prayerType.ordinal, builder.build())

        Timber.d("Notification shown for $prayerType (athan playing: $isAthanPlaying)")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Prayer Times"
            val descriptionText = "Prayer time notifications and athan"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getPrayerNotificationContent(prayerType: PrayerType, isArabic: Boolean): Pair<String, String> {
        return when (prayerType) {
            PrayerType.FAJR -> if (isArabic)
                Pair("صلاة الفجر", "حان وقت صلاة الفجر")
            else
                Pair("Fajr Prayer", "It's time for Fajr prayer")
            PrayerType.SUNRISE -> if (isArabic)
                Pair("الشروق", "وقت الشروق")
            else
                Pair("Sunrise", "Sunrise time")
            PrayerType.DHUHR -> if (isArabic)
                Pair("صلاة الظهر", "حان وقت صلاة الظهر")
            else
                Pair("Dhuhr Prayer", "It's time for Dhuhr prayer")
            PrayerType.ASR -> if (isArabic)
                Pair("صلاة العصر", "حان وقت صلاة العصر")
            else
                Pair("Asr Prayer", "It's time for Asr prayer")
            PrayerType.MAGHRIB -> if (isArabic)
                Pair("صلاة المغرب", "حان وقت صلاة المغرب")
            else
                Pair("Maghrib Prayer", "It's time for Maghrib prayer")
            PrayerType.ISHA -> if (isArabic)
                Pair("صلاة العشاء", "حان وقت صلاة العشاء")
            else
                Pair("Isha Prayer", "It's time for Isha prayer")
        }
    }
}
