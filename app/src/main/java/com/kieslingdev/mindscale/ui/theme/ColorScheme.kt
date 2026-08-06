package com.kieslingdev.mindscale.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Every Material 3 colour role, set explicitly (`docs/specs/SPEC-visual-foundation.md`, D-9).
 *
 * Thirteen roles set and the rest defaulted is how a half-populated scheme leaks Material's
 * purple into a warm bone-and-gold app. `MindScaleColorSchemeTest` reflects over both schemes
 * and fails if any declared role still holds a Material default.
 *
 * Two mappings carry more meaning than the rest:
 *
 * - `inverseSurface`/`inverseOnSurface` are `ink`/`onInk`. This is the exact Material expression
 *   of the design's selected-segment and selected-chip treatment. Using the role rather than a
 *   bespoke pair is what keeps light and dark from collapsing into one flipped palette:
 *   `onInk` is `#E3CE9F` in light and `#2A2114` in dark.
 * - `surfaceTint` is [Color.Transparent]. Material's tonal-elevation overlay would otherwise
 *   tint every raised surface toward `primary`, and the design is near-flat warm neutrals.
 *   Killing the tint here is what makes D-13 hold without auditing every call site.
 *
 * Dynamic colour stays off, as decided in Phase 4.
 */

// Derived ladder steps. The design gives literal values only for `bg`, `card`, and the sleep
// band; these interpolate between them so a stray Material component that reaches for a
// container role still lands on a MindScale neutral. Every step is checked for contrast.
private val LightSurfaceContainer = Color(0xFFF8F6F1)
private val LightSurfaceContainerHigh = Color(0xFFF3F0EA)
private val LightSurfaceContainerHighest = Color(0xFFEFECE6)
private val LightGoldContainer = Color(0xFFF2E3C4)
private val LightNeutralContainer = Color(0xFFF4EAD7)
private val LightErrorContainer = Color(0xFFF7E2DE)
private val LightOnErrorContainer = Color(0xFF5C1F17)

private val DarkSurfaceContainerLowest = Color(0xFF0B0907)
private val DarkSurfaceContainerLow = Color(0xFF161310)
private val DarkSurfaceContainerHigh = Color(0xFF201C17)
private val DarkSurfaceContainerHighest = Color(0xFF24211C)
private val DarkGoldContainer = Color(0xFF49391F)
private val DarkNeutralContainer = Color(0xFF342B1D)
private val DarkErrorContainer = Color(0xFF4A211B)
private val DarkOnErrorContainer = Color(0xFFF7D9D4)

/**
 * The twelve `…Fixed` roles keep one value across both themes by definition, so they are declared
 * once. MindScale uses none of them — they exist here because a role left unset is a role holding
 * Material's purple, which is exactly what `MindScaleColorSchemeTest` caught during Phase 15.
 * `onFixed` and `onFixedVariant` are both full ink: ink at 70% over [LightGold] measures 3.68:1
 * and would fail, while full ink measures 5.87:1.
 */
private val FixedContainer = LightGoldContainer
private val FixedContainerDim = LightGold
private val OnFixed = LightInk

val MindScaleLightColorScheme = lightColorScheme(
    primary = LightGoldText,
    onPrimary = LightBg,
    primaryContainer = LightGoldContainer,
    onPrimaryContainer = LightInk,
    inversePrimary = DarkGold,

    secondary = LightGoldText,
    onSecondary = LightBg,
    secondaryContainer = LightNeutralContainer,
    onSecondaryContainer = LightInk,

    tertiary = LightGoldText,
    onTertiary = LightBg,
    tertiaryContainer = LightGoldContainer,
    onTertiaryContainer = LightInk,

    background = LightBg,
    onBackground = LightInk,

    surface = LightBg,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = LightInk.copy(alpha = 0.70f),
    surfaceTint = Color.Transparent,

    inverseSurface = LightInk,
    inverseOnSurface = LightOnInk,

    error = LightDanger,
    onError = LightBg,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,

    outline = LightInk.copy(alpha = 0.50f),
    outlineVariant = LightInk.copy(alpha = 0.09f),
    scrim = LightInk,

    surfaceBright = LightCard,
    surfaceDim = LightSurfaceContainerHighest,
    surfaceContainerLowest = LightCard,
    surfaceContainerLow = LightBg,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,

    primaryFixed = FixedContainer,
    primaryFixedDim = FixedContainerDim,
    onPrimaryFixed = OnFixed,
    onPrimaryFixedVariant = OnFixed,
    secondaryFixed = FixedContainer,
    secondaryFixedDim = FixedContainerDim,
    onSecondaryFixed = OnFixed,
    onSecondaryFixedVariant = OnFixed,
    tertiaryFixed = FixedContainer,
    tertiaryFixedDim = FixedContainerDim,
    onTertiaryFixed = OnFixed,
    onTertiaryFixedVariant = OnFixed
)

val MindScaleDarkColorScheme = darkColorScheme(
    primary = DarkGoldText,
    onPrimary = DarkBg,
    primaryContainer = DarkGoldContainer,
    onPrimaryContainer = DarkInk,
    // The light theme's gold text is exactly right here: `inversePrimary` is the accent painted
    // on `inverseSurface`, which in dark is the bone `#F4F0E8`. Measured 4.92:1.
    inversePrimary = LightGoldText,

    secondary = DarkGoldText,
    onSecondary = DarkBg,
    secondaryContainer = DarkNeutralContainer,
    onSecondaryContainer = DarkInk,

    tertiary = DarkGoldText,
    onTertiary = DarkBg,
    tertiaryContainer = DarkGoldContainer,
    onTertiaryContainer = DarkInk,

    background = DarkBg,
    onBackground = DarkInk,

    surface = DarkBg,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = DarkInk.copy(alpha = 0.70f),
    surfaceTint = Color.Transparent,

    inverseSurface = DarkInk,
    inverseOnSurface = DarkOnInk,

    error = DarkDanger,
    onError = DarkBg,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,

    outline = DarkInk.copy(alpha = 0.40f),
    outlineVariant = DarkInk.copy(alpha = 0.09f),
    scrim = Color(0xFF000000),

    surfaceBright = DarkSurfaceContainerHighest,
    surfaceDim = DarkBg,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,

    primaryFixed = FixedContainer,
    primaryFixedDim = FixedContainerDim,
    onPrimaryFixed = OnFixed,
    onPrimaryFixedVariant = OnFixed,
    secondaryFixed = FixedContainer,
    secondaryFixedDim = FixedContainerDim,
    onSecondaryFixed = OnFixed,
    onSecondaryFixedVariant = OnFixed,
    tertiaryFixed = FixedContainer,
    tertiaryFixedDim = FixedContainerDim,
    onTertiaryFixed = OnFixed,
    onTertiaryFixedVariant = OnFixed
)
