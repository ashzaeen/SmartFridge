package com.team8.smartfridge.ui.water

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.team8.smartfridge.data.FirebaseRepository
import com.team8.smartfridge.model.DailyLog
import com.team8.smartfridge.model.User
import com.team8.smartfridge.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ── ViewModel ─────────────────────────────────────────────────────────────────

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FirebaseRepository()

    private val _glassCount = MutableStateFlow(0)
    val glassCount: StateFlow<Int> = _glassCount

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentLog: DailyLog? = null

    fun loadToday() {
        val uid = repo.currentUid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            var log = repo.getLogForDate(uid, today)
            if (log == null) {
                log = DailyLog(date = today)
                repo.saveLog(uid, log)
                log = repo.getLogForDate(uid, today)
            }
            currentLog = log
            _glassCount.value = log?.waterIntake ?: 0
            _isLoading.value = false
        }
    }

    fun addGlass() {
        val newCount = _glassCount.value + 1
        _glassCount.value = newCount
        viewModelScope.launch { persistWater(newCount) }
    }

    fun removeGlass() {
        if (_glassCount.value <= 0) return
        val newCount = _glassCount.value - 1
        _glassCount.value = newCount
        viewModelScope.launch { persistWater(newCount) }
    }

    private suspend fun persistWater(count: Int) {
        val uid = repo.currentUid ?: return
        try {
            val log = currentLog ?: return
            val updated = log.copy(waterIntake = count)
            repo.saveLog(uid, updated)
            currentLog = updated
        } catch (_: Exception) {
            _glassCount.value = currentLog?.waterIntake ?: 0
        }
    }
}

// ── Midnight Reset Worker ─────────────────────────────────────────────────────

class MidnightResetWorker(context: android.content.Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = Firebase.auth.currentUser?.uid ?: return Result.failure()
        val repo = FirebaseRepository()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val existing = repo.getLogForDate(uid, today)
        if (existing == null) {
            repo.saveLog(uid, DailyLog(date = today))
        }
        scheduleMidnightReset(applicationContext)
        return Result.success()
    }
}

fun scheduleMidnightReset(context: android.content.Context) {
    val midnight = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val delay = (midnight.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0)
    val request = OneTimeWorkRequestBuilder<MidnightResetWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork("midnight_reset", ExistingWorkPolicy.REPLACE, request)
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun WaterScreen(
    user: User?,
    viewModel: WaterViewModel = viewModel()
) {
    val glassCount by viewModel.glassCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val waterGoal = (user?.dailyWaterGoal ?: 8).coerceAtLeast(1)
    val progress = (glassCount.toFloat() / waterGoal).coerceIn(0f, 1f)

    LaunchedEffect(Unit) {
        viewModel.loadToday()
        scheduleMidnightReset(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSecondary),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPrimary)
                .padding(start = 24.dp, end = 24.dp, top = 52.dp, bottom = 16.dp)
        ) {
            Column {
                Text(
                    "Water Intake",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text("Stay hydrated today", fontSize = 13.sp, color = TextSecondary)
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Brand) }
            return@Column
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Water Drop Visual ─────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val w = size.width
                val h = size.height
                // Draw water drop shape (filled background)
                val dropPath = Path().apply {
                    moveTo(w / 2f, 0f)
                    cubicTo(w * 0.9f, h * 0.3f, w, h * 0.65f, w / 2f, h)
                    cubicTo(0f, h * 0.65f, w * 0.1f, h * 0.3f, w / 2f, 0f)
                    close()
                }
                // Background drop (light fill)
                drawPath(dropPath, color = CardPurple.copy(alpha = 0.15f), style = Fill)

                // Filled water level — clipped to the drop shape
                val fillHeight = h * progress
                clipPath(dropPath) {
                    drawRect(
                        color = CardPurple,
                        topLeft = Offset(0f, h - fillHeight),
                        size = Size(w, fillHeight)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$glassCount",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (progress > 0.4f) White else CardPurple,
                    letterSpacing = (-1).sp
                )
                Text(
                    "glasses",
                    fontSize = 14.sp,
                    color = if (progress > 0.4f) White.copy(alpha = 0.8f) else TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Goal: $waterGoal glasses",
            fontSize = 14.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Progress Bar ──────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = BgPrimary),
            elevation = CardDefaults.cardElevation(0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progress", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${(progress * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CardPurple)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = CardPurple,
                    trackColor = CardPurple.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (glassCount >= waterGoal) "Goal reached! Great job!" else "${waterGoal - glassCount} glasses remaining",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── +/- Buttons ───────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Remove button
            IconButton(
                onClick = { viewModel.removeGlass() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Divider)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Remove glass",
                    tint = if (glassCount > 0) CardPurple else TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Glass count circle
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(CardPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Add button
            IconButton(
                onClick = { viewModel.addGlass() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardPurple)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add glass",
                    tint = White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Tap + to log a glass of water",
            fontSize = 12.sp,
            color = TextMuted
        )

        // ── Glass icons row ───────────────────────────────────────────────
        Spacer(modifier = Modifier.height(28.dp))
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(waterGoal.coerceAtMost(10)) { i ->
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = if (i < glassCount) CardPurple else CardPurple.copy(alpha = 0.2f),
                    modifier = Modifier.size(if (waterGoal <= 10) 28.dp else 22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
