package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.authStateFlow
import com.example.data.ai.CustomBackendAiEngine
import com.example.data.ai.FirebaseAiEngine
import com.example.data.prefs.SettingsRepository
import com.example.data.repository.GhostMalliRepository
import com.example.ui.components.DarkFantasyBackground
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CodeEditorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.AbyssalPurple
import com.example.ui.theme.DeepVioletContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GhostMalliTheme
import com.example.ui.theme.LavenderText
import com.example.ui.theme.StarlightWhite
import com.example.ui.viewmodel.GhostMalliViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private const val ROUTE_CHAT = "chat"
private const val ROUTE_STUDIO = "studio"
private const val ROUTE_EDITOR = "editor"
private const val ROUTE_SETTINGS = "settings"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val DESTINATIONS = listOf(
    NavDestination(ROUTE_CHAT, "Chat", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_chat"),
    NavDestination(ROUTE_STUDIO, "Studio", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_studio"),
    NavDestination(ROUTE_EDITOR, "Editor", Icons.Rounded.Code, Icons.Outlined.Code, "nav_editor"),
    NavDestination(ROUTE_SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun GhostMalliApp() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        GhostMalliTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            GhostMalliMainShell(currentUserId = user.uid)
        }
    }
}

@Composable
private fun GhostMalliMainShell(currentUserId: String) {
    val viewModel: GhostMalliViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val settingsRepo = SettingsRepository(app.applicationContext)
                GhostMalliViewModel(
                    repository = GhostMalliRepository(db, Firebase.auth),
                    settingsRepository = settingsRepo,
                    firebaseAiEngine = FirebaseAiEngine(),
                    customBackendEngine = CustomBackendAiEngine(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_CHAT

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val showToast: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    GhostMalliTheme(accentTheme = settings.accentTheme) {
        DarkFantasyBackground(particlesEnabled = settings.particlesEnabled) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 700.dp

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar(
                                containerColor = AbyssalPurple.copy(alpha = 0.94f),
                                modifier = Modifier.navigationBarsPadding()
                            ) {
                                DESTINATIONS.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTo(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ElectricCyan,
                                            selectedTextColor = ElectricCyan,
                                            indicatorColor = DeepVioletContainer,
                                            unselectedIconColor = LavenderText,
                                            unselectedTextColor = LavenderText
                                        ),
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .imePadding()
                    ) {
                        if (useRail) {
                            NavigationRail(
                                containerColor = AbyssalPurple.copy(alpha = 0.92f)
                            ) {
                                DESTINATIONS.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTo(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.label) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = ElectricCyan,
                                            selectedTextColor = ElectricCyan,
                                            indicatorColor = DeepVioletContainer,
                                            unselectedIconColor = LavenderText,
                                            unselectedTextColor = LavenderText
                                        ),
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_CHAT
                            ) {
                                composable(ROUTE_CHAT) {
                                    ChatScreen(
                                        viewModel = viewModel,
                                        onOpenCodeInEditor = { code, lang ->
                                            viewModel.openCodeInEditor(code, lang)
                                            navigateTo(ROUTE_EDITOR)
                                        },
                                        onShowSnackbar = showToast
                                    )
                                }
                                composable(ROUTE_STUDIO) {
                                    StudioScreen(
                                        viewModel = viewModel,
                                        onOpenCodeInEditor = { code, lang, title, fileName ->
                                            viewModel.openCodeInEditor(code, lang, title, fileName)
                                            navigateTo(ROUTE_EDITOR)
                                        },
                                        onShowSnackbar = showToast
                                    )
                                }
                                composable(ROUTE_EDITOR) {
                                    CodeEditorScreen(
                                        viewModel = viewModel,
                                        onShowSnackbar = showToast
                                    )
                                }
                                composable(ROUTE_SETTINGS) {
                                    SettingsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
