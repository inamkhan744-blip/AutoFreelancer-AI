package com.example.data.repository

import com.example.data.model.OutreachTone
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplyRateRepositoryTest {
    @Test
    fun bestToneFromOutcomes_selectsHighestReplyRate() {
        val outcomes = listOf(
            OutreachTone.FRIENDLY to true,
            OutreachTone.FRIENDLY to false,
            OutreachTone.PROFESSIONAL to true,
            OutreachTone.AGGRESSIVE to false,
            OutreachTone.AGGRESSIVE to false
        )

        assertEquals(OutreachTone.PROFESSIONAL, ReplyRateRepository.bestToneFromOutcomes(outcomes))
    }

    @Test
    fun bestToneFromOutcomes_defaultsToProfessionalWhenNoDataOrTied() {
        assertEquals(OutreachTone.PROFESSIONAL, ReplyRateRepository.bestToneFromOutcomes(emptyList()))
        assertEquals(
            OutreachTone.PROFESSIONAL,
            ReplyRateRepository.bestToneFromOutcomes(listOf(OutreachTone.FRIENDLY to false, OutreachTone.PROFESSIONAL to false))
        )
    }
}