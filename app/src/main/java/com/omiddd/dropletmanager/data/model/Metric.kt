package com.omiddd.dropletmanager.data.model

data class MetricPoint(
    val timestamp: Long,
    val value: Double
)

data class MetricsResponse(
    val data: MetricsData
)

data class MetricsData(
    val result: List<MetricResult>
)

data class MetricResult(
    val metric: Map<String, String>,
    val values: List<List<Any>>
) 