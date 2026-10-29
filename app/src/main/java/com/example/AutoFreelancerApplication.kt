package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

object AppStartupState {
    @Volatile
    var errorMessage: String? = null
        private set

    fun recordFailure(error: Throwable) {
        errorMessage = error.localizedMessage ?: "Firebase initialization failed."
        Log.e("AutoFreelancerApplication", errorMessage, error)
    }
}

class AutoFreelancerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty() && FirebaseApp.initializeApp(this) == null) {
                throw IllegalStateException("Firebase configuration is missing.")
            }
        } catch (error: Exception) {
            AppStartupState.recordFailure(error)
        }
    }
}