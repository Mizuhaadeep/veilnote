package com.glassnote.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.glassnote.R
import com.glassnote.domain.model.AppMode
import com.glassnote.domain.model.AppSettings
import com.glassnote.domain.model.BackgroundSource
import com.glassnote.domain.model.DarkModePreference
import com.glassnote.domain.model.GlassGradient
import com.glassnote.domain.model.StorageMode
import com.glassnote.domain.model.TransitionStyle
import com.glassnote.ui.components.GlassIconButton
import com.glassnote.ui.components.GlassPill
import com.glassnote.ui.components.GlassSurface
import com.glassnote.ui.components.iosOverscrollBounce
import com.glassnote.ui.components.rememberOverscrollBounceState
import com.glassnote.ui.theme.GlassMotion
import com.glassnote.ui.theme.backdropPalette
import com.glassnote.ui.theme.isDarkGlassTheme
import com.glassnote.ui.theme.labelRes
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onRequestSafFolder: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val darkTheme = isDarkGlassTheme()
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.useBackgroundImage(uri)
        }
    }

    fun openImagePicker() {
        viewModel.dismissImageError()
        imagePicker.launch(arrayOf("image/*"))
    }

    val settingsScrollState = rememberScrollState()
    val bounceState = rememberOverscrollBounceState(settingsScrollState.isScrollInProgress)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SettingsHeader(onBack = onBack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .iosOverscrollBounce(bounceState)
                    .verticalScroll(settingsScrollState)
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            ) {
                SettingsSectionLabel(text = stringResource(R.string.settings_background_title))

                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionTitle(
                                title = stringResource(R.string.settings_gradient_title),
                                modifier = Modifier.weight(1f),
                            )
                            if (uiState.settings.effectiveBackgroundSource == BackgroundSource.GRADIENT) {
                                ActiveDot(
                                    text = stringResource(uiState.settings.gradient.labelRes()),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(13.dp))
                        GradientChoiceGrid(
                            selected = uiState.settings.gradient,
                            darkTheme = darkTheme,
                            onSelect = viewModel::selectGradient,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle(
                                title = stringResource(R.string.settings_image_title),
                                modifier = Modifier.weight(1f),
                            )
                            if (
                                uiState.settings.backgroundSource == BackgroundSource.IMAGE &&
                                !uiState.settings.backgroundImageUri.isNullOrBlank()
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_image_active),
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        BackgroundImagePreview(
                            imageUri = uiState.settings.backgroundImageUri,
                            settings = uiState.settings,
                            darkTheme = darkTheme,
                            onActivate = viewModel::activateSavedBackgroundImage,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlassPill(
                                selected = true,
                                onClick = { openImagePicker() },
                                enabled = !uiState.isImportingImage,
                            ) {
                                if (uiState.isImportingImage) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                                Text(
                                    text = stringResource(
                                        when {
                                            uiState.isImportingImage -> R.string.settings_image_importing
                                            uiState.settings.backgroundImageUri.isNullOrBlank() ->
                                                R.string.settings_image_pick
                                            else -> R.string.settings_image_change
                                        },
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            if (!uiState.settings.backgroundImageUri.isNullOrBlank()) {
                                GlassPill(
                                    selected = false,
                                    onClick = viewModel::clearBackgroundImage,
                                    enabled = !uiState.isImportingImage,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_image_remove),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(9.dp))
                        Text(
                            text = stringResource(R.string.settings_image_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                        AnimatedVisibility(
                            visible = uiState.imageImportFailed,
                            enter = fadeIn(tween(GlassMotion.QUICK_MS)),
                            exit = fadeOut(tween(GlassMotion.QUICK_MS)),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_image_failed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 7.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))
                SettingsSectionLabel(text = stringResource(R.string.settings_appearance_title))

                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle(title = stringResource(R.string.settings_mode_title))
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = stringResource(R.string.settings_mode_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppModeSelector(
                            selected = uiState.settings.appMode,
                            onSelect = viewModel::setAppMode,
                        )
                        if (uiState.settings.appMode == AppMode.FULL) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FeatureSwitchRow(
                                title = stringResource(R.string.settings_transition_title),
                                checked = uiState.settings.transitionsEnabled,
                                onCheckedChange = viewModel::setTransitionsEnabled,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FeatureSwitchRow(
                                title = stringResource(R.string.settings_url_switch),
                                checked = uiState.settings.urlDetectionEnabled,
                                onCheckedChange = viewModel::setUrlDetectionEnabled,
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalRule()
                        Spacer(modifier = Modifier.height(18.dp))
                        BlurControl(
                            blurRadiusDp = uiState.settings.blurRadiusDp,
                            onCommit = viewModel::setBlurRadiusDp,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalRule()
                        Spacer(modifier = Modifier.height(18.dp))
                        SectionTitle(title = stringResource(R.string.settings_dark_title))
                        Spacer(modifier = Modifier.height(12.dp))
                        DarkModeSelector(
                            selected = uiState.settings.darkModePreference,
                            onSelect = viewModel::setDarkModePreference,
                        )
                        if (uiState.settings.appMode == AppMode.FULL) {
                            AnimatedVisibility(
                                visible = uiState.settings.transitionsEnabled,
                                enter = expandVertically(
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                ) + fadeIn(animationSpec = tween(180)),
                                exit = shrinkVertically(
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                ) + fadeOut(animationSpec = tween(140)),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    HorizontalRule()
                                    Spacer(modifier = Modifier.height(18.dp))
                                    SectionTitle(
                                        title = stringResource(R.string.settings_transition_style_title),
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = stringResource(R.string.settings_transition_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    TransitionStyleSelector(
                                        selected = uiState.settings.transitionStyle,
                                        onSelect = viewModel::setTransitionStyle,
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.settings_mode_minimal_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalRule()
                        Spacer(modifier = Modifier.height(18.dp))
                        SectionTitle(title = stringResource(R.string.settings_language_title))
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = stringResource(R.string.settings_language_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LanguageSelector()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionLabel(text = stringResource(R.string.settings_data_title))
                StorageStatusCard(
                    mode = uiState.settings.storageMode,
                    folderName = uiState.settings.safFolderName,
                    onRequestSafFolder = onRequestSafFolder,
                )
                Spacer(modifier = Modifier.height(12.dp))
                LocalOnlyCard()

                Spacer(modifier = Modifier.height(22.dp))
                TextButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = stringResource(R.string.settings_reset),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_reset_title)) },
            text = { Text(stringResource(R.string.settings_reset_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetToDefaults()
                    },
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = if (darkTheme) Color(0xFF1B1E25) else Color(0xFFFAF7F1),
        )
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            onClick = onBack,
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 4.dp, bottom = 9.dp),
    )
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
private fun GradientChoiceGrid(
    selected: GlassGradient,
    darkTheme: Boolean,
    onSelect: (GlassGradient) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        GlassGradient.entries.chunked(3).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                rowEntries.forEach { gradient ->
                    GradientChoice(
                        gradient = gradient,
                        selected = gradient == selected,
                        darkTheme = darkTheme,
                        onClick = { onSelect(gradient) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowEntries.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GradientChoice(
    gradient: GlassGradient,
    selected: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(15.dp)
    val palette = gradient.backdropPalette(darkTheme)
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(shape)
                .background(Brush.linearGradient(palette.colors))
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White.copy(alpha = if (darkTheme) 0.12f else 0.5f)
                    },
                    shape = shape,
                )
                .clickable(onClick = onClick),
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = stringResource(gradient.labelRes()),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun BackgroundImagePreview(
    imageUri: String?,
    settings: AppSettings,
    darkTheme: Boolean,
    onActivate: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val palette = settings.gradient.backdropPalette(darkTheme)
    val canActivate = !imageUri.isNullOrBlank() &&
        settings.backgroundSource != BackgroundSource.IMAGE
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(shape)
            .background(Brush.linearGradient(palette.colors))
            .border(1.dp, Color.White.copy(alpha = if (darkTheme) 0.13f else 0.5f), shape)
            .clickable(
                enabled = canActivate,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onActivate,
            ),
    ) {
        if (imageUri.isNullOrBlank()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(26.dp),
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = stringResource(R.string.settings_image_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            AsyncImage(
                model = imageUri,
                contentDescription = stringResource(R.string.cd_background_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (canActivate) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.48f))
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.size(5.dp))
                    Text(
                        text = stringResource(R.string.settings_image_use),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun BlurControl(
    blurRadiusDp: Int,
    onCommit: (Int) -> Unit,
) {
    var sliderValue by remember(blurRadiusDp) {
        mutableFloatStateOf(blurRadiusDp.toFloat())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(
            title = stringResource(R.string.settings_blur_title),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.settings_blur_value, sliderValue.roundToInt()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Slider(
        value = sliderValue,
        onValueChange = { sliderValue = it },
        onValueChangeFinished = { onCommit(sliderValue.roundToInt()) },
        valueRange = AppSettings.MIN_BLUR_RADIUS_DP.toFloat()..AppSettings.MAX_BLUR_RADIUS_DP.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
        ),
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_blur_value, AppSettings.MIN_BLUR_RADIUS_DP),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.settings_blur_value, AppSettings.MAX_BLUR_RADIUS_DP),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
    Spacer(modifier = Modifier.height(9.dp))
    Text(
        text = stringResource(R.string.settings_blur_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
    )
}

@Composable
private fun AppModeSelector(
    selected: AppMode,
    onSelect: (AppMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppMode.entries.forEach { mode ->
            GlassPill(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            when (mode) {
                                AppMode.MINIMAL -> R.string.settings_mode_minimal
                                AppMode.FULL -> R.string.settings_mode_full
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colorSpec = spring<Color>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessMediumLow,
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        },
        animationSpec = colorSpec,
        label = "featureSwitchTrack",
    )
    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        },
        animationSpec = colorSpec,
        label = "featureSwitchBorder",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f)
        },
        animationSpec = colorSpec,
        label = "featureSwitchThumb",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "featureSwitchThumbOffset",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            AnimatedContent(
                targetState = checked,
                transitionSpec = {
                    fadeIn(tween(140)) togetherWith fadeOut(tween(100))
                },
                label = "featureSwitchStateLabel",
            ) { isOn ->
                Text(
                    text = if (isOn) "ON" else "OFF",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .width(52.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(trackColor)
                .border(
                    border = BorderStroke(1.dp, trackBorderColor),
                    shape = CircleShape,
                )
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    }
}

@Composable
private fun TransitionStyleSelector(
    selected: TransitionStyle,
    onSelect: (TransitionStyle) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TransitionStyle.entries.forEach { style ->
            GlassPill(
                selected = style == selected,
                onClick = { onSelect(style) },
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            when (style) {
                                TransitionStyle.BOTTOM_UP -> R.string.settings_transition_bottom_up
                                TransitionStyle.SCALE -> R.string.settings_transition_scale
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkModeSelector(
    selected: DarkModePreference,
    onSelect: (DarkModePreference) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DarkModePreference.entries.forEach { preference ->
            GlassPill(
                selected = preference == selected,
                onClick = { onSelect(preference) },
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            when (preference) {
                                DarkModePreference.SYSTEM -> R.string.settings_dark_system
                                DarkModePreference.LIGHT -> R.string.settings_dark_light
                                DarkModePreference.DARK -> R.string.settings_dark_dark
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private enum class AppLanguage(
    val languageTag: String?,
    val fixedLabel: String?,
) {
    SYSTEM(null, null),
    SIMPLIFIED_CHINESE("zh-CN", "简体中文"),
    TRADITIONAL_CHINESE("zh-TW", "繁體中文"),
    ENGLISH("en", "English"),
}

@Composable
private fun LanguageSelector() {
    val appLocales = AppCompatDelegate.getApplicationLocales()
    val activeLocale = appLocales.get(0)
    val selectedLanguage = when {
        appLocales.isEmpty() -> AppLanguage.SYSTEM
        activeLocale?.language?.equals("zh", ignoreCase = true) == true &&
            activeLocale?.country?.equals("TW", ignoreCase = true) == true ->
            AppLanguage.TRADITIONAL_CHINESE
        activeLocale?.language?.equals("zh", ignoreCase = true) == true ->
            AppLanguage.SIMPLIFIED_CHINESE
        activeLocale?.language?.equals("en", ignoreCase = true) == true -> AppLanguage.ENGLISH
        else -> AppLanguage.SYSTEM
    }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedLanguage) {
        listState.animateScrollToItem(AppLanguage.entries.indexOf(selectedLanguage))
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = AppLanguage.entries, key = { it.name }) { language ->
            GlassPill(
                selected = language == selectedLanguage,
                onClick = {
                    if (language != selectedLanguage) {
                        val locales = if (language.languageTag == null) {
                            LocaleListCompat.getEmptyLocaleList()
                        } else {
                            LocaleListCompat.forLanguageTags(language.languageTag)
                        }
                        AppCompatDelegate.setApplicationLocales(locales)
                    }
                },
            ) {
                val label = language.fixedLabel
                    ?: stringResource(R.string.settings_language_system)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StorageStatusCard(
    mode: StorageMode?,
    folderName: String?,
    onRequestSafFolder: () -> Unit,
) {
    val darkTheme = isDarkGlassTheme()
    val safeColor = if (darkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
    val isSaf = mode == StorageMode.SAF_MODE

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSaf) {
                            safeColor.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (isSaf) safeColor else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (isSaf) R.string.storage_status_saf
                        else R.string.storage_status_private,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSaf) safeColor else MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(3.dp))
                if (isSaf) {
                    Text(
                        text = stringResource(
                            R.string.storage_status_folder,
                            folderName ?: "Veilnote",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.storage_status_safe),
                        style = MaterialTheme.typography.bodySmall,
                        color = safeColor,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.storage_status_private_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestSafFolder,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(text = stringResource(R.string.storage_status_enable))
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalOnlyCard() {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_local_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.settings_local_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HorizontalRule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
    )
}

@Composable
private fun ActiveDot(text: String) {
    if (text.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
