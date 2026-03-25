package ooo.simone.vibescout.core.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import kotlinx.coroutines.delay
import ooo.simone.vibescout.R
import ooo.simone.vibescout.core.api.ApiManager
import ooo.simone.vibescout.core.api.ApiManager.heartbeat
import ooo.simone.vibescout.core.api.models.TrackRequest
import ooo.simone.vibescout.core.audio.AudioRecorder
import ooo.simone.vibescout.core.audioFileDuration
import ooo.simone.vibescout.core.exeptions.ReleaseWifiLockException
import ooo.simone.vibescout.core.log.d
import ooo.simone.vibescout.core.log.w
import ooo.simone.vibescout.core.notifications.local.buildWorkerServiceNotification
import ooo.simone.vibescout.core.notifications.local.notify
import ooo.simone.vibescout.core.notifications.local.stoppedMonitoringNotification
import ooo.simone.vibescout.core.audio.AudioRecognizer
import ooo.simone.vibescout.core.audio.MatchChecker
import ooo.simone.vibescout.core.data.Track
import ooo.simone.vibescout.core.exeptions.NoAuthKeyException
import ooo.simone.vibescout.core.exeptions.NoNetworkException
import ooo.simone.vibescout.core.exeptions.NoPermissionsException
import ooo.simone.vibescout.core.network.isNetworkOnline
import ooo.simone.vibescout.core.preferences.SharedPreferencesManager
import ooo.simone.vibescout.core.stoppedWorkerNotifId
import ooo.simone.vibescout.core.workerDelayConst
import ooo.simone.vibescout.core.workerNotifId

class VibeScoutWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    private val tag by lazy { this::class.java.simpleName }
    private lateinit var wifiLock: WifiManager.WifiLock
    private var workerDelay = workerDelayConst


    private val authKey: String?
        get() = SharedPreferencesManager.getAuthKeySP(applicationContext)

    override suspend fun doWork(): Result {
        runCatching {
            displayServiceNotification(createForegroundInfo(this.applicationContext.resources.getString(
                R.string.worker_running)))
            acquireWifiLock()

            while (true) {
                if (isStopped) {
                    d("stop detected")
                    releaseResources()
                    return Result.success()
                }

                if (authKey.isNullOrEmpty()) throw NoAuthKeyException()
                if (!permissionGranted()) throw NoPermissionsException()

                runCatching {
                    if (!isNetworkOnline(applicationContext)) throw NoNetworkException()
                    workerDelay = workerDelayConst
                    performHeartbeat()
                    doStuff()
                }.onFailure {
                    w(it)
                    when (it) {
                        is NoNetworkException -> workerDelay = 60000L
                    }
                }

                delay(workerDelay)
            }

        }.onFailure {
            w(it)
            releaseResources()
            return Result.failure()
        }

        releaseResources()
        return Result.success()
    }

    private suspend fun performHeartbeat(): Boolean {
        val hb = heartbeat(authKey!!)
        return hb.isSuccessful
    }

    private fun permissionGranted(): Boolean {
        return XXPermissions.isGrantedPermissions(
            applicationContext,
            arrayOf(PermissionLists.getPostNotificationsPermission(), PermissionLists.getRecordAudioPermission())
        )
    }

    private suspend fun doStuff() {
        val pcmArray = recordAudio()
        val json = analyzeAudio(pcmArray)
        val track = checkIfMatched(json)
        val matched = track != null
        if (matched) sendTrackToApi(track)
    }

    private suspend fun sendTrackToApi(track: Track): Boolean {
        val resp = ApiManager.tracks(authKey!!, body = TrackRequest(track.title, track.artist))
        return resp.isSuccessful
    }

    private fun checkIfMatched(json: String): Track? {
        return MatchChecker.checkIfMatched(json)
    }

    private fun analyzeAudio(pcmArray: FloatArray): String {
        return AudioRecognizer.analyzeAudio(pcmArray)
    }

    private fun recordAudio(): FloatArray {
        return AudioRecorder.recordAudio(audioFileDuration) ?: floatArrayOf()
    }


    private suspend fun displayServiceNotification(foregroundInfo: ForegroundInfo) {
        setForeground(foregroundInfo)
    }

    private fun createForegroundInfo(
        notificationText: String
    ): ForegroundInfo {
        val notification = buildWorkerServiceNotification(
            applicationContext,
            notificationText,
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(workerNotifId, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            ForegroundInfo(workerNotifId, notification.build())
        }
    }

    private fun acquireWifiLock() {
        if (!this::wifiLock.isInitialized) {
            val wifiManager: WifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, tag)
        }

        wifiLock.setReferenceCounted(false)
        if (!wifiLock.isHeld) {
            wifiLock.acquire()
        }
    }

    private fun releaseWifiLock() {
        if (this::wifiLock.isInitialized && wifiLock.isHeld) {
            wifiLock.release()
        } else {
            w(ReleaseWifiLockException())
        }
    }

    private fun releaseResources() {
        releaseWifiLock()

        notify(applicationContext, stoppedWorkerNotifId, stoppedMonitoringNotification(applicationContext))
    }
}