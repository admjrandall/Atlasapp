package com.jmr.atlas

import android.content.ClipboardManager
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.SystemClock
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
import com.jmr.atlas.integrations.Web
import com.jmr.atlas.model.KObject
import com.jmr.atlas.ui.AtlasRoot
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.Overlay
import com.jmr.atlas.ui.theme.AtlasTheme
import com.jmr.atlas.widget.CaptureWidget
import kotlinx.coroutines.flow.MutableStateFlow

/** System actions the UI can trigger. Every one of them is real; nothing here is a placeholder. */
interface Platform {
    fun pickImage()
    fun pickFile()
    fun scan()
    fun export(json: String)
    fun openFile(o: KObject)
    /** System speech recognizer, asked to prefer on-device recognition. Atlas itself never handles audio. */
    fun dictate(onText: (String) -> Unit)
    fun pickPhoto(onUri: (Uri) -> Unit)
    fun request(perms: List<String>, onResult: (Boolean) -> Unit)
    fun launch(i: Intent, failMsg: String = "No app on this phone can do that.")
    fun copy(label: String, text: String)
    fun clipboardUrl(): String?
    fun canAuthenticate(): Boolean
    fun authenticate(onOk: () -> Unit)
}

val LocalPlatform = staticCompositionLocalOf<Platform> { error("no platform") }

class MainActivity : ComponentActivity(), Platform {
    private val vm: AtlasViewModel by viewModels()
    private var pendingExport: String? = null
    /** True while the lock screen is shown. */
    val locked = MutableStateFlow(false)
    private var backgroundedAt = 0L

    private val imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.captureUri(it) } }
    private var photoCb: ((Uri) -> Unit)? = null
    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { photoCb?.invoke(it) }; photoCb = null }
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
    private var permCb: ((Boolean) -> Unit)? = null
    private val perms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r -> permCb?.invoke(r.values.all { it }); permCb = null; vm.sync.start() }

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
    override fun pickPhoto(onUri: (Uri) -> Unit) { photoCb = onUri; photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
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
        launch(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(uri, o.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), o.title), "No app on this phone can open ${o.mime}.")
    }
    override fun request(perms: List<String>, onResult: (Boolean) -> Unit) {
        val need = perms.filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (need.isEmpty()) { onResult(true); return }
        permCb = onResult; this.perms.launch(need.toTypedArray())
    }
    override fun launch(i: Intent, failMsg: String) { runCatching { startActivity(i) }.onFailure { vm.say(failMsg) } }
    override fun copy(label: String, text: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(android.content.ClipData.newPlainText(label, text))
        if (Build.VERSION.SDK_INT < 33) vm.say("Copied.")
    }
    override fun clipboardUrl(): String? = runCatching {
        getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.let { Web.firstUrl(it) }
    }.getOrNull()

    override fun canAuthenticate(): Boolean = Build.VERSION.SDK_INT >= 30 &&
        getSystemService(BiometricManager::class.java).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    override fun authenticate(onOk: () -> Unit) {
        if (Build.VERSION.SDK_INT < 30) { onOk(); return }
        val prompt = BiometricPrompt.Builder(this).setTitle("Unlock Atlas")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL).build()
        prompt.authenticate(CancellationSignal(), mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        locked.value = vm.settings.lockOn
        applyRecents()
        if (savedInstanceState == null) handle(intent)
        setContent { AtlasTheme { CompositionLocalProvider(LocalPlatform provides this) { AtlasRoot(vm, locked) } } }
        if (vm.onboarded.value) vm.sync.start()
    }

    /** With the lock on, the recents thumbnail is blank so the app's content isn't visible in the task switcher. */
    fun applyRecents() { if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!vm.settings.lockOn) }

    override fun onStop() { super.onStop(); backgroundedAt = SystemClock.elapsedRealtime() }
    override fun onStart() {
        super.onStart()
        val s = vm.settings
        if (s.lockOn && backgroundedAt > 0 && SystemClock.elapsedRealtime() - backgroundedAt >= s.lockAfterMinutes * 60_000L) locked.value = true
        if (vm.onboarded.value) vm.sync.refreshCalendars()
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handle(intent) }

    private fun handle(i: Intent?) {
        when (i?.action) {
            Intent.ACTION_SEND -> handleShare(i)
            ACTION_QUICK -> when (i.getStringExtra(EXTRA_MODE)) {
                CaptureWidget.MODE_VOICE -> dictate { vm.home(); vm.openCatch(it) }
                CaptureWidget.MODE_SCAN -> scan()
                CaptureWidget.MODE_LINK -> window.decorView.post {   // clipboard is readable only once the window has focus
                    window.decorView.postDelayed({
                        val url = clipboardUrl()
                        if (url != null) vm.captureLink(url) else { vm.openCatch(); vm.say("No link on the clipboard.") }
                    }, 300)
                }
                else -> { vm.home(); vm.openCatch() }
            }
        }
        i?.getStringExtra(EXTRA_OPEN)?.let { vm.open(it) }
    }

    /** Shared content is untrusted data. It is saved as an original first, then processed on the phone. */
    private fun handleShare(i: Intent) {
        val text = i.getStringExtra(Intent.EXTRA_TEXT)
        val subject = i.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
        val from = referrer?.host?.let { appName(it) }.orEmpty()
        val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else @Suppress("DEPRECATION") i.getParcelableExtra(Intent.EXTRA_STREAM)
        when {
            uri != null -> vm.captureUri(uri, i.type, listOf(subject, text.orEmpty()).filter { it.isNotBlank() }.joinToString("\n"), from)
            !text.isNullOrBlank() -> {
                val url = Web.soleUrl(text)
                if (url != null) vm.captureLink(url, subject.takeIf { it.isNotBlank() && !text.contains(it) }.orEmpty(), from)
                else vm.captureText(if (subject.isNotBlank() && !text.startsWith(subject)) "$subject\n\n$text" else text, from)
            }
            else -> return
        }
    }

    private fun appName(pkg: String) = when (pkg) {
        "com.google.android.gm" -> "Gmail"; "com.android.chrome" -> "Chrome"; "com.sec.android.app.sbrowser" -> "Samsung Internet"
        else -> runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrDefault("")
    }

    companion object {
        const val ACTION_QUICK = "com.jmr.atlas.QUICK_CAPTURE"
        const val EXTRA_MODE = "mode"
        const val EXTRA_OPEN = "open"
    }
}
