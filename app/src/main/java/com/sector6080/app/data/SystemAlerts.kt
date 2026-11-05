package com.sector6080.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sector6080.app.R
import com.sector6080.app.model.GameNotification
import java.util.concurrent.TimeUnit

/** Annuncia le notifiche nuove una volta sola: con l'app aperta le suona l'app, altrimenti le mostra Android. */
object SystemAlerts {
    private const val OLD_CHANNEL = "eventi"
    private const val CHANNEL = "eventi_suono"
    private const val PREFERENCES = "notifiche"
    private const val LAST_SHOWN = "ultima"
    private const val MAX_AT_ONCE = 5

    /** Le notifiche non ancora annunciate. Da qui in poi contano come annunciate. */
    fun takeFresh(context: Context, notifications: List<GameNotification>): List<GameNotification> {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val lastShown = preferences.getLong(LAST_SHOWN, 0)
        val newest = notifications.maxOfOrNull { it.id } ?: return emptyList()
        preferences.edit { putLong(LAST_SHOWN, maxOf(lastShown, newest)) }
        return notifications.filter { it.id > lastShown && !it.read }
    }

    // Il permesso lo controlla allowed(), che lint non riesce a seguire
    @SuppressLint("MissingPermission")
    fun show(context: Context, notifications: List<GameNotification>) {
        val fresh = takeFresh(context, notifications)
        if (fresh.isEmpty() || !allowed(context)) return
        createChannel(context)
        val manager = NotificationManagerCompat.from(context)
        fresh.takeLast(MAX_AT_ONCE).forEach { notification ->
            val alert = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openApp(context))
                .setAutoCancel(true)
                .build()
            manager.notify(notification.id.toInt(), alert)
        }
    }

    private fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun createChannel(context: Context) {
        // Android non cambia il suono di un canale esistente: per questo ha un nome nuovo
        val channel = NotificationChannel(CHANNEL, "Eventi di gioco", NotificationManager.IMPORTANCE_DEFAULT)
        channel.setSound(
            "android.resource://${context.packageName}/${R.raw.sfx_alert}".toUri(),
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(OLD_CHANNEL)
        manager.createNotificationChannel(channel)
    }

    private fun openApp(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}

/** Controlla le notifiche anche ad app chiusa. Android lo fa girare circa ogni 15 minuti. */
class AlertsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val remote = RemoteGame.shared
        return runCatching {
            if (remote.hasAccount()) SystemAlerts.show(applicationContext, remote.notifications())
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AlertsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("notifiche", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
