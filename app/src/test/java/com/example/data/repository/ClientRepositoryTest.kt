package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CrmClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClientRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: ClientRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "autofreelancer_crm_clients.json")
        if (file.exists()) {
            file.delete()
        }
        repository = ClientRepository(context)
    }

    @Test
    fun upsertAndObserveClients_persistsLocallyAndUpdates() = runBlocking {
        val initialClient = CrmClient(
            name = "Acme Store",
            company = "Acme E-Commerce",
            websiteUrl = "https://acme-store.com",
            email = "founder@acme-store.com",
            status = "Pitched",
            leadSource = "LinkedIn Scout",
            pitchMessage = "Saw your store. We can boost your rankings."
        )

        val result = repository.upsertClient(initialClient)
        assertTrue(result.isSuccess)

        val list = repository.observeClients().first()
        assertEquals(1, list.size)
        assertEquals("Acme Store", list[0].name)
        assertEquals("Pitched", list[0].status)
        assertEquals("https://acme-store.com", list[0].websiteUrl)

        // Update status to Paid
        repository.updateClientStatus(list[0].id, "Paid")
        val updatedList = repository.observeClients().first()
        assertEquals("Paid", updatedList[0].status)

        // Update report
        repository.updateClientReport(list[0].id, "5-Point SEO Audit Report: All issues fixed.")
        val reportUpdated = repository.observeClients().first()
        assertEquals("5-Point SEO Audit Report: All issues fixed.", reportUpdated[0].seoReport)

        // Update status to Completed
        repository.updateClientStatus(list[0].id, "Completed")
        val completedList = repository.observeClients().first()
        assertEquals("Completed", completedList[0].status)
    }

    @Test
    fun crmClientSerialization_roundTrip_matches() {
        val client = CrmClient(
            name = "Apex Tech",
            company = "Apex Systems",
            websiteUrl = "https://apex.io",
            email = "growth@apex.io",
            status = "Paid",
            leadSource = "Google Search",
            pitchMessage = "High converting SEO pitch",
            seoReport = "Point 1 to 5 Audit",
            estimatedValue = 750.0
        )

        val json = client.toJson()
        val parsed = CrmClient.fromJson(json)

        assertEquals(client.id, parsed.id)
        assertEquals(client.name, parsed.name)
        assertEquals(client.company, parsed.company)
        assertEquals(client.websiteUrl, parsed.websiteUrl)
        assertEquals(client.email, parsed.email)
        assertEquals("Paid", parsed.status)
        assertEquals("Point 1 to 5 Audit", parsed.seoReport)
    }
}
