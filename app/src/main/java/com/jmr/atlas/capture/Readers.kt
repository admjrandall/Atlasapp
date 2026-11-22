package com.jmr.atlas.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

/** Reads shared/picked content and pulls text out of it entirely on this phone (ML Kit bundled model, PdfRenderer). */
object Readers {
    const val MAX_BYTES = 40L * 1024 * 1024

    class Read(val bytes: ByteArray, val mime: String, val name: String)

    /** Copies the original bytes. Content is untrusted data: it is stored and indexed, never executed or treated as instructions. */
    fun read(ctx: Context, uri: Uri, mimeHint: String? = null): Read? = runCatching {
        val cr = ctx.contentResolver
        val mime = cr.getType(uri) ?: mimeHint ?: "application/octet-stream"
        var name = "Capture"
        var size = -1L
        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) { c.getString(0)?.let { name = it }; if (!c.isNull(1)) size = c.getLong(1) }
        }
        if (size > MAX_BYTES) return null
        val bytes = cr.openInputStream(uri)?.use { s ->
            val out = ByteArrayOutputStream(); val buf = ByteArray(16 * 1024)
            while (true) { val n = s.read(buf); if (n < 0) break; out.write(buf, 0, n); if (out.size() > MAX_BYTES) return null }
            out.toByteArray()
        } ?: return null
        if (name == "Capture") name = if (mime.startsWith("image/")) "Photo ${java.time.LocalDate.now()}" else "Document ${java.time.LocalDate.now()}"
        Read(bytes, mime, name)
    }.getOrNull()

    suspend fun text(ctx: Context, r: Read): String = when {
        r.mime.startsWith("image/") -> BitmapFactory.decodeByteArray(r.bytes, 0, r.bytes.size)?.let { ocr(scaled(it)) } ?: ""
        r.mime == "application/pdf" -> pdf(ctx, r.bytes)
        r.mime.startsWith("text/") -> String(r.bytes, Charsets.UTF_8).take(200_000)
        else -> ""
    }

    private fun scaled(b: Bitmap): Bitmap {
        val max = 2400; val m = maxOf(b.width, b.height)
        return if (m <= max) b else Bitmap.createScaledBitmap(b, b.width * max / m, b.height * max / m, true)
    }

    suspend fun ocr(bitmap: Bitmap): String = runCatching {
        suspendCancellableCoroutine { cont ->
            val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            rec.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it.text); rec.close() }
                .addOnFailureListener { cont.resume(""); rec.close() }
        }
    }.getOrDefault("")

    /** Renders up to [maxPages] pages and OCRs them; works for scanned and digital PDFs. Temp file is deleted afterwards. */
    suspend fun pdf(ctx: Context, bytes: ByteArray, maxPages: Int = 6): String {
        val tmp = File.createTempFile("ocr", ".pdf", ctx.cacheDir)
        return try {
            tmp.writeBytes(bytes)
            PdfRenderer(ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY)).use { r ->
                buildString {
                    for (i in 0 until minOf(maxPages, r.pageCount)) r.openPage(i).use { p ->
                        val bmp = Bitmap.createBitmap(p.width * 2, p.height * 2, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
                        p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        append(ocr(bmp)).append("\n\n"); bmp.recycle()
                    }
                }.trim()
            }
        } catch (e: Exception) { "" } finally { tmp.delete() }
    }
}
