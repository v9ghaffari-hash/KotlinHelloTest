package ir.yadavaryar.reminder

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder

class AlarmRingingService : Service() {
    private var player: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.createChannels(this)
        val title = intent?.getStringExtra("title") ?: "زمان انجام عمل فرا رسیده است"
        val message = intent?.getStringExtra("message") ?: "یا صاحب‌الزمان، یاریم کنید در مسیر رضایت شما قدم بردارم."

        startForeground(
            NotificationHelper.RINGING_NOTIFICATION_ID,
            NotificationHelper.ringingNotification(this, title, message)
        )
        startAlarmSound()
        return START_NOT_STICKY
    }

    private fun startAlarmSound() {
        if (player?.isPlaying == true) return
        stopAlarmSound()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingingService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            stopAlarmSound()
        }
    }

    private fun stopAlarmSound() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    override fun onDestroy() {
        stopAlarmSound()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
