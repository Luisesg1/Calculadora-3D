package com.print3d.calculator.data.quota

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.guestQuotaStore by preferencesDataStore("guest_quota")

/**
 * Daily quote quota for users who are NOT signed in: [DAILY_LIMIT] new quotes per rolling
 * 24h window. The window starts at the first attempt; 24h after that, the count fully resets.
 * Signed-in users are never limited — this manager is only consulted when there's no session.
 */
@Singleton
class GuestQuotaManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val COUNT = intPreferencesKey("count")
    private val WINDOW_START = longPreferencesKey("window_start")

    /** Quotes still available in the current window (0..DAILY_LIMIT). */
    suspend fun remaining(): Int {
        val prefs = context.guestQuotaStore.data.first()
        val start = prefs[WINDOW_START] ?: 0L
        val count = prefs[COUNT] ?: 0
        val effective = if (windowExpired(start)) 0 else count
        return (DAILY_LIMIT - effective).coerceAtLeast(0)
    }

    /**
     * Try to spend one attempt. Returns true and increments if the user is under the limit;
     * returns false (and changes nothing) once the daily limit is reached.
     */
    suspend fun tryConsume(): Boolean {
        var allowed = false
        context.guestQuotaStore.edit { prefs ->
            val now = System.currentTimeMillis()
            val start = prefs[WINDOW_START] ?: 0L
            val expired = windowExpired(start)
            val count = if (expired) 0 else (prefs[COUNT] ?: 0)
            if (count < DAILY_LIMIT) {
                // Window begins on the first attempt of a fresh batch.
                if (count == 0) prefs[WINDOW_START] = now
                prefs[COUNT] = count + 1
                allowed = true
            } else {
                prefs[COUNT] = count
                allowed = false
            }
        }
        return allowed
    }

    private fun windowExpired(start: Long): Boolean =
        start == 0L || System.currentTimeMillis() - start >= WINDOW_MS

    companion object {
        const val DAILY_LIMIT = 3
        private const val WINDOW_MS = 24L * 60 * 60 * 1000
    }
}
