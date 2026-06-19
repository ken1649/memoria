package com.guoyuan.memoria.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.guoyuan.memoria.ui.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// 新增護眼配色方案
private val EyeCareColorScheme = lightColorScheme(
    primary = Color(0xFF8D6E63),      // 深棕色
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFCCBC),
    onPrimaryContainer = Color(0xFF3E2723),
    inversePrimary = Color(0xFFFFCCBC),
    secondary = Color(0xFF7C4D3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFCCBC),
    onSecondaryContainer = Color(0xFF3E2723),
    tertiary = Color(0xFF6D4C41),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFCCBC),
    onTertiaryContainer = Color(0xFF3E2723),
    background = Color(0xFFFFF8F0),   // 暖米色背景
    onBackground = Color(0xFF3E2723), // 深棕色文字
    surface = Color(0xFFFFF8F0),
    onSurface = Color(0xFF3E2723),
    surfaceVariant = Color(0xFFF5D9C6),
    onSurfaceVariant = Color(0xFF7C4D3F),
    inverseSurface = Color(0xFF3E2723),
    inverseOnSurface = Color(0xFFFFF8F0),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCD8DF),
    onErrorContainer = Color(0xFF3E2723),
    outline = Color(0xFF7C4D3F),
)

@Composable
fun MemoriaTheme(
    appTheme: AppTheme,  // 使用AppTheme取代darkTheme
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.DARK -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(LocalContext.current)
            } else {
                DarkColorScheme
            }
        }
        AppTheme.EYE_CARE -> EyeCareColorScheme
        AppTheme.SYSTEM -> {
            if (isSystemInDarkTheme()) {
                if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    dynamicDarkColorScheme(LocalContext.current)
                } else {
                    DarkColorScheme
                }
            } else {
                if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    dynamicLightColorScheme(LocalContext.current)
                } else {
                    LightColorScheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
