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

// ---- Colores de marca (degradados y acentos) ----
val BrandGradientStart = Color(0xFF8B7CF6)
val BrandGradientMid = Color(0xFF7C6BEF)
val BrandGradientEnd = Color(0xFF2DD4BF)

val BrandGradient: Brush = Brush.linearGradient(
    colors = listOf(BrandGradientStart, BrandGradientMid, BrandGradientEnd)
)
