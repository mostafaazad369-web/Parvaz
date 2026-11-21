package app.parvaz.vpn.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFFB45309),
    background = Color(0xFFF4F9F8),
    onBackground = Color(0xFF0E1D1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1D1B),
    surfaceVariant = Color(0xFFE3EEEC),
    onSurfaceVariant = Color(0xFF3F4F4D),
    outline = Color(0xFF8AA09D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF0B3D38),
    onPrimaryContainer = Color(0xFFB9F5EC),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF0A1110),
    onBackground = Color(0xFFE2EFED),
    surface = Color(0xFF101A19),
    onSurface = Color(0xFFE2EFED),
    surfaceVariant = Color(0xFF1B2927),
    onSurfaceVariant = Color(0xFFB5C7C4),
    outline = Color(0xFF5F7774),
)

/** mode: 0 = system, 1 = light, 2 = dark */
@Composable
fun ParvazTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { w ->
                WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !dark
                WindowCompat.getInsetsController(w, view).isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
