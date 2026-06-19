package com.example.todoquadrant.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.todoquadrant.MainActivity
import com.example.todoquadrant.R
import com.example.todoquadrant.data.Quadrant
import com.example.todoquadrant.data.ReminderMode
import com.example.todoquadrant.data.TodoEntity
import com.example.todoquadrant.data.quadrant

object NotificationHelper {
    private const val CHANNEL_NOTIFICATION = "todo_reminders_notification_v3"
    private const val CHANNEL_VIBRATE = "todo_reminders_vibrate_v3"
    private const val CHANNEL_ALARM = "todo_reminders_alarm_v3"
    private const val CHANNEL_ALARM_VIBRATE = "todo_reminders_alarm_vibrate_v3"
    private val VIBRATION_PATTERN = longArrayOf(0, 450, 160, 450)
    const val EXTRA_OPEN_TODO_ID = "extra_open_todo_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        manager.createNotificationChannel(
            createChannel(
                id = CHANNEL_NOTIFICATION,
                name = context.getString(R.string.reminder_mode_notification),
                description = context.getString(R.string.notification_channel_description),
                sound = null,
                vibrate = false,
            ),
        )
        manager.createNotificationChannel(
            createChannel(
                id = CHANNEL_VIBRATE,
                name = context.getString(R.string.reminder_mode_vibrate),
                description = context.getString(R.string.notification_channel_description),
                sound = null,
                vibrate = true,
            ),
        )
        manager.createNotificationChannel(
            createChannel(
                id = CHANNEL_ALARM,
                name = context.getString(R.string.reminder_mode_alarm),
                description = context.getString(R.string.notification_channel_description),
                sound = alarmSound,
                vibrate = false,
            ),
        )
        manager.createNotificationChannel(
            createChannel(
                id = CHANNEL_ALARM_VIBRATE,
                name = context.getString(R.string.reminder_mode_alarm_vibrate),
                description = context.getString(R.string.notification_channel_description),
                sound = alarmSound,
                vibrate = true,
            ),
        )
    }

    private fun createChannel(
        id: String,
        name: String,
        description: String,
        sound: android.net.Uri?,
        vibrate: Boolean,
    ): NotificationChannel {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        return NotificationChannel(
            id,
            name,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            this.description = description
            setSound(sound, if (sound == null) null else attributes)
            enableVibration(vibrate)
            vibrationPattern = if (vibrate) VIBRATION_PATTERN else null
        }
    }

    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun showReminder(context: Context, todo: TodoEntity) {
        ensureChannel(context)
        if (!canPostNotifications(context)) {
            return
        }

        val openIntent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_TODO_ID, todo.id)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val contentIntent = PendingIntent.getActivity(
            context,
            todo.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val detail = todo.note?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.notification_detail_quadrant, quadrantLabel(context, todo.quadrant))

        val mode = todo.reminderMode
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelIdFor(mode))
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(todo.title)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(
                if (mode == ReminderMode.ALARM || mode == ReminderMode.ALARM_VIBRATE) {
                    NotificationCompat.CATEGORY_ALARM
                } else {
                    NotificationCompat.CATEGORY_REMINDER
                },
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    if (mode == ReminderMode.ALARM || mode == ReminderMode.ALARM_VIBRATE) {
                        setSound(alarmSound)
                    }
                    if (mode == ReminderMode.VIBRATE || mode == ReminderMode.ALARM_VIBRATE) {
                        setVibrate(VIBRATION_PATTERN)
                    }
                }
            }
            .build()

        NotificationManagerCompat.from(context).notify(todo.id.toInt(), notification)
    }

    private fun channelIdFor(mode: String): String =
        when (mode) {
            ReminderMode.VIBRATE -> CHANNEL_VIBRATE
            ReminderMode.ALARM -> CHANNEL_ALARM
            ReminderMode.ALARM_VIBRATE -> CHANNEL_ALARM_VIBRATE
            else -> CHANNEL_NOTIFICATION
        }

    private fun quadrantLabel(context: Context, quadrant: Quadrant): String =
        when (quadrant) {
            Quadrant.ImportantUrgent -> context.getString(R.string.quadrant_important_urgent)
            Quadrant.ImportantNotUrgent -> context.getString(R.string.quadrant_important_not_urgent)
            Quadrant.UrgentNotImportant -> context.getString(R.string.quadrant_urgent_not_important)
            Quadrant.NotImportantNotUrgent -> context.getString(R.string.quadrant_not_important_not_urgent)
        }
}
