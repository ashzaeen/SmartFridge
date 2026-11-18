package com.team8.smartfridge.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.team8.smartfridge.data.FirebaseRepository
import com.team8.smartfridge.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle         : AuthState()
    object Loading      : AuthState()
    object Success      : AuthState()
    object ResetSent    : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FirebaseRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _loggedInUser = MutableStateFlow<User?>(null)
    val loggedInUser: StateFlow<User?> = _loggedInUser

    private val _isCheckingAuth = MutableStateFlow(true)
    val isCheckingAuth: StateFlow<Boolean> = _isCheckingAuth

    init {
        val uid = repo.currentUid
        if (uid != null) {
            viewModelScope.launch {
                try {
                    _loggedInUser.value = repo.getUserProfile(uid)
                } catch (_: Exception) { }
                _isCheckingAuth.value = false
            }
        } else {
            _isCheckingAuth.value = false
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val uid = repo.login(email.trim(), password)
                _loggedInUser.value = repo.getUserProfile(uid)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e))
            }
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        age: Int,
        sex: String,
        weight: Float,
        height: Float,
        bloodType: String,
        allergies: String,
        dietPreference: String,
        dailyCalorieGoal: Int,
        dailyWaterGoal: Int
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val uid = repo.register(email.trim(), password)
                val user = User(
                    firstName        = firstName,
                    lastName         = lastName,
                    email            = email.trim(),
                    age              = age,
                    sex              = sex,
                    weight           = weight,
                    height           = height,
                    bloodType        = bloodType,
                    allergies        = allergies,
                    dietPreference   = dietPreference,
                    dailyCalorieGoal = dailyCalorieGoal,
                    dailyWaterGoal   = dailyWaterGoal
                )
                repo.saveUserProfile(uid, user)
                _loggedInUser.value = user
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e))
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                repo.sendPasswordResetEmail(email.trim())
                _authState.value = AuthState.ResetSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e))
            }
        }
    }

    fun updateUser(updatedUser: User) {
        val uid = repo.currentUid ?: return
        viewModelScope.launch {
            try {
                repo.updateUserProfile(uid, updatedUser)
                _loggedInUser.value = updatedUser
            } catch (_: Exception) { }
        }
    }

    fun logout() {
        repo.logout()
        _loggedInUser.value = null
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    private fun friendlyError(e: Exception): String =
        when {
            e.message?.contains("email address is already in use") == true ->
                "An account with this email already exists"
            e.message?.contains("badly formatted") == true ->
                "Invalid email address"
            e.message?.contains("password is invalid") == true ||
            e.message?.contains("no user record") == true ->
                "Invalid email or password"
            e.message?.contains("weak password") == true ->
                "Password must be at least 6 characters"
            else -> e.message ?: "Something went wrong. Please try again."
        }
}
