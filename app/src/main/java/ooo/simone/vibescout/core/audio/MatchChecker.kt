package ooo.simone.vibescout.core.audio

import com.google.gson.JsonParser
import ooo.simone.vibescout.core.data.Track
import ooo.simone.vibescout.core.log.w

object MatchChecker {
    fun checkIfMatched(json: String): Track? {
        runCatching {
            val root = JsonParser.parseString(json).asJsonObject

            // 1. Check if matches array has count > 1
            val matches = root.getAsJsonArray("matches")
            if (matches == null || matches.size() < 1) {
                return null
            }

            // 2. Check if track section exists
            val trackObj = root.getAsJsonObject("track") ?: return null

            // Build the track object
            val title = trackObj.get("title")?.asString ?: return null
            val artist = trackObj.get("subtitle")?.asString ?: return null

            return Track(title, artist)
        }.onFailure {
            w(it)
            return null
        }

        return null
    }
}
