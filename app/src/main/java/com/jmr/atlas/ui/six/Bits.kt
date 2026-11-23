package com.jmr.atlas.ui.six

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.model.KObject
import com.jmr.atlas.model.ObjType
import com.jmr.atlas.ui.cards.rememberPhoto
import com.jmr.atlas.ui.theme.Atlas
import com.jmr.atlas.ui.theme.T

/** True on the Fold's inner screen and tablets (≥ 600 dp wide). Layouts read this instead of measuring again. */
val LocalWide = compositionLocalOf { false }

/** Side padding for page content: 32 dp unfolded, 20 dp on the cover screen. */
val pad: Dp @Composable get() = if (LocalWide.current) 32.dp else 20.dp

/** Section label: small caps, letter-spaced, secondary ink. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Atlas.SecondaryText) =
    Text(text.uppercase(), style = T.eyebrow, color = color, modifier = modifier.semantics { heading() })

/** Container + ink per person, stable from the id, so the same person always has the same face color. */
fun faceColors(o: KObject): Pair<Color, Color> = listOf(
    Atlas.PrimaryContainer to Atlas.Primary, Atlas.CoolMint to Atlas.DeepTeal, Atlas.PowderBlue to Atlas.BlueText, Atlas.Peach to Atlas.CoralText,
)[(o.id.hashCode() and 0x7fffffff) % 4]

@Composable
fun Face(o: KObject, size: Dp, modifier: Modifier = Modifier, ring: Color? = null) {
    val photo = if (o.type == ObjType.PERSON) rememberPhoto(o, 256) else null
    val (bg, ink) = faceColors(o)
    Box(modifier.size(size).then(if (ring != null) Modifier.border(3.dp, ring, CircleShape) else Modifier).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        if (photo != null) Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(o.initials, color = ink, style = T.tileTitle.copy(fontSize = (size.value * 0.34f).sp, lineHeight = (size.value * 0.4f).sp))
    }
}

@Composable
fun RoundIcon(icon: ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier, container: Color = Atlas.Chip, tint: Color = Atlas.DeepPlum, size: Dp = 44.dp) =
    Box(modifier.size(size).clip(CircleShape).background(container).clickable(role = Role.Button, onClickLabel = desc, onClick = onClick).semantics { contentDescription = desc },
        contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }

/** "‹ Home Renovation" — where Back goes, said in words. */
@Composable
fun BackPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    Row(modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).background(Atlas.Chip).clickable(role = Role.Button, onClickLabel = "Back to $label", onClick = onClick)
        .padding(start = 8.dp, end = 16.dp).testTag("back"), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, Modifier.size(20.dp), tint = Atlas.DeepPlum); Spacer(Modifier.width(6.dp))
        Text(label, style = T.metaStrong.copy(fontSize = 14.sp), color = Atlas.DeepPlum, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }

/** A tappable chip used in the input box preview (due date, space, person). Crossed out when you tap it off. */
@Composable
fun PartChip(text: String, container: Color, ink: Color, off: Boolean, desc: String, onClick: () -> Unit) =
    Box(Modifier.heightIn(min = 38.dp).clip(RoundedCornerShape(12.dp)).background(if (off) Color.Transparent else container)
        .then(if (off) Modifier.border(1.5.dp, Atlas.Hint, RoundedCornerShape(12.dp)) else Modifier)
        .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.row.copy(fontSize = 17.sp, textDecoration = if (off) androidx.compose.ui.text.style.TextDecoration.LineThrough else null), color = if (off) Atlas.SecondaryText else ink)
    }

/** The result line at the top of the screen, with Undo when the action can be undone. */
@Composable
fun Toast(text: String, onUndo: (() -> Unit)?, modifier: Modifier = Modifier) =
    Row(modifier.shadow(12.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Atlas.DeepTeal).padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp).testTag("toast"),
        verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = T.metaStrong.copy(fontSize = 14.sp), color = Color.White, modifier = Modifier.padding(vertical = 8.dp).widthIn(max = 420.dp), maxLines = 2)
        if (onUndo != null) {
            Spacer(Modifier.width(12.dp))
            Box(Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.18f)).clickable(role = Role.Button, onClick = onUndo).padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center) { Text("Undo", style = T.metaStrong.copy(fontSize = 14.sp), color = Color.White) }
        } else Spacer(Modifier.width(12.dp))
    }

/** The ✦ mark that says "Atlas did this", always with words next to it. */
@Composable
fun AtlasDid(text: String, modifier: Modifier = Modifier, color: Color = Atlas.SecondaryText) =
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Sparkle, null, Modifier.size(14.dp), tint = Atlas.Primary); Spacer(Modifier.width(6.dp))
        Text(text, style = T.meta, color = color)
    }

val Sparkle: ImageVector get() = Icons.Outlined.AutoAwesome
