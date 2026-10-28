package com.example.util

object BusinessEmailValidator {
    private val emailPattern = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    private val blockedDomains = setOf(
        "email4pr.com",
        "example.com",
        "example.net",
        "example.org",
        "guerrillamail.com",
        "mailinator.com",
        "sharklasers.com",
        "temp-mail.org",
        "tempmail.com",
        "10minutemail.com",
        "yopmail.com",
        "gmail.com",
        "googlemail.com",
        "yahoo.com",
        "outlook.com",
        "hotmail.com",
        "live.com",
        "icloud.com",
        "aol.com",
        "proton.me",
        "protonmail.com"
    )

    fun findIn(text: String): String? = emailPattern.findAll(text)
        .map { it.value }
        .firstOrNull(::isValid)

    fun isValid(email: String): Boolean {
        if (!emailPattern.matches(email)) return false

        val localPart = email.substringBefore('@')
        val domain = email.substringAfter('@').lowercase()
        if (localPart.startsWith('.') || localPart.endsWith('.') || ".." in localPart) return false
        if (domain.startsWith('.') || domain.endsWith('.') || ".." in domain) return false
        if (domain.split('.').any { it.isBlank() || it.startsWith('-') || it.endsWith('-') }) return false

        return domain !in blockedDomains && blockedDomains.none { domain.endsWith(".$it") }
    }
}