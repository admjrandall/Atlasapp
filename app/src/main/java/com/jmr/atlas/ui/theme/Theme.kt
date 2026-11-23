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
    // color = state (design 0.7): coral late/blocked, amber waiting, green done; everything else white
    val LateBg = Color(0xFFFFE3DC); val LateInk = Color(0xFF8F2A1B)
    val WaitBg = Color(0xFFFFF1D6); val WaitInk = Color(0xFF6E4700)
    val DoneBg = Color(0xFFDDF2EE); val DoneInk = Color(0xFF1F5C52)
    // 0.7 — bands are neutral paper; only the Tasks "Late" band is faintly coral.
    val BandNeutral = Color(0xFFF3EFF5); val BandLate = Color(0xFFFFF0EC)
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

    // 0.6 — paper, ink and the band tints behind grouped tiles (decorative only; text on them is DeepPlum).
    val Hairline = Color(0xFFEEE7F2)        // decorative only
    val Hint = Color(0xFF8E829A)            // placeholder text and inactive icons (3.4:1, never for content)
    val Chip = Color(0xFFF1E9F6)
    val BandMint = Color(0xFFEAF5F2)
    val BandLilac = Color(0xFFF3ECF8)
    val BandBlue = Color(0xFFEBF1FA)
    val BandPeach = Color(0xFFFCEEEA)
    val bands = listOf(BandMint, BandLilac, BandBlue, BandPeach)

    val all = mapOf(
        "background" to Background, "surface" to Surface, "surfaceSecondary" to SurfaceSecondary, "primary" to Primary,
        "primaryContainer" to PrimaryContainer, "orchid" to Orchid, "peach" to Peach, "coral" to Coral, "coralText" to CoralText,
        "coolMint" to CoolMint, "deepTeal" to DeepTeal, "powderBlue" to PowderBlue, "deepPlum" to DeepPlum,
        "secondaryText" to SecondaryText, "border" to Border, "error" to Error, "blueText" to BlueText,
        "hairline" to Hairline, "hint" to Hint, "chip" to Chip, "bandMint" to BandMint, "bandLilac" to BandLilac, "bandBlue" to BandBlue, "bandPeach" to BandPeach,
    )
}

/** Container + accessible foreground for each object type (objectAccentTokens). Color never carries meaning alone: icon + label always accompany it. */
data class Accent(val container: Color, val content: Color)

/** 0.7: type is an icon, not a color — every type sits on the same quiet chip; people keep their face, spaces their own look. */
fun accent(t: ObjType): Accent = when (t) {
    ObjType.PERSON -> Accent(Atlas.PrimaryContainer, Atlas.Primary)
    ObjType.PROJECT -> Accent(Atlas.Primary, Color.White)
    else -> Accent(Atlas.Chip, Atlas.DeepPlum)
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

private fun flex(width: Float) = FontFamily(
    listOf(300, 400, 500, 600, 700, 800).map { w ->
        Font(R.font.roboto_flex, weight = FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.width(width)))
    }
)

/** Roboto Flex at normal width, and narrowed (wdth 82) for headlines, band names and the briefing sentence. */
val Flex: FontFamily = flex(100f)
val FlexNarrow: FontFamily = flex(82f)

/** 0.6 type roles. Sizes are the Fold inner screen's; `compact` variants are for the cover screen and phones. */
object T {
    private fun t(f: FontFamily, size: Int, w: Int, lh: Int, ls: Double = 0.0) = TextStyle(fontFamily = f, fontSize = size.sp, fontWeight = FontWeight(w), lineHeight = lh.sp, letterSpacing = ls.sp)
    val eyebrow = t(Flex, 12, 700, 16, 1.4)
    val sentence = t(FlexNarrow, 36, 400, 48, -0.4)
    val sentenceCompact = t(FlexNarrow, 28, 400, 38, -0.3)
    val title = t(FlexNarrow, 34, 800, 38, -0.3)
    val titleCompact = t(FlexNarrow, 28, 800, 32, -0.2)
    val heroTitle = t(FlexNarrow, 30, 800, 34, -0.2)
    val cover = t(FlexNarrow, 22, 800, 26)
    val band = t(FlexNarrow, 18, 800, 22)
    val tileTitle = t(Flex, 16, 700, 20)
    val tileQuote = t(FlexNarrow, 18, 600, 22)
    val tileLabel = t(Flex, 11, 700, 14, 1.2)
    val body = t(Flex, 16, 400, 23)
    val row = t(Flex, 17, 600, 22)
    val meta = t(Flex, 13, 400, 18)
    val metaStrong = t(Flex, 13, 650, 18)
    val input = t(Flex, 17, 400, 24)
    val preview = t(Flex, 20, 400, 30)
}

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

/**
 * A space's own color and icon (design 0.7, H). Soft, low-chroma colors so they never read as late (coral),
 * waiting (amber) or done (green). Until you choose, Atlas picks from the space's name.
 */
data class SpaceLook(val color: String, val iconName: String, val bg: androidx.compose.ui.graphics.Color, val ink: androidx.compose.ui.graphics.Color, val icon: ImageVector) {
    companion object {
        val COLORS = linkedMapOf(
            "blue" to (androidx.compose.ui.graphics.Color(0xFFDCE8FA) to androidx.compose.ui.graphics.Color(0xFF2F5C93)),
            "tan" to (androidx.compose.ui.graphics.Color(0xFFF0E2D2) to androidx.compose.ui.graphics.Color(0xFF7A4E25)),
            "slate" to (androidx.compose.ui.graphics.Color(0xFFE6EDF3) to androidx.compose.ui.graphics.Color(0xFF3E5A73)),
            "rose" to (androidx.compose.ui.graphics.Color(0xFFF6E3EC) to androidx.compose.ui.graphics.Color(0xFF8C3A5E)),
            "sage" to (androidx.compose.ui.graphics.Color(0xFFE5F0DC) to androidx.compose.ui.graphics.Color(0xFF46632F)),
            "lilac" to (androidx.compose.ui.graphics.Color(0xFFEFE6FA) to androidx.compose.ui.graphics.Color(0xFF6A3FA0)),
        )
        val ICONS: Map<String, ImageVector> = linkedMapOf(
            "folder" to androidx.compose.material.icons.Icons.Outlined.Folder,
            "home" to androidx.compose.material.icons.Icons.Outlined.Home,
            "health" to androidx.compose.material.icons.Icons.Outlined.MedicalServices,
            "money" to androidx.compose.material.icons.Icons.Outlined.ReceiptLong,
            "travel" to androidx.compose.material.icons.Icons.Outlined.Flight,
            "school" to androidx.compose.material.icons.Icons.Outlined.School,
            "car" to androidx.compose.material.icons.Icons.Outlined.DirectionsCar,
            "work" to androidx.compose.material.icons.Icons.Outlined.Work,
            "garden" to androidx.compose.material.icons.Icons.Outlined.Park,
            "heart" to androidx.compose.material.icons.Icons.Outlined.FavoriteBorder,
            "star" to androidx.compose.material.icons.Icons.Outlined.StarOutline,
            "book" to androidx.compose.material.icons.Icons.Outlined.MenuBook,
        )
        private val GUESS = listOf(
            Regex("surg|health|doctor|medic|hospital|dental|therapy|fitness|gym") to ("blue" to "health"),
            Regex("home|house|renovat|kitchen|bath|hoa|rental|apartment|move") to ("tan" to "home"),
            Regex("tax|budget|financ|insur|bank|bill|retire|money|lease") to ("slate" to "money"),
            Regex("trip|travel|japan|vacation|holiday|flight") to ("rose" to "travel"),
            Regex("school|kid|class|study|course") to ("sage" to "school"),
            Regex("car|truck|vehicle") to ("lilac" to "car"),
            Regex("garden|yard|plant") to ("sage" to "garden"),
            Regex("work|job|career|client|side project") to ("slate" to "work"),
            Regex("wedding|family|mom|dad|friend|care") to ("rose" to "heart"),
            Regex("book|read|club") to ("lilac" to "book"),
        )
        fun make(color: String, icon: String): SpaceLook {
            val (bg, ink) = COLORS[color] ?: COLORS.values.first()
            return SpaceLook(color, icon, bg, ink, ICONS[icon] ?: ICONS.values.first())
        }
        fun guess(title: String): SpaceLook {
            val t = title.lowercase()
            GUESS.firstOrNull { it.first.containsMatchIn(t) }?.second?.let { (c, i) -> return make(c, i) }
            return make(COLORS.keys.elementAt(Math.floorMod(title.hashCode(), COLORS.size)), "folder")
        }
        /** The look you chose (a LOOK detail), else Atlas's guess from the name. */
        fun of(s: com.jmr.atlas.model.Snapshot?, o: com.jmr.atlas.model.KObject): SpaceLook {
            val v = s?.detailsOf(o.id, com.jmr.atlas.model.DetailKind.LOOK)?.firstOrNull()?.value
            if (v != null && ':' in v) return make(v.substringBefore(':'), v.substringAfter(':'))
            return guess(o.title)
        }
    }
}
