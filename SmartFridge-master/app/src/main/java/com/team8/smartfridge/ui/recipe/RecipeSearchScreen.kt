package com.team8.smartfridge.ui.recipe

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.team8.smartfridge.data.FirebaseRepository
import com.team8.smartfridge.model.SavedRecipe
import com.team8.smartfridge.model.User
import com.team8.smartfridge.network.RecipeSearchItem
import com.team8.smartfridge.repository.RecipeRepository
import com.team8.smartfridge.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

class RecipeSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val firebase = FirebaseRepository()
    private val repository = RecipeRepository()

    private val _recipes = MutableStateFlow<List<RecipeSearchItem>>(emptyList())
    val recipes: StateFlow<List<RecipeSearchItem>> = _recipes

    private val _favoriteRecipes = MutableStateFlow<List<RecipeSearchItem>>(emptyList())
    val favoriteRecipes: StateFlow<List<RecipeSearchItem>> = _favoriteRecipes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _savedIds = MutableStateFlow<Set<Int>>(emptySet())
    val savedIds: StateFlow<Set<Int>> = _savedIds

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    var searchQuery by mutableStateOf("")
    var selectedFilter by mutableStateOf("All")

    fun loadSavedIds() {
        val uid = firebase.currentUid ?: return
        viewModelScope.launch {
            val saved = firebase.getSavedRecipes(uid)
            _savedIds.value = saved.map { it.recipeId }.toSet()
        }
    }

    fun loadFavorites() {
        val uid = firebase.currentUid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val saved = firebase.getSavedRecipes(uid)
            _favoriteRecipes.value = saved.map { s ->
                RecipeSearchItem(
                    id = s.recipeId,
                    title = s.title,
                    image = s.imageUrl,
                    readyInMinutes = 0,
                    nutrition = com.team8.smartfridge.network.NutritionInfo(
                        listOf(
                            com.team8.smartfridge.network.Nutrient("Calories", s.calories, "kcal"),
                            com.team8.smartfridge.network.Nutrient("Protein",  s.protein,  "g"),
                            com.team8.smartfridge.network.Nutrient("Carbohydrates", s.carbs, "g"),
                            com.team8.smartfridge.network.Nutrient("Fat",      s.fat,      "g")
                        )
                    )
                )
            }
            if (_favoriteRecipes.value.isEmpty()) {
                _errorMessage.value = "No saved recipes yet. Tap ♡ on any recipe to save it."
            }
            _isLoading.value = false
        }
    }

    fun search(user: User?) {
        if (selectedFilter == "Favorites") return   // favorites loaded separately
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val intolerances = user?.allergies?.takeIf { it.isNotBlank() }

            val (type, diet) = when (selectedFilter) {
                "Breakfast" -> Pair("breakfast", user?.dietPreference?.takeIf { it.isNotBlank() })
                "Lunch"     -> Pair("lunch",     user?.dietPreference?.takeIf { it.isNotBlank() })
                "Dinner"    -> Pair("dinner",    user?.dietPreference?.takeIf { it.isNotBlank() })
                "Snacks"    -> Pair("snack",     user?.dietPreference?.takeIf { it.isNotBlank() })
                "Vegan"     -> Pair(null, "vegan")
                "Keto"      -> Pair(null, "ketogenic")
                else        -> Pair(null, user?.dietPreference?.takeIf { it.isNotBlank() })
            }

            val result = repository.searchRecipes(
                query = searchQuery.ifBlank { "healthy" },
                intolerances = intolerances,
                diet = diet,
                type = type
            )

            if (result.isEmpty()) {
                _errorMessage.value = "No recipes found. Try a different search."
            }
            _recipes.value = result
            _isLoading.value = false
        }
    }

    fun toggleSave(recipe: RecipeSearchItem) {
        val uid = firebase.currentUid ?: return
        viewModelScope.launch {
            val id = recipe.id
            if (_savedIds.value.contains(id)) {
                firebase.removeRecipe(uid, id)
                _savedIds.value = _savedIds.value - id
            } else {
                val nutrients = recipe.nutrition?.nutrients ?: emptyList()
                fun nut(name: String) = nutrients.find { it.name == name }?.amount ?: 0f
                firebase.saveRecipe(
                    uid,
                    SavedRecipe(
                        recipeId     = id,
                        title        = recipe.title,
                        imageUrl     = recipe.image,
                        instructions = "",
                        calories     = nut("Calories"),
                        protein      = nut("Protein"),
                        carbs        = nut("Carbohydrates"),
                        fat          = nut("Fat")
                    )
                )
                _savedIds.value = _savedIds.value + id
            }
            if (selectedFilter == "Favorites") loadFavorites()
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

private val filters = listOf("All", "Favorites", "Breakfast", "Lunch", "Dinner", "Snacks", "Vegan", "Keto")

@Composable
fun RecipeSearchScreen(
    user: User?,
    onNavigateToDetail: (Int) -> Unit,
    initialFilter: String = "All",
    viewModel: RecipeSearchViewModel = viewModel()
) {
    val recipes by viewModel.recipes.collectAsState()
    val favoriteRecipes by viewModel.favoriteRecipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val savedIds by viewModel.savedIds.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val focusManager = LocalFocusManager.current

    val isFavoritesTab = viewModel.selectedFilter == "Favorites"
    val displayRecipes = if (isFavoritesTab) favoriteRecipes else recipes

    LaunchedEffect(Unit) {
        viewModel.loadSavedIds()
        if (initialFilter != "All") {
            viewModel.selectedFilter = initialFilter
        }
    }

    LaunchedEffect(viewModel.selectedFilter) {
        if (viewModel.selectedFilter == "Favorites") viewModel.loadFavorites()
        else viewModel.search(user)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSecondary)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Color(0xFF1C3B22))
                .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 20.dp)
        ) {
            Column {
                Text(
                    text = "RECIPES",
                    fontSize = 10.sp,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF5A7A5A)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Recipes",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFFFFF),
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Discover meals you'll love",
                    fontSize = 11.sp,
                    color = Color(0xFF5A7A5A)
                )
            }
        }

        // ── Search Bar (hidden on Favorites tab) ─────────────────────────────
        if (!isFavoritesTab) OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("Search recipes…", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
            },
            trailingIcon = {
                if (viewModel.searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.searchQuery = ""
                        viewModel.search(user)
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                viewModel.search(user)
            }),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Brand,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = BgPrimary,
                unfocusedContainerColor = BgPrimary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Brand
            ),
            singleLine = true
        )

        // ── Filter Chips ─────────────────────────────────────────────────────
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                val selected = viewModel.selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = {
                        viewModel.selectedFilter = filter
                    },
                    label = {
                        Text(
                            text = filter,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Brand,
                        selectedLabelColor = White,
                        containerColor = BgPrimary,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        selectedBorderColor = Color.Transparent,
                        borderColor = Divider
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Content ──────────────────────────────────────────────────────────
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Brand)
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMessage!!, color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayRecipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            isSaved = savedIds.contains(recipe.id),
                            onCardClick = { onNavigateToDetail(recipe.id) },
                            onSaveClick = {
                                viewModel.toggleSave(recipe)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

// ── Recipe Card ───────────────────────────────────────────────────────────────

@Composable
private fun RecipeCard(
    recipe: RecipeSearchItem,
    isSaved: Boolean,
    onCardClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val nutrients = recipe.nutrition?.nutrients ?: emptyList()
    val calories = nutrients.find { it.name == "Calories" }?.amount?.toInt()

    Card(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgPrimary),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            AsyncImage(
                model = recipe.image,
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (calories != null) {
                        RecipeBadge(
                            icon = Icons.Default.LocalFireDepartment,
                            label = "$calories kcal",
                            color = CardOrange
                        )
                    }
                    if (recipe.readyInMinutes > 0) {
                        RecipeBadge(
                            icon = Icons.Default.Timer,
                            label = "${recipe.readyInMinutes} min",
                            color = CardTeal
                        )
                    }
                }
            }

            // Save button
            IconButton(onClick = onSaveClick) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isSaved) "Unsave" else "Save",
                    tint = if (isSaved) Color(0xFFE53935) else TextMuted
                )
            }
        }
    }
}

@Composable
private fun RecipeBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
