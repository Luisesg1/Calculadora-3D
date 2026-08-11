package com.print3d.calculator.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

/** A Google ID token plus the raw nonce it was minted with (Supabase needs the un-hashed value). */
data class GoogleCredential(val idToken: String, val rawNonce: String)

/** Raised when the user cancels the Google chooser — the UI treats it as a silent no-op. */
class GoogleSignInCancelled : Exception()

/**
 * Drives the Android Credential Manager to obtain a Google ID token for Supabase sign-in.
 * Uses a nonce: a random raw value is hashed (SHA-256) into the token request, and the raw value
 * is handed back so Supabase can verify it. Must be called with an Activity [Context] (shows UI).
 */
object GoogleSignInHelper {

    suspend fun getCredential(context: Context, webClientId: String): GoogleCredential {
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = sha256(rawNonce)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // show all accounts, not only previously used
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = try {
            CredentialManager.create(context).getCredential(context, request)
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            throw GoogleSignInCancelled()
        }

        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
        return GoogleCredential(idToken = credential.idToken, rawNonce = rawNonce)
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
