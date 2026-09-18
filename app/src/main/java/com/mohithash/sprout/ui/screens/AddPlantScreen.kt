@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.mohithash.sprout.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.mohithash.sprout.domain.PlantProfile
import com.mohithash.sprout.ui.AppViewModel
import com.mohithash.sprout.ui.HeroCard
import com.mohithash.sprout.ui.Job
import com.mohithash.sprout.ui.KeyValue
import com.mohithash.sprout.ui.Label
import com.mohithash.sprout.ui.MealPhoto
import com.mohithash.sprout.ui.Photo
import com.mohithash.sprout.ui.StatCard
import com.mohithash.sprout.ui.theme.Brand

@Composable
fun CareProfileCard(p: PlantProfile) {
    val cs = MaterialTheme.colorScheme
    StatCard {
        Label("Care plan")
        KeyValue("Water", "every ${p.water_every_days} days · ${p.water_note}")
        KeyValue("Light", p.light); KeyValue("Humidity", p.humidity); KeyValue("Temperature", p.temperature); KeyValue("Feed", p.fertilize)
        KeyValue("Toxicity", p.toxicity, if (p.toxicity.contains("non", true)) cs.primary else cs.error)
        if (p.tips.isNotEmpty()) { Label("Tips"); p.tips.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) } }
    }
}

@Composable
fun AddPlantScreen(vm: AppViewModel, onBack: () -> Unit) {
    val ai by vm.ai.collectAsState()
    val job by vm.identify.collectAsState()
    val cs = MaterialTheme.colorScheme
    var text by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<MealPhoto?>(null) }
    val ctx = LocalContext.current
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { b -> b?.let { photo = Photo.fromBitmap(it) } }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> u?.let { photo = Photo.fromUri(ctx, it) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Add a plant") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface), navigationIcon = { IconButton({ vm.clearIdentify(); onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard {
                Label("Photo or description")
                photo?.let { p -> Box(Modifier.fillMaxWidth()) { Image(p.bitmap.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(240.dp).clip(MaterialTheme.shapes.large)); FilledTonalIconButton({ photo = null }, Modifier.align(Alignment.TopEnd).padding(8.dp)) { Icon(Icons.Default.Close, null) } } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ camera.launch(null) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Icon(Icons.Default.PhotoCamera, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Camera") }
                    OutlinedButton({ gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Gallery") }
                }
                OutlinedTextField(text, { text = it }, placeholder = { Text("…or describe it: “tall, big split leaves, lives in the living room”") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = MaterialTheme.shapes.large)
                if (!ai.configured) Text("Add an API key in Settings to identify plants.", color = cs.error, style = MaterialTheme.typography.bodySmall)
                Button({ vm.identifyPlant(photo?.base64, text) }, enabled = ai.configured && (photo != null || text.isNotBlank()) && job != Job.Loading, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (job == Job.Loading) { LoadingIndicator(Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text("Identifying…") } else { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.size(8.dp)); Text("Identify & build care plan") }
                }
            }
            when (val j = job) {
                is Job.Failed -> StatCard(container = cs.errorContainer) { Text(j.message, color = cs.onErrorContainer) }
                is Job.Done -> {
                    val p = j.value
                    HeroCard(colors = listOf(cs.primary, Brand.heroDeep), blobShape = MaterialShapes.Clover8Leaf) {
                        Label("${p.confidence} confidence · ${p.difficulty}", cs.onPrimary.copy(alpha = 0.8f)); Text(p.common_name, style = MaterialTheme.typography.headlineMedium, color = cs.onPrimary); Text(p.scientific_name, color = cs.onPrimary.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyLarge)
                    }
                    CareProfileCard(p)
                    StatCard {
                        OutlinedTextField(nickname, { nickname = it }, label = { Text("Nickname") }, placeholder = { Text(p.common_name) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large)
                        OutlinedTextField(location, { location = it }, label = { Text("Where it lives") }, placeholder = { Text("Kitchen window, balcony…") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large)
                        Button({ vm.addPlant(p, nickname, location, photo?.bitmap); onBack() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Add to garden") }
                    }
                }
                else -> {}
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
