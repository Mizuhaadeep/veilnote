package com.glassnote.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.glassnote.R
import com.glassnote.domain.model.AmbienceColor
import com.glassnote.domain.model.GlassGradient

/** Colours used for the tint inside a note's glass card. */
data class AmbiencePalette(
    val primary: Color,
    val secondary: Color,
    val dot: Color,
    @StringRes val labelRes: Int,
)

/** Background gradient stops and the colour used by its drifting glow. */
data class BackdropPalette(
    val colors: List<Color>,
    val glow: Color,
)

@Composable
fun isDarkGlassTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

@StringRes
fun AmbienceColor.labelRes(): Int = when (this) {
    AmbienceColor.MIST -> R.string.ambience_mist
    AmbienceColor.SAGE -> R.string.ambience_sage
    AmbienceColor.OCEAN -> R.string.ambience_ocean
    AmbienceColor.AMBER -> R.string.ambience_amber
    AmbienceColor.ROSE -> R.string.ambience_rose
    AmbienceColor.VIOLET -> R.string.ambience_violet
}

@StringRes
fun GlassGradient.labelRes(): Int = when (this) {
    GlassGradient.AURORA -> R.string.gradient_aurora
    GlassGradient.SUNSET -> R.string.gradient_sunset
    GlassGradient.MIDNIGHT -> R.string.gradient_midnight
    GlassGradient.SAGE -> R.string.gradient_sage
    GlassGradient.BLUSH -> R.string.gradient_blush
    GlassGradient.OCEAN -> R.string.gradient_ocean
}

fun AmbienceColor.palette(darkTheme: Boolean): AmbiencePalette = when (this) {
    AmbienceColor.MIST -> AmbiencePalette(
        primary = Color(0xFFA2B6C8),
        secondary = Color(0xFFD9E4EA),
        dot = if (darkTheme) Color(0xFFB9CFDF) else Color(0xFF8EA7BA),
        labelRes = R.string.ambience_mist,
    )
    AmbienceColor.SAGE -> AmbiencePalette(
        primary = Color(0xFF83A98F),
        secondary = Color(0xFFD6E4D6),
        dot = if (darkTheme) Color(0xFFAAD0B6) else Color(0xFF74997F),
        labelRes = R.string.ambience_sage,
    )
    AmbienceColor.OCEAN -> AmbiencePalette(
        primary = Color(0xFF7EA9CB),
        secondary = Color(0xFFD1E5F1),
        dot = if (darkTheme) Color(0xFF96C2DF) else Color(0xFF6C99BC),
        labelRes = R.string.ambience_ocean,
    )
    AmbienceColor.AMBER -> AmbiencePalette(
        primary = Color(0xFFCBA16B),
        secondary = Color(0xFFF2DFC4),
        dot = if (darkTheme) Color(0xFFE7C18C) else Color(0xFFB88B54),
        labelRes = R.string.ambience_amber,
    )
    AmbienceColor.ROSE -> AmbiencePalette(
        primary = Color(0xFFC68A95),
        secondary = Color(0xFFF1D6DA),
        dot = if (darkTheme) Color(0xFFE4AEB7) else Color(0xFFB57A85),
        labelRes = R.string.ambience_rose,
    )
    AmbienceColor.VIOLET -> AmbiencePalette(
        primary = Color(0xFF9785C5),
        secondary = Color(0xFFDED5F1),
        dot = if (darkTheme) Color(0xFFBBA8E7) else Color(0xFF8774B5),
        labelRes = R.string.ambience_violet,
    )
}

fun GlassGradient.backdropPalette(darkTheme: Boolean): BackdropPalette =
    if (darkTheme) {
        when (this) {
            GlassGradient.AURORA -> BackdropPalette(
                colors = listOf(
                    Color(0xFF071416),
                    Color(0xFF0B2428),
                    Color(0xFF241E37),
                    Color(0xFF0C1016),
                ),
                glow = Color(0xFF1B6D67),
            )
            GlassGradient.SUNSET -> BackdropPalette(
                colors = listOf(
                    Color(0xFF1B0C12),
                    Color(0xFF35151C),
                    Color(0xFF291A31),
                    Color(0xFF120E14),
                ),
                glow = Color(0xFF9A4C43),
            )
            GlassGradient.MIDNIGHT -> BackdropPalette(
                colors = listOf(
                    Color(0xFF06080E),
                    Color(0xFF10182B),
                    Color(0xFF1D2140),
                    Color(0xFF0A0C12),
                ),
                glow = Color(0xFF2E457A),
            )
            GlassGradient.SAGE -> BackdropPalette(
                colors = listOf(
                    Color(0xFF0A110D),
                    Color(0xFF132019),
                    Color(0xFF1B2116),
                    Color(0xFF0C100C),
                ),
                glow = Color(0xFF365B43),
            )
            GlassGradient.BLUSH -> BackdropPalette(
                colors = listOf(
                    Color(0xFF1B0D13),
                    Color(0xFF301722),
                    Color(0xFF2A1C30),
                    Color(0xFF100C10),
                ),
                glow = Color(0xFF7E3F54),
            )
            GlassGradient.OCEAN -> BackdropPalette(
                colors = listOf(
                    Color(0xFF061016),
                    Color(0xFF0C2029),
                    Color(0xFF0F2233),
                    Color(0xFF071014),
                ),
                glow = Color(0xFF1F566B),
            )
        }
    } else {
        when (this) {
            GlassGradient.AURORA -> BackdropPalette(
                colors = listOf(
                    Color(0xFFE7F0EA),
                    Color(0xFFD3E5E6),
                    Color(0xFFD9D9F0),
                    Color(0xFFF2EBE0),
                ),
                glow = Color(0xFF9FC7C2),
            )
            GlassGradient.SUNSET -> BackdropPalette(
                colors = listOf(
                    Color(0xFFF7E6D8),
                    Color(0xFFEFD1D2),
                    Color(0xFFD9D3EC),
                    Color(0xFFF4E7D5),
                ),
                glow = Color(0xFFE8B4A0),
            )
            GlassGradient.MIDNIGHT -> BackdropPalette(
                colors = listOf(
                    Color(0xFFE4E9F2),
                    Color(0xFFCFD9E7),
                    Color(0xFFDFDCEB),
                    Color(0xFFF0ECE5),
                ),
                glow = Color(0xFFAEBBD6),
            )
            GlassGradient.SAGE -> BackdropPalette(
                colors = listOf(
                    Color(0xFFECEADF),
                    Color(0xFFDCE4D7),
                    Color(0xFFE9DFCF),
                    Color(0xFFF4F0E7),
                ),
                glow = Color(0xFFB9C8AC),
            )
            GlassGradient.BLUSH -> BackdropPalette(
                colors = listOf(
                    Color(0xFFF5E5E5),
                    Color(0xFFEAD4DC),
                    Color(0xFFE1D9F0),
                    Color(0xFFF7EADE),
                ),
                glow = Color(0xFFDCB5C1),
            )
            GlassGradient.OCEAN -> BackdropPalette(
                colors = listOf(
                    Color(0xFFE3EDF0),
                    Color(0xFFCFE1E9),
                    Color(0xFFD7DFEB),
                    Color(0xFFEDEFE9),
                ),
                glow = Color(0xFFA4C6D3),
            )
        }
    }
