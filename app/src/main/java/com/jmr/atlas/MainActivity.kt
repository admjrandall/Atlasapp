package com.jmr.atlas

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.FileProvider
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.jmr.atlas.model.KObject
import com.jmr.atlas.ui.AtlasRoot
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Route
import com.jmr.atlas.ui.theme.AtlasTheme

/** System actions the UI can trigger. Every one of them is real; nothing here is a placeholder. */
interface Platform {
    fun pickImage()
    fun pickFile()
    fun scan()
    fun export(json: String)
    fun openFile(o: KObject)
    /** System speech recognizer, asked to prefer on-device recognition. Atlas itself never handles audio. */
    fun dictate(onText: (String) -> Unit)
}

val LocalPlatform = staticCompositionLocalOf<Platform> { error("no platform") }

class MainActivity : ComponentActivity(), Platform {
    private val vm: AtlasViewModel by viewModels()
    private var pendingExport: String? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.captureUri(it) } }
    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.captureUri(it) } }
    private val scanner = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        GmsDocumentScanningResult.fromActivityResultIntent(r.data)?.pdf?.uri?.let { vm.captureUri(it, "application/pdf") }
    }
    private val exporter = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = pendingExport; pendingExport = null
        if (uri == null || json == null) return@registerForActivityResult
        val ok = runCatching { contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } != null }.getOrDefault(false)
        vm.say(if (ok) "Exported." else "Export failed.")
    }

    private var dictation: ((String) -> Unit)? = null
    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { dictation?.invoke(it) }
        dictation = null
    }
    override fun dictate(onText: (String) -> Unit) {
        dictation = onText
        val i = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        runCatching { speech.launch(i) }.onFailure { dictation = null; vm.say("No speech recognizer is available on this phone.") }
    }

    override fun pickImage() = imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    override fun pickFile() = filePicker.launch(arrayOf("application/pdf", "image/*", "text/plain"))
    override fun scan() {
        val opts = GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(20)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF).setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
        GmsDocumentScanning.getClient(opts).getStartScanIntent(this)
            .addOnSuccessListener { scanner.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener { vm.say("The document scanner isn't available on this phone.") }
    }
    override fun export(json: String) { pendingExport = json; exporter.launch("atlas-export-${java.time.LocalDate.now()}.json") }
    override fun openFile(o: KObject) {
        val ext = when { o.mime == "application/pdf" -> "pdf"; o.mime.startsWith("image/") -> o.mime.substringAfter('/').substringBefore('+'); o.mime == "text/plain" -> "txt"; else -> "bin" }
        val f = vm.repo.files.openCopy(o.vaultFile, ext) ?: run { vm.say("The original file isn't available."); return }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", f)
        runCatching { startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(uri, o.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), o.title)) }
            .onFailure { vm.say("No app on this phone can open ${o.mime}.") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleShare(intent)
        setContent { AtlasTheme { CompositionLocalProvider(LocalPlatform provides this) { AtlasRoot(vm) } } }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleShare(intent) }

    /** Shared content is untrusted data. It is saved as an original first, then processed on the phone. */
    private fun handleShare(i: Intent?) {
        if (i?.action != Intent.ACTION_SEND) return
        val text = i.getStringExtra(Intent.EXTRA_TEXT)
        val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else @Suppress("DEPRECATION") i.getParcelableExtra(Intent.EXTRA_STREAM)
        when {
            uri != null -> vm.captureUri(uri, i.type, text.orEmpty())
            !text.isNullOrBlank() -> vm.captureText(text)
            else -> return
        }
        vm.go(Route.Capture)
    }
}
