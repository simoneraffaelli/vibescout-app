package ooo.simone.vibescout.core.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val SPFileName = "VibeScoutSharedPrefs"
private const val authKeySpKey = "auth-key"

object SharedPreferencesManager {
    private fun sharedPreferences(ctx: Context): SharedPreferences {
        return ctx.getSharedPreferences(SPFileName, Context.MODE_PRIVATE)
    }

    fun getAuthKeySP(ctx: Context): String? {
        return sharedPreferences(ctx).getString(authKeySpKey, null)
    }

    fun setAuthKeySP(ctx: Context, value: String) {
        sharedPreferences(ctx).edit { putString(authKeySpKey, value) }
    }

    fun clear(ctx: Context) {
        sharedPreferences(ctx).edit { clear() }
    }
}
