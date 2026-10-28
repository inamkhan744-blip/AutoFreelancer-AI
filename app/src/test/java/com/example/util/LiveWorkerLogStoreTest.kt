package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveWorkerLogStoreTest {
    @Test
    fun record_appendsEntriesAndRetainsOnlyRecentHistory() {
        repeat(90) { LiveWorkerLogStore.record("TEST", "event-$it") }

        val entries = LiveWorkerLogStore.entries.value
        assertEquals(80, entries.size)
        assertTrue(entries.last().message == "event-89")
        assertTrue(entries.first().message == "event-10")
    }
}