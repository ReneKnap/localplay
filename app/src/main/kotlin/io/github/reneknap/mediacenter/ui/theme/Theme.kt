package io.github.reneknap.mediacenter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun MediaCenterTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    val colorScheme = when {
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
