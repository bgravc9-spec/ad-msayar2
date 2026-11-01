package com.adimsayar.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import java.text.NumberFormat
import java.util.Locale

/**
 * Kullanıcı başlatınca çalışan, devamlı bildirim gösteren Health foreground service.
 * Android pil yönetimi ve sensör desteği marka/model bazında değişebilir.
 */
class StepTrackingService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var repo: StepRepository
    private var sensor: Sensor? = null
    private var lastNotificationAt = 0L

    override fun onCreate() {
        super.onCreate()
        repo = StepRepository(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            repo.stopTracking()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!repo.trackingEnabled() || !hasActivityPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            repo.stopTracking()
            stopSelf()
            return START_NOT_STICKY
        }
        // Birden fazla START intent'i gelirse eski dinleyiciyi kaldır.
        sensorManager.unregisterListener(this)
        val selectedSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        sensor = selectedSensor
        if (selectedSensor == null || !sensorManager.registerListener(this, selectedSensor, SensorManager.SENSOR_DELAY_NORMAL)) {
            repo.stopTracking()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> repo.onCounterValue(event.values[0].toLong())
            Sensor.TYPE_STEP_DETECTOR -> repo.onDetectedStep()
            else -> return
        }
        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastNotificationAt >= 25_000L) {
            lastNotificationAt = elapsed
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, createNotification())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasActivityPermission(): Boolean = Build.VERSION.SDK_INT < 29 ||
        checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW
        ).apply { description = getString(R.string.notification_description) }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, StepTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val steps = NumberFormat.getNumberInstance(Locale("tr", "TR")).format(repo.stepsToday())
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Color.rgb(53, 209, 233))
            .setContentTitle("ADIM SAYAR • Takip açık")
            .setContentText("Bugün $steps adım • Adımlar takip ediliyor")
            .setContentIntent(openIntent)
            .addAction(Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_notification),
                "Durdur", stopIntent
            ).build())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val ACTION_START = "com.adimsayar.app.START_TRACKING"
        const val ACTION_STOP = "com.adimsayar.app.STOP_TRACKING"
        private const val CHANNEL_ID = "adim_sayar_tracking"
        private const val NOTIFICATION_ID = 4120
    }
}
