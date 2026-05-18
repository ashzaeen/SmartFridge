package com.team8.smartfridge.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.team8.smartfridge.data.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ── Worker ────────────────────────────────────────────────────────────────────

class NotificationWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID_MEALS  = "smartfridge_meals"
        const val CHANNEL_ID_HEALTH = "smartfridge_health"

        const val KEY_USER_ID = "USER_ID"
        const val KEY_TYPE    = "NOTIFICATION_TYPE"

        const val TYPE_BREAKFAST     = "breakfast"
        const val TYPE_LUNCH         = "lunch"
        const val TYPE_DINNER        = "dinner"
        const val TYPE_EVENING_CHECK = "evening_check"

        const val NOTIF_ID_BREAKFAST     = 1002
        const val NOTIF_ID_LUNCH         = 1003
        const val NOTIF_ID_DINNER        = 1004
        const val NOTIF_ID_EVENING_CHECK = 1001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val type = inputData.getString(KEY_TYPE) ?: return@withContext Result.failure()

        createChannels()

        when (type) {
            TYPE_BREAKFAST -> sendSimple(
                title    = "🍳 Breakfast time!",
                body     = "Start your day right — log your breakfast to stay on track.",
                notifId  = NOTIF_ID_BREAKFAST,
                channel  = CHANNEL_ID_MEALS
            )
            TYPE_LUNCH -> sendSimple(
                title    = "🥗 Lunch break!",
                body     = "Midday fuel — don't forget to log your lunch.",
                notifId  = NOTIF_ID_LUNCH,
                channel  = CHANNEL_ID_MEALS
            )
            TYPE_DINNER -> sendSimple(
                title    = "🍽️ Dinner time!",
                body     = "Log your evening meal to keep your nutrition on track.",
                notifId  = NOTIF_ID_DINNER,
                channel  = CHANNEL_ID_MEALS
            )
            TYPE_EVENING_CHECK -> sendEveningCheck()
        }

        Result.success()
    }

    // ── Simple meal reminder ──────────────────────────────────────────────────

    private fun sendSimple(title: String, body: String, notifId: Int, channel: String) {
        val notification = NotificationCompat.Builder(applicationContext, channel)
            .setSmallIcon(com.team8.smartfridge.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notify(notifId, notification)
    }

    // ── 10 PM goal check ─────────────────────────────────────────────────────

    private suspend fun sendEveningCheck() {
        val uid  = Firebase.auth.currentUser?.uid ?: return
        val repo = FirebaseRepository()
        val user = repo.getUserProfile(uid) ?: return

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val log   = repo.getLogForDate(uid, today)

        val lines = mutableListOf<String>()

        val caloriesConsumed = log?.totalCalories ?: 0f
        if (caloriesConsumed < user.dailyCalorieGoal * 0.8f) {
            val remaining = (user.dailyCalorieGoal - caloriesConsumed).toInt()
            lines.add("~$remaining kcal short of your calorie goal.")
        }

        val waterConsumed = log?.waterIntake ?: 0
        if (waterConsumed < user.dailyWaterGoal) {
            val remaining = user.dailyWaterGoal - waterConsumed
            lines.add("$remaining glass${if (remaining == 1) "" else "es"} of water left for today.")
        }

        // Only fire the notification when at least one goal is unmet
        if (lines.isEmpty()) return

        val body = lines.joinToString(" • ")

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_HEALTH)
            .setSmallIcon(com.team8.smartfridge.R.drawable.ic_notification)
            .setContentTitle("🌙 Daily goals check-in")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notify(NOTIF_ID_EVENING_CHECK, notification)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun notify(id: Int, notification: android.app.Notification) {
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_MEALS,
                    "Meal Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Breakfast, lunch and dinner logging reminders"
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_HEALTH,
                    "Daily Goal Check",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "10 PM check — notifies if calorie or water goal is unmet"
                }
            )
        }
    }
}

// ── Scheduler — call once after login ─────────────────────────────────────────

fun scheduleDailyNotification(context: Context) {
    val wm = WorkManager.getInstance(context)

    listOf(
        Triple(NotificationWorker.TYPE_BREAKFAST,       8, "notif_breakfast"),
        Triple(NotificationWorker.TYPE_LUNCH,          12, "notif_lunch"),
        Triple(NotificationWorker.TYPE_DINNER,         18, "notif_dinner"),
        Triple(NotificationWorker.TYPE_EVENING_CHECK,  22, "notif_evening_check"),
    ).forEach { (type, hour, tag) ->

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val initialDelay = (target.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)

        val request = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(NotificationWorker.KEY_TYPE to type))
            .build()

        wm.enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
