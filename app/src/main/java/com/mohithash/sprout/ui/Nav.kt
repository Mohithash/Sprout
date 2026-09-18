@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.mohithash.sprout.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohithash.sprout.ui.screens.AddPlantScreen
import com.mohithash.sprout.ui.screens.GardenScreen
import com.mohithash.sprout.ui.screens.OnboardingScreen
import com.mohithash.sprout.ui.screens.PlantScreen
import com.mohithash.sprout.ui.screens.SettingsScreen

@Composable
fun Nav(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    if (!s.onboarded) { OnboardingScreen(vm); return }
    val nav = rememberNavController()
    NavHost(nav, "garden") {
        composable("garden") { GardenScreen(vm, onAdd = { nav.navigate("add") }, onOpen = { nav.navigate("plant") }, onSettings = { nav.navigate("settings") }) }
        composable("add") { AddPlantScreen(vm, onBack = { nav.popBackStack() }) }
        composable("plant") { PlantScreen(vm, onBack = { nav.popBackStack() }) }
        composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }) }
    }
}
