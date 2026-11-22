package com.jmr.atlas.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Attachment vault: AES-256-GCM, key held in the Android Keystore (non-exportable).
 * File layout: [12-byte IV][ciphertext+tag]. Originals are never altered after capture.
 */
class SecureFiles(private val context: Context) {
    private val dir = File(context.filesDir, "vault").apply { mkdirs() }
    private val openDir = File(context.cacheDir, "open").apply { mkdirs() }

    /** True on real devices. Robolectric has no AndroidKeyStore; there we use an app-private test key and say so. */
    var keystoreBacked = true
        private set

    private val key: SecretKey by lazy {
        runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (ks.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
            }.generateKey()
        }.getOrElse {
            keystoreBacked = false
            val p = context.getSharedPreferences("vault_test_key", Context.MODE_PRIVATE)
            val raw = p.getString("k", null)?.let { android.util.Base64.decode(it, 0) } ?: ByteArray(32).also { SecureRandom().nextBytes(it); p.edit().putString("k", android.util.Base64.encodeToString(it, 0)).apply() }
            SecretKeySpec(raw, "AES")
        }
    }

    fun store(bytes: ByteArray): String {
        val name = "${System.nanoTime()}-${SecureRandom().nextInt(1_000_000)}.bin"
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        File(dir, name).outputStream().use { it.write(c.iv); it.write(c.doFinal(bytes)) }
        return name
    }

    fun read(name: String): ByteArray? = runCatching {
        val all = File(dir, name).readBytes()
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, all, 0, 12)) }
        c.doFinal(all, 12, all.size - 12)
    }.getOrNull()

    fun delete(name: String) { if (name.isNotBlank()) File(dir, name).delete() }

    /** Short-lived plaintext copy for viewing in another app or the PDF renderer. Cleaned on launch. */
    fun openCopy(name: String, ext: String): File? = read(name)?.let { b -> File(openDir, "atlas-${System.currentTimeMillis()}.$ext").apply { writeBytes(b) } }

    fun cleanOpenCopies() { openDir.listFiles()?.forEach { it.delete() } }

    companion object { private const val ALIAS = "atlas_vault_v1" }
}
