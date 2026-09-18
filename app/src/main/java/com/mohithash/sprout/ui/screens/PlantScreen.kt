@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.mohithash.sprout.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mohithash.sprout.ui.AppViewModel
import com.mohithash.sprout.ui.HeroCard
import com.mohithash.sprout.ui.Job
import com.mohithash.sprout.ui.Label
import com.mohithash.sprout.ui.MealPhoto
import com.mohithash.sprout.ui.Photo
import com.mohithash.sprout.ui.PlantThumb
import com.mohithash.sprout.ui.ShapeIcon
import com.mohithash.sprout.ui.StatCard
import com.mohithash.sprout.ui.theme.Brand
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class Tab { CARE, DOCTOR, LOG }

@Composable
fun PlantScreen(vm: AppViewModel, onBack: () -> Unit) {
    val sel by vm.selected.collectAsState()
    val plant = sel ?: run { onBack(); return }
    val prof = vm.profile(plant)
    val events by vm.events.collectAsState()
    val diag by vm.diagnosis.collectAsState()
    val ans by vm.ask.collectAsState()
    val ai by vm.ai.collectAsState()
    val cs = MaterialTheme.colorScheme
    var tab by remember { mutableStateOf(Tab.CARE) }
    var symptoms by remember { mutableStateOf("") }
    var q by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<MealPhoto?>(null) }
    val ctx = LocalContext.current
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { b -> b?.let { photo = Photo.fromBitmap(it) } }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> u?.let { photo = Photo.fromUri(ctx, it) } }
    val until = vm.daysUntilWater(plant)
    val fmt = DateTimeFormatter.ofPattern("d MMM, HH:mm")

    Scaffold(topBar = { TopAppBar(title = { Text(plant.nickname) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface), navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }, actions = { IconButton({ vm.delete(plant); onBack() }) { Icon(Icons.Default.Delete, null) } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroCard(colors = listOf(cs.primary, Brand.heroDeep), blobShape = MaterialShapes.Clover8Leaf) {
                val on = cs.onPrimary
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PlantThumb(plant.thumb, 72)
                    Column(Modifier.weight(1f)) {
                        Text(prof.common_name, style = MaterialTheme.typography.headlineSmall, color = on); Text(prof.scientific_name, color = on.copy(alpha = 0.85f))
                        Text(when { until < 0 -> "Water overdue by ${-until} days"; until == 0L -> "Water today"; else -> "Next water in $until days" }, style = MaterialTheme.typography.labelLarge, color = if (until <= 0) cs.errorContainer else on.copy(alpha = 0.9f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Button({ vm.water(plant) }, shapes = ButtonDefaults.shapes(), colors = ButtonDefaults.buttonColors(containerColor = cs.secondary, contentColor = cs.onSecondary), modifier = Modifier.weight(1f)) { Icon(Icons.Default.WaterDrop, null); Text(" Watered") }
                    FilledTonalButton({ vm.fertilize(plant) }, shapes = ButtonDefaults.shapes(), colors = ButtonDefaults.filledTonalButtonColors(containerColor = on.copy(alpha = 0.16f), contentColor = on), modifier = Modifier.weight(1f)) { Icon(Icons.Default.Grass, null); Text(" Fed") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                Tab.entries.forEachIndexed { i, t ->
                    ToggleButton(checked = tab == t, onCheckedChange = { tab = t }, modifier = Modifier.weight(1f), shapes = when (i) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); 2 -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() }) { Text(t.name.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                }
            }
            AnimatedContent(tab, label = "tab") { t ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (t) {
                        Tab.CARE -> {
                            StatCard { Label("Watering interval: every ${plant.waterEveryDays} days"); Slider(plant.waterEveryDays.toFloat(), { vm.setInterval(plant, it.toInt()) }, valueRange = 1f..30f, steps = 28) }
                            CareProfileCard(prof)
                            StatCard {
                                Label("Ask the expert")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(q, { q = it }, placeholder = { Text("Can I propagate it in water?") }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge, maxLines = 3)
                                    FilledIconButton({ vm.askExpert(plant, q); q = "" }, enabled = q.isNotBlank() && ai.configured && ans != Job.Loading, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.Send, null) }
                                }
                                when (val a = ans) { Job.Loading -> LoadingIndicator(); is Job.Done -> Text(a.value); is Job.Failed -> Text(a.message, color = cs.error); Job.Idle -> {} }
                            }
                        }
                        Tab.DOCTOR -> {
                            StatCard(container = cs.secondaryContainer) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { ShapeIcon(Icons.Default.HealthAndSafety, cs.secondary, cs.onSecondary, MaterialShapes.Sunny); Column { Text("Plant doctor", style = MaterialTheme.typography.titleMedium, color = cs.onSecondaryContainer); Label("Yellow leaves? Drooping? Spots?", cs.onSecondaryContainer.copy(alpha = 0.8f)) } }
                                photo?.let { p -> Box(Modifier.fillMaxWidth()) { Image(p.bitmap.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.large)); FilledTonalIconButton({ photo = null }, Modifier.align(Alignment.TopEnd).padding(8.dp)) { Icon(Icons.Default.Close, null) } } }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton({ camera.launch(null) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Icon(Icons.Default.PhotoCamera, null, Modifier.size(18.dp)); Text(" Camera") }
                                    OutlinedButton({ gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp)); Text(" Gallery") }
                                }
                                OutlinedTextField(symptoms, { symptoms = it }, placeholder = { Text("Describe what you see") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = MaterialTheme.shapes.large)
                                Button({ vm.diagnose(plant, photo?.base64, symptoms) }, enabled = ai.configured && (photo != null || symptoms.isNotBlank()) && diag != Job.Loading, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(50.dp)) { if (diag == Job.Loading) { LoadingIndicator(Modifier.size(18.dp)); Text("  Examining…") } else Text("Diagnose") }
                            }
                            when (val d = diag) {
                                is Job.Failed -> StatCard(container = cs.errorContainer) { Text(d.message, color = cs.onErrorContainer) }
                                is Job.Done -> {
                                    HeroCard(colors = listOf(if (d.value.urgency == "high") cs.error else cs.tertiary, Brand.heroDeep), blobShape = MaterialShapes.Sunny) { Label("${d.value.urgency} urgency", cs.onPrimary.copy(alpha = 0.8f)); Text(d.value.likely_issue, style = MaterialTheme.typography.headlineSmall, color = cs.onPrimary) }
                                    StatCard { Label("Likely causes"); d.value.causes.forEach { Text("•  $it") } }
                                    StatCard(container = cs.primaryContainer) { Label("Fix", cs.onPrimaryContainer); d.value.fix.forEachIndexed { i, s -> Text("${i + 1}.  $s", color = cs.onPrimaryContainer, style = MaterialTheme.typography.bodyLarge) } }
                                    if (d.value.prevention.isNotBlank()) StatCard { Label("Prevent"); Text(d.value.prevention) }
                                }
                                else -> {}
                            }
                        }
                        Tab.LOG -> {
                            StatCard {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(note, { note = it }, placeholder = { Text("Add a note (repotted, new leaf…)") }, singleLine = true, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge)
                                    FilledIconButton({ vm.addNote(plant, note); note = "" }, enabled = note.isNotBlank(), modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Notes, null) }
                                }
                            }
                            events.forEach { e ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ShapeIcon(when (e.kind) { "water" -> Icons.Default.WaterDrop; "fertilize" -> Icons.Default.Grass; "diagnosis" -> Icons.Default.HealthAndSafety; else -> Icons.Default.Notes },
                                        when (e.kind) { "water" -> cs.tertiaryContainer; "fertilize" -> cs.primaryContainer; "diagnosis" -> cs.errorContainer; else -> cs.surfaceContainerHigh },
                                        when (e.kind) { "water" -> cs.onTertiaryContainer; "fertilize" -> cs.onPrimaryContainer; "diagnosis" -> cs.onErrorContainer; else -> cs.onSurface }, MaterialShapes.Cookie6Sided, 36)
                                    Column { Text(when (e.kind) { "water" -> "Watered"; "fertilize" -> "Fertilised"; "diagnosis" -> "Diagnosed: ${e.note}"; else -> e.note }, style = MaterialTheme.typography.bodyLarge); Text(Instant.ofEpochMilli(e.timestamp).atZone(ZoneId.systemDefault()).format(fmt), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant) }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
