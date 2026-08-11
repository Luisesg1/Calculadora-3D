package com.print3d.calculator.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.authStore: DataStore<Preferences> by preferencesDataStore("auth_session")

/**
 * Persists the Supabase [UserSession] in a dedicated DataStore so the user stays signed in
 * across app restarts. Serialized as JSON — avoids relying on multiplatform-settings defaults.
 */
class AuthSessionManager(private val context: Context) : SessionManager {

    private val key = stringPreferencesKey("session_json")
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        context.authStore.edit { it[key] = json.encodeToString(UserSession.serializer(), session) }
    }

    override suspend fun loadSession(): UserSession? {
        val raw = context.authStore.data.first()[key] ?: return null
        return runCatching { json.decodeFromString(UserSession.serializer(), raw) }.getOrNull()
    }

    override suspend fun deleteSession() {
        context.authStore.edit { it.remove(key) }
    }
}
