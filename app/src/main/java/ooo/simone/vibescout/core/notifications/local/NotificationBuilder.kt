package ooo.simone.vibescout.core.notifications.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import ooo.simone.vibescout.MainActivity
import ooo.simone.vibescout.R
import ooo.simone.vibescout.core.notifChannelId
import ooo.simone.vibescout.core.workerNotifId

fun buildNotification(ctx: Context, title: String, text: String, notificationId: Int): Notification {
    createNotificationChannel(ctx)
    return buildDetailedNotification(
        ctx = ctx,
        title = title,
        text = text,
        notificationId = notificationId,
    )
}

fun buildDetailedNotification(
    ctx: Context,
    title: String,
    text: String,
    notificationId: Int,
    @DrawableRes icon: Int = R.drawable.baseline_hearing_24,
    priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
    silent: Boolean = false,
    vibrate: Boolean = true,
    contentIntent: PendingIntent? = createPendingIntent(ctx, notificationId),
    ): Notification {
    createNotificationChannel(ctx, importance, vibrate)
    return NotificationCompat.Builder(ctx, notifChannelId)
        .setSmallIcon(icon)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(priority)
        .setSilent(silent)
        .setContentIntent(createPendingIntent(ctx, notificationId))
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .build()
}

fun buildWorkerServiceNotification(
    ctx: Context,
    text: String,
    cancelIntent: PendingIntent,
): NotificationCompat.Builder {
    createNotificationChannel(ctx)

    // Create the notification itself
    return NotificationCompat.Builder(ctx, notifChannelId)
        .setContentTitle(ctx.getString(R.string.worker_name))
        .setContentText(text)
        .setOngoing(true)
        .setContentIntent(createPendingIntent(ctx, workerNotifId))
        .setForegroundServiceBehavior(Notification. FOREGROUND_SERVICE_IMMEDIATE)
        .setSmallIcon(R.drawable.baseline_hearing_24)
        .addAction(R.drawable.outline_crisis_alert_24, ctx.resources.getString(R.string.worker_cancel_notification), cancelIntent)
        .setOnlyAlertOnce(true)
}


private fun createNotificationChannel(context: Context, importance: Int = NotificationManager.IMPORTANCE_DEFAULT, vibrate: Boolean = true) {
    val name = "Default"
    val descriptionText = "Default"
    val channel = NotificationChannel(
        notifChannelId,
        name,
        importance,
        ).apply {
        description = descriptionText
        enableVibration(true)
        vibrationPattern =
            longArrayOf(100, 100, 100)
        enableVibration(vibrate)
    }
    // Register the channel with the system
    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

private fun createPendingIntent(context: Context, notificationId: Int): PendingIntent? {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
