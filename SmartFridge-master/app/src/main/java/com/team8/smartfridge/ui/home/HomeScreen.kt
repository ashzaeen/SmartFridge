package com.team8.smartfridge.ui.home

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.team8.smartfridge.data.FirebaseRepository
import com.team8.smartfridge.model.DailyLog
import com.team8.smartfridge.model.User
import com.team8.smartfridge.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── ViewModel ─────────────────────────────────────────────────────────────────

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FirebaseRepository()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _todayLog = MutableStateFlow<DailyLog?>(null)
    val todayLog: StateFlow<DailyLog?> = _todayLog

    private val _weekActivity = MutableStateFlow(List(7) { false })
    val weekActivity: StateFlow<List<Boolean>> = _weekActivity

    private val _selectedLog = MutableStateFlow<DailyLog?>(null)
    val selectedLog: StateFlow<DailyLog?> = _selectedLog

    private val _streak = MutableStateFlow(1)
    val streak: StateFlow<Int> = _streak

    fun loadToday() {
        val uid = repo.currentUid ?: return
        viewModelScope.launch {
            try {
                val today = sdf.format(Date())
                var log = repo.getLogForDate(uid, today)
                if (log == null) {
                    log = DailyLog(date = today)
                    repo.saveLog(uid, log)
                    log = repo.getLogForDate(uid, today)
                }
                _todayLog.value = log
                _selectedLog.value = log

                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                val activity = (0..6).map { offset ->
                    val c = cal.clone() as Calendar
                    c.add(Calendar.DAY_OF_YEAR, offset)
                    val dayLog = repo.getLogForDate(uid, sdf.format(c.time))
                    (dayLog?.totalCalories ?: 0f) > 0f
                }
                _weekActivity.value = activity

                val allLogs = repo.getAllLogs(uid)
                val logDates = allLogs.filter { it.totalCalories > 0f }.map { it.date }.toSet()

                val dayCal = Calendar.getInstance()
                if (sdf.format(dayCal.time) !in logDates) {
                    dayCal.add(Calendar.DAY_OF_YEAR, -1)
                }
                var count = 0
                while (sdf.format(dayCal.time) in logDates) {
                    count++
                    dayCal.add(Calendar.DAY_OF_YEAR, -1)
                }
                _streak.value = count.coerceAtLeast(1)
            } catch (_: Exception) { }
        }
    }

    fun loadDayLog(dayIndex: Int) {
        val uid = repo.currentUid ?: return
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                cal.add(Calendar.DAY_OF_YEAR, dayIndex)
                val date = sdf.format(cal.time)
                _selectedLog.value = repo.getLogForDate(uid, date)
            } catch (_: Exception) { }
        }
    }

    fun logCustomEntry(calories: Float, protein: Float, carbs: Float, fat: Float) {
        val uid = repo.currentUid ?: return
        viewModelScope.launch {
            try {
                val today = sdf.format(Date())
                val log = repo.getLogForDate(uid, today) ?: DailyLog(date = today)
                val updated = log.copy(
                    totalCalories = log.totalCalories + calories,
                    totalProtein  = log.totalProtein  + protein,
                    totalCarbs    = log.totalCarbs    + carbs,
                    totalFat      = log.totalFat      + fat
                )
                repo.saveLog(uid, updated)
                _todayLog.value    = updated
                _selectedLog.value = updated
            } catch (_: Exception) { }
        }
    }
}

// ── Local header palette ───────────────────────────────────────────────────────
private val HeaderBg      = Color(0xFF1C3B22)
private val HeaderSubtext = Color(0xFF5A7A5A)
private val HeaderAvatar  = Color(0xFF2A5830)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    user: User?,
    onNavigateToRecipes: (String) -> Unit,
    onNavigateToPantry: () -> Unit,
    onNavigateToFitness: () -> Unit,
    onNavigateToWater: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val todayLog     by viewModel.todayLog.collectAsState()
    val weekActivity by viewModel.weekActivity.collectAsState()
    val selectedLog  by viewModel.selectedLog.collectAsState()
    val streak       by viewModel.streak.collectAsState()

    val todayDowInit = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
    var selectedDayIndex by remember { mutableIntStateOf(todayDowInit) }
    var showCustomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadToday()
    }

    val displayLog = if (selectedDayIndex == todayDowInit) todayLog else selectedLog

    val caloriesConsumed = displayLog?.totalCalories?.toInt() ?: 0
    val caloriesGoal     = user?.dailyCalorieGoal ?: 2000
    val caloriesLeft     = (caloriesGoal - caloriesConsumed).coerceAtLeast(0)
    val calorieProgress  = if (caloriesGoal > 0) (caloriesConsumed.toFloat() / caloriesGoal).coerceIn(0f, 1f) else 0f

    val proteinGoal = (caloriesGoal * 0.25f / 4f).toInt()
    val carbGoal    = (caloriesGoal * 0.50f / 4f).toInt()
    val fatGoal     = (caloriesGoal * 0.25f / 9f).toInt()
    val protein     = displayLog?.totalProtein?.toInt() ?: 0
    val carbs       = displayLog?.totalCarbs?.toInt()   ?: 0
    val fat         = displayLog?.totalFat?.toInt()     ?: 0

    val waterConsumed = displayLog?.waterIntake ?: 0
    val waterGoal     = user?.dailyWaterGoal ?: 8

    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11  -> "Good morning"
        in 12..16 -> "Good afternoon"
        else      -> "Good evening"
    }
    val dayLabel = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    val todayDow = todayDowInit

    // ── Root layout: fixed header + scrollable content ─────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSecondary)
    ) {

        // ── Option A header ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(HeaderBg)
                .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 0.dp)
        ) {
                Column {
                    // ── Top row: greeting + streak pill + avatar ──────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left: greeting / name / date
                        Column {
                            Text(
                                text = greeting.uppercase(),
                                fontSize = 10.sp,
                                letterSpacing = 1.3.sp,
                                fontWeight = FontWeight.Normal,
                                color = HeaderSubtext
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = user?.firstName ?: "there",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = White,
                                lineHeight = 28.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = dayLabel,
                                fontSize = 11.sp,
                                color = HeaderSubtext,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Right: streak pill + avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Streak pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(White.copy(alpha = 0.07f))
                                    .border(1.dp, White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$streak",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = White,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "day streak",
                                        fontSize = 8.sp,
                                        color = HeaderSubtext,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(HeaderAvatar)
                                    .border(1.5.dp, White.copy(alpha = 0.12f), CircleShape)
                                    .clickable { onNavigateToSettings() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user?.firstName?.firstOrNull()?.uppercase() ?: "U",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            }
                        }
                    }

                    // ── Week strip ────────────────────────────────────────────
                    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayLetters.forEachIndexed { index, letter ->
                            val isSelected   = index == selectedDayIndex
                            val isPast       = index < todayDow
                            val isSelectable = index <= todayDow

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected)
                                            Modifier
                                                .background(Color.Transparent, CircleShape)
                                                .border(2.dp, White.copy(alpha = 0.55f), CircleShape)
                                        else
                                            Modifier.background(White.copy(alpha = 0.07f), CircleShape)
                                    )
                                    .clickable(enabled = isSelectable) {
                                        selectedDayIndex = index
                                        if (index != todayDow) viewModel.loadDayLog(index)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) White else Color(0xFF4A6A4A)
                                )
                            }
                        }
                    }
                }
            }

        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Past-day banner
            if (selectedDayIndex != todayDow) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                cal.add(Calendar.DAY_OF_YEAR, selectedDayIndex)
                val selectedDateLabel = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cal.time)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandAccent.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Viewing $selectedDateLabel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandAccent
                    )
                    Text(
                        text = "Today",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Brand,
                        modifier = Modifier.clickable { selectedDayIndex = todayDow }
                    )
                }
            }

            // ── Option C: Calorie card ────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgPrimary),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    // Top: label + remaining
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CALORIES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            "$caloriesLeft left",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Brand
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // Inner: big number + ring
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$caloriesConsumed",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                lineHeight = 42.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "/ $caloriesGoal cal",
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        // Ring
                        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(52.dp)) {
                                val stroke = 5.dp.toPx()
                                val r = (size.width - stroke) / 2
                                val tl = Offset(stroke / 2, stroke / 2)
                                val sz = Size(size.width - stroke, size.height - stroke)
                                drawArc(Color(0xFFE0DAD0), 0f, 360f, false, tl, sz, style = Stroke(stroke))
                                drawArc(Color(0xFF3A8040), -90f, calorieProgress * 360f, false, tl, sz,
                                    style = Stroke(stroke, cap = StrokeCap.Round))
                            }
                            Text(
                                "${(calorieProgress * 100).toInt()}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color(0xFFE0DAD0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(calorieProgress)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color(0xFF3A8040))
                        )
                    }
                }
            }

            // ── Option C: Macro cards ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                MacroCard(label = "CARBS",   current = carbs,   goal = carbGoal,   accentColor = Color(0xFF3A8040), modifier = Modifier.weight(1f))
                MacroCard(label = "FAT",     current = fat,     goal = fatGoal,    accentColor = Color(0xFF2A6090), modifier = Modifier.weight(1f))
                MacroCard(label = "PROTEIN", current = protein, goal = proteinGoal,accentColor = Color(0xFF6E6E6E), modifier = Modifier.weight(1f))
            }

            // ── Option C: Water card ──────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgPrimary),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.06f)),
                onClick = onNavigateToWater
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6DBE73).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null,
                            tint = Color(0xFF6DBE73), modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text("Water", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("$waterConsumed / $waterGoal glasses", fontSize = 10.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val waterPct = if (waterGoal > 0) (waterConsumed.toFloat() / waterGoal).coerceIn(0f, 1f) else 0f
                        Text(
                            "${(waterPct * 100).toInt()}%",
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6DBE73)
                        )
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color(0xFFE0DAD0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(waterPct)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(Color(0xFF6DBE73))
                            )
                        }
                    }
                }
            }

            // ── Diary section ─────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Diary",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        "Log a meal",
                        fontSize = 12.sp,
                        color = Brand,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToRecipes("All") }
                    )
                }

                // 2×2 grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MealGridCard(
                            name       = "Breakfast",
                            icon       = Icons.Default.FreeBreakfast,
                            subtitle   = "Start your day right",
                            iconBg     = Color(0xFFE8F0E0),
                            iconTint   = Color(0xFF3A8040),
                            modifier   = Modifier.weight(1f),
                            onLog      = { onNavigateToRecipes("Breakfast") }
                        )
                        MealGridCard(
                            name       = "Lunch",
                            icon       = Icons.Default.LunchDining,
                            subtitle   = "Midday fuel",
                            iconBg     = Color(0xFFE0EAE8),
                            iconTint   = Color(0xFF2A7060),
                            modifier   = Modifier.weight(1f),
                            onLog      = { onNavigateToRecipes("Lunch") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MealGridCard(
                            name       = "Dinner",
                            icon       = Icons.Default.DinnerDining,
                            subtitle   = "Evening meal",
                            iconBg     = Color(0xFFEAE8E0),
                            iconTint   = Color(0xFF706040),
                            modifier   = Modifier.weight(1f),
                            onLog      = { onNavigateToRecipes("Dinner") }
                        )
                        MealGridCard(
                            name       = "Snacks",
                            icon       = Icons.Default.Fastfood,
                            subtitle   = "Extras & snacks",
                            iconBg     = Color(0xFFEDE8E0),
                            iconTint   = Color(0xFF8A6040),
                            modifier   = Modifier.weight(1f),
                            onLog      = { onNavigateToRecipes("Snacks") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom calories dashed row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A5830).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF2A5830).copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                        .clickable { showCustomDialog = true }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Brand,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "Can't find it? Add custom calories",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Brand
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showCustomDialog) {
        CustomCaloriesDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { calories, protein, carbs, fat ->
                viewModel.logCustomEntry(calories, protein, carbs, fat)
                showCustomDialog = false
            }
        )
    }
}

// ── Custom calories dialog ────────────────────────────────────────────────────

@Composable
private fun CustomCaloriesDialog(
    onDismiss: () -> Unit,
    onConfirm: (calories: Float, protein: Float, carbs: Float, fat: Float) -> Unit
) {
    var name     by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein  by remember { mutableStateOf("") }
    var carbs    by remember { mutableStateOf("") }
    var fat      by remember { mutableStateOf("") }
    var error    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = BgPrimary,
        shape            = RoundedCornerShape(14.dp),
        title = {
            Text(
                "Add Custom Entry",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "For food not found in Recipes. Only calories is required.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Name (optional)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name (optional)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = customFieldColors()
                )

                // Calories (required)
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { c -> c.isDigit() || c == '.' }; error = false },
                    label = { Text("Calories (kcal) *", fontSize = 12.sp) },
                    singleLine = true,
                    isError = error,
                    supportingText = if (error) {{ Text("Enter a valid calorie amount", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }} else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = customFieldColors()
                )

                // Macros row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Protein g", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = customFieldColors()
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Carbs g", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = customFieldColors()
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Fat g", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = customFieldColors()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = calories.toFloatOrNull()
                    if (cal == null || cal <= 0f) { error = true; return@Button }
                    onConfirm(
                        cal,
                        protein.toFloatOrNull() ?: 0f,
                        carbs.toFloatOrNull()   ?: 0f,
                        fat.toFloatOrNull()     ?: 0f
                    )
                },
                shape  = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand)
            ) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun customFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Brand,
    unfocusedBorderColor = Divider,
    focusedLabelColor    = Brand,
    unfocusedLabelColor  = TextSecondary,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary,
    cursorColor          = Brand,
    focusedContainerColor   = BgPrimary,
    unfocusedContainerColor = BgPrimary
)

// ── Macro card ────────────────────────────────────────────────────────────────

@Composable
private fun MacroCard(
    label:       String,
    current:     Int,
    goal:        Int,
    accentColor: Color,
    modifier:    Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgPrimary)
            .border(0.5.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            Text(
                label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.7.sp
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                "${current}g",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                lineHeight = 18.sp
            )
            Text(
                "/ $goal",
                fontSize = 9.sp,
                color = Color(0xFFA0AA90)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        // Bottom accent bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(accentColor)
        )
    }
}

// ── Meal row ──────────────────────────────────────────────────────────────────

@Composable
private fun MealGridCard(
    name:     String,
    icon:     ImageVector,
    subtitle: String,
    iconBg:   Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onLog:    () -> Unit
) {
    Card(
        onClick = onLog,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgPrimary),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(name,     fontSize = 13.sp, fontWeight = FontWeight.Bold,   color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            // Progress bar (3dp, empty by default — no per-meal tracking)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFFE0DAD0))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Log", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Brand)
        }
    }
}


