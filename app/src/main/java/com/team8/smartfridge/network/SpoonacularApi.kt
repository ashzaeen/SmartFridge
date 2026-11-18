package com.team8.smartfridge.network

import retrofit2.http.*

interface SpoonacularApi {

    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("intolerances") intolerances: String? = null,
        @Query("diet") diet: String? = null,
        @Query("type") type: String? = null,
        @Query("addRecipeNutrition") addRecipeNutrition: Boolean = true,
        @Query("number") number: Int = 20,
        @Query("apiKey") apiKey: String
    ): RecipeSearchResponse

    @GET("recipes/findByIngredients")
    suspend fun findByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 20,
        @Query("ranking") ranking: Int = 1,
        @Query("apiKey") apiKey: String
    ): List<RecipeMatchResult>

    @GET("recipes/{id}/information")
    suspend fun getRecipeById(
        @Path("id") id: Int,
        @Query("includeNutrition") includeNutrition: Boolean = true,
        @Query("apiKey") apiKey: String
    ): RecipeDetail

    @GET("food/ingredients/autocomplete")
    suspend fun autocompleteIngredient(
        @Query("query") query: String,
        @Query("number") number: Int = 15,
        @Query("apiKey") apiKey: String
    ): List<IngredientSuggestion>

    @GET("food/ingredients/{ingredientName}/substitutes")
    suspend fun getSubstitutes(
        @Path("ingredientName") ingredientName: String,
        @Query("apiKey") apiKey: String
    ): SubstituteResponse
}
