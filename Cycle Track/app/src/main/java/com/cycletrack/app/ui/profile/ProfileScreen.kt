package com.cycletrack.app.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cycletrack.app.domain.model.CyclingIntensity
import com.cycletrack.app.domain.model.UserProfile
import kotlinx.coroutines.launch

// ── Brand colours ──────────────────────────────────────────────────────────
private val CycleGreen = Color(0xFF00C853)
private val CycleDarkBlue = Color(0xFF0D1B2A)
private val CycleGreenLight = Color(0xFFB9F6CA)
// ───────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSaved: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ── Local editable state initialised from persisted profile ────────────
    var name by remember(userProfile.name) { mutableStateOf(userProfile.name) }
    var age by remember(userProfile.ageYears) {
        mutableStateOf(if (userProfile.ageYears == 0) "" else userProfile.ageYears.toString())
    }
    var weight by remember(userProfile.weightKg) {
        mutableStateOf(if (userProfile.weightKg == 70f && userProfile.name.isEmpty()) ""
        else userProfile.weightKg.toString())
    }
    var dailyGoal by remember(userProfile.dailyGoalKm) {
        mutableStateOf(if (userProfile.dailyGoalKm == 10f && userProfile.name.isEmpty()) ""
        else userProfile.dailyGoalKm.toString())
    }
    var selectedIntensity by remember(userProfile.intensity) { mutableStateOf(userProfile.intensity) }
    var preferKm by remember(userProfile.preferKilometers) { mutableStateOf(userProfile.preferKilometers) }
    var preferKmh by remember(userProfile.preferKmh) { mutableStateOf(userProfile.preferKmh) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Profile",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onSaved) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CycleDarkBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Avatar ─────────────────────────────────────────────────────
            AvatarSection(name = name)

            // ── Personal Info Card ─────────────────────────────────────────
            ProfileCard(title = "Personal Info") {
                ProfileTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name / Nickname",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
                ProfileTextField(
                    value = age,
                    onValueChange = { if (it.length <= 3) age = it.filter { c -> c.isDigit() } },
                    label = "Age (years)",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
                ProfileTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Weight (kg)",
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
                ProfileTextField(
                    value = dailyGoal,
                    onValueChange = { dailyGoal = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Daily goal (km)",
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                )
            }

            // ── Intensity Card ─────────────────────────────────────────────
            ProfileCard(title = "Cycling Intensity") {
                IntensitySelector(
                    selected = selectedIntensity,
                    onSelect = { selectedIntensity = it }
                )
            }

            // ── Units Card ─────────────────────────────────────────────────
            ProfileCard(title = "Preferred Units") {
                UnitToggleRow(
                    label = "Distance",
                    optionA = "Kilometers",
                    optionB = "Miles",
                    isA = preferKm,
                    onToggle = { preferKm = it }
                )
                Spacer(modifier = Modifier.height(4.dp))
                UnitToggleRow(
                    label = "Speed",
                    optionA = "km/h",
                    optionB = "mph",
                    isA = preferKmh,
                    onToggle = { preferKmh = it }
                )
            }

            // ── Save Button ────────────────────────────────────────────────
            Button(
                onClick = {
                    val profile = UserProfile(
                        name = name.trim(),
                        ageYears = age.toIntOrNull() ?: 0,
                        weightKg = weight.toFloatOrNull() ?: 70f,
                        dailyGoalKm = dailyGoal.toFloatOrNull() ?: 10f,
                        intensity = selectedIntensity,
                        preferKilometers = preferKm,
                        preferKmh = preferKmh,
                        darkMode = userProfile.darkMode,
                        onboardingDone = userProfile.onboardingDone
                    )
                    viewModel.saveProfile(profile)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Profile saved!",
                            duration = SnackbarDuration.Short
                        )
                    }
                    onSaved()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CycleGreen,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Save Profile",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Avatar ──────────────────────────────────────────────────────────────────
@Composable
private fun AvatarSection(name: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(CycleGreen)
                .border(3.dp, CycleDarkBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val initials = name
                .trim()
                .split(" ")
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }

            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = "Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (name.isNotBlank()) name else "Your Profile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

// ── Reusable Card ────────────────────────────────────────────────────────────
@Composable
private fun ProfileCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = CycleGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

// ── Text Field ──────────────────────────────────────────────────────────────
@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CycleGreen,
            focusedLabelColor = CycleGreen,
            cursorColor = CycleGreen
        )
    )
}

// ── Intensity Selector ───────────────────────────────────────────────────────
@Composable
private fun IntensitySelector(
    selected: CyclingIntensity,
    onSelect: (CyclingIntensity) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CyclingIntensity.entries.forEach { intensity ->
            val isSelected = intensity == selected
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) CycleGreen else Color.Transparent,
                animationSpec = tween(200),
                label = "intensityBg_${intensity.name}"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(200),
                label = "intensityText_${intensity.name}"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) CycleGreen else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(intensity) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (intensity) {
                        CyclingIntensity.EASY -> "Easy"
                        CyclingIntensity.MODERATE -> "Moderate"
                        CyclingIntensity.VIGOROUS -> "Vigorous"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Selected: ${selected.displayName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ── Unit Toggle Row ──────────────────────────────────────────────────────────
@Composable
private fun UnitToggleRow(
    label: String,
    optionA: String,
    optionB: String,
    isA: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isA) optionA else optionB,
                style = MaterialTheme.typography.bodySmall,
                color = CycleGreen
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = optionA,
                style = MaterialTheme.typography.labelSmall,
                color = if (isA) CycleGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isA) FontWeight.Bold else FontWeight.Normal
            )
            Switch(
                checked = !isA,
                onCheckedChange = { onToggle(!it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CycleDarkBlue,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = CycleGreen
                )
            )
            Text(
                text = optionB,
                style = MaterialTheme.typography.labelSmall,
                color = if (!isA) CycleDarkBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (!isA) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
