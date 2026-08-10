package com.col.notnetmediaforge.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---- Paleta oscura: negros azulados con violeta eléctrico, cian y rosa ----
val DarkPrimary = Color(0xFFA294FF)
val DarkOnPrimary = Color(0xFF221A5C)
val DarkPrimaryContainer = Color(0xFF4534BA)
val DarkOnPrimaryContainer = Color(0xFFEAE4FF)

val DarkSecondary = Color(0xFF3FD8C8)
val DarkOnSecondary = Color(0xFF00332E)
val DarkSecondaryContainer = Color(0xFF00544C)
val DarkOnSecondaryContainer = Color(0xFFB2F5EA)

val DarkTertiary = Color(0xFFFF8FB3)
val DarkOnTertiary = Color(0xFF531A2F)
val DarkTertiaryContainer = Color(0xFF7E3352)
val DarkOnTertiaryContainer = Color(0xFFFFD9E4)

val DarkBackground = Color(0xFF07080C)
val DarkOnBackground = Color(0xFFE8EAF2)
val DarkSurface = Color(0xFF0C0E15)
val DarkOnSurface = Color(0xFFE8EAF2)
val DarkSurfaceVariant = Color(0xFF151925)
val DarkOnSurfaceVariant = Color(0xFFABB1C2)
val DarkSurfaceContainerLow = Color(0xFF0F121B)
val DarkSurfaceContainer = Color(0xFF141822)
val DarkSurfaceContainerHigh = Color(0xFF1A1E2B)
val DarkSurfaceContainerHighest = Color(0xFF232837)
val DarkOnSurfaceContainer = Color(0xFFE1E4EE)
val DarkSurfaceTint = Color(0xFFA294FF)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
val DarkOutline = Color(0xFF8B91A2)
val DarkOutlineVariant = Color(0xFF2A2F3D)

// ---- Paleta modo claro (refinada) ----
val LightPrimary = Color(0xFF5A4BC0)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE8E0FF)
val LightOnPrimaryContainer = Color(0xFF1C006B)

val LightSecondary = Color(0xFF006A64)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFF9BF2EA)
val LightOnSecondaryContainer = Color(0xFF00201D)

val LightTertiary = Color(0xFF8C4A5C)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFD9E0)
val LightOnTertiaryContainer = Color(0xFF3B071B)

val LightBackground = Color(0xFFFDFBFF)
val LightOnBackground = Color(0xFF1B1B1F)
val LightSurface = Color(0xFFFDFBFF)
val LightOnSurface = Color(0xFF1B1B1F)
val LightSurfaceVariant = Color(0xFFE4E1EC)
val LightOnSurfaceVariant = Color(0xFF47464F)
val LightSurfaceContainerLow = Color(0xFFF7F4FA)
val LightSurfaceContainer = Color(0xFFF1EDF7)
val LightSurfaceContainerHigh = Color(0xFFEBE7F1)
val LightSurfaceContainerHighest = Color(0xFFE5E1EC)
val LightOnSurfaceContainer = Color(0xFF1F1E24)
val LightSurfaceTint = Color(0xFF5A4BC0)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)
val LightOutline = Color(0xFF777680)
val LightOutlineVariant = Color(0xFFC7C5D0)

// ---- Colores de marca (degradados y acentos) ----
val BrandGradientStart = Color(0xFF8B7CF6)
val BrandGradientMid = Color(0xFF7C6BEF)
val BrandGradientEnd = Color(0xFF2DD4BF)

val BrandGradient: Brush = Brush.linearGradient(
    colors = listOf(BrandGradientStart, BrandGradientMid, BrandGradientEnd)
)

val BrandGradientSoft: Brush = Brush.linearGradient(
    colors = listOf(
        BrandGradientStart.copy(alpha = 0.18f),
        BrandGradientEnd.copy(alpha = 0.10f)
    )
)
