package com.homiq.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.homiq.app.data.preferences.TextSizeMode
import com.homiq.app.data.preferences.TextSizePreferences

private val LightColors = lightColorScheme(
    primary = HomikaTeal,
    onPrimary = HomikaSurface,
    primaryContainer = HomikaMintSoft,
    onPrimaryContainer = HomikaTealDeep,
    secondary = HomikaTealDeep,
    onSecondary = HomikaSurface,
    secondaryContainer = HomikaSurfaceSoft,
    onSecondaryContainer = HomikaInk,
    background = HomikaCanvas,
    onBackground = HomikaInk,
    surface = HomikaSurface,
    onSurface = HomikaInk,
    surfaceVariant = HomikaSurfaceSoft,
    onSurfaceVariant = HomikaInkMuted,
    outline = HomikaOutline,
    error = HomikaDanger,
)

private val ColorErrorDark = androidx.compose.ui.graphics.Color(0xFFFFB4AB)

private val DarkColors = darkColorScheme(
    primary = HomikaMint,
    onPrimary = HomikaTealDeep,
    primaryContainer = HomikaTealDeep,
    onPrimaryContainer = HomikaMintSoft,
    secondary = HomikaMint,
    onSecondary = HomikaTealDeep,
    secondaryContainer = HomikaDarkSurfaceRaised,
    onSecondaryContainer = HomikaDarkOnSurface,
    background = HomikaDarkCanvas,
    onBackground = HomikaDarkOnSurface,
    surface = HomikaDarkSurface,
    onSurface = HomikaDarkOnSurface,
    surfaceVariant = HomikaDarkSurfaceRaised,
    onSurfaceVariant = HomikaDarkMuted,
    outline = HomikaDarkOutline,
    error = ColorErrorDark,
)

private val HomikaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

data class HomikaTextSizeController(
    val mode: TextSizeMode,
    val setMode: (TextSizeMode) -> Unit,
)

val LocalHomikaTextSize = staticCompositionLocalOf<HomikaTextSizeController> {
    error("Homika text-size controller is not available")
}

@Composable
fun HomiqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val textSizePreferences = remember(context) {
        TextSizePreferences(context.applicationContext)
    }
    var textSizeMode by remember {
        mutableStateOf(textSizePreferences.mode)
    }
    val systemDensity = LocalDensity.current
    val homikaDensity = remember(
        systemDensity.density,
        systemDensity.fontScale,
        textSizeMode,
    ) {
        Density(
            density = systemDensity.density,
            fontScale = systemDensity.fontScale * textSizeMode.scale,
        )
    }
    val textSizeController = HomikaTextSizeController(
        mode = textSizeMode,
        setMode = { mode ->
            textSizePreferences.set(mode)
            textSizeMode = mode
        },
    )

    CompositionLocalProvider(
        LocalDensity provides homikaDensity,
        LocalHomikaTextSize provides textSizeController,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = HomikaTypography,
            shapes = HomikaShapes,
            content = content,
        )
    }
}
