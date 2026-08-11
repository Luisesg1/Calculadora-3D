package com.print3d.calculator.data.auth

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Row stored in the Supabase `profiles` table. Keyed by the auth user id so RLS can enforce
 * `id = auth.uid()`. Timestamps are filled by DB defaults/triggers, not sent from the client.
 */
@Serializable
data class ProfileRow(
    val id: String,
    val email: String
)

/**
 * Persists a minimal user record in Supabase Postgres whenever the user signs in / signs up.
 * This is intentionally scoped to identity only — full data sync (materials, quotes, etc.) is
 * still local-only Room. Failures never block auth; they are swallowed and returned as a Result.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) {
    /** Upserts the currently authenticated user's profile row. No-op if no active session. */
    suspend fun syncCurrentUser(): Result<Unit> = runCatching {
        val user = auth.currentUserOrNull() ?: return@runCatching
        postgrest.from("profiles").upsert(
            ProfileRow(id = user.id, email = user.email.orEmpty())
        )
    }
}
