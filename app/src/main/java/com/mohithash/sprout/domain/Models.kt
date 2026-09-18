package com.mohithash.sprout.domain

import kotlinx.serialization.Serializable

@Serializable
data class PlantProfile(
    val common_name: String = "",
    val scientific_name: String = "",
    val confidence: String = "",
    val water_every_days: Int = 7,
    val water_note: String = "",
    val light: String = "",
    val humidity: String = "",
    val temperature: String = "",
    val fertilize: String = "",
    val toxicity: String = "",
    val difficulty: String = "",
    val tips: List<String> = emptyList(),
)

@Serializable
data class Diagnosis(val likely_issue: String = "", val causes: List<String> = emptyList(), val fix: List<String> = emptyList(), val urgency: String = "", val prevention: String = "")

@Serializable data class Settings(val name: String = "", val climate: String = "temperate", val indoor: Boolean = true, val onboarded: Boolean = false)
