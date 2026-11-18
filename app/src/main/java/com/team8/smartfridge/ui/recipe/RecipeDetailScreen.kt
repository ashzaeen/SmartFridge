package com.team8.smartfridge.ui.recipe

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import com.team8.smartfridge.R
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.team8.smartfridge.data.FirebaseRepository
import com.team8.smartfridge.model.DailyLog
import com.team8.smartfridge.model.SavedRecipe
import com.team8.smartfridge.network.OpenAIClient
import com.team8.smartfridge.network.RecipeDetail
import com.team8.smartfridge.repository.RecipeRepository
import com.team8.smartfridge.ui.theme.*
import com.team8.smartfridge.utils.RecipeMatchStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── ViewModel ─────────────────────────────────────────────────────────────────

class RecipeDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val firebase = FirebaseRepository()
    private val repository = RecipeRepository()

    private val _recipe = MutableStateFlow<RecipeDetail?>(null)
    val recipe: StateFlow<RecipeDetail?> = _recipe

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _mealLogged = MutableStateFlow(false)
    val mealLogged: StateFlow<Boolean> = _mealLogged

    private val _mealLogError = MutableStateFlow(false)
    val mealLogError: StateFlow<Boolean> = _mealLogError

    private val _usedNames = MutableStateFlow<Set<String>>(emptySet())
    val usedNames: StateFlow<Set<String>> = _usedNames

    private val _missedNames = MutableStateFlow<Set<String>>(emptySet())
    val missedNames: StateFlow<Set<String>> = _missedNames

    private val _hasPantryMatchData = MutableStateFlow(false)
    val hasPantryMatchData: StateFlow<Boolean> = _hasPantryMatchData

    private val _swapResults = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val swapResults: StateFlow<Map<String, List<String>>> = _swapResults

    private val _isLoadingSwaps = MutableStateFlow(false)
    val isLoadingSwaps: StateFlow<Boolean> = _isLoadingSwaps

    fun fetchSwaps() {
        val missed = _missedNames.value.toList().ifEmpty { return }
        _swapResults.value = emptyMap()
        _isLoadingSwaps.value = true
        viewModelScope.launch {
            _swapResults.value = OpenAIClient.getIngredientSubstitutes(missed)
            _isLoadingSwaps.value = false
        }
    }

    fun clearSwaps() {
        _swapResults.value = emptyMap()
    }

    fun loadRecipe(recipeId: Int) {
        val uid = firebase.currentUid ?: run { _isLoading.value = false; return }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _recipe.value = repository.getRecipeById(recipeId)
                _isSaved.value = firebase.isSaved(uid, recipeId)

                val match = RecipeMatchStore.lastMatch?.takeIf { it.id == recipeId }
                RecipeMatchStore.lastMatch = null

                if (match != null) {
                    _usedNames.value = match.usedIngredients.map { it.name.lowercase() }.toSet()
                    _missedNames.value = match.missedIngredients.map { it.name.lowercase() }.toSet()
                    _hasPantryMatchData.value = true
                } else {
                    _usedNames.value = firebase.getPantry(uid)
                        .map { it.name.lowercase() }
                        .toSet()
                    _missedNames.value = emptySet()
                    _hasPantryMatchData.value = false
                }
            } catch (e: Exception) {
                _recipe.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleSave() {
        val uid = firebase.currentUid ?: return
        viewModelScope.launch {
            val r = _recipe.value ?: return@launch
            val nutrients = r.nutrition?.nutrients ?: emptyList()
            fun nut(name: String) = nutrients.find { it.name == name }?.amount ?: 0f
            val instructions = r.analyzedInstructions
                .flatMap { it.steps }
                .joinToString("\n") { "${it.number}. ${it.step}" }

            val savedRecipe = SavedRecipe(
                recipeId     = r.id,
                title        = r.title,
                imageUrl     = r.image,
                instructions = instructions,
                calories     = nut("Calories"),
                protein      = nut("Protein"),
                carbs        = nut("Carbohydrates"),
                fat          = nut("Fat")
            )
            if (_isSaved.value) {
                firebase.removeRecipe(uid, savedRecipe.recipeId)
                _isSaved.value = false
            } else {
                firebase.saveRecipe(uid, savedRecipe)
                _isSaved.value = true
            }
        }
    }

    fun logMeal() {
        val uid = firebase.currentUid ?: return
        viewModelScope.launch {
            val r = _recipe.value ?: return@launch
            try {
                val nutrients = r.nutrition?.nutrients ?: emptyList()
                fun nut(name: String) = nutrients.find { it.name == name }?.amount ?: 0f

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val existing = firebase.getLogForDate(uid, today)
                val updatedLog = existing?.copy(
                    totalCalories = existing.totalCalories + nut("Calories"),
                    totalProtein  = existing.totalProtein  + nut("Protein"),
                    totalCarbs    = existing.totalCarbs    + nut("Carbohydrates"),
                    totalFat      = existing.totalFat      + nut("Fat")
                ) ?: DailyLog(
                    date          = today,
                    totalCalories = nut("Calories"),
                    totalProtein  = nut("Protein"),
                    totalCarbs    = nut("Carbohydrates"),
                    totalFat      = nut("Fat")
                )
                firebase.saveLog(uid, updatedLog)
                _mealLogged.value = true
            } catch (e: Exception) {
                _mealLogError.value = true
            }
        }
    }

    fun resetMealLogged() { _mealLogged.value = false }
    fun resetMealLogError() { _mealLogError.value = false }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Int,
    onBack: () -> Unit,
    viewModel: RecipeDetailViewModel = viewModel()
) {
    val recipe by viewModel.recipe.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val mealLogged by viewModel.mealLogged.collectAsState()
    val mealLogError by viewModel.mealLogError.collectAsState()
    val usedNames by viewModel.usedNames.collectAsState()
    val missedNames by viewModel.missedNames.collectAsState()
    val hasPantryMatchData by viewModel.hasPantryMatchData.collectAsState()
    val swapResults by viewModel.swapResults.collectAsState()
    val isLoadingSwaps by viewModel.isLoadingSwaps.collectAsState()
    var showSwapSheet by remember { mutableStateOf(false) }
    var showOrderSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }

    LaunchedEffect(mealLogged) {
        if (mealLogged) {
            snackbarHostState.showSnackbar("Meal logged to today's diary!")
            viewModel.resetMealLogged()
        }
    }

    LaunchedEffect(mealLogError) {
        if (mealLogError) {
            snackbarHostState.showSnackbar("Failed to log meal. Please try again.")
            viewModel.resetMealLogError()
        }
    }

    if (showOrderSheet && missedNames.isNotEmpty() && recipe != null) {
        val missingItems = recipe!!.extendedIngredients.filter { ing ->
            val n = ing.name.lowercase()
            missedNames.any { m -> n.contains(m) || m.contains(n) }
        }.map { it.original.ifBlank { it.name } }

        OrderMissingItemsSheet(
            recipeName = recipe!!.title,
            missingItems = missingItems.ifEmpty { missedNames.toList() },
            onDismiss = { showOrderSheet = false }
        )
    }

    if (showSwapSheet && missedNames.isNotEmpty()) {
        RecipeSwapItSheet(
            missedNames = missedNames.toList(),
            swapResults = swapResults,
            isLoading = isLoadingSwaps,
            onDismiss = {
                showSwapSheet = false
                viewModel.clearSwaps()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgSecondary
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Brand) }
            }
            recipe == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Recipe not found.", color = TextSecondary)
                }
            }
            else -> {
                val r = recipe!!
                val nutrients = r.nutrition?.nutrients ?: emptyList()
                fun nut(name: String) = nutrients.find { it.name == name }?.amount?.toInt() ?: 0

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Hero Image ────────────────────────────────────────
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        AsyncImage(
                            model = r.image,
                            contentDescription = r.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Gradient overlay + nav buttons
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        listOf(Color.Black.copy(0.4f), Color.Transparent)
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(0.4f))
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                            }
                            IconButton(
                                onClick = { viewModel.toggleSave() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(0.4f))
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Save",
                                    tint = if (isSaved) Color(0xFFE53935) else White
                                )
                            }
                        }
                    }

                    // ── Content Card ──────────────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = BgPrimary),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {

                            // Title
                            Text(
                                text = r.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                lineHeight = 28.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Cook time
                            if (r.readyInMinutes > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = CardTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "${r.readyInMinutes} minutes",
                                        fontSize = 13.sp,
                                        color = CardTeal,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "${r.servings} servings",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Macro badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MacroBadge("Calories", "${nut("Calories")}", CardYellow, Modifier.weight(1f))
                                MacroBadge("Protein", "${nut("Protein")}g", CardPurple, Modifier.weight(1f))
                                MacroBadge("Carbs", "${nut("Carbohydrates")}g", Brand, Modifier.weight(1f))
                                MacroBadge("Fat", "${nut("Fat")}g", CardOrange, Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Log Meal button
                            Button(
                                onClick = { viewModel.logMeal() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Brand)
                            ) {
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Log This Meal",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Order Missing Items button — only shown when pantry match data exists
                            if (missedNames.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { showOrderSheet = true },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.5.dp, CardOrange),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CardOrange)
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Order Missing Items", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // ── Ingredients ───────────────────────────────
                            if (r.extendedIngredients.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(28.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Ingredients",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    if (missedNames.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Brand)
                                                .clickable {
                                                    viewModel.fetchSwaps()
                                                    showSwapSheet = true
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Autorenew,
                                                contentDescription = null,
                                                tint = White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                "Swap It",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = White
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                if (usedNames.isNotEmpty() || missedNames.isNotEmpty()) {
                                    Text(
                                        if (hasPantryMatchData)
                                            "Green = in your pantry  ·  Orange = need to buy"
                                        else
                                            "Green = in your pantry",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                r.extendedIngredients.forEach { ingredient ->
                                    val ingName = ingredient.name.lowercase()

                                    val status = when {
                                        // From pantry match: exact names from Spoonacular
                                        hasPantryMatchData && usedNames.any { u ->
                                            ingName.contains(u) || u.contains(ingName)
                                        } -> 1  // have it

                                        hasPantryMatchData && missedNames.any { m ->
                                            ingName.contains(m) || m.contains(ingName)
                                        } -> -1  // missing

                                        // Fallback pantry check (came from recipe search)
                                        !hasPantryMatchData && usedNames.any { u ->
                                            ingName.contains(u) || u.contains(ingName)
                                        } -> 1

                                        else -> 0  // unknown / no pantry data
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (status) {
                                                1    -> Icons.Default.CheckCircle
                                                -1   -> Icons.Default.RadioButtonUnchecked
                                                else -> Icons.Default.FiberManualRecord
                                            },
                                            contentDescription = null,
                                            tint = when (status) {
                                                1    -> BrandAccent
                                                -1   -> CardOrange
                                                else -> TextMuted
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = ingredient.original.ifBlank { ingredient.name },
                                            fontSize = 14.sp,
                                            color = when (status) {
                                                1    -> TextPrimary
                                                -1   -> TextSecondary
                                                else -> TextSecondary
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    HorizontalDivider(
                                        color = Divider.copy(alpha = 0.4f),
                                        thickness = 0.5.dp
                                    )
                                }
                            }

                            // ── Instructions ──────────────────────────────
                            if (r.analyzedInstructions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(28.dp))
                                Text(
                                    "Instructions",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val allSteps = r.analyzedInstructions.flatMap { it.steps }
                                allSteps.forEach { step ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Brand),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${step.number}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = step.step,
                                            fontSize = 14.sp,
                                            color = TextPrimary,
                                            lineHeight = 22.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Order Missing Items Sheet ─────────────────────────────────────────────────

private data class DeliveryApp(
    val name: String,
    @DrawableRes val logoRes: Int,
    val bgColor: Color,
    val buildUrl: (List<String>) -> String
)

private val deliveryApps = listOf(
    DeliveryApp("Uber Eats", R.drawable.logo_ubereats, Color(0xFF06C167)) { items ->
        "https://www.ubereats.com/search?q=${Uri.encode(items.joinToString(" "))}"
    },
    DeliveryApp("DoorDash", R.drawable.logo_doordash, Color.White) { items ->
        "https://www.doordash.com/convenience/search/?query=${Uri.encode(items.joinToString(" "))}"
    },
    DeliveryApp("Instacart", R.drawable.logo_instacart, Color.White) { items ->
        "https://www.instacart.com/store/s?k=${Uri.encode(items.joinToString(" "))}"
    },
    DeliveryApp("Amazon Fresh", R.drawable.logo_amazonfresh, Color(0xFF0A2A0A)) { items ->
        "https://www.amazon.com/s?k=${Uri.encode(items.joinToString("+"))}&i=amazonfresh"
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderMissingItemsSheet(
    recipeName: String,
    missingItems: List<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var copied by remember { mutableStateOf(false) }

    val shoppingListText = buildString {
        appendLine("Shopping list for: $recipeName")
        appendLine()
        missingItems.forEach { appendLine("• $it") }
    }.trim()

    fun copyList() {
        clipboardManager.setText(AnnotatedString(shoppingListText))
        copied = true
    }

    fun launchApp(app: DeliveryApp) {
        copyList()
        val url = app.buildUrl(missingItems)
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BgPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Divider)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(CardOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = CardOrange, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Order Missing Items", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("${missingItems.size} item${if (missingItems.size != 1) "s" else ""} needed", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shopping list card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    missingItems.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(CardOrange))
                            Text(item, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Copy button
            OutlinedButton(
                onClick = { copyList() },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (copied) Brand else Divider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (copied) Brand else TextSecondary)
            ) {
                Icon(
                    if (copied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                    contentDescription = null, modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (copied) "Copied to clipboard!" else "Copy Shopping List",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("ORDER VIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // Delivery app logo grid
            deliveryApps.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { app ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(app.bgColor)
                                .clickable { launchApp(app) },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(app.logoRes),
                                contentDescription = app.name,
                                modifier = Modifier.fillMaxWidth(0.8f).heightIn(max = 48.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Share button
            OutlinedButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shoppingListText)
                        putExtra(Intent.EXTRA_SUBJECT, "Shopping list for $recipeName")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share shopping list"))
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Divider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share via…", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeSwapItSheet(
    missedNames: List<String>,
    swapResults: Map<String, List<String>>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BgPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Divider)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brand),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, tint = White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Swap It", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Alternatives for missing ingredients", fontSize = 13.sp, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Brand)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Finding substitutes…", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            } else {
                missedNames.forEach { name ->
                    val key = swapResults.keys.firstOrNull {
                        it.equals(name, ignoreCase = true) ||
                        name.contains(it, ignoreCase = true) ||
                        it.contains(name, ignoreCase = true)
                    }
                    val subs = if (key != null) swapResults[key].orEmpty() else emptyList()
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = BgSecondary),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CardOrange))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(name.replaceFirstChar { it.uppercase() }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (subs.isEmpty()) {
                                Text("No substitutes found", fontSize = 12.sp, color = TextMuted)
                            } else {
                                subs.forEach { sub ->
                                    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                        Text("•", fontSize = 13.sp, color = BrandAccent, modifier = Modifier.padding(end = 6.dp, top = 1.dp))
                                        Text(sub, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MacroBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
    }
}
