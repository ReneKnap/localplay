package io.github.reneknap.mediacenter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.reneknap.mediacenter.data.theme.ThemeMode

@Composable
fun MediaCenterTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val useDarkTheme =
        when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    val useDynamicColor = themeMode == ThemeMode.SYSTEM
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    val colorScheme =
        when {
            useDynamicColor && supportsDynamic && useDarkTheme -> dynamicDarkColorScheme(context)
            useDynamicColor && supportsDynamic && !useDarkTheme -> dynamicLightColorScheme(context)
            useDarkTheme -> MediaCenterDarkColors
            else -> MediaCenterLightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MediaCenterTypography,
        content = content,
    )
}
