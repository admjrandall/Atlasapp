package com.jmr.atlas.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.R
import com.jmr.atlas.model.ObjType

/** Morph light palette (references/design-tokens.json v1.0). No yellow family anywhere — enforced by TokenTest. */
object Atlas {
    val Background = Color(0xFFFBF7FC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSecondary = Color(0xFFF4EDF8)
    val Primary = Color(0xFF7443B1)
    val PrimaryContainer = Color(0xFFE7D8FC)
    val Orchid = Color(0xFFC3A6F4)          // decorative only
    val Peach = Color(0xFFFFDDD3)
    val Coral = Color(0xFFC75D58)           // large text / UI parts only
    val CoralText = Color(0xFFA8443F)       // coral for normal text (5.56:1)
    val CoolMint = Color(0xFFD5EFEB)
    val DeepTeal = Color(0xFF286D65)
    val PowderBlue = Color(0xFFDCE8FA)
    val DeepPlum = Color(0xFF352044)
    val SecondaryText = Color(0xFF61566B)
    val Border = Color(0xFFDDD4E5)          // decorative only
    val Error = Color(0xFFB3263E)
    val BlueText = Color(0xFF2F5C93)        // accessible foreground on powder blue (derived, 6.0:1)

    val all = mapOf(
        "background" to Background, "surface" to Surface, "surfaceSecondary" to SurfaceSecondary, "primary" to Primary,
        "primaryContainer" to PrimaryContainer, "orchid" to Orchid, "peach" to Peach, "coral" to Coral, "coralText" to CoralText,
        "coolMint" to CoolMint, "deepTeal" to DeepTeal, "powderBlue" to PowderBlue, "deepPlum" to DeepPlum,
        "secondaryText" to SecondaryText, "border" to Border, "error" to Error, "blueText" to BlueText,
    )
}

/** Container + accessible foreground for each object type (objectAccentTokens). Color never carries meaning alone: icon + label always accompany it. */
data class Accent(val container: Color, val content: Color)

fun accent(t: ObjType): Accent = when (t) {
    ObjType.PERSON -> Accent(Atlas.PrimaryContainer, Atlas.Primary)
    ObjType.PROJECT -> Accent(Atlas.Primary, Color.White)
    ObjType.DOCUMENT, ObjType.LINK -> Accent(Atlas.PowderBlue, Atlas.BlueText)
    ObjType.TASK -> Accent(Atlas.CoolMint, Atlas.DeepTeal)
    ObjType.IDEA, ObjType.IMAGE -> Accent(Atlas.Peach, Atlas.CoralText)
    ObjType.NOTE -> Accent(Atlas.SurfaceSecondary, Atlas.Primary)
    ObjType.COLLECTION -> Accent(Atlas.PrimaryContainer, Atlas.DeepPlum)
    ObjType.EVENT -> Accent(Atlas.CoolMint, Atlas.DeepTeal)
    ObjType.DECISION -> Accent(Atlas.Peach, Atlas.DeepPlum)
}

fun icon(t: ObjType): ImageVector = when (t) {
    ObjType.PERSON -> Icons.Outlined.Person
    ObjType.PROJECT -> Icons.Outlined.FolderOpen
    ObjType.TASK -> Icons.Outlined.CheckCircle
    ObjType.IDEA -> Icons.Outlined.Lightbulb
    ObjType.NOTE -> Icons.Outlined.StickyNote2
    ObjType.DOCUMENT -> Icons.Outlined.Description
    ObjType.IMAGE -> Icons.Outlined.Image
    ObjType.LINK -> Icons.Outlined.Link
    ObjType.COLLECTION -> Icons.Outlined.Layers
    ObjType.EVENT -> Icons.Outlined.Flag
    ObjType.DECISION -> Icons.Outlined.Gavel
}

val Flex: FontFamily = FontFamily(
    listOf(300, 400, 500, 600, 700, 800).map { w ->
        Font(R.font.roboto_flex, weight = FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

private fun s(size: Int, w: Int, lh: Int, ls: Double = 0.0) = TextStyle(fontFamily = Flex, fontSize = size.sp, fontWeight = FontWeight(w), lineHeight = lh.sp, letterSpacing = ls.sp)

val AtlasType = Typography(
    displayLarge = s(40, 600, 46, -0.6), displayMedium = s(36, 600, 42, -0.5), displaySmall = s(34, 600, 40, -0.4),
    headlineLarge = s(32, 650, 38, -0.4), headlineMedium = s(28, 650, 34, -0.3), headlineSmall = s(24, 600, 30, -0.2),
    titleLarge = s(20, 600, 26, -0.1), titleMedium = s(17, 600, 23), titleSmall = s(16, 600, 22),
    bodyLarge = s(16, 400, 24), bodyMedium = s(14, 400, 20), bodySmall = s(13, 400, 18),
    labelLarge = s(15, 600, 20), labelMedium = s(13, 600, 18), labelSmall = s(12, 600, 16, 0.2),
)

val AtlasShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
)

private val Scheme = lightColorScheme(
    primary = Atlas.Primary, onPrimary = Color.White, primaryContainer = Atlas.PrimaryContainer, onPrimaryContainer = Atlas.DeepPlum,
    secondary = Atlas.DeepTeal, onSecondary = Color.White, secondaryContainer = Atlas.CoolMint, onSecondaryContainer = Atlas.DeepTeal,
    tertiary = Atlas.CoralText, onTertiary = Color.White, tertiaryContainer = Atlas.Peach, onTertiaryContainer = Atlas.DeepPlum,
    background = Atlas.Background, onBackground = Atlas.DeepPlum, surface = Atlas.Surface, onSurface = Atlas.DeepPlum,
    surfaceVariant = Atlas.SurfaceSecondary, onSurfaceVariant = Atlas.SecondaryText, surfaceTint = Color.Transparent,
    surfaceContainerLowest = Atlas.Surface, surfaceContainerLow = Color(0xFFFDFAFE), surfaceContainer = Atlas.SurfaceSecondary,
    surfaceContainerHigh = Color(0xFFEFE6F5), surfaceContainerHighest = Color(0xFFE9DFF0),
    outline = Color(0xFF8E829A), outlineVariant = Atlas.Border, error = Atlas.Error, onError = Color.White,
    errorContainer = Color(0xFFFCE4E7), onErrorContainer = Color(0xFF6E1324), inverseSurface = Atlas.DeepPlum, inverseOnSurface = Atlas.Background,
    inversePrimary = Atlas.Orchid, scrim = Color(0x66352044),
)

/** Light only for this release; wallpaper dynamic color is intentionally not used (design-tokens.json allowWallpaperDynamicColors=false). */
@Composable
fun AtlasTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, typography = AtlasType, shapes = AtlasShapes, content = content)
