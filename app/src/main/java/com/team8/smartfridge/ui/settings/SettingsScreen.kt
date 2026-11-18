package com.team8.smartfridge.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.unit.sp
import com.team8.smartfridge.model.User
import com.team8.smartfridge.ui.theme.*

private val allergyOptions  = listOf("Gluten", "Dairy", "Nuts", "Shellfish", "Eggs", "Soy", "Fish", "Wheat", "None")
private val dietOptions     = listOf("None", "Vegetarian", "Vegan", "Keto", "Paleo", "Gluten-Free", "Mediterranean")
private val bloodTypeOptions = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown")

private val HeaderBg    = Color(0xFF1C3B22)
private val HeaderMuted = Color(0xFF5A7A5A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: User?,
    onSave: (User) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    if (user == null) return

    var firstName        by remember { mutableStateOf(user.firstName) }
    var lastName         by remember { mutableStateOf(user.lastName) }
    var age              by remember { mutableStateOf(user.age.toString()) }
    var height           by remember { mutableStateOf(if (user.height > 0f) user.height.toString() else "") }
    var weight           by remember { mutableStateOf(user.weight.toString()) }
    var sex              by remember { mutableStateOf(user.sex.ifBlank { "Male" }) }
    var bloodType        by remember { mutableStateOf(user.bloodType.ifBlank { "Unknown" }) }
    var selectedAllergies by remember {
        mutableStateOf(user.allergies.split(", ").filter { it.isNotBlank() }.toSet())
    }
    var dietPreference   by remember { mutableStateOf(user.dietPreference.ifBlank { "None" }) }
    var dailyCalorieGoal by remember { mutableStateOf(user.dailyCalorieGoal.toString()) }
    var dailyWaterGoal   by remember { mutableStateOf(user.dailyWaterGoal.toString()) }
    var bloodTypeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSecondary)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(HeaderBg)
                .padding(start = 8.dp, end = 20.dp, top = 48.dp, bottom = 20.dp)
        ) {
            Column {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SETTINGS",
                    fontSize = 10.sp,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.Normal,
                    color = HeaderMuted,
                    modifier = Modifier.padding(start = 12.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Settings",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Manage your profile",
                    fontSize = 11.sp,
                    color = HeaderMuted,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Personal Information ─────────────────────────────────────────
            SettingsSectionLabel("PERSONAL INFORMATION")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BgPrimary),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsTextField(value = firstName, onValueChange = { firstName = it }, label = "First Name")
                    SettingsTextField(value = lastName,  onValueChange = { lastName  = it }, label = "Last Name")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsTextField(
                            value = age, onValueChange = { age = it },
                            label = "Age", keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        SettingsTextField(
                            value = height, onValueChange = { height = it },
                            label = "Height (cm)", keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SettingsTextField(
                        value = weight, onValueChange = { weight = it },
                        label = "Weight (kg)", keyboardType = KeyboardType.Number
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Sex", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other").forEach { option ->
                            SettingsSelectorChip(
                                label = option,
                                selected = sex == option,
                                onClick = { sex = option },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Medical Information ──────────────────────────────────────────
            SettingsSectionLabel("MEDICAL INFORMATION")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BgPrimary),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Blood Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = bloodTypeExpanded,
                        onExpandedChange = { bloodTypeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = bloodType,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodTypeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Brand,
                                unfocusedBorderColor = Divider,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = bloodTypeExpanded,
                            onDismissRequest = { bloodTypeExpanded = false },
                            modifier = Modifier.background(BgPrimary)
                        ) {
                            bloodTypeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = TextPrimary, fontSize = 14.sp) },
                                    onClick = { bloodType = option; bloodTypeExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            // ── Food Allergies ───────────────────────────────────────────────
            SettingsSectionLabel("FOOD ALLERGIES")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BgPrimary),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select all that apply", fontSize = 12.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    allergyOptions.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { option ->
                                SettingsAllergyChip(
                                    label = option,
                                    selected = selectedAllergies.contains(option),
                                    onClick = {
                                        selectedAllergies = if (option == "None") {
                                            setOf("None")
                                        } else {
                                            val without = selectedAllergies - "None"
                                            if (without.contains(option)) without - option else without + option
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }

            // ── Diet Preference ──────────────────────────────────────────────
            SettingsSectionLabel("DIET PREFERENCE")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BgPrimary),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select your dietary style", fontSize = 12.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    dietOptions.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { option ->
                                SettingsAllergyChip(
                                    label = option,
                                    selected = dietPreference == option,
                                    onClick = { dietPreference = option },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }

            // ── Daily Goals ──────────────────────────────────────────────────
            SettingsSectionLabel("DAILY GOALS")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BgPrimary),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsTextField(
                        value = dailyCalorieGoal,
                        onValueChange = { dailyCalorieGoal = it },
                        label = "Calorie Goal (kcal)",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    SettingsTextField(
                        value = dailyWaterGoal,
                        onValueChange = { dailyWaterGoal = it },
                        label = "Water Goal (glasses)",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Save Button ──────────────────────────────────────────────────
            Button(
                onClick = {
                    val allergyString = selectedAllergies.filter { it != "None" }.joinToString(", ")
                    onSave(
                        user.copy(
                            firstName        = firstName.trim(),
                            lastName         = lastName.trim(),
                            age              = age.toIntOrNull() ?: user.age,
                            height           = height.toFloatOrNull() ?: user.height,
                            weight           = weight.toFloatOrNull() ?: user.weight,
                            sex              = sex,
                            bloodType        = if (bloodType == "Unknown") "" else bloodType,
                            allergies        = allergyString,
                            dietPreference   = if (dietPreference == "None") "" else dietPreference,
                            dailyCalorieGoal = dailyCalorieGoal.toIntOrNull() ?: user.dailyCalorieGoal,
                            dailyWaterGoal   = dailyWaterGoal.toIntOrNull() ?: user.dailyWaterGoal
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HeaderBg)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }

            // ── Logout Button ────────────────────────────────────────────────
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE53935)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextMuted,
        letterSpacing = 1.sp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand,
            unfocusedBorderColor = Divider,
            focusedLabelColor = Brand,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun SettingsSelectorChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Brand.copy(alpha = 0.14f) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Brand else Divider,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Brand else TextSecondary
        )
    }
}

@Composable
private fun SettingsAllergyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Brand.copy(alpha = 0.14f) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Brand else Divider,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Brand else TextSecondary,
            maxLines = 1
        )
    }
}
