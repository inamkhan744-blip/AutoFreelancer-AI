package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object FollowUpScheduler {
    private const val WORK_NAME = "AutomatedFollowUpWorker"

    fun schedule(context: Context, enabled: Boolean = true) {
        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Check every 6 hours for eligible follow-ups (3-day and 7-day milestones)
            val workRequest = PeriodicWorkRequestBuilder<FollowUpWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    5, TimeUnit.MINUTES
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } else {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }
}
