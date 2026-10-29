package com.example.util

import com.example.data.model.CrmClient
import com.example.data.model.Task
import java.net.URI

class ProspectDeduplicator(
    tasks: Iterable<Task>,
    clients: Iterable<CrmClient>
) {
    private val knownEmails = mutableSetOf<String>()
    private val knownUrls = mutableSetOf<String>()

    init {
        tasks.forEach { task ->
            addEmail(task.clientEmail)
            task.details.findAll(EMAIL_PATTERN).forEach(::addEmail)
            task.details.findAll(URL_PATTERN).forEach(::addUrl)
        }
        clients.forEach { client ->
            addEmail(client.email)
            addUrl(client.websiteUrl)
        }
    }

    fun contains(url: String, email: String): Boolean {
        val normalizedUrl = normalizeUrl(url)
        val normalizedEmail = normalizeEmail(email)
        return (normalizedUrl.isNotBlank() && normalizedUrl in knownUrls) ||
            (normalizedEmail.isNotBlank() && normalizedEmail in knownEmails)
    }

    fun remember(url: String, email: String) {
        addUrl(url)
        addEmail(email)
    }

    private fun addEmail(email: String) {
        normalizeEmail(email).takeIf { it.isNotBlank() }?.let(knownEmails::add)
    }

    private fun addUrl(url: String) {
        normalizeUrl(url).takeIf { it.isNotBlank() }?.let(knownUrls::add)
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun normalizeUrl(url: String): String {
        val cleanUrl = url.trim().trimEnd('.', ',', ';', ')', ']', '}')
        if (cleanUrl.isBlank()) return ""

        val uri = runCatching { URI(cleanUrl) }.getOrNull()
        val host = uri?.host?.lowercase()?.removePrefix("www.")
        if (host != null) {
            return host + uri?.path.orEmpty().trimEnd('/')
        }

        return cleanUrl.substringBefore('#').substringBefore('?').trimEnd('/').lowercase()
    }

    private fun String.findAll(pattern: Regex): List<String> =
        pattern.findAll(this).map { it.value.trimEnd('.', ',', ';', ')', ']', '}') }.toList()

    private companion object {
        val EMAIL_PATTERN = Regex("""[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}""", RegexOption.IGNORE_CASE)
        val URL_PATTERN = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    }
}
