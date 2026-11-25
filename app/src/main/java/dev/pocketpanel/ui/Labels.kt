package dev.pocketpanel.ui

import androidx.annotation.StringRes
import dev.pocketpanel.R
import dev.pocketpanel.settings.ThemeMode

@StringRes
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
fun ThemeMode.descriptionRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system_description
    ThemeMode.LIGHT -> R.string.theme_light_description
    ThemeMode.DARK -> R.string.theme_dark_description
}
