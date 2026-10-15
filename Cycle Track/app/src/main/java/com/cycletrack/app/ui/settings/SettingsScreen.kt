package com.cycletrack.app.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycletrack.app.domain.model.CyclingIntensity
import com.cycletrack.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editWeight by remember { mutableStateOf("") }
    var editAge by remember { mutableStateOf("") }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editWeight,
                        onValueChange = { editWeight = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAge,
                        onValueChange = { editAge = it },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val weight = editWeight.toFloatOrNull() ?: userProfile.weightKg
                        val age = editAge.toIntOrNull() ?: userProfile.ageYears
                        viewModel.updateProfile(editName.trim(), weight, age)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CycleGreen)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Section 1: Profile ──────────────────────────────────────────
            SettingsSectionHeader(title = "Profile")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CycleDarkBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, null, tint = CycleGreen, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (userProfile.name.isNotBlank()) userProfile.name else "Rider",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "%.1f kg • %d years".format(userProfile.weightKg, userProfile.ageYears),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                editName = userProfile.name
                                editWeight = userProfile.weightKg.toString()
                                editAge = userProfile.ageYears.toString()
                                showEditProfileDialog = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Edit")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "* Weight is used directly in MET formula calculations for calorie accuracy.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Section 2: Units ────────────────────────────────────────────
            SettingsSectionHeader(title = "Units of Measurement")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Distance & Speed Unit", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (userProfile.preferKilometers) "Kilometers (km, km/h)" else "Miles (mi, mph)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = userProfile.preferKilometers,
                                onClick = { viewModel.updatePreferKilometers(true) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = CycleGreen,
                                    activeContentColor = CycleDarkBlue
                                )
                            ) {
                                Text("KM")
                            }
                            SegmentedButton(
                                selected = !userProfile.preferKilometers,
                                onClick = { viewModel.updatePreferKilometers(false) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = CycleGreen,
                                    activeContentColor = CycleDarkBlue
                                )
                            ) {
                                Text("MI")
                            }
                        }
                    }
                }
            }

            // ── Section 3: Goals ────────────────────────────────────────────
            SettingsSectionHeader(title = "Goals & Workout Setup")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily Distance Target", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "%.0f km".format(userProfile.dailyGoalKm),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CycleGreenDark
                        )
                    }
                    Slider(
                        value = userProfile.dailyGoalKm,
                        onValueChange = { viewModel.updateDailyGoal(it) },
                        valueRange = 2f..50f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = CycleGreen,
                            activeTrackColor = CycleGreen
                        )
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("Default Cycling Effort", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyclingIntensity.values().forEach { intensity ->
                            FilterChip(
                                selected = userProfile.intensity == intensity,
                                onClick = { viewModel.updateIntensity(intensity) },
                                label = { Text(intensity.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CycleGreen,
                                    selectedLabelColor = CycleDarkBlue
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Section 4: Appearance ───────────────────────────────────────
            SettingsSectionHeader(title = "Appearance")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Theme", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Switch to dark color palette", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = userProfile.darkMode,
                        onCheckedChange = { viewModel.updateDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CycleGreen
                        )
                    )
                }
            }

            // ── Section 5: Privacy & About ──────────────────────────────────
            SettingsSectionHeader(title = "Privacy & About")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, null, tint = CycleGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("100% On-Device & Private", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Your GPS coordinates and cycling activities are stored strictly inside your phone's Room SQLite database. No location data is sent to external cloud servers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Map Provider", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("OpenStreetMap (OSMDroid)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("App Version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("1.0.0", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = CycleGreenDark,
        modifier = Modifier.padding(start = 4.dp)
    )
}
