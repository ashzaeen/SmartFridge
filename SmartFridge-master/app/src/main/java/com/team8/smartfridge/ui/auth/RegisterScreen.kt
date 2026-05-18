package com.team8.smartfridge.ui.auth

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Shared auth design tokens (used by RegisterScreen) ────────────────────────
val AuthDarkBg        = Color(0xFFF2F0E8)
val AuthDarkCard      = Color(0xFFE8E6DC)
val AuthDarkBorder    = Color(0xFFC8C4B4)
val AuthAccentBlue    = Color(0xFF3A6B1A)
val AuthProgressGreen = Color(0xFFB8D490)
val AuthTextPrimary   = Color(0xFF1A2E10)
val AuthTextSecondary = Color(0xFF5A7040)
val AuthErrorRed      = Color(0xFF8B2020)

@Composable
fun AuthFieldLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AuthTextSecondary)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onVisibilityToggle: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, color = AuthTextSecondary, fontSize = 14.sp) },
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onVisibilityToggle?.invoke(!passwordVisible) }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = AuthTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor       = AuthTextPrimary,
            unfocusedTextColor     = AuthTextPrimary,
            focusedBorderColor     = AuthAccentBlue,
            unfocusedBorderColor   = AuthDarkBorder,
            focusedContainerColor  = AuthDarkCard,
            unfocusedContainerColor = AuthDarkCard,
            cursorColor            = AuthAccentBlue
        )
    )
}

// ── Screen ─────────────────────────────────────────────────────────────────────
// 5-step dark onboarding:
//  1 → Name
//  2 → Goals (maps to dietPreference)
//  3 → Food allergies / intolerances
//  4 → Personal info (sex, age, weight)
//  5 → Account setup (email, password, goals)

private const val TOTAL_STEPS = 5

@Composable
fun RegisterScreen(
    onRegisterClick: (
        firstName: String, lastName: String, email: String, password: String,
        age: Int, sex: String, weight: Float, height: Float, bloodType: String,
        allergies: String, dietPreference: String,
        dailyCalorieGoal: Int, dailyWaterGoal: Int
    ) -> Unit,
    onBackToLogin: () -> Unit,
    isLoading: Boolean = false,
    authError: String? = null
) {
    var step by remember { mutableStateOf(1) }
    var errorMessage by remember { mutableStateOf("") }

    // Step 1
    var firstName by remember { mutableStateOf("") }
    var lastName  by remember { mutableStateOf("") }

    // Step 2 — goals (up to 3, stored as comma-separated dietPreference)
    val goalOptions = listOf(
        "Lose Weight", "Maintain Weight", "Gain Weight",
        "Gain Muscle", "Eat Healthier", "Plan Meals",
        "Track Calories", "Drink More Water"
    )
    var selectedGoals by remember { mutableStateOf(setOf<String>()) }

    // Step 3 — allergies
    val allergyOptions = listOf(
        "Gluten", "Dairy", "Nuts", "Shellfish",
        "Eggs", "Soy", "Fish", "Wheat", "None"
    )
    var selectedAllergies by remember { mutableStateOf(setOf<String>()) }

    // Step 4 — personal
    var selectedSex by remember { mutableStateOf("Male") }
    var age    by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    // Step 5 — account
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var calorieGoal     by remember { mutableStateOf("2000") }
    var waterGoal       by remember { mutableStateOf("8") }

    fun validate(): String = when (step) {
        1 -> if (firstName.isBlank()) "Please enter your first name" else ""
        4 -> {
            val a = age.toIntOrNull()
            val w = weight.toFloatOrNull()
            when {
                age.isBlank() || weight.isBlank()         -> "Please fill in both fields"
                a == null || a < 1 || a > 120             -> "Enter a valid age (1–120)"
                w == null || w < 10f || w > 500f          -> "Enter a valid weight (10–500 kg)"
                else                                       -> ""
            }
        }
        5 -> when {
            email.isBlank() || password.isBlank()         -> "Please fill in all fields"
            !email.contains("@") || !email.contains(".") -> "Enter a valid email address"
            password.length < 6                           -> "Password must be at least 6 characters"
            else                                           -> ""
        }
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(52.dp))

            Text(
                text = stepTitle(step),
                color = AuthTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress segments
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(TOTAL_STEPS) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i < step) AuthProgressGreen else AuthDarkBorder)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Step content ─────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (step) {
                    1 -> StepName(firstName, lastName, { firstName = it }, { lastName = it })
                    2 -> StepGoals(firstName, goalOptions, selectedGoals) { goal ->
                        selectedGoals =
                            if (selectedGoals.contains(goal)) selectedGoals - goal
                            else if (selectedGoals.size < 3) selectedGoals + goal
                            else selectedGoals
                    }
                    3 -> StepAllergies(allergyOptions, selectedAllergies) { allergy ->
                        selectedAllergies = if (allergy == "None") {
                            setOf("None")
                        } else {
                            val without = selectedAllergies - "None"
                            if (without.contains(allergy)) without - allergy else without + allergy
                        }
                    }
                    4 -> StepPersonal(selectedSex, age, height, weight, { selectedSex = it }, { age = it }, { height = it }, { weight = it })
                    5 -> StepAccount(
                        email, password, passwordVisible, calorieGoal, waterGoal,
                        { email = it }, { password = it }, { passwordVisible = it },
                        { calorieGoal = it }, { waterGoal = it }
                    )
                }
            }

            // Error (local validation or server)
            val displayError = errorMessage.ifEmpty { if (step == TOTAL_STEPS) authError ?: "" else "" }
            if (displayError.isNotEmpty()) {
                Text(
                    displayError,
                    color = AuthErrorRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // ── Bottom navigation ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back circle button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AuthDarkCard)
                        .clickable {
                            errorMessage = ""
                            if (step > 1) step-- else onBackToLogin()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AuthTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Next / Create button
                Button(
                    onClick = {
                        val err = validate()
                        if (err.isNotEmpty()) {
                            errorMessage = err
                        } else {
                            errorMessage = ""
                            if (step < TOTAL_STEPS) {
                                step++
                            } else {
                                onRegisterClick(
                                    firstName.trim(),
                                    lastName.trim(),
                                    email.trim(),
                                    password,
                                    age.toIntOrNull() ?: 0,
                                    selectedSex,
                                    weight.toFloatOrNull() ?: 0f,
                                    height.toFloatOrNull() ?: 0f,
                                    "",   // bloodType not collected
                                    selectedAllergies.filter { it != "None" }.joinToString(", "),
                                    selectedGoals.joinToString(", "),
                                    calorieGoal.toIntOrNull() ?: 2000,
                                    waterGoal.toIntOrNull() ?: 8
                                )
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AuthAccentBlue)
                ) {
                    if (isLoading && step == TOTAL_STEPS) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = if (step == TOTAL_STEPS) "Create Account" else "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun stepTitle(step: Int) = when (step) {
    1    -> "Welcome"
    2, 3 -> "Goals"
    4    -> "About You"
    5    -> "Account"
    else -> ""
}

// ── Step 1: Name ──────────────────────────────────────────────────────────────

@Composable
private fun StepName(
    firstName: String, lastName: String,
    onFirstName: (String) -> Unit, onLastName: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Text(
            "First, what can\nwe call you?",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AuthTextPrimary,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "We'd like to get to know you.",
            fontSize = 14.sp,
            color = AuthTextSecondary
        )
        Spacer(modifier = Modifier.height(32.dp))
        OnboardingLabel("Preferred first name")
        AuthTextField(value = firstName, onValueChange = onFirstName, placeholder = "First name")
        Spacer(modifier = Modifier.height(16.dp))
        OnboardingLabel("Last name (optional)")
        AuthTextField(value = lastName, onValueChange = onLastName, placeholder = "Last name")
    }
}

// ── Step 2: Goals ─────────────────────────────────────────────────────────────

@Composable
private fun StepGoals(
    firstName: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Text(
            "Hey${if (firstName.isNotBlank()) ", $firstName" else ""}! 👋\nLet's start with your goals.",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AuthTextPrimary,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Select up to three that matter most to you.",
            fontSize = 14.sp,
            color = AuthTextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        options.forEach { option ->
            val isSelected = selected.contains(option)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuthDarkCard)
                    .then(
                        if (isSelected)
                            Modifier.border(1.5.dp, AuthAccentBlue, RoundedCornerShape(12.dp))
                        else
                            Modifier
                    )
                    .clickable { onToggle(option) }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(option, fontSize = 15.sp, color = AuthTextPrimary, fontWeight = FontWeight.Medium)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AuthAccentBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.5.dp, AuthTextSecondary, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Step 3: Allergies ─────────────────────────────────────────────────────────

@Composable
private fun StepAllergies(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Text(
            "Any food allergies\nor intolerances?",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AuthTextPrimary,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "We'll filter recipes to keep you safe.",
            fontSize = 14.sp,
            color = AuthTextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Wrapped chip rows
        options.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { option ->
                    val isSelected = selected.contains(option)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) AuthAccentBlue.copy(alpha = 0.18f) else AuthDarkCard
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AuthAccentBlue else AuthDarkBorder,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onToggle(option) }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            option,
                            fontSize = 14.sp,
                            color = if (isSelected) AuthAccentBlue else AuthTextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

// ── Step 4: Personal info ─────────────────────────────────────────────────────

@Composable
private fun StepPersonal(
    selectedSex: String, age: String, height: String, weight: String,
    onSex: (String) -> Unit, onAge: (String) -> Unit, onHeight: (String) -> Unit, onWeight: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Text(
            "Tell us a little bit\nabout yourself",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AuthTextPrimary,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "We use this to calculate your calorie needs.",
            fontSize = 14.sp,
            color = AuthTextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Sex selector
        Text("Which sex should we use?", fontSize = 13.sp, color = AuthTextSecondary)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Male", "Female", "Other").forEach { sex ->
                val isSelected = selectedSex == sex
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AuthDarkCard)
                        .then(
                            if (isSelected)
                                Modifier.border(1.5.dp, AuthAccentBlue, RoundedCornerShape(8.dp))
                            else
                                Modifier
                        )
                        .clickable { onSex(sex) }
                        .padding(vertical = 14.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        sex,
                        fontSize = 14.sp,
                        color = AuthTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(AuthAccentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, AuthTextSecondary, CircleShape)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "We use sex at birth and age to calculate an accurate goal for you.",
            fontSize = 12.sp,
            color = AuthTextSecondary,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))
        OnboardingLabel("How old are you?")
        AuthTextField(
            value = age,
            onValueChange = onAge,
            placeholder = "Age",
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(16.dp))
        OnboardingLabel("What's your height? (cm)")
        AuthTextField(
            value = height,
            onValueChange = onHeight,
            placeholder = "e.g. 170",
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(16.dp))
        OnboardingLabel("What's your weight? (kg)")
        AuthTextField(
            value = weight,
            onValueChange = onWeight,
            placeholder = "e.g. 70",
            keyboardType = KeyboardType.Number
        )
    }
}

// ── Step 5: Account setup ─────────────────────────────────────────────────────

@Composable
private fun StepAccount(
    email: String, password: String, passwordVisible: Boolean,
    calorieGoal: String, waterGoal: String,
    onEmail: (String) -> Unit, onPassword: (String) -> Unit,
    onVisibility: (Boolean) -> Unit,
    onCalorie: (String) -> Unit, onWater: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Text(
            "Create your\naccount",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AuthTextPrimary,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Almost done! Set up your login details.",
            fontSize = 14.sp,
            color = AuthTextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))

        OnboardingLabel("Email address")
        AuthTextField(
            value = email,
            onValueChange = onEmail,
            placeholder = "your@email.com",
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))
        OnboardingLabel("Password")
        AuthTextField(
            value = password,
            onValueChange = onPassword,
            placeholder = "Min. 6 characters",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            passwordVisible = passwordVisible,
            onVisibilityToggle = onVisibility
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Daily goals",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AuthTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "You can always change these later.",
            fontSize = 13.sp,
            color = AuthTextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                OnboardingLabel("Calorie goal (kcal)")
                AuthTextField(
                    value = calorieGoal,
                    onValueChange = onCalorie,
                    placeholder = "2000",
                    keyboardType = KeyboardType.Number
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                OnboardingLabel("Water goal (glasses)")
                AuthTextField(
                    value = waterGoal,
                    onValueChange = onWater,
                    placeholder = "8",
                    keyboardType = KeyboardType.Number
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun OnboardingLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AuthTextSecondary
    )
    Spacer(modifier = Modifier.height(8.dp))
}
