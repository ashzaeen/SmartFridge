package com.team8.smartfridge

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.team8.smartfridge.ui.auth.AuthState
import com.team8.smartfridge.ui.auth.AuthViewModel
import com.team8.smartfridge.ui.auth.LoginScreen
import com.team8.smartfridge.ui.auth.RegisterScreen
import com.team8.smartfridge.ui.chatbot.ChatbotScreen
import com.team8.smartfridge.ui.fitness.FitnessScreen
import com.team8.smartfridge.ui.home.HomeScreen
import com.team8.smartfridge.ui.pantry.PantryScreen
import com.team8.smartfridge.ui.recipe.RecipeDetailScreen
import com.team8.smartfridge.ui.recipe.RecipeSearchScreen
import com.team8.smartfridge.ui.settings.SettingsScreen
import com.team8.smartfridge.ui.theme.SmartFridgeTheme
import com.team8.smartfridge.ui.water.WaterScreen
import com.team8.smartfridge.utils.scheduleDailyNotification

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent {
            SmartFridgeTheme {
                SmartFridgeApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home    : Screen("home",    "Home",    Icons.Default.Home)
    object Pantry  : Screen("pantry",  "Pantry",  Icons.Default.LocalGroceryStore)
    object Recipes : Screen("recipes", "Recipes", Icons.Default.Search)
    object Fitness : Screen("fitness", "Fitness", Icons.AutoMirrored.Filled.DirectionsRun)
    object Chat    : Screen("chatbot", "AI Chat", Icons.Default.AutoAwesome)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Recipes,
    Screen.Pantry,
    Screen.Fitness,
    Screen.Chat
)

@Composable
fun SmartFridgeApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState      by authViewModel.authState.collectAsState()
    val loggedInUser   by authViewModel.loggedInUser.collectAsState()
    val isCheckingAuth by authViewModel.isCheckingAuth.collectAsState()
    val context = LocalContext.current

    val authRoutes = listOf("login", "register", "splash")
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on auth/splash routes, recipe detail, and settings
    val showBottomBar = currentRoute != null
            && currentRoute !in authRoutes
            && !currentRoute.startsWith("recipe_detail")
            && currentRoute != "settings"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color(0xFF1C3B22),
                    tonalElevation = 0.dp,
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                            selected = currentRoute == screen.route ||
                                (screen == Screen.Recipes && currentRoute?.startsWith("recipes/") == true),
                            onClick = {
                                if (screen == Screen.Home) {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(screen.route) {
                                        popUpTo("home") {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Color(0xFFB8D490),
                                selectedTextColor   = Color(0xFFB8D490),
                                indicatorColor      = Color(0xFFB8D490).copy(alpha = 0.14f),
                                unselectedIconColor = Color(0xFF4A6040),
                                unselectedTextColor = Color(0xFF4A6040)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // startDestination is always "splash" so the graph is never rebuilt when
        // loggedInUser changes — all routes remain registered at all times.
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Splash / auth gate ────────────────────────────────────────
            composable("splash") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A2E10)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFB8D490))
                }
                LaunchedEffect(isCheckingAuth, loggedInUser) {
                    if (!isCheckingAuth) {
                        val dest = if (loggedInUser != null) "home" else "login"
                        navController.navigate(dest) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            }

            // ── Auth ──────────────────────────────────────────────────────
            composable("login") {
                LoginScreen(
                    onLoginClick = { email, password ->
                        authViewModel.login(email, password)
                    },
                    onRegisterClick = { navController.navigate("register") },
                    onForgotClick = { email -> authViewModel.forgotPassword(email) },
                    isLoading = authState is AuthState.Loading,
                    authError = (authState as? AuthState.Error)?.message,
                    resetSent = authState is AuthState.ResetSent
                )
                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.Success -> {
                            scheduleDailyNotification(context)
                            navController.navigate("home") {
                                popUpTo(0) { inclusive = true }
                            }
                            authViewModel.resetState()
                        }
                        is AuthState.ResetSent -> {
                            kotlinx.coroutines.delay(4000)
                            authViewModel.resetState()
                        }
                        else -> {}
                    }
                }
            }

            composable("register") {
                RegisterScreen(
                    onRegisterClick = { firstName, lastName, email, password,
                                        age, sex, weight, height, bloodType,
                                        allergies, dietPreference,
                                        dailyCalorieGoal, dailyWaterGoal ->
                        authViewModel.register(
                            firstName, lastName, email, password,
                            age, sex, weight, height, bloodType,
                            allergies, dietPreference,
                            dailyCalorieGoal, dailyWaterGoal
                        )
                    },
                    onBackToLogin = { navController.popBackStack() },
                    isLoading = authState is AuthState.Loading,
                    authError = (authState as? AuthState.Error)?.message
                )
                LaunchedEffect(authState) {
                    if (authState is AuthState.Success) {
                        scheduleDailyNotification(context)
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                        authViewModel.resetState()
                    }
                }
            }

            // ── Main Screens ──────────────────────────────────────────────
            composable("home") {
                HomeScreen(
                    user = loggedInUser,
                    onNavigateToRecipes  = { filter -> navController.navigate("recipes/$filter") },
                    onNavigateToPantry   = { navController.navigate("pantry") },
                    onNavigateToFitness  = { navController.navigate("fitness") },
                    onNavigateToWater    = { navController.navigate("water") },
                    onNavigateToChatbot  = { navController.navigate("chatbot") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }

            composable("settings") {
                SettingsScreen(
                    user = loggedInUser,
                    onSave = { updatedUser ->
                        authViewModel.updateUser(updatedUser)
                        navController.popBackStack()
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("recipes") {
                RecipeSearchScreen(
                    user = loggedInUser,
                    onNavigateToDetail = { recipeId ->
                        navController.navigate("recipe_detail/$recipeId")
                    }
                )
            }

            composable(
                route = "recipes/{filter}",
                arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "All" })
            ) { backStackEntry ->
                val filter = backStackEntry.arguments?.getString("filter") ?: "All"
                RecipeSearchScreen(
                    user = loggedInUser,
                    onNavigateToDetail = { recipeId ->
                        navController.navigate("recipe_detail/$recipeId")
                    },
                    initialFilter = filter
                )
            }

            composable(
                route = "recipe_detail/{recipeId}",
                arguments = listOf(navArgument("recipeId") { type = NavType.IntType })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getInt("recipeId") ?: return@composable
                RecipeDetailScreen(
                    recipeId = recipeId,
                    onBack   = { navController.popBackStack() }
                )
            }

            composable("pantry") {
                PantryScreen(
                    onNavigateToRecipe = { recipeId ->
                        navController.navigate("recipe_detail/$recipeId")
                    }
                )
            }

            composable("fitness") {
                FitnessScreen(user = loggedInUser)
            }

            composable("water") {
                WaterScreen(user = loggedInUser)
            }

            composable("chatbot") {
                ChatbotScreen(user = loggedInUser)
            }
        }
    }
}
