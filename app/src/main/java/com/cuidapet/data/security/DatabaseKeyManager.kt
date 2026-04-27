package com.cuidadopet.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerencia a chave AES-256 usada pelo SQLCipher para cifrar o banco.
 *
 * Fluxo:
 *  1ª abertura → gera 32 bytes aleatórios via SecureRandom
 *  Salva em EncryptedSharedPreferences (cifrado com chave no Android Keystore)
 *  Aperturas seguintes → lê a chave armazenada
 *
 * A chave nunca trafega fora do dispositivo nem fica em texto claro em disco.
 */
@Singleton
class DatabaseKeyManager @Inject constructor(
    private val context: Context
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "cuidadopet_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getOrCreateKey(): CharArray {
        val stored = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (stored != null) return stored.toCharArray()

        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encoded = Base64.getEncoder().encodeToString(bytes)
        prefs.edit().putString(KEY_DB_PASSPHRASE, encoded).apply()
        return encoded.toCharArray()
    }

    private companion object {
        const val KEY_DB_PASSPHRASE = "db_passphrase"
    }
}
