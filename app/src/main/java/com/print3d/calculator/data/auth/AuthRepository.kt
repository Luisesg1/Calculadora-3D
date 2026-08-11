package com.print3d.calculator.data.auth

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Machine-readable auth failure — the UI maps each value to a localized string. */
enum class AuthError {
    NETWORK, EMAIL_TAKEN, INVALID_CREDENTIALS, EMAIL_NOT_CONFIRMED, WEAK_PASSWORD,
    INVALID_EMAIL, EMPTY_PASSWORD, PASSWORD_POLICY, PASSWORD_MISMATCH, UNKNOWN
}

/** Outcome of a sign-up attempt. */
sealed interface SignUpResult {
    /** Account created and session active — user is signed in. */
    data object SignedIn : SignUpResult
    /** Account created but Supabase requires email confirmation before signing in. */
    data object NeedsEmailConfirmation : SignUpResult
    data class Error(val error: AuthError) : SignUpResult
}

sealed interface SignInResult {
    data object Success : SignInResult
    data class Error(val error: AuthError) : SignInResult
}

@Singleton
class AuthRepository @Inject constructor(
    private val auth: Auth,
    private val profiles: ProfileRepository,
    private val cloudSync: com.print3d.calculator.data.sync.CloudSync
) {
    val sessionStatus: StateFlow<SessionStatus> = auth.sessionStatus

    /** True once a valid session exists. Emits after auto-load from storage completes. */
    val isSignedIn: Flow<Boolean> = sessionStatus.map { it is SessionStatus.Authenticated }

    /** Email of the signed-in user, or null. Observable across session changes. */
    val userEmail: Flow<String?> = sessionStatus.map {
        (it as? SessionStatus.Authenticated)?.session?.user?.email
    }

    suspend fun signUp(email: String, password: String): SignUpResult = try {
        auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        // If confirmations are OFF, a session exists immediately; if ON, it does not.
        if (auth.currentSessionOrNull() != null) {
            postSignIn()
            SignUpResult.SignedIn
        } else SignUpResult.NeedsEmailConfirmation
    } catch (e: Exception) {
        SignUpResult.Error(classify(e))
    }

    suspend fun signIn(email: String, password: String): SignInResult = try {
        auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        postSignIn()
        SignInResult.Success
    } catch (e: Exception) {
        SignInResult.Error(classify(e))
    }

    /**
     * Signs in with a Google ID token obtained via Credential Manager. [rawNonce] is the
     * un-hashed nonce; Supabase verifies it against the SHA-256 hash embedded in the token.
     */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String): SignInResult = try {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
            this.nonce = rawNonce
        }
        postSignIn()
        SignInResult.Success
    } catch (e: Exception) {
        SignInResult.Error(classify(e))
    }

    /** Runs after any successful sign-in: register the profile and restore the cloud snapshot. */
    private suspend fun postSignIn() {
        profiles.syncCurrentUser()
        cloudSync.pull()
    }

    suspend fun signOut() {
        // Flush latest local state to the cloud before the session (and its user_id) is gone.
        // Only wipe local data if the flush actually landed — otherwise an offline sign-out would
        // lose data that never made it to the backup.
        val pushed = cloudSync.push()
        auth.signOut()
        if (pushed) cloudSync.clearLocal()
    }

    /** Classifies raw Supabase/network exceptions; the UI localizes the result. */
    private fun classify(e: Exception): AuthError {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("resolve host", true) || msg.contains("Unable to", true) ||
                msg.contains("timeout", true) || msg.contains("Failed to connect", true) ||
                msg.contains("Connection", true) -> AuthError.NETWORK
            msg.contains("already registered", true) || msg.contains("already been registered", true) ||
                msg.contains("User already", true) -> AuthError.EMAIL_TAKEN
            msg.contains("Invalid login credentials", true) -> AuthError.INVALID_CREDENTIALS
            msg.contains("Email not confirmed", true) -> AuthError.EMAIL_NOT_CONFIRMED
            msg.contains("Password should be at least", true) -> AuthError.WEAK_PASSWORD
            else -> AuthError.UNKNOWN
        }
    }
}
