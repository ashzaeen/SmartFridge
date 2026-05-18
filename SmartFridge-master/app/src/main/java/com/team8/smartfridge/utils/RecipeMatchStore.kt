package com.team8.smartfridge.utils

import com.team8.smartfridge.network.RecipeMatchResult

/**
 * Temporary in-memory store for passing a RecipeMatchResult from the pantry
 * "What can I make?" list to RecipeDetailScreen without nav serialization.
 * Consumed once on load and cleared automatically.
 */
object RecipeMatchStore {
    var lastMatch: RecipeMatchResult? = null
}
