package com.team8.smartfridge.ui.fitness

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.team8.smartfridge.data.FirebaseRepository
import com.team8.smartfridge.model.DailyLog
import com.team8.smartfridge.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Design tokens — Garden Terminal palette ────────────────────────────────────

private val FitBg      = Color(0xFFF2F0E8)   // gt_linen
private val FitCard    = Color(0xFFE8E6DC)   // gt_parchment
private val FitDivider = Color(0xFFC8C4B4)   // gt_border_default
private val FitText    = Color(0xFF1A2E10)   // gt_text_primary
private val FitMuted   = Color(0xFF8A9A78)   // gt_text_tertiary
private val FitBlue    = Color(0xFF2A6080)   // gt_water_blue
private val FitOrange  = Color(0xFFB8750A)   // gt_missing_amber — calories
private val FitGreen   = Color(0xFF3A6B1A)   // gt_deep_leaf — goals / weight
private val FitCyan    = Color(0xFF4A6A30)   // gt_moss — carbs
private val FitPurple  = Color(0xFF8B2020)   // gt_allergen_red — fat
private val FitAmber   = Color(0xFF5A8A2A)   // gt_mid_leaf — protein

private fun dateFormatter() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private fun dayLabelFormatter() = SimpleDateFormat("EEE", Locale.getDefault())

// ── ViewModel ─────────────────────────────────────────────────────────────────

class FitnessViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FirebaseRepository()

    private val _todayLog   = MutableStateFlow<DailyLog?>(null)
    val todayLog: StateFlow<DailyLog?> = _todayLog

    private val _weekLogs   = MutableStateFlow<List<Pair<String, DailyLog?>>>(emptyList())
    val weekLogs: StateFlow<List<Pair<String, DailyLog?>>> = _weekLogs

    private val _isLoading  = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun load() {
        val uid = repo.currentUid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fmt = dateFormatter()
                val today = fmt.format(Date())
                var log = repo.getLogForDate(uid, today)
                if (log == null) {
                    log = DailyLog(date = today)
                    repo.saveLog(uid, log)
                    log = repo.getLogForDate(uid, today)
                }
                _todayLog.value = log

                val dayFmt = dayLabelFormatter()
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                val week = (0..6).map { offset ->
                    val c = cal.clone() as Calendar
                    c.add(Calendar.DAY_OF_YEAR, offset)
                    val dateStr = fmt.format(c.time)
                    val label   = dayFmt.format(c.time).take(1)
                    label to repo.getLogForDate(uid, dateStr)
                }
                _weekLogs.value = week
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addWater() {
        val uid = repo.currentUid ?: return
        viewModelScope.launch {
            val log = _todayLog.value ?: return@launch
            val updated = log.copy(waterIntake = log.waterIntake + 1)
            repo.saveLog(uid, updated)
            _todayLog.value = updated
            refreshWeek(uid)
        }
    }

    fun removeWater() {
        val uid = repo.currentUid ?: return
        viewModelScope.launch {
            val log = _todayLog.value ?: return@launch
            if (log.waterIntake <= 0) return@launch
            val updated = log.copy(waterIntake = log.waterIntake - 1)
            repo.saveLog(uid, updated)
            _todayLog.value = updated
            refreshWeek(uid)
        }
    }

    private suspend fun refreshWeek(uid: String) {
        val fmt = dateFormatter()
        val dayFmt = dayLabelFormatter()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        _weekLogs.value = (0..6).map { offset ->
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, offset)
            val dateStr = fmt.format(c.time)
            val label   = dayFmt.format(c.time).take(1)
            label to repo.getLogForDate(uid, dateStr)
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

private val tabs = listOf("Overview", "Calories", "Water", "Weight")

@Composable
fun FitnessScreen(
    user: User?,
    viewModel: FitnessViewModel = viewModel()
) {
    val todayLog  by viewModel.todayLog.collectAsState()
    val weekLogs  by viewModel.weekLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    val calorieGoal = user?.dailyCalorieGoal ?: 2000
    val waterGoal   = user?.dailyWaterGoal   ?: 8
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FitBg)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Color(0xFF1C3B22))
                .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 0.dp)
        ) {
            Text(
                "FITNESS",
                fontSize = 10.sp,
                letterSpacing = 1.3.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF5A7A5A)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                "Fitness",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFFFFF),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                "Track your progress",
                fontSize = 11.sp,
                color = Color(0xFF5A7A5A)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── Tab row ──────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = index == selectedTab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                            .padding(bottom = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFFFFFFFF) else Color(0xFF5A7A5A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isSelected) Color(0xFFFFFFFF) else Color.Transparent)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFFFFFFF).copy(alpha = 0.1f), thickness = 1.dp)
        }

        // ── Content ───────────────────────────────────────────────────────────
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = FitBlue, strokeWidth = 2.dp) }
            return@Column
        }

        when (selectedTab) {
            0 -> OverviewTab(todayLog, weekLogs, calorieGoal, waterGoal)
            1 -> CaloriesTab(todayLog, weekLogs, calorieGoal)
            2 -> WaterTab(todayLog, weekLogs, waterGoal, viewModel)
            3 -> WeightTab(user)
        }
    }
}

// ── Tab 0: Overview ───────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(
    log: DailyLog?,
    week: List<Pair<String, DailyLog?>>,
    calorieGoal: Int,
    waterGoal: Int
) {
    val consumed  = log?.totalCalories ?: 0f
    val progress  = if (calorieGoal > 0) (consumed / calorieGoal).coerceIn(0f, 1f) else 0f
    val remaining = (calorieGoal - consumed).toInt().coerceAtLeast(0)
    val water     = log?.waterIntake ?: 0
    val proteinGoal = (calorieGoal * 0.25f / 4f).toInt()
    val carbGoal    = (calorieGoal * 0.50f / 4f).toInt()
    val fatGoal     = (calorieGoal * 0.25f / 9f).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Calorie ring
        FitCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Calories", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FitText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${consumed.toInt()} consumed", fontSize = 13.sp, color = FitMuted)
                    Text("$remaining remaining", fontSize = 13.sp, color = FitOrange)
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                    Canvas(modifier = Modifier.size(88.dp)) {
                        val stroke = 9.dp.toPx()
                        val r = (size.minDimension - stroke) / 2
                        val tl = Offset((size.width - r * 2) / 2, (size.height - r * 2) / 2)
                        drawArc(FitDivider, -90f, 360f, false, tl, Size(r * 2, r * 2),
                            style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(FitOrange, -90f, progress * 360f, false, tl, Size(r * 2, r * 2),
                            style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FitText
                    )
                }
            }
        }

        // Macros row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MacroChipCard("Protein", log?.totalProtein ?: 0f, proteinGoal.toFloat(), "g", FitAmber, Modifier.weight(1f))
            MacroChipCard("Carbs", log?.totalCarbs ?: 0f, carbGoal.toFloat(), "g", FitCyan, Modifier.weight(1f))
            MacroChipCard("Fat", log?.totalFat ?: 0f, fatGoal.toFloat(), "g", FitPurple, Modifier.weight(1f))
        }

        // Water
        FitCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(FitBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = FitBlue, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Water", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FitText)
                        Text("$water / $waterGoal glasses", fontSize = 13.sp, color = FitMuted)
                    }
                }
                Text(
                    "${if (waterGoal > 0) (water.toFloat() / waterGoal).coerceIn(0f, 1f).times(100).toInt() else 0}%",
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = FitBlue
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (waterGoal > 0) (water.toFloat() / waterGoal).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = FitBlue,
                trackColor = FitDivider
            )
        }

        // Weekly calorie bar chart
        FitCard {
            Text("This Week", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FitText)
            Spacer(modifier = Modifier.height(4.dp))
            val avg = week.mapNotNull { it.second?.totalCalories }.let { if (it.isEmpty()) 0f else it.average().toFloat() }
            Text("Avg ${avg.toInt()} kcal/day", fontSize = 12.sp, color = FitMuted)
            Spacer(modifier = Modifier.height(16.dp))
            WeekBarChart(
                days = week,
                maxValue = week.maxOfOrNull { it.second?.totalCalories ?: 0f }
                    .let { maxOf(it ?: 0f, calorieGoal.toFloat()) },
                barColor = FitOrange,
                valueSelector = { it?.totalCalories ?: 0f }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Tab 1: Calories ───────────────────────────────────────────────────────────

@Composable
private fun CaloriesTab(
    log: DailyLog?,
    week: List<Pair<String, DailyLog?>>,
    calorieGoal: Int
) {
    val consumed    = log?.totalCalories ?: 0f
    val remaining   = (calorieGoal - consumed).toInt().coerceAtLeast(0)
    val progress    = if (calorieGoal > 0) (consumed / calorieGoal).coerceIn(0f, 1f) else 0f
    val proteinGoal = (calorieGoal * 0.25f / 4f).toInt()
    val carbGoal    = (calorieGoal * 0.50f / 4f).toInt()
    val fatGoal     = (calorieGoal * 0.25f / 9f).toInt()
    val avg = week.mapNotNull { it.second?.totalCalories }.let { if (it.isEmpty()) 0f else it.average().toFloat() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Big calorie summary card
        FitCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Today's Calories", fontSize = 13.sp, color = FitMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${consumed.toInt()}",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FitOrange,
                        letterSpacing = (-1).sp
                    )
                    Text("of $calorieGoal kcal goal", fontSize = 13.sp, color = FitMuted)
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        val stroke = 10.dp.toPx()
                        val r = (size.minDimension - stroke) / 2
                        val tl = Offset((size.width - r * 2) / 2, (size.height - r * 2) / 2)
                        drawArc(FitDivider, -90f, 360f, false, tl, Size(r * 2, r * 2),
                            style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(FitOrange, -90f, progress * 360f, false, tl, Size(r * 2, r * 2),
                            style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$remaining", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = FitText)
                        Text("left", fontSize = 10.sp, color = FitMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = FitDivider)
            Spacer(modifier = Modifier.height(14.dp))

            // Stat row
            Row(modifier = Modifier.fillMaxWidth()) {
                CalStatItem("Goal", "$calorieGoal", "kcal", FitGreen, Modifier.weight(1f))
                CalStatItem("Consumed", "${consumed.toInt()}", "kcal", FitOrange, Modifier.weight(1f))
                CalStatItem("Remaining", "$remaining", "kcal", FitBlue, Modifier.weight(1f))
            }
        }

        // Weekly bar chart
        FitCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Weekly Calories", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FitText)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("This week's avg  ${avg.toInt()} kcal", fontSize = 12.sp, color = FitMuted)
            Spacer(modifier = Modifier.height(16.dp))
            WeekBarChart(
                days = week,
                maxValue = maxOf(week.maxOfOrNull { it.second?.totalCalories ?: 0f } ?: 0f, calorieGoal.toFloat()),
                barColor = FitOrange,
                valueSelector = { it?.totalCalories ?: 0f },
                goalLine = calorieGoal.toFloat()
            )
        }

        // Macros breakdown
        FitCard {
            Text("Macros", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FitText)
            Spacer(modifier = Modifier.height(16.dp))
            MacroRow(Icons.Default.Circle, "Carbs", log?.totalCarbs ?: 0f, carbGoal.toFloat(), FitCyan)
            HorizontalDivider(color = FitDivider, modifier = Modifier.padding(vertical = 12.dp))
            MacroRow(Icons.Default.Circle, "Fat", log?.totalFat ?: 0f, fatGoal.toFloat(), FitPurple)
            HorizontalDivider(color = FitDivider, modifier = Modifier.padding(vertical = 12.dp))
            MacroRow(Icons.Default.Circle, "Protein", log?.totalProtein ?: 0f, proteinGoal.toFloat(), FitAmber)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Tab 2: Water ──────────────────────────────────────────────────────────────

@Composable
private fun WaterTab(
    log: DailyLog?,
    week: List<Pair<String, DailyLog?>>,
    waterGoal: Int,
    viewModel: FitnessViewModel
) {
    val water   = log?.waterIntake ?: 0
    val progress = if (waterGoal > 0) (water.toFloat() / waterGoal).coerceIn(0f, 1f) else 0f
    val avg = week.mapNotNull { it.second?.waterIntake?.toFloat() }.let { if (it.isEmpty()) 0f else it.average().toFloat() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main water card
        FitCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Today's Water", fontSize = 13.sp, color = FitMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "$water",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FitBlue,
                    letterSpacing = (-2).sp
                )
                Text(
                    "of $waterGoal glasses",
                    fontSize = 14.sp,
                    color = FitMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = FitBlue,
                    trackColor = FitDivider
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${(progress * 100).toInt()}% of daily goal",
                    fontSize = 12.sp,
                    color = FitMuted,
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = FitDivider)
                Spacer(modifier = Modifier.height(20.dp))

                // +/- controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.removeWater() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(FitDivider)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Remove glass", tint = FitText, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = FitBlue, modifier = Modifier.size(28.dp))
                        Text("Log glass", fontSize = 11.sp, color = FitMuted)
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    IconButton(
                        onClick = { viewModel.addWater() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(FitBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add glass", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // Stats row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalStatItem("Today", "$water", "glasses", FitBlue, Modifier.weight(1f), card = true)
            CalStatItem("Goal", "$waterGoal", "glasses", FitGreen, Modifier.weight(1f), card = true)
            CalStatItem("Week avg", "${avg.toInt()}", "glasses", FitMuted, Modifier.weight(1f), card = true)
        }

        // Weekly bar chart
        FitCard {
            Text("This Week", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FitText)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Avg ${avg.toInt()} glasses/day", fontSize = 12.sp, color = FitMuted)
            Spacer(modifier = Modifier.height(16.dp))
            WeekBarChart(
                days = week,
                maxValue = maxOf(week.maxOfOrNull { it.second?.waterIntake?.toFloat() ?: 0f } ?: 0f, waterGoal.toFloat()),
                barColor = FitBlue,
                valueSelector = { it?.waterIntake?.toFloat() ?: 0f }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Tab 3: Weight ─────────────────────────────────────────────────────────────

@Composable
private fun WeightTab(user: User?) {
    val weightKg  = user?.weight ?: 0f
    val weightLbs = weightKg * 2.20462f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main weight card
        FitCard {
            Text("Current Weight", fontSize = 13.sp, color = FitMuted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (weightKg > 0) "%.1f".format(weightKg) else "--",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FitGreen,
                    letterSpacing = (-1.5).sp
                )
                Text("kg", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = FitMuted,
                    modifier = Modifier.padding(bottom = 10.dp))
            }
            if (weightKg > 0) {
                Text("${"%.1f".format(weightLbs)} lbs", fontSize = 14.sp, color = FitMuted)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = FitDivider)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                WeightStat("Logged", if (weightKg > 0) "${"%.1f".format(weightKg)} kg" else "--", Modifier.weight(1f))
                WeightStat("In lbs", if (weightKg > 0) "${"%.1f".format(weightLbs)} lbs" else "--", Modifier.weight(1f))
                WeightStat("Change", "—", Modifier.weight(1f))
            }
        }

        // BMI estimate (if weight available)
        if (weightKg > 0 && (user?.age ?: 0) > 0) {
            FitCard {
                Text("Body Info", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FitText)
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    BodyInfoItem("Age", "${user?.age ?: "--"} yrs", Modifier.weight(1f))
                    BodyInfoItem("Sex", user?.sex ?: "--", Modifier.weight(1f))
                    BodyInfoItem("Weight", "${"%.1f".format(weightKg)} kg", Modifier.weight(1f))
                }
            }
        }

        // Tip card
        FitCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(FitGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = FitGreen, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Weight tracking", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FitText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Log meals regularly to keep your calorie and macro data accurate. Weight shown is from your profile.",
                        fontSize = 13.sp,
                        color = FitMuted,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Shared components ─────────────────────────────────────────────────────────

@Composable
private fun FitCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = FitCard),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FitDivider)
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun WeekBarChart(
    days: List<Pair<String, DailyLog?>>,
    maxValue: Float,
    barColor: Color,
    valueSelector: (DailyLog?) -> Float,
    goalLine: Float? = null,
    barHeight: Dp = 120.dp
) {
    val todayStr = dateFormatter().format(Date())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { (label, dayLog) ->
            val value    = valueSelector(dayLog)
            val fraction = if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f
            val isToday  = dayLog?.date == todayStr

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Value label above bar (only if non-zero)
                if (value > 0f) {
                    Text(
                        value.toInt().toString(),
                        fontSize = 9.sp,
                        color = FitMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Box(
                    modifier = Modifier.height(barHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Track
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(FitDivider)
                    )
                    // Fill
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isToday) barColor else barColor.copy(alpha = 0.45f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    label,
                    fontSize = 11.sp,
                    color = if (isToday) FitText else FitMuted,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun MacroRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    current: Float,
    goal: Float,
    color: Color
) {
    val progress = if (goal > 0) (current / goal).coerceIn(0f, 1f) else 0f
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
            Spacer(modifier = Modifier.weight(1f))
            Text("${current.toInt()}g / ${goal.toInt()}g", fontSize = 12.sp, color = FitMuted)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = FitDivider
        )
    }
}

@Composable
private fun MacroChipCard(
    label: String, current: Float, goal: Float, unit: String,
    color: Color, modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = FitCard),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.height(8.dp))
            Text("${current.toInt()}$unit", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = FitText)
            Text(label, fontSize = 11.sp, color = FitMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (goal > 0) (current / goal).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = FitDivider
            )
        }
    }
}

@Composable
private fun CalStatItem(
    label: String, value: String, unit: String, color: Color,
    modifier: Modifier, card: Boolean = false
) {
    if (card) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = FitCard),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
                Text(unit, fontSize = 10.sp, color = FitMuted)
                Text(label, fontSize = 11.sp, color = FitMuted)
            }
        }
    } else {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(unit, fontSize = 10.sp, color = FitMuted)
            Text(label, fontSize = 11.sp, color = FitMuted)
        }
    }
}

@Composable
private fun WeightStat(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FitText)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = FitMuted)
    }
}

@Composable
private fun BodyInfoItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FitText)
        Text(label, fontSize = 11.sp, color = FitMuted)
    }
}
