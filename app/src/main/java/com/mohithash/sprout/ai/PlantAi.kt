package com.mohithash.sprout.ai

import com.mohithash.sprout.domain.Diagnosis
import com.mohithash.sprout.domain.PlantProfile
import com.mohithash.sprout.domain.Settings

class PlantAi(private val client: AiClient) {
    private val profileSchema = Schema.obj(
        "common_name" to Schema.str, "scientific_name" to Schema.str, "confidence" to Schema.enum("low", "medium", "high"),
        "water_every_days" to Schema.int, "water_note" to Schema.str, "light" to Schema.str, "humidity" to Schema.str, "temperature" to Schema.str,
        "fertilize" to Schema.str, "toxicity" to Schema.str, "difficulty" to Schema.enum("easy", "medium", "fussy"), "tips" to Schema.arr(Schema.str),
    )
    private val diagnosisSchema = Schema.obj("likely_issue" to Schema.str, "causes" to Schema.arr(Schema.str), "fix" to Schema.arr(Schema.str), "urgency" to Schema.enum("low", "medium", "high"), "prevention" to Schema.str)

    private fun ctx(s: Settings) = "Grower context: ${if (s.indoor) "indoor" else "outdoor"} plants, ${s.climate} climate."

    suspend fun identify(ai: AiSettings, s: Settings, image: String?, text: String): PlantProfile {
        val system = """You are a botanist and houseplant expert. Identify the plant from the photo and/or description and give a practical care profile. ${ctx(s)}
            |water_every_days: typical interval for this context (integer days). water_note: how to check (e.g. "top 3 cm dry"). light/humidity/temperature/fertilize: one short line each.
            |toxicity: note for pets/children, or "non-toxic". tips: 3-5 specific, non-generic tips. If unsure of the species, give the most likely and set confidence accordingly.""".trimMargin()
        return client.ask(ai, system, text.ifBlank { "Identify this plant and tell me how to care for it." }, profileSchema, image, 3000)
    }

    suspend fun diagnose(ai: AiSettings, s: Settings, profile: PlantProfile, image: String?, symptoms: String, daysSinceWater: Long): Diagnosis {
        val system = "You are a plant doctor. Plant: ${profile.common_name} (${profile.scientific_name}). Last watered $daysSinceWater days ago; recommended every ${profile.water_every_days}. ${ctx(s)} " +
            "likely_issue: one line. causes: 1-3 ranked. fix: 2-5 concrete steps in order. urgency: low/medium/high. prevention: one line."
        return client.ask(ai, system, symptoms.ifBlank { "What's wrong with my plant? See photo." }, diagnosisSchema, image, 2000)
    }

    suspend fun ask(ai: AiSettings, s: Settings, profile: PlantProfile, q: String): String =
        client.chat(ai, "You are a friendly plant expert. Plant: ${profile.common_name}. ${ctx(s)} Answer in under 100 words.", listOf(ChatMsg("user", q)), maxTokens = 500)
}
