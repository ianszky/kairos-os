package com.kairos.os.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.HttpRequestException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: Auth
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun signInWithGoogle() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.signInWith(Google)
                // The actual redirect will happen via deep link back to LauncherActivity
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapErrorMessage(e))
            }
        }
    }

    private fun mapErrorMessage(e: Exception): String {
        return when (e) {
            is RestException -> {
                // Often Supabase RestException has a descriptive error string in the body or standard description
                if (e.message?.contains("Invalid login credentials") == true) {
                    "Invalid email or password."
                } else if (e.message?.contains("already registered") == true) {
                    "This email is already registered."
                } else {
                    "Authentication failed. Please try again."
                }
            }
            is HttpRequestException, is UnknownHostException -> "Network error. Please check your connection."
            else -> "An unexpected error occurred."
        }
    }


    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapErrorMessage(e))
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                // If email confirmations are disabled, this logs the user in automatically
                // If enabled, we should probably handle an intermediate state
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapErrorMessage(e))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                // Ignore errors on sign out
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
