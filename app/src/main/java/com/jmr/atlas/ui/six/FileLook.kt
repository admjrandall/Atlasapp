package com.jmr.atlas.ui.six

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmr.atlas.model.KObject
import com.jmr.atlas.model.ObjType
import com.jmr.atlas.ui.components.LocalFiles
import com.jmr.atlas.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * What a file is, the way a file manager shows it (0.7.1): a page with a folded corner in the format's own color and
 * its short name — red PDF, blue DOC, green XLS — so a document reads as a document at a glance.
 */
object FileKind {
    data class K(val label: String, val color: Color, val glyph: ImageVector?, val ext: String)

    private val PDF = Color(0xFFD93025); private val WORD = Color(0xFF2B5797); private val EXCEL = Color(0xFF1E7145)
    private val SLIDES = Color(0xFFC6511F); private val IMAGE = Color(0xFF1B7F86); private val VIDEO = Color(0xFF6B3FA0)
    private val AUDIO = Color(0xFFB3306B); private val ZIP = Color(0xFF8A6418); private val TEXT = Color(0xFF5B5566); private val WEB = Color(0xFF2F6FB0)

    fun ext(mime: String, title: String = ""): String =
        title.substringAfterLast('.', "").lowercase().takeIf { it.length in 2..5 && it.all(Char::isLetterOrDigit) }
            ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.lowercase()
            ?: when { mime == "application/pdf" -> "pdf"; mime.startsWith("image/") -> mime.substringAfter('/').substringBefore('+'); mime.startsWith("text/") -> "txt"; else -> "" }

    fun of(mime: String, title: String = ""): K {
        val e = ext(mime, title)
        return when {
            e == "pdf" || mime == "application/pdf" -> K("PDF", PDF, null, "pdf")
            e in setOf("doc", "docx", "odt", "rtf") || mime.contains("wordprocessing") || mime == "application/msword" -> K("DOC", WORD, null, e.ifBlank { "docx" })
            e in setOf("xls", "xlsx", "ods", "csv") || mime.contains("spreadsheet") || mime.contains("excel") -> K(if (e == "csv") "CSV" else "XLS", EXCEL, null, e.ifBlank { "xlsx" })
            e in setOf("ppt", "pptx", "odp", "key") || mime.contains("presentation") || mime.contains("powerpoint") -> K("PPT", SLIDES, null, e.ifBlank { "pptx" })
            mime.startsWith("image/") -> K(e.uppercase().take(4).ifBlank { "IMG" }, IMAGE, Icons.Outlined.Image, e.ifBlank { "jpg" })
            mime.startsWith("video/") -> K("VID", VIDEO, Icons.Outlined.PlayArrow, e.ifBlank { "mp4" })
            mime.startsWith("audio/") -> K("AUD", AUDIO, Icons.Outlined.MusicNote, e.ifBlank { "m4a" })
            e in setOf("zip", "rar", "7z", "gz", "tar") || mime.contains("zip") || mime.contains("compressed") -> K("ZIP", ZIP, null, e.ifBlank { "zip" })
            e in setOf("html", "htm") -> K("HTML", WEB, null, e)
            mime.startsWith("text/") || e in setOf("txt", "md", "json", "xml") -> K(e.uppercase().take(4).ifBlank { "TXT" }, TEXT, null, e.ifBlank { "txt" })
            else -> K(e.uppercase().take(4).ifBlank { "FILE" }, TEXT, null, e.ifBlank { "bin" })
        }
    }

    fun of(o: KObject): K = if (o.type == ObjType.LINK) K("WEB", WEB, Icons.Outlined.Language, "") else of(o.mime, o.title)

    /** "PDF · 545 KB" */
    fun facts(o: KObject): String = listOfNotNull(of(o).label.takeIf { o.type != ObjType.LINK }, size(o.sizeBytes).takeIf { o.sizeBytes > 0 }).joinToString(" · ")

    fun size(b: Long): String = when { b >= 1_000_000_000 -> "%.1f GB".format(b / 1e9); b >= 1_000_000 -> "%.1f MB".format(b / 1e6); b >= 1_000 -> "${b / 1000} KB"; else -> "$b B" }

    /** Title without a trailing ".pdf" when the badge already says so. */
    fun name(o: KObject): String = o.title.let { t -> val e = t.substringAfterLast('.', ""); if (e.isNotEmpty() && e.lowercase() == of(o).ext) t.removeSuffix(".$e") else t }
}

/** A page with a folded corner, filled in the format's color, with its short name across the bottom. */
@Composable
fun FileBadge(k: FileKind.K, size: Dp, modifier: Modifier = Modifier) = Box(modifier.size(width = size * 0.82f, height = size), contentAlignment = Alignment.BottomCenter) {
    Canvas(Modifier.fillMaxSize()) {
        val w = this.size.width; val h = this.size.height; val f = w * 0.32f; val r = w * 0.12f
        val page = Path().apply {
            moveTo(r, 0f); lineTo(w - f, 0f); lineTo(w, f); lineTo(w, h - r)
            quadraticTo(w, h, w - r, h); lineTo(r, h); quadraticTo(0f, h, 0f, h - r); lineTo(0f, r); quadraticTo(0f, 0f, r, 0f); close()
        }
        drawPath(page, k.color)
        val fold = Path().apply { moveTo(w - f, 0f); lineTo(w - f, f - r * 0.4f); quadraticTo(w - f, f, w - f + r * 0.4f, f); lineTo(w, f); close() }
        drawPath(fold, Color.White.copy(alpha = 0.45f))
    }
    if (k.glyph != null) Icon(k.glyph, null, Modifier.align(Alignment.Center).padding(top = size * 0.06f).size(size * 0.38f), tint = Color.White)
    else Text(k.label, style = T.metaStrong.copy(fontSize = (size.value * 0.24f).coerceAtLeast(8f).sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.2.sp, lineHeight = (size.value * 0.26f).sp),
        color = Color.White, maxLines = 1, modifier = Modifier.padding(bottom = size * 0.14f))
}

@Composable
fun FileBadge(o: KObject, size: Dp, modifier: Modifier = Modifier) = FileBadge(FileKind.of(o), size, modifier)

/**
 * First page of a PDF in the vault, rendered on the phone. The decrypted copy lives in the app's private cache only
 * while PdfRenderer reads it and is deleted straight after; the bitmap stays in memory.
 */
@Composable
fun rememberPdfPage(o: KObject?, px: Int = 720): ImageBitmap? {
    val files = LocalFiles.current
    return produceState<ImageBitmap?>(null, o?.vaultFile) {
        value = if (o == null || files == null || o.vaultFile.isBlank() || FileKind.of(o).ext != "pdf") null else withContext(Dispatchers.IO) {
            var tmp: File? = null
            runCatching {
                tmp = files.openCopy(o.vaultFile, "pdf") ?: return@runCatching null
                ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { r ->
                        if (r.pageCount == 0) return@use null
                        r.openPage(0).use { p ->
                            val scale = px.toFloat() / maxOf(p.width, p.height)
                            val bmp = Bitmap.createBitmap((p.width * scale).toInt().coerceAtLeast(1), (p.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp.asImageBitmap()
                        }
                    }
                }
            }.getOrNull().also { tmp?.delete() }
        }
    }.value
}
