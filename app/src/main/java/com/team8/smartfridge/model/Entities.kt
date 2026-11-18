package com.team8.smartfridge.model

data class User(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val age: Int = 0,
    val sex: String = "",
    val weight: Float = 0f,
    val height: Float = 0f,
    val bloodType: String = "",
    val allergies: String = "",
    val dietPreference: String = "",
    val dailyCalorieGoal: Int = 2000,
    val dailyWaterGoal: Int = 8
)

data class DailyLog(
    val date: String = "",
    val totalCalories: Float = 0f,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val waterIntake: Int = 0
)

data class PantryItem(
    val itemId: String = "",
    val name: String = "",
    val quantity: Float = 1f,
    val unit: String = "item",
    val isAvailable: Boolean = true
)

data class SavedRecipe(
    val recipeId: Int = 0,
    val title: String = "",
    val imageUrl: String = "",
    val instructions: String = "",
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f
)
