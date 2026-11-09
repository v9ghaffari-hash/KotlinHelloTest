package ir.yadavaryar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "${context.packageName}.STOP_RINGING") {
            context.stopService(Intent(context, AlarmRingingService::class.java))
            return
        }

        val id = intent?.getIntExtra("schedule_id", -1) ?: -1
        if (id < 0) return
        val item = ScheduleRepository.getAll(context).firstOrNull { it.id == id } ?: return
        if (!item.enabled) return

        if (item.daily) {
            AlarmScheduler.schedule(context, item)
        } else {
            ScheduleRepository.upsert(context, item.copy(enabled = false))
        }

        if (item.kind == KIND_ALARM) {
            val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                putExtra("title", item.title)
                putExtra("message", item.message)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (_: Exception) {
                // If background service start is blocked, at least show a notification.
                NotificationHelper.showReminder(context, item)
            }
        } else {
            NotificationHelper.showReminder(context, item)
        }
    }
}
