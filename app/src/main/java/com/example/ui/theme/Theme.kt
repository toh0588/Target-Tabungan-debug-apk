package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = MintPrimaryDark,
    onPrimary = MintOnPrimaryDark,
    primaryContainer = MintContainerDark,
    onPrimaryContainer = MintOnContainerDark,
    secondary = AmberSecondaryDark,
    onSecondary = AmberOnSecondaryDark,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberOnContainerDark,
    tertiary = TealTertiaryDark,
    onTertiary = TealOnTertiaryDark,
    tertiaryContainer = TealContainerDark,
    onTertiaryContainer = TealOnContainerDark,
    background = PineDarkBackground,
    onBackground = PineDarkOnBackground,
    surface = PineDarkSurface,
    onSurface = PineDarkOnBackground,
    surfaceVariant = PineDarkSurfaceVariant,
    onSurfaceVariant = PineDarkOnSurfaceVariant,
    outline = PineDarkOutline,
    outlineVariant = PineDarkOutlineVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = GoldSecondary,
    onSecondary = GoldOnSecondary,
    secondaryContainer = GoldSecondaryContainer,
    onSecondaryContainer = GoldOnSecondaryContainer,
    tertiary = SlateTealTertiary,
    onTertiary = SlateTealOnTertiary,
    tertiaryContainer = SlateTealContainer,
    onTertiaryContainer = SlateTealOnTertiaryContainer,
    background = WarmIvoryBackground,
    onBackground = WarmInkOnBackground,
    surface = PurePaperSurface,
    onSurface = WarmInkOnBackground,
    surfaceVariant = WarmPaperVariant,
    onSurfaceVariant = MutedSlateOnVariant,
    outline = LedgerOutline,
    outlineVariant = LedgerOutlineVariant,
    error = ErrorRedLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun TargetTabunganTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
