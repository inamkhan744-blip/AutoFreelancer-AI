package com.example.data.model

data class PipelineFunnelStage(
    val stage: String,
    val displayName: String,
    val count: Int,
    val totalValue: Double,
    val conversionRate: Float,
    val dropOffRate: Float
)

data class ResponseTimeDistribution(
    val bucketLabel: String,
    val count: Int,
    val percentage: Float,
    val colorHex: Long
)

data class ClosureTrendPoint(
    val dateLabel: String,
    val wonCount: Int,
    val revenue: Double,
    val winRate: Float
)

data class ConversionAnalyticsSummary(
    val totalLeads: Int = 0,
    val wonLeads: Int = 0,
    val winRatePercent: Float = 0f,
    val totalPipelineValue: Double = 0.0,
    val totalWonRevenue: Double = 0.0,
    val avgResponseTimeHours: Double = 0.0,
    val avgDealSize: Double = 0.0,
    val unresponsiveCount: Int = 0
)
