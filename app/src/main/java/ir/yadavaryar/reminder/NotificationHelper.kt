package ir.yadavaryar.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object NotificationHelper {
    const val CHANNEL_REMINDERS = "yad_reminders"
    const val CHANNEL_RINGING = "yad_ringing"
    const val RINGING_NOTIFICATION_ID = 8801

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            "یادآورهای اعمال",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "اعلان زمان انجام کارها همراه با پیام‌های امام‌زمانی"
            enableVibration(true)
        }
        val ringing = NotificationChannel(
            CHANNEL_RINGING,
            "زنگ‌های هشدار در حال پخش",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "اعلان ثابت هنگام پخش زنگ هشدار"
            setSound(null, null)
            setShowBadge(false)
        }
        manager.createNotificationChannel(reminders)
        manager.createNotificationChannel(ringing)
    }

    fun showReminder(context: Context, item: ScheduleItem) {
        createChannels(context)
        val openIntent = PendingIntent.getActivity(
            context, item.id + 10_000,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_REMINDERS)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("وقتِ ${item.title} رسیده است")
            .setContentText(item.message)
            .setStyle(Notification.BigTextStyle().bigText("${item.title}\n${item.message}"))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(item.id, notification)
    }

    fun ringingNotification(context: Context, title: String, message: String): Notification {
        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "${context.packageName}.STOP_RINGING"
        }
        val stopPending = PendingIntent.getBroadcast(
            context, 88_801, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPending = PendingIntent.getActivity(
            context, 88_802, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_RINGING)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context).setPriority(Notification.PRIORITY_HIGH)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("زنگ هشدار")
            .setContentText(title)
            .setStyle(Notification.BigTextStyle().bigText("$title\n$message"))
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_media_pause, "خاموش کردن زنگ", stopPending)
            .build()
    }
}
