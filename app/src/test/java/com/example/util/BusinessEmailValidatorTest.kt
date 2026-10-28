package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessEmailValidatorTest {
    @Test
    fun findIn_returnsValidBusinessEmailAndSkipsDisposableAddresses() {
        assertEquals(
            "owner@acme-example.com",
            BusinessEmailValidator.findIn("408309@email4pr.com or owner@acme-example.com")
        )
    }

    @Test
    fun isValid_rejectsMalformedAndPersonalEmailAddresses() {
        assertFalse(BusinessEmailValidator.isValid("not-an-email"))
        assertFalse(BusinessEmailValidator.isValid("person..name@acme.com"))
        assertFalse(BusinessEmailValidator.isValid("person@gmail.com"))
        assertFalse(BusinessEmailValidator.isValid("person@email4pr.com"))
    }

    @Test
    fun isValid_acceptsBusinessEmailAddress() {
        assertTrue(BusinessEmailValidator.isValid("owner@acme-example.com"))
    }
}