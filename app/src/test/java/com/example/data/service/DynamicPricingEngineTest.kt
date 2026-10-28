package com.example.data.service

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DynamicPricingEngineTest {
    private val engine = DynamicPricingEngine()

    @Test
    fun classifyFromEvidence_usesCorporateAndStarterTiers() {
        assertEquals(350, engine.classifyFromEvidence("https://global-bank.example", "enterprise banking").amount)
        assertEquals(50, engine.classifyFromEvidence("https://my-travel-blog.example", "personal blog").amount)
    }

    @Test
    fun paymentLinkForAmount_replacesTemplateOrQueryValue() {
        assertEquals(
            "https://checkout.example/pay?price=350",
            engine.paymentLinkForAmount("https://checkout.example/pay?price={amount}", 350)
        )
        assertEquals(
            "https://checkout.example/pay?token=abc&amount=50",
            engine.paymentLinkForAmount("https://checkout.example/pay?token=abc&amount=99", 50)
        )
    }
}