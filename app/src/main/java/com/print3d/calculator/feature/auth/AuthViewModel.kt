package com.print3d.calculator.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.print3d.calculator.BuildConfig
import com.print3d.calculator.data.auth.AuthError
import com.print3d.calculator.data.auth.AuthRepository
import com.print3d.calculator.data.auth.GoogleSignInCancelled
import com.print3d.calculator.data.auth.GoogleSignInHelper
import com.print3d.calculator.data.auth.SignInResult
import com.print3d.calculator.data.auth.SignUpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Password rules enforced client-side before hitting Supabase. */
object PasswordPolicy {
    const val MIN_LENGTH = 8
    fun hasMinLength(p: String) = p.length >= MIN_LENGTH
    fun hasUppercase(p: String) = p.any { it.isUpperCase() }
    fun hasLowercase(p: String) = p.any { it.isLowerCase() }
    fun hasDigit(p: String) = p.any { it.isDigit() }
    fun isValid(p: String) = hasMinLength(p) && hasUppercase(p) && hasLowercase(p) && hasDigit(p)
}

fun isValidEmail(email: String): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

data class AuthUiState(
    val loading: Boolean = false,
    val error: AuthError? = null,
    /** True when sign-up succeeded but email confirmation is pending. */
    val needsConfirmation: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    val isSignedIn: StateFlow<Boolean> = repo.isSignedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userEmail: StateFlow<String?> = repo.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun signOut() = viewModelScope.launch { repo.signOut() }

    fun clearMessages() = _ui.update { it.copy(error = null, needsConfirmation = false) }

    fun signIn(email: String, password: String) {
        if (!isValidEmail(email)) { _ui.update { it.copy(error = AuthError.INVALID_EMAIL) }; return }
        if (password.isBlank()) { _ui.update { it.copy(error = AuthError.EMPTY_PASSWORD) }; return }
        _ui.update { it.copy(loading = true, error = null, needsConfirmation = false) }
        viewModelScope.launch {
            when (val r = repo.signIn(email, password)) {
                is SignInResult.Success -> _ui.update { it.copy(loading = false) }
                is SignInResult.Error -> _ui.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    /** Google sign-in via Credential Manager. [context] must be an Activity (shows the chooser). */
    fun signInWithGoogle(context: Context) {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) { _ui.update { it.copy(error = AuthError.UNKNOWN) }; return }
        _ui.update { it.copy(loading = true, error = null, needsConfirmation = false) }
        viewModelScope.launch {
            try {
                val cred = GoogleSignInHelper.getCredential(context, clientId)
                when (val r = repo.signInWithGoogle(cred.idToken, cred.rawNonce)) {
                    is SignInResult.Success -> _ui.update { it.copy(loading = false) }
                    is SignInResult.Error -> _ui.update { it.copy(loading = false, error = r.error) }
                }
            } catch (e: GoogleSignInCancelled) {
                _ui.update { it.copy(loading = false) } // user backed out — no error banner
            } catch (e: Exception) {
                // Log the real cause: a silent Google loop is almost always a config problem
                // (Play App Signing SHA-1/-256 missing from the Google Cloud OAuth client, or a
                // wrong web client id) rather than a code bug. This makes it visible in logcat /
                // Play pre-launch reports instead of failing silently.
                android.util.Log.e("GoogleSignIn", "Credential Manager sign-in failed", e)
                _ui.update { it.copy(loading = false, error = AuthError.UNKNOWN) }
            }
        }
    }

    fun signUp(email: String, password: String, confirm: String) {
        if (!isValidEmail(email)) { _ui.update { it.copy(error = AuthError.INVALID_EMAIL) }; return }
        if (!PasswordPolicy.isValid(password)) {
            _ui.update { it.copy(error = AuthError.PASSWORD_POLICY) }; return
        }
        if (password != confirm) { _ui.update { it.copy(error = AuthError.PASSWORD_MISMATCH) }; return }
        _ui.update { it.copy(loading = true, error = null, needsConfirmation = false) }
        viewModelScope.launch {
            when (val r = repo.signUp(email, password)) {
                is SignUpResult.SignedIn -> _ui.update { it.copy(loading = false) }
                is SignUpResult.NeedsEmailConfirmation -> _ui.update {
                    it.copy(loading = false, needsConfirmation = true)
                }
                is SignUpResult.Error -> _ui.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}
