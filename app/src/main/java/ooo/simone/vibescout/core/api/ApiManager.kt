package ooo.simone.vibescout.core.api

import ooo.simone.vibescout.core.api.models.TrackRequest
import ooo.simone.vibescout.core.api.models.TrackResponse
import retrofit2.Response

object ApiManager{
    private val apiManager =  ApiClient.apiManager

    suspend fun tracks(auth: String, body: TrackRequest): Response<TrackResponse> {
        return apiManager!!.registerTrack("Bearer $auth", body)
    }
}