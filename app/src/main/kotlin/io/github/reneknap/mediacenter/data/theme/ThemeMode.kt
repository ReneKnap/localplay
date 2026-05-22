package io.github.reneknap.mediacenter.data.theme

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM,
}

fun ThemeMode.next(): ThemeMode =
    when (this) {
        ThemeMode.DARK -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.SYSTEM
        ThemeMode.SYSTEM -> ThemeMode.DARK
    }
