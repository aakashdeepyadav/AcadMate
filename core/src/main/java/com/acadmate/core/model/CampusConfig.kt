package com.acadmate.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CampusConfig(
    val institutionName: String = "NIT Delhi",
    val latitude: Double = 28.5921,
    val longitude: Double = 28.5921,
    val radiusMeters: Float = 200f,
    val lastUpdated: Long = System.currentTimeMillis()
)
