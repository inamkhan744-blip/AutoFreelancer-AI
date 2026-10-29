package com.example.util

import com.example.data.model.CrmClient
import com.example.data.model.Task
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProspectDeduplicatorTest {
    @Test
    fun `matches task email and discovered URL`() {
        val deduplicator = ProspectDeduplicator(
            tasks = listOf(
                Task(
                    clientEmail = "Lead@Example.com",
                    details = "Discovered Lead URL: https://www.example.com/prospect/?utm_source=search"
                )
            ),
            clients = emptyList()
        )

        assertTrue(deduplicator.contains("https://example.com/prospect", ""))
        assertTrue(deduplicator.contains("", "lead@example.com"))
        assertFalse(deduplicator.contains("https://example.com/another-prospect", "new@example.com"))
    }

    @Test
    fun `matches CRM identifiers and remembers leads in current batch`() {
        val deduplicator = ProspectDeduplicator(
            tasks = emptyList(),
            clients = listOf(CrmClient(name = "Existing", websiteUrl = "https://example.com/client", email = "client@example.com"))
        )

        assertTrue(deduplicator.contains("https://example.com/client/", ""))
        assertTrue(deduplicator.contains("", "CLIENT@example.com"))
        deduplicator.remember("https://example.com/new", "new@example.com")
        assertTrue(deduplicator.contains("https://example.com/new", ""))
        assertTrue(deduplicator.contains("", "new@example.com"))
    }
}
