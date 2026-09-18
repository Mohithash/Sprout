@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.mohithash.sprout.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.mohithash.sprout.ui.AppViewModel
import com.mohithash.sprout.ui.EmptyState
import com.mohithash.sprout.ui.HeroCard
import com.mohithash.sprout.ui.Label
import com.mohithash.sprout.ui.PlantThumb
import com.mohithash.sprout.ui.StatCard
import com.mohithash.sprout.ui.theme.Brand

@Composable
fun GardenScreen(vm: AppViewModel, onAdd: () -> Unit, onOpen: () -> Unit, onSettings: () -> Unit) {
    val plants by vm.plants.collectAsState()
    val cs = MaterialTheme.colorScheme
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val thirsty = plants.filter { vm.daysUntilWater(it) <= 0 }
    Scaffold(modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { MediumFlexibleTopAppBar(title = { Text("Your garden") }, subtitle = { Text("${plants.size} plants · ${thirsty.size} need water") }, actions = { IconButton(onSettings) { Icon(Icons.Default.Settings, null) } }, scrollBehavior = scroll, colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface, scrolledContainerColor = cs.surface)) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add plant") }, containerColor = cs.primary, contentColor = cs.onPrimary) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                HeroCard(colors = listOf(cs.primary, Brand.heroDeep), blobShape = MaterialShapes.Clover8Leaf) {
                    val on = cs.onPrimary
                    Label(if (thirsty.isEmpty()) "All watered" else "Thirsty today", on.copy(alpha = 0.8f))
                    Text(if (thirsty.isEmpty()) "Everyone's happy 🌿" else thirsty.joinToString { it.nickname }, style = MaterialTheme.typography.headlineSmall, color = on)
                    if (thirsty.isNotEmpty()) FilledTonalButton({ thirsty.forEach(vm::water) }, shapes = ButtonDefaults.shapes(), colors = ButtonDefaults.filledTonalButtonColors(containerColor = cs.secondary, contentColor = cs.onSecondary), modifier = Modifier.padding(top = 6.dp)) { Icon(Icons.Default.WaterDrop, null); Text(" Water all ${thirsty.size}") }
                }
            }
            if (plants.isEmpty()) item { EmptyState(Icons.Default.Spa, "No plants yet", "Snap a photo — Sprout identifies it and builds a care plan.") }
            items(plants, key = { it.id }) { p ->
                val until = vm.daysUntilWater(p)
                val prof = vm.profile(p)
                StatCard(Modifier.clip(MaterialTheme.shapes.extraLarge).clickable { vm.open(p); onOpen() }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PlantThumb(p.thumb)
                        Column(Modifier.weight(1f)) {
                            Text(p.nickname, style = MaterialTheme.typography.titleMedium)
                            Text(listOf(prof.common_name, p.location).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(when { until < 0 -> "${-until}d overdue"; until == 0L -> "Water today"; else -> "in ${until}d" }, style = MaterialTheme.typography.labelLarge, color = if (until <= 0) cs.error else cs.tertiary)
                            IconButton({ vm.water(p) }) { Icon(Icons.Default.WaterDrop, "Water", tint = cs.tertiary) }
                        }
                    }
                    LinearWavyProgressIndicator(progress = { (1f - vm.daysSinceWater(p).toFloat() / p.waterEveryDays).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = if (until <= 0) cs.error else cs.tertiary)
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}
