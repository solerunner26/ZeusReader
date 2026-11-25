package com.solerunner26.zeusreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ZeusPrimaryLight,
    onPrimary = ZeusOnPrimaryLight,
    primaryContainer = ZeusPrimaryContainerLight,
    onPrimaryContainer = ZeusOnPrimaryContainerLight,
    secondary = ZeusSecondaryLight,
    onSecondary = ZeusOnSecondaryLight,
    secondaryContainer = ZeusPrimaryContainerLight,
    onSecondaryContainer = ZeusOnPrimaryContainerLight,
    background = ZeusBackgroundLight,
    onBackground = ZeusOnBackgroundLight,
    surface = ZeusSurfaceLight,
    onSurface = ZeusOnSurfaceLight,
    surfaceVariant = ZeusSurfaceVariantLight,
    onSurfaceVariant = ZeusOnSurfaceVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = ZeusPrimaryDark,
    onPrimary = ZeusOnPrimaryDark,
    primaryContainer = ZeusPrimaryContainerDark,
    onPrimaryContainer = ZeusOnPrimaryContainerDark,
    secondary = ZeusSecondaryDark,
    onSecondary = ZeusOnSecondaryDark,
    secondaryContainer = ZeusPrimaryContainerDark,
    onSecondaryContainer = ZeusOnPrimaryContainerDark,
    background = ZeusBackgroundDark,
    onBackground = ZeusOnBackgroundDark,
    surface = ZeusSurfaceDark,
    onSurface = ZeusOnSurfaceDark,
    surfaceVariant = ZeusSurfaceVariantDark,
    onSurfaceVariant = ZeusOnSurfaceVariantDark
)

private val PureBlackOledColorScheme = DarkColorScheme.copy(
    background = ZeusPureBlackOled,
    surface = ZeusPureBlackOled
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZeusReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlackOled: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme && pureBlackOled -> PureBlackOledColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = ZeusTypography,
        content = content
    )
}
