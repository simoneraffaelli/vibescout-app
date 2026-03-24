package ooo.simone.vibescout.core.notifications.local

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import ooo.simone.vibescout.R
import ooo.simone.vibescout.core.log.w
import ooo.simone.vibescout.core.stoppedWorkerNotifId

@SuppressLint("MissingPermission")
fun notify(context: Context, notificationId: Int, notification: Notification) {
    with(NotificationManagerCompat.from(context)) {
        runCatching {
            notify(notificationId, notification)
        }.onFailure {
            w(it)
        }
    }
}

fun stoppedMonitoringNotification(ctx: Context): Notification {
    return buildDetailedNotification(
        ctx = ctx,
        title = ctx.resources.getString(R.string.worker_stopped_alert_notification_title),
        text = ctx.resources.getString(R.string.worker_stopped_alert_notification_title),
        notificationId = stoppedWorkerNotifId,
        icon = R.drawable.hearing_disabled_24px

    )
}

