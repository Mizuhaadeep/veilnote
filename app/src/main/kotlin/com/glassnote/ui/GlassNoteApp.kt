package com.glassnote.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.glassnote.AppContainer
import com.glassnote.R
import com.glassnote.domain.model.AppMode
import com.glassnote.domain.model.DarkModePreference
import com.glassnote.domain.model.StorageMode
import com.glassnote.domain.model.TransitionStyle
import com.glassnote.ui.components.AmbientBackground
import com.glassnote.ui.editor.EditorScreen
import com.glassnote.ui.editor.EditorViewModel
import com.glassnote.ui.home.HomeScreen
import com.glassnote.ui.home.HomeViewModel
import com.glassnote.ui.settings.SettingsScreen
import com.glassnote.ui.settings.SettingsViewModel
import com.glassnote.ui.storage.OpenVeilnoteTreeContract
import com.glassnote.ui.storage.SafGuideDialog
import com.glassnote.ui.storage.StorageChoiceDialog
import com.glassnote.ui.storage.StorageSkipWarningDialog
import com.glassnote.ui.storage.VeilnoteDocumentsInitialUri
import com.glassnote.ui.theme.GlassNoteTheme
import com.glassnote.ui.theme.LocalAppMode
import com.glassnote.ui.theme.LocalUrlDetectionEnabled
import com.glassnote.ui.transition.ContainerTransformPage
import com.glassnote.ui.transition.HomeTransitionBackdrop
import com.glassnote.ui.transition.MaskRevealPage
import com.glassnote.ui.transition.TransitionOrigin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{noteId}"

    fun editor(noteId: Long): String = "editor/$noteId"
}

private enum class MaskPage {
    SETTINGS,
    NEW_EDITOR,
}

private enum class StorageStage {
    CHOICE,
    SKIP_WARNING,
    SAF_GUIDE,
}

@Composable
fun GlassNoteApp(container: AppContainer) {
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            settingsRepository = container.settingsRepository,
            backgroundImageStore = container.backgroundImageStore,
        ),
    )
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val settingsLoaded by settingsViewModel.settingsLoaded.collectAsStateWithLifecycle()
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (settings.darkModePreference) {
        DarkModePreference.SYSTEM -> systemDarkTheme
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }

    GlassNoteTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(
            LocalAppMode provides settings.appMode,
            LocalUrlDetectionEnabled provides (
                settings.appMode == AppMode.FULL &&
                    settings.urlDetectionEnabled
                ),
        ) {
        val navController = rememberNavController()
        val appScope = rememberCoroutineScope()
        val context = LocalContext.current
        var storageStage by remember { mutableStateOf<StorageStage?>(null) }

        val safLauncher = rememberLauncherForActivityResult(
            contract = OpenVeilnoteTreeContract(),
        ) { treeUri ->
            if (treeUri == null) {
                storageStage = if (settings.storageMode == null) {
                    StorageStage.CHOICE
                } else {
                    null
                }
            } else {
                appScope.launch {
                    try {
                        val folder = container.safNoteStore.prepare(treeUri)
                        settingsViewModel.setStorageMode(
                            mode = StorageMode.SAF_MODE,
                            treeUri = folder.treeUri.toString(),
                            folderName = folder.folderName,
                        )
                        storageStage = null
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            R.string.storage_saf_setup_failed,
                            Toast.LENGTH_LONG,
                        ).show()
                        storageStage = if (settings.storageMode == null) {
                            StorageStage.CHOICE
                        } else {
                            null
                        }
                    }
                }
            }
        }

        LaunchedEffect(settingsLoaded, settings.storageMode) {
            if (!settingsLoaded) return@LaunchedEffect
            when {
                settings.storageMode == null -> {
                    if (storageStage == null) storageStage = StorageStage.CHOICE
                }
                storageStage == StorageStage.CHOICE -> storageStage = null
            }
        }

        LaunchedEffect(settings.storageMode, settings.safTreeUri) {
            if (settings.storageMode == StorageMode.SAF_MODE) {
                val treeUri = settings.safTreeUri?.toUri()
                if (treeUri == null) {
                    settingsViewModel.setStorageMode(StorageMode.PRIVATE_MODE)
                } else {
                    // Imports `.veil` files first, then mirrors every Room
                    // change. It forces PRIVATE_MODE internally on failure.
                    container.safSyncManager.run(treeUri)
                }
            }
        }
        var navigationInFlight by remember { mutableStateOf(false) }
        var pageClosing by remember { mutableStateOf(false) }
        var maskPage by remember { mutableStateOf<MaskPage?>(null) }
        var maskOrigin by remember { mutableStateOf<TransitionOrigin?>(null) }
        var maskClosing by remember { mutableStateOf(false) }
        val currentEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentEntry?.destination?.route
        val animationsEnabled =
            settings.appMode == AppMode.FULL && settings.transitionsEnabled

        LaunchedEffect(navigationInFlight) {
            if (navigationInFlight) {
                delay(900)
                navigationInFlight = false
            }
        }

        val closePage: () -> Unit = {
            if (!pageClosing && currentRoute != null && currentRoute != Routes.HOME) {
                pageClosing = true
                appScope.launch {
                    // Keep the NavHost destination composed for the BOTTOM_UP
                    // exit; the page's own animation is the only visual driver.
                    delay(180)
                    navController.popBackStack()
                    pageClosing = false
                }
            }
        }

        val closeMask: () -> Unit = {
            if (maskPage != null && !maskClosing) {
                maskClosing = true
            }
        }

        val finishMask: () -> Unit = {
            maskPage = null
            maskOrigin = null
            maskClosing = false
        }

        BackHandler(
            enabled = currentRoute != null && currentRoute != Routes.HOME,
        ) {
            closePage()
        }

        BackHandler(
            enabled = maskPage != null && !maskClosing,
        ) {
            closeMask()
        }

        val openNewNote: (TransitionOrigin?) -> Unit = { sourceOrigin ->
            if (!navigationInFlight && navController.currentDestination?.route == Routes.HOME) {
                if (
                    animationsEnabled &&
                    settings.transitionStyle == TransitionStyle.SCALE &&
                    sourceOrigin != null
                ) {
                    // New note is one of the three mask sources.
                    maskOrigin = sourceOrigin
                    maskClosing = false
                    maskPage = MaskPage.NEW_EDITOR
                } else {
                    navigationInFlight = true
                    navController.navigate(Routes.editor(0L)) {
                        launchSingleTop = true
                    }
                }
            }
        }

        val openExistingNote: (Long, TransitionOrigin?) -> Unit = { noteId, _ ->
            if (!navigationInFlight && navController.currentDestination?.route == Routes.HOME) {
                // Existing note cards are explicitly excluded from the mask:
                // they always keep the BOTTOM_UP navigation path.
                navigationInFlight = true
                navController.navigate(Routes.editor(noteId)) {
                    launchSingleTop = true
                }
            }
        }

        val openSettings: (TransitionOrigin?) -> Unit = { sourceOrigin ->
            if (!navigationInFlight && navController.currentDestination?.route == Routes.HOME) {
                if (
                    animationsEnabled &&
                    settings.transitionStyle == TransitionStyle.SCALE &&
                    sourceOrigin != null
                ) {
                    maskOrigin = sourceOrigin
                    maskClosing = false
                    maskPage = MaskPage.SETTINGS
                } else {
                    navigationInFlight = true
                    navController.navigate(Routes.SETTINGS) {
                        launchSingleTop = true
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AmbientBackground(
                settings = settings,
                modifier = Modifier.fillMaxSize(),
            )

            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { NavKeeperEnter },
                exitTransition = { NavKeeperExit },
                popEnterTransition = { NavKeeperEnter },
                popExitTransition = { NavKeeperExit },
            ) {
                composable(route = Routes.HOME) {
                    val animatedScope = this
                    HomeTransitionBackdrop(
                        animatedVisibilityScope = animatedScope,
                        exiting = pageClosing,
                        minimal = !animationsEnabled,
                    ) {
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = HomeViewModel.factory(container.noteRepository),
                        )
                        HomeScreen(
                            viewModel = homeViewModel,
                            onOpenNote = openExistingNote,
                            onCreateNote = openNewNote,
                            onOpenSettings = openSettings,
                        )
                    }
                }

                composable(
                    route = Routes.EDITOR,
                    arguments = listOf(
                        navArgument("noteId") { type = NavType.LongType },
                    ),
                ) { backStackEntry ->
                    val animatedScope = this
                    val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                    val editorViewModel: EditorViewModel = viewModel(
                        key = "editor-$noteId",
                        factory = EditorViewModel.factory(
                            noteId = noteId,
                            repository = container.noteRepository,
                        ),
                    )
                    ContainerTransformPage(
                        animatedVisibilityScope = animatedScope,
                        closing = pageClosing,
                        minimal = !animationsEnabled,
                    ) {
                        EditorScreen(
                            viewModel = editorViewModel,
                            onBack = closePage,
                        )
                    }
                }

                composable(route = Routes.SETTINGS) {
                    val animatedScope = this
                    ContainerTransformPage(
                        animatedVisibilityScope = animatedScope,
                        closing = pageClosing,
                        minimal = !animationsEnabled,
                    ) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = closePage,
                            onRequestSafFolder = {
                                storageStage = StorageStage.SAF_GUIDE
                            },
                        )
                    }
                }
            }

            if (maskPage != null && maskOrigin != null) {
                val page = maskPage!!
                val origin = maskOrigin!!
                MaskRevealPage(
                    origin = origin,
                    closing = maskClosing,
                    onExitFinished = finishMask,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (page) {
                        MaskPage.SETTINGS -> SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = closeMask,
                            onRequestSafFolder = {
                                storageStage = StorageStage.SAF_GUIDE
                            },
                        )
                        MaskPage.NEW_EDITOR -> MaskNewEditor(
                            container = container,
                            onBack = closeMask,
                        )
                    }
                }
            }
        }

        when (storageStage) {
            StorageStage.CHOICE -> StorageChoiceDialog(
                onAgree = {
                    storageStage = StorageStage.SAF_GUIDE
                },
                onSkip = {
                    storageStage = StorageStage.SKIP_WARNING
                },
            )

            StorageStage.SKIP_WARNING -> StorageSkipWarningDialog(
                onSkipAnyway = {
                    settingsViewModel.setStorageMode(StorageMode.PRIVATE_MODE)
                    storageStage = null
                },
                onCreate = {
                    storageStage = StorageStage.SAF_GUIDE
                },
            )

            StorageStage.SAF_GUIDE -> SafGuideDialog(
                onKnow = {
                    storageStage = null
                    safLauncher.launch(VeilnoteDocumentsInitialUri)
                },
            )

            null -> Unit
        }
        }
    }
}

@Composable
private fun MaskNewEditor(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val overlayOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(overlayOwner) {
        onDispose {
            overlayOwner.viewModelStore.clear()
        }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides overlayOwner) {
        val editorViewModel: EditorViewModel = viewModel(
            key = "mask-new-editor",
            factory = EditorViewModel.factory(
                noteId = 0L,
                repository = container.noteRepository,
            ),
        )
        EditorScreen(
            viewModel = editorViewModel,
            onBack = onBack,
        )
    }
}

/**
 * NavHost still needs a finite AnimatedContent transition so both destinations
 * stay composed while [ContainerTransformPage] runs its measured-bounds spring.
 *
 * The keeper transition deliberately changes the page scale by only 0.1%, i.e.
 * it is visually invisible. The real container transform is entirely driven by
 * each destination's AnimatedVisibilityScope.transition.
 */
private val NavKeeperSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow,
    visibilityThreshold = 0.00001f,
)

private val NavKeeperEnter: EnterTransition = scaleIn(
    animationSpec = NavKeeperSpec,
    initialScale = 0.999f,
)

private val NavKeeperExit: ExitTransition = scaleOut(
    animationSpec = NavKeeperSpec,
    targetScale = 0.999f,
)

