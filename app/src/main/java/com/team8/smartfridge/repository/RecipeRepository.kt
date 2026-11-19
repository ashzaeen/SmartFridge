package com.team8.smartfridge.repository

import com.team8.smartfridge.BuildConfig
import com.team8.smartfridge.demo.DemoData
import com.team8.smartfridge.network.*

class RecipeRepository(
    private val api: SpoonacularApi = RetrofitInstance.spoonacularApi
) {
    private val apiKey: String get() = BuildConfig.SPOONACULAR_API_KEY

    suspend fun searchRecipes(
        query: String,
        intolerances: String? = null,
        diet: String? = null,
        type: String? = null
    ): List<RecipeSearchItem> = runCatching {
        if (BuildConfig.DEMO) return@runCatching DemoData.searchRecipes(query, intolerances, diet, type)
        api.searchRecipes(
            query = query,
            intolerances = intolerances?.takeIf { it.isNotBlank() },
            diet = diet?.takeIf { it.isNotBlank() },
            type = type?.takeIf { it.isNotBlank() },
            apiKey = apiKey
        ).results
    }.getOrElse { emptyList() }

    suspend fun findByIngredients(ingredients: List<String>): List<RecipeMatchResult> =
        runCatching {
            if (BuildConfig.DEMO) return@runCatching DemoData.findByIngredients(ingredients)
            api.findByIngredients(
                ingredients = ingredients.joinToString(","),
                apiKey = apiKey
            )
        }.getOrElse { emptyList() }

    suspend fun getRecipeById(id: Int): RecipeDetail? = runCatching {
        if (BuildConfig.DEMO) return@runCatching DemoData.recipeById(id)
        api.getRecipeById(id = id, apiKey = apiKey)
    }.getOrNull()

    suspend fun autocompleteIngredients(query: String): List<IngredientSuggestion> =
        runCatching {
            if (BuildConfig.DEMO) return@runCatching DemoData.autocomplete(query)
            api.autocompleteIngredient(query = query, apiKey = apiKey)
        }.getOrElse { emptyList() }

    suspend fun getSubstitutes(ingredientName: String): SubstituteResponse? = runCatching {
        if (BuildConfig.DEMO) return@runCatching DemoData.substituteResponse(ingredientName)
        api.getSubstitutes(ingredientName = ingredientName, apiKey = apiKey)
    }.getOrNull()
}
