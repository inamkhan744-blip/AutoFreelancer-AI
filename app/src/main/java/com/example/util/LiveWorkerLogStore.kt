package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WorkerLogEntry(
    val id: Long,
    val worker: String,
    val message: String,
    val timestamp: Long
)

object LiveWorkerLogStore {
    private const val MAX_ENTRIES = 80
    private var nextId = 0L
    private val _entries = MutableStateFlow(
        listOf(WorkerLogEntry(0L, "SYSTEM", "Terminal initialized; waiting for worker activity.", System.currentTimeMillis()))
    )
    val entries: StateFlow<List<WorkerLogEntry>> = _entries.asStateFlow()

    @Synchronized
    fun record(worker: String, message: String) {
        nextId += 1
        _entries.value = (_entries.value + WorkerLogEntry(nextId, worker, message, System.currentTimeMillis()))
            .takeLast(MAX_ENTRIES)
    }
}