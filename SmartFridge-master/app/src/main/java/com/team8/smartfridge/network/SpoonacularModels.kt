package com.team8.smartfridge.network

import com.google.gson.annotations.SerializedName

// ── Search ──────────────────────────────────────────────────────────────────

data class RecipeSearchResponse(
    @SerializedName("results") val results: List<RecipeSearchItem> = emptyList(),
    @SerializedName("totalResults") val totalResults: Int = 0
)

data class RecipeSearchItem(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("image") val image: String = "",
    @SerializedName("imageType") val imageType: String? = null,
    @SerializedName("readyInMinutes") val readyInMinutes: Int = 0,
    @SerializedName("servings") val servings: Int = 0,
    @SerializedName("nutrition") val nutrition: NutritionInfo? = null
)

// ── Nutrition ────────────────────────────────────────────────────────────────

data class NutritionInfo(
    @SerializedName("nutrients") val nutrients: List<Nutrient> = emptyList()
)

data class Nutrient(
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Float,
    @SerializedName("unit") val unit: String
)

// ── Find by Ingredients ──────────────────────────────────────────────────────

data class RecipeMatchResult(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("image") val image: String = "",
    @SerializedName("usedIngredientCount") val usedIngredientCount: Int = 0,
    @SerializedName("missedIngredientCount") val missedIngredientCount: Int = 0,
    @SerializedName("usedIngredients") val usedIngredients: List<IngredientItem> = emptyList(),
    @SerializedName("missedIngredients") val missedIngredients: List<IngredientItem> = emptyList()
)

data class IngredientItem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Float = 0f,
    @SerializedName("unit") val unit: String = "",
    @SerializedName("image") val image: String? = null
)

// ── Recipe Detail ────────────────────────────────────────────────────────────

data class RecipeDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("image") val image: String = "",
    @SerializedName("readyInMinutes") val readyInMinutes: Int = 0,
    @SerializedName("servings") val servings: Int = 0,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("nutrition") val nutrition: NutritionInfo? = null,
    @SerializedName("extendedIngredients") val extendedIngredients: List<ExtendedIngredient> = emptyList(),
    @SerializedName("analyzedInstructions") val analyzedInstructions: List<AnalyzedInstruction> = emptyList()
)

data class ExtendedIngredient(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("original") val original: String = "",
    @SerializedName("amount") val amount: Float = 0f,
    @SerializedName("unit") val unit: String = ""
)

data class AnalyzedInstruction(
    @SerializedName("name") val name: String = "",
    @SerializedName("steps") val steps: List<InstructionStep> = emptyList()
)

data class InstructionStep(
    @SerializedName("number") val number: Int,
    @SerializedName("step") val step: String,
    @SerializedName("ingredients") val ingredients: List<IngredientItem>? = null
)

// ── Ingredient Autocomplete ──────────────────────────────────────────────────

data class IngredientSuggestion(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String,
    @SerializedName("image") val image: String? = null
)

// ── Substitutes ──────────────────────────────────────────────────────────────

data class SubstituteResponse(
    @SerializedName("ingredient") val ingredient: String = "",
    @SerializedName("substitutes") val substitutes: List<String> = emptyList(),
    @SerializedName("message") val message: String? = null
)
