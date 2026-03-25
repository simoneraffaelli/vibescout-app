package ooo.simone.vibescout.core.workers

import android.content.Context
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import ooo.simone.vibescout.R

fun startWorker(ctx: Context) {
    val tag = ctx.resources.getString(R.string.worker_tag)
    createWorker(ctx, tag)
}

fun stopWorker(ctx: Context) {
    val tag = ctx.resources.getString(R.string.worker_tag)
    WorkManager.getInstance(ctx).cancelAllWorkByTag(tag)
}

fun getWorkerStatus(ctx: Context): LiveData<WorkInfo?> {
    val tag = ctx.resources.getString(R.string.worker_tag)
    return WorkManager.getInstance(ctx)
        .getWorkInfosForUniqueWorkLiveData(tag).map { it.takeIf { it.isNotEmpty() }?.first() }
}


private fun createWorker(ctx: Context, tag: String) {
    val builder = OneTimeWorkRequestBuilder<VibeScoutWorker>()
        .addTag(tag)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
    }

    val oneTimeRequest: OneTimeWorkRequest = builder.build()

    WorkManager.getInstance(ctx).enqueueUniqueWork(
        tag,
        ExistingWorkPolicy.KEEP,
        oneTimeRequest
    )
}