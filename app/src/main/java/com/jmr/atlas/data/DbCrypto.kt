package com.jmr.atlas.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Database encryption (SQLCipher, AES-256). The database passphrase is 32 random bytes, generated on first run and
 * stored only wrapped by a non-exportable Android Keystore key, in no-backup storage. Copying the app's files to
 * another device yields nothing readable.
 *
 * A 0.4 database (plain SQLite) is converted once with sqlcipher_export, then the plain file is deleted.
 */
object DbCrypto {
    private const val ALIAS = "atlas_db_v1"

    fun factory(context: Context, dbName: String): SupportSQLiteOpenHelper.Factory {
        System.loadLibrary("sqlcipher")
        val pass = passphrase(context)
        migratePlain(context, dbName, pass)
        return net.zetetic.database.sqlcipher.SupportOpenHelperFactory(pass.toByteArray())
    }

    private fun passphrase(context: Context): String {
        val f = File(context.noBackupFilesDir, "db.key")
        val key = keystoreKey()
        if (f.exists()) {
            val all = f.readBytes()
            val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, all, 0, 12)) }
            return String(c.doFinal(all, 12, all.size - 12))
        }
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val hex = raw.joinToString("") { "%02x".format(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        f.outputStream().use { it.write(c.iv); it.write(c.doFinal(hex.toByteArray())) }
        return hex
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (ks.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        }.generateKey()
    }

    private fun isPlain(f: File) = f.exists() && f.length() >= 16 && f.inputStream().use { s -> ByteArray(16).also { s.read(it) } }.decodeToString(0, 15) == "SQLite format 3"

    private fun migratePlain(context: Context, dbName: String, pass: String) {
        val plain = context.getDatabasePath(dbName)
        if (!isPlain(plain)) return
        val enc = File(plain.parentFile, "$dbName.enc")
        enc.delete()
        val db = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(plain.path, "", null, net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE, null, null)
        try {
            val version = db.version
            db.rawExecSQL("ATTACH DATABASE '${enc.path}' AS encrypted KEY '$pass'")
            db.rawExecSQL("SELECT sqlcipher_export('encrypted')")
            db.rawExecSQL("PRAGMA encrypted.user_version = $version")
            db.rawExecSQL("DETACH DATABASE encrypted")
        } finally { db.close() }
        listOf("", "-wal", "-shm", "-journal").forEach { File(plain.path + it).delete() }
        check(enc.renameTo(plain)) { "could not replace the database with its encrypted copy" }
    }
}
