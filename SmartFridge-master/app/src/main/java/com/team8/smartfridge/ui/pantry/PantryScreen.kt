package com.team8.smartfridge.ui.pantry

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.Bitmap
import android.util.Base64
import coil.compose.AsyncImage
import com.team8.smartfridge.network.OpenAIClient
import com.team8.smartfridge.data.FirebaseRepository
import com.team8.smartfridge.model.PantryItem
import com.team8.smartfridge.network.IngredientItem
import com.team8.smartfridge.network.IngredientSuggestion
import com.team8.smartfridge.network.RecipeMatchResult
import com.team8.smartfridge.repository.RecipeRepository
import com.team8.smartfridge.ui.theme.*
import com.team8.smartfridge.utils.RecipeMatchStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

// ── ViewModel ─────────────────────────────────────────────────────────────────

class PantryViewModel(application: Application) : AndroidViewModel(application) {

    private val firebase   = FirebaseRepository()
    private val repository = RecipeRepository()

    private val _pantryItems = MutableStateFlow<List<PantryItem>>(emptyList())
    val pantryItems: StateFlow<List<PantryItem>> = _pantryItems

    private val _detectedLabels = MutableStateFlow<List<String>>(emptyList())
    val detectedLabels: StateFlow<List<String>> = _detectedLabels

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    private val _matchedRecipes = MutableStateFlow<List<RecipeMatchResult>>(emptyList())
    val matchedRecipes: StateFlow<List<RecipeMatchResult>> = _matchedRecipes

    private val _isFindingRecipes = MutableStateFlow(false)
    val isFindingRecipes: StateFlow<Boolean> = _isFindingRecipes

    private val _recipeSearchDone = MutableStateFlow(false)
    val recipeSearchDone: StateFlow<Boolean> = _recipeSearchDone

    private val _ingredientSuggestions = MutableStateFlow<List<IngredientSuggestion>>(emptyList())
    val ingredientSuggestions: StateFlow<List<IngredientSuggestion>> = _ingredientSuggestions

    private val _isAutocompleting = MutableStateFlow(false)
    val isAutocompleting: StateFlow<Boolean> = _isAutocompleting

    private var autocompleteJob: Job? = null

    private val _swapIngredients = MutableStateFlow<List<IngredientItem>>(emptyList())
    val swapIngredients: StateFlow<List<IngredientItem>> = _swapIngredients

    private val _swapResults = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val swapResults: StateFlow<Map<String, List<String>>> = _swapResults

    private val _isLoadingSwaps = MutableStateFlow(false)
    val isLoadingSwaps: StateFlow<Boolean> = _isLoadingSwaps

    fun fetchSwaps(missedIngredients: List<IngredientItem>) {
        _swapIngredients.value = missedIngredients
        _swapResults.value = emptyMap()
        _isLoadingSwaps.value = true
        viewModelScope.launch {
            val names = missedIngredients.map { it.name }
            val aiResults = OpenAIClient.getIngredientSubstitutes(names)
            // Match results back to original ingredient names (case-insensitive)
            val mapped = mutableMapOf<String, List<String>>()
            missedIngredients.forEach { ingredient ->
                val key = aiResults.keys.firstOrNull {
                    it.equals(ingredient.name, ignoreCase = true) ||
                    ingredient.name.contains(it, ignoreCase = true) ||
                    it.contains(ingredient.name, ignoreCase = true)
                }
                mapped[ingredient.name] = if (key != null) aiResults[key].orEmpty() else emptyList()
            }
            _swapResults.value = mapped
            _isLoadingSwaps.value = false
        }
    }

    fun clearSwaps() {
        _swapIngredients.value = emptyList()
        _swapResults.value = emptyMap()
    }

    data class LastScan(val date: String, val count: Int)

    private val prefs = application.getSharedPreferences("pantry_prefs", Context.MODE_PRIVATE)
    private val _lastScan = MutableStateFlow(
        prefs.getString("last_scan_date", null)?.let { date ->
            LastScan(date, prefs.getInt("last_scan_count", 0))
        }
    )
    val lastScan: StateFlow<LastScan?> = _lastScan

    fun findRecipesByIngredients() {
        val uid = firebase.currentUid ?: return
        viewModelScope.launch {
            _isFindingRecipes.value = true
            _recipeSearchDone.value = false
            val items = firebase.getPantry(uid)
            if (items.isEmpty()) {
                _matchedRecipes.value = emptyList()
                _isFindingRecipes.value = false
                _recipeSearchDone.value = true
                return@launch
            }
            val results = repository.findByIngredients(items.map { it.name })
            _matchedRecipes.value = results.sortedWith(
                compareBy<RecipeMatchResult> { it.missedIngredientCount }
                    .thenByDescending { it.usedIngredientCount }
            )
            _isFindingRecipes.value = false
            _recipeSearchDone.value = true
        }
    }

    fun searchIngredients(query: String) {
        autocompleteJob?.cancel()
        if (query.isBlank()) {
            _ingredientSuggestions.value = emptyList()
            _isAutocompleting.value = false
            return
        }
        autocompleteJob = viewModelScope.launch {
            delay(300)
            _isAutocompleting.value = true
            _ingredientSuggestions.value = repository.autocompleteIngredients(query)
            _isAutocompleting.value = false
        }
    }

    fun clearSuggestions() {
        autocompleteJob?.cancel()
        _ingredientSuggestions.value = emptyList()
        _isAutocompleting.value = false
    }

    fun loadPantry() {
        val uid = firebase.currentUid ?: return
        viewModelScope.launch {
            _pantryItems.value = firebase.getPantry(uid)
        }
    }

    fun addItem(name: String) {
        val uid = firebase.currentUid ?: return
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        if (_pantryItems.value.any { it.name.equals(trimmed, ignoreCase = true) }) return
        viewModelScope.launch {
            firebase.addPantryItem(uid, PantryItem(name = trimmed))
            _pantryItems.value = firebase.getPantry(uid)
        }
    }

    fun deleteItem(item: PantryItem) {
        val uid = firebase.currentUid ?: return
        viewModelScope.launch {
            firebase.deletePantryItem(uid, item.itemId)
            _pantryItems.value = firebase.getPantry(uid)
        }
    }

    fun deleteItemByName(name: String) {
        val uid = firebase.currentUid ?: return
        viewModelScope.launch {
            val item = _pantryItems.value.find { it.name.equals(name, ignoreCase = true) } ?: return@launch
            firebase.deletePantryItem(uid, item.itemId)
            _pantryItems.value = firebase.getPantry(uid)
        }
    }

    fun addDetectedItems(labels: List<String>) {
        val uid = firebase.currentUid ?: return
        viewModelScope.launch {
            val existing = _pantryItems.value.map { it.name.lowercase() }.toSet()
            val newItems = labels
                .filter { it.isNotBlank() && it.lowercase() !in existing }
                .map { PantryItem(name = it.trim()) }
            if (newItems.isNotEmpty()) firebase.addPantryItems(uid, newItems)
            _pantryItems.value = firebase.getPantry(uid)
            _detectedLabels.value = emptyList()
            val dateStr = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                .format(java.util.Date())
            val scan = LastScan(dateStr, labels.size)
            _lastScan.value = scan
            prefs.edit().putString("last_scan_date", scan.date).putInt("last_scan_count", scan.count).apply()
        }
    }

    fun analyzeImage(imageUri: Uri) {
        _isAnalyzing.value = true
        viewModelScope.launch {
            try {
                val bitmap = getApplication<android.app.Application>()
                    .contentResolver.openInputStream(imageUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                if (bitmap == null) {
                    _isAnalyzing.value = false
                    return@launch
                }
                // Scale down to reduce base64 payload size
                val scaled = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                val jpegBase64 = java.io.ByteArrayOutputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                }
                val items = OpenAIClient.analyzeImageForFood(jpegBase64)
                _detectedLabels.value = items
            } catch (e: Exception) {
                // leave detectedLabels empty
            } finally {
                _isAnalyzing.value = false
                // Delete the temp JPEG file to avoid accumulating cache
                try {
                    imageUri.path?.let { java.io.File(it).delete() }
                } catch (_: Exception) {}
            }
        }
    }

    fun clearDetectedLabels() { _detectedLabels.value = emptyList() }
    fun removeDetectedLabel(label: String) {
        _detectedLabels.value = _detectedLabels.value - label
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun PantryScreen(
    onNavigateToRecipe: (Int) -> Unit = {},
    viewModel: PantryViewModel = viewModel()
) {
    val context = LocalContext.current
    val pantryItems      by viewModel.pantryItems.collectAsState()
    val detectedLabels   by viewModel.detectedLabels.collectAsState()
    val isAnalyzing      by viewModel.isAnalyzing.collectAsState()
    val matchedRecipes   by viewModel.matchedRecipes.collectAsState()
    val isFindingRecipes by viewModel.isFindingRecipes.collectAsState()
    val recipeSearchDone by viewModel.recipeSearchDone.collectAsState()

    val ingredientSuggestions by viewModel.ingredientSuggestions.collectAsState()
    val isAutocompleting     by viewModel.isAutocompleting.collectAsState()
    val swapIngredients      by viewModel.swapIngredients.collectAsState()
    val swapResults          by viewModel.swapResults.collectAsState()
    val isLoadingSwaps       by viewModel.isLoadingSwaps.collectAsState()

    var showCamera by remember { mutableStateOf(false) }
    var showIngredientPicker by remember { mutableStateOf(false) }
    var showSwapSheet by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted; if (granted) showCamera = true }

    LaunchedEffect(Unit) { viewModel.loadPantry() }


    if (showCamera) {
        CameraView(
            onImageCaptured = { uri ->
                viewModel.analyzeImage(uri)
                showCamera = false
            },
            onClose = { showCamera = false }
        )
        return
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
                    text = "MY PANTRY",
                    fontSize = 10.sp,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF5A7A5A)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "My Pantry",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = White,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${pantryItems.size} items available",
                    fontSize = 11.sp,
                    color = Color(0xFF5A7A5A)
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Camera / Scan Section ─────────────────────────────────────
            item {
                Card(
                    onClick = {
                        if (hasCameraPermission) showCamera = true
                        else permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Brand),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandLight.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Crop169,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Scan Groceries",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                            Text(
                                "Point camera at food items",
                                fontSize = 12.sp,
                                color = White.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Loading / Detected Labels ─────────────────────────────────
            if (isAnalyzing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = BgPrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Brand,
                                strokeWidth = 2.dp
                            )
                            Text("Analyzing image…", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }

            if (detectedLabels.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = BgPrimary),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Detected Items",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                TextButton(onClick = { viewModel.clearDetectedLabels() }) {
                                    Text("Discard", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                            Text(
                                "Review and confirm items to add to pantry:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            detectedLabels.forEach { label ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(BrandLight)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        label,
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeDetectedLabel(label) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.addDetectedItems(detectedLabels) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Brand)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add ${detectedLabels.size} Items to Pantry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Add Ingredients ───────────────────────────────────────────
            item {
                OutlinedButton(
                    onClick = { showIngredientPicker = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Divider),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = BgPrimary)
                ) {
                    Text(
                        "+ Add Ingredients",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            // ── Pantry Items ──────────────────────────────────────────────
            if (pantryItems.isNotEmpty()) {
                item {
                    Text(
                        "CURRENT PANTRY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                items(pantryItems, key = { it.itemId }) { item ->
                    PantryItemRow(
                        item = item,
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            } else if (!isAnalyzing) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Kitchen,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your pantry is empty", color = TextSecondary, fontSize = 14.sp)
                            Text(
                                "Scan groceries or add items manually",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── What Can I Make? ──────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "WHAT CAN I MAKE?",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = {
                        if (!isFindingRecipes) viewModel.findRecipesByIngredients()
                    },
                    enabled = !isFindingRecipes && pantryItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand,
                        disabledContainerColor = Divider
                    )
                ) {
                    if (isFindingRecipes) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Finding recipes…", fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            if (pantryItems.isEmpty()) "Add ingredients first"
                            else "Find recipes with my ingredients",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            if (isFindingRecipes) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Brand, strokeWidth = 2.dp)
                    }
                }
            } else if (recipeSearchDone) {
                if (matchedRecipes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No matching recipes found",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Try adding more items to your pantry",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    items(matchedRecipes, key = { it.id }) { match ->
                        RecipeMatchCard(
                            match = match,
                            onClick = {
                                RecipeMatchStore.lastMatch = match
                                onNavigateToRecipe(match.id)
                            },
                            onSwapIt = {
                                viewModel.fetchSwaps(match.missedIngredients)
                                showSwapSheet = true
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showSwapSheet && swapIngredients.isNotEmpty()) {
        SwapItSheet(
            ingredients = swapIngredients,
            swapResults = swapResults,
            isLoading = isLoadingSwaps,
            onDismiss = {
                showSwapSheet = false
                viewModel.clearSwaps()
            }
        )
    }

    if (showIngredientPicker) {
        IngredientPickerSheet(
            suggestions      = ingredientSuggestions,
            isAutocompleting = isAutocompleting,
            onQueryChange    = { viewModel.searchIngredients(it) },
            onIngredientSelected = { name -> viewModel.addItem(name) },
            onIngredientRemoved  = { name -> viewModel.deleteItemByName(name) },
            onDismiss = {
                showIngredientPicker = false
                viewModel.clearSuggestions()
            }
        )
    }
}

@Composable
private fun PantryItemRow(item: PantryItem, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgPrimary),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandAccent.copy(alpha = 0.30f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Eco,
                    contentDescription = null,
                    tint = Brand,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Recipe Match Card ─────────────────────────────────────────────────────────

@Composable
private fun RecipeMatchCard(
    match: RecipeMatchResult,
    onClick: () -> Unit,
    onSwapIt: () -> Unit = {}
) {
    Card(
        onClick = onClick,
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
            AsyncImage(
                model = match.image,
                contentDescription = match.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Divider),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = match.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Used ingredients badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BrandAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BrandAccent,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            "Uses ${match.usedIngredientCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandAccent
                        )
                    }
                    // Missing ingredients badge
                    if (match.missedIngredientCount > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CardOrange.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AddShoppingCart,
                                contentDescription = null,
                                tint = CardOrange,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                "Missing ${match.missedIngredientCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CardOrange
                            )
                        }
                    }
                }
                if (match.missedIngredientCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brand)
                            .clickable { onSwapIt() }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "Swap It",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Swap It Sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapItSheet(
    ingredients: List<IngredientItem>,
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brand),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Autorenew,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Swap It",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        "Alternatives for missing ingredients",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Brand)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Finding substitutes…",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                ingredients.forEach { ingredient ->
                    val subs = swapResults[ingredient.name].orEmpty()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = BgSecondary),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CardOrange)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    ingredient.name.replaceFirstChar { it.uppercase() },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (subs.isEmpty()) {
                                Text(
                                    "No substitutes found",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            } else {
                                subs.forEach { sub ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            "•",
                                            fontSize = 13.sp,
                                            color = BrandAccent,
                                            modifier = Modifier.padding(end = 6.dp, top = 1.dp)
                                        )
                                        Text(
                                            sub,
                                            fontSize = 13.sp,
                                            color = TextSecondary,
                                            lineHeight = 18.sp
                                        )
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

// ── Ingredient Categories ─────────────────────────────────────────────────────

private val INGREDIENT_CATEGORIES = linkedMapOf(
    "Dairy & Eggs" to listOf(
        "milk", "whole milk", "skim milk", "heavy cream", "butter",
        "cheddar cheese", "mozzarella cheese", "parmesan cheese", "cream cheese",
        "greek yogurt", "yogurt", "sour cream", "eggs"
    ),
    "Meat & Poultry" to listOf(
        "chicken breast", "chicken thigh", "ground beef", "beef steak",
        "pork chops", "bacon", "ham", "turkey breast", "sausage", "ground turkey", "lamb"
    ),
    "Seafood" to listOf(
        "salmon", "tuna", "shrimp", "cod", "tilapia",
        "crab", "sardines", "anchovies", "scallops", "halibut"
    ),
    "Vegetables" to listOf(
        "tomato", "onion", "garlic", "broccoli", "spinach",
        "carrot", "potato", "sweet potato", "bell pepper", "zucchini",
        "cucumber", "lettuce", "kale", "mushrooms", "corn",
        "peas", "green beans", "celery", "cauliflower", "asparagus"
    ),
    "Fruits" to listOf(
        "apple", "banana", "orange", "lemon", "lime",
        "strawberry", "blueberry", "raspberry", "mango", "avocado",
        "grape", "pineapple", "peach", "pear", "watermelon"
    ),
    "Grains & Bread" to listOf(
        "white rice", "brown rice", "pasta", "spaghetti", "bread",
        "whole wheat bread", "all-purpose flour", "oats", "quinoa", "barley"
    ),
    "Legumes & Nuts" to listOf(
        "black beans", "chickpeas", "lentils", "kidney beans",
        "almonds", "walnuts", "peanuts", "cashews", "peanut butter", "tofu"
    ),
    "Oils & Condiments" to listOf(
        "olive oil", "vegetable oil", "coconut oil", "soy sauce",
        "hot sauce", "ketchup", "mustard", "mayonnaise", "white vinegar", "lemon juice"
    ),
    "Herbs & Spices" to listOf(
        "salt", "black pepper", "cumin", "paprika", "oregano",
        "basil", "thyme", "rosemary", "cinnamon", "chili powder",
        "ginger", "turmeric", "garlic powder", "onion powder", "cayenne pepper"
    ),
    "Baking" to listOf(
        "sugar", "brown sugar", "baking powder", "baking soda",
        "vanilla extract", "honey", "maple syrup", "chocolate chips", "cocoa powder"
    )
)

// ── Ingredient Picker Sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientPickerSheet(
    suggestions: List<IngredientSuggestion>,
    isAutocompleting: Boolean,
    onQueryChange: (String) -> Unit,
    onIngredientSelected: (String) -> Unit,
    onIngredientRemoved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
    val addedItems = remember { mutableStateMapOf<String, Boolean>() }
    val focusManager = LocalFocusManager.current

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
        ) {
            // Title
            Text(
                "Add Ingredients",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                "Search or browse by category",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Search field
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onQueryChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search ingredients…", color = TextMuted) },
                leadingIcon = {
                    if (isAutocompleting)
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Brand,
                            strokeWidth = 2.dp
                        )
                    else
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            onQueryChange("")
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brand,
                    unfocusedBorderColor = Divider,
                    focusedContainerColor = BgSecondary,
                    unfocusedContainerColor = BgSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Brand
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (query.isNotEmpty() && suggestions.isNotEmpty()) {
                // ── Autocomplete results ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    suggestions.forEach { suggestion ->
                        SuggestionRow(
                            name = suggestion.name,
                            isAdded = addedItems[suggestion.name] == true,
                            onClick = {
                                val alreadyAdded = addedItems[suggestion.name] == true
                                if (alreadyAdded) {
                                    addedItems[suggestion.name] = false
                                    onIngredientRemoved(suggestion.name)
                                } else {
                                    addedItems[suggestion.name] = true
                                    onIngredientSelected(suggestion.name)
                                    query = ""
                                    onQueryChange("")
                                    focusManager.clearFocus()
                                }
                            }
                        )
                    }
                }
            } else if (query.isNotEmpty() && !isAutocompleting) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matches found", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                // ── Category browser ──────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    INGREDIENT_CATEGORIES.forEach { (category, items) ->
                        val expanded = expandedCategories[category] ?: false
                        // Category header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedCategories[category] = !expanded
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                category,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        HorizontalDivider(color = Divider, thickness = 0.5.dp)

                        AnimatedVisibility(visible = expanded) {
                            Column {
                                items.forEach { ingredient ->
                                    SuggestionRow(
                                        name = ingredient,
                                        isAdded = addedItems[ingredient] == true,
                                        onClick = {
                                            val alreadyAdded = addedItems[ingredient] == true
                                            if (alreadyAdded) {
                                                addedItems[ingredient] = false
                                                onIngredientRemoved(ingredient)
                                            } else {
                                                addedItems[ingredient] = true
                                                onIngredientSelected(ingredient)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Done button
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand)
            ) {
                val addedCount = addedItems.count { it.value }
                if (addedCount > 0) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Done · $addedCount added",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Text("Done", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SuggestionRow(name: String, isAdded: Boolean = false, onClick: () -> Unit) {
    val iconBg by animateColorAsState(
        targetValue = if (isAdded) Brand.copy(alpha = 0.18f) else Brand.copy(alpha = 0.12f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isAdded) BrandAccent else Brand,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconTint"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (isAdded) 0.5f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "textAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isAdded) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = name,
            fontSize = 14.sp,
            color = TextPrimary.copy(alpha = textAlpha),
            modifier = Modifier.weight(1f)
        )
        if (isAdded) {
            Text(
                "Tap to remove",
                fontSize = 11.sp,
                color = Brand.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    HorizontalDivider(color = Divider.copy(alpha = 0.5f), thickness = 0.5.dp)
}

// ── CameraView ────────────────────────────────────────────────────────────────

@Composable
fun CameraView(
    onImageCaptured: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        // Camera not available
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(0.5f))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = White)
        }

        // Capture button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            IconButton(
                onClick = {
                    val file = File.createTempFile("scan_", ".jpg", context.cacheDir)
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(
                        options,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onImageCaptured(Uri.fromFile(file))
                            }
                            override fun onError(exception: ImageCaptureException) {
                                onClose()
                            }
                        }
                    )
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(White)
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Capture",
                    tint = Brand,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // Label
        Text(
            "Point at groceries and tap to scan",
            color = White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 136.dp)
        )
    }
}
