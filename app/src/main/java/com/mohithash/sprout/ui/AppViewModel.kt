package com.mohithash.sprout.ui

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohithash.sprout.App
import com.mohithash.sprout.ai.AiSettings
import com.mohithash.sprout.data.CareEvent
import com.mohithash.sprout.data.Plant
import com.mohithash.sprout.domain.Diagnosis
import com.mohithash.sprout.domain.PlantProfile
import com.mohithash.sprout.domain.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

sealed interface Job<out T> {
    data object Idle : Job<Nothing>
    data object Loading : Job<Nothing>
    data class Done<T>(val value: T) : Job<T>
    data class Failed(val message: String) : Job<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val app: App) : ViewModel() {
    private val db = app.db
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val client get() = app.client

    val ai: StateFlow<AiSettings> = app.store.flow("ai", AiSettings.serializer(), AiSettings())
    val settings: StateFlow<Settings> = app.store.flow("settings", Settings.serializer(), Settings())
    fun saveAi(a: AiSettings) = app.store.set("ai", AiSettings.serializer(), a)
    fun saveSettings(s: Settings) = app.store.set("settings", Settings.serializer(), s.copy(onboarded = true))

    val plants = db.plants().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selected = MutableStateFlow<Plant?>(null)
    val events: StateFlow<List<CareEvent>> = selected.flatMapLatest { p -> if (p == null) flowOf(emptyList()) else db.events().forPlant(p.id) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _identify = MutableStateFlow<Job<PlantProfile>>(Job.Idle)
    val identify: StateFlow<Job<PlantProfile>> = _identify
    private val _diagnosis = MutableStateFlow<Job<Diagnosis>>(Job.Idle)
    val diagnosis: StateFlow<Job<Diagnosis>> = _diagnosis
    private val _ask = MutableStateFlow<Job<String>>(Job.Idle)
    val ask: StateFlow<Job<String>> = _ask

    fun profile(p: Plant): PlantProfile = runCatching { json.decodeFromString(PlantProfile.serializer(), p.profile) }.getOrDefault(PlantProfile())
    fun daysSinceWater(p: Plant): Long = (System.currentTimeMillis() - p.lastWatered) / 86_400_000
    /** Negative = overdue by that many days. */
    fun daysUntilWater(p: Plant): Long = p.waterEveryDays - daysSinceWater(p)

    fun identifyPlant(image: String?, text: String) {
        _identify.value = Job.Loading
        viewModelScope.launch { _identify.value = runCatching { app.plants.identify(ai.value, settings.value, image, text) }.fold({ Job.Done(it) }, { Job.Failed(it.message ?: "Failed") }) }
    }
    fun clearIdentify() { _identify.value = Job.Idle }

    fun addPlant(profile: PlantProfile, nickname: String, location: String, thumb: Bitmap?) = viewModelScope.launch {
        val t = thumb?.let { b ->
            val s = Bitmap.createScaledBitmap(b, 160, (160f * b.height / b.width).toInt().coerceAtLeast(1), true)
            ByteArrayOutputStream().also { s.compress(Bitmap.CompressFormat.JPEG, 70, it) }.toByteArray().let { Base64.encodeToString(it, Base64.NO_WRAP) }
        }.orEmpty()
        val id = db.plants().insert(Plant(nickname = nickname.ifBlank { profile.common_name.ifBlank { "My plant" } }, location = location, profile = json.encodeToString(PlantProfile.serializer(), profile), waterEveryDays = profile.water_every_days.coerceIn(1, 60), thumb = t))
        db.events().insert(CareEvent(plantId = id, kind = "note", note = "Added to Sprout"))
        _identify.value = Job.Idle
    }
    fun open(p: Plant) { selected.value = p; _diagnosis.value = Job.Idle; _ask.value = Job.Idle }
    fun delete(p: Plant) = viewModelScope.launch { db.events().deleteForPlant(p.id); db.plants().delete(p.id); if (selected.value?.id == p.id) selected.value = null }

    fun water(p: Plant) = viewModelScope.launch { val u = p.copy(lastWatered = System.currentTimeMillis()); db.plants().update(u); db.events().insert(CareEvent(plantId = p.id, kind = "water")); if (selected.value?.id == p.id) selected.value = u }
    fun fertilize(p: Plant) = viewModelScope.launch { val u = p.copy(lastFertilized = System.currentTimeMillis()); db.plants().update(u); db.events().insert(CareEvent(plantId = p.id, kind = "fertilize")); if (selected.value?.id == p.id) selected.value = u }
    fun setInterval(p: Plant, days: Int) = viewModelScope.launch { val u = p.copy(waterEveryDays = days.coerceIn(1, 60)); db.plants().update(u); if (selected.value?.id == p.id) selected.value = u }
    fun addNote(p: Plant, note: String) = viewModelScope.launch { if (note.isNotBlank()) db.events().insert(CareEvent(plantId = p.id, kind = "note", note = note.trim())) }

    fun diagnose(p: Plant, image: String?, symptoms: String) {
        _diagnosis.value = Job.Loading
        viewModelScope.launch {
            _diagnosis.value = runCatching { app.plants.diagnose(ai.value, settings.value, profile(p), image, symptoms, daysSinceWater(p)) }.fold({ d ->
                db.events().insert(CareEvent(plantId = p.id, kind = "diagnosis", note = d.likely_issue)); Job.Done(d)
            }, { Job.Failed(it.message ?: "Failed") })
        }
    }
    fun askExpert(p: Plant, q: String) { _ask.value = Job.Loading; viewModelScope.launch { _ask.value = runCatching { app.plants.ask(ai.value, settings.value, profile(p), q) }.fold({ Job.Done(it) }, { Job.Failed(it.message ?: "Failed") }) } }
}
