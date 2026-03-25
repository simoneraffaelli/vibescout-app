package ooo.simone.vibescout.core.api

import ooo.simone.vibescout.core.api.models.HeartbeatResponse
import ooo.simone.vibescout.core.api.models.TrackRequest
import ooo.simone.vibescout.core.api.models.TrackResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface VibescoutApiClient {

    @POST("api/tracks")
    suspend fun registerTrack(
        @Header("Authorization") auth: String,
        @Body body: TrackRequest
    ): Response<TrackResponse>

    @POST("/api/heartbeat")
    suspend fun heartbeat(
        @Header("Authorization") auth: String
    ): Response<HeartbeatResponse>
}