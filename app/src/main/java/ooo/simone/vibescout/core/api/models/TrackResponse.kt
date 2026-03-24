package ooo.simone.vibescout.core.api.models

data class TrackResponse(
    val id: Int,
    val title: String,
    val artist: String,
    val spottedAt: String,
    val deviceId: Int
)