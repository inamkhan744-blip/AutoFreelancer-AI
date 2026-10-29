package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

object AppStartupState {
    @Volatile
    var isGuestMode: Boolean = false

    @Volatile
    var errorMessage: String? = null
        private set

    fun recordFailure(error: Throwable) {
        errorMessage = error.localizedMessage ?: "Non-fatal startup notice"
        Log.w("AutoFreelancerApplication", "Non-fatal startup notice: $errorMessage", error)
    }

    fun clearError() {
        errorMessage = null
    }
}

class AutoFreelancerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app == null) {
                    Log.i("AutoFreelancerApplication", "FirebaseApp returned null; running in Guest/Offline mode.")
                    AppStartupState.isGuestMode = true
                }
            }
        } catch (error: Throwable) {
            Log.w("AutoFreelancerApplication", "FirebaseApp init warning (non-fatal, continuing in Guest Mode): ${error.message}", error)
            AppStartupState.isGuestMode = true
            AppStartupState.recordFailure(error)
        }
    }
}