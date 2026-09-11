package com.example.respotapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.TimeUnit

class TokenManager(context: Context) {

    companion object {
        const val REDIRECT_URI = "respot://auth/callback"
        const val SCOPES = "user-read-private user-read-email user-library-read user-library-modify playlist-read-private playlist-modify-private playlist-modify-public streaming"
        private const val PREFS_FILE = "respot_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_CODE_VERIFIER = "code_verifier"
        private const val KEY_CLIENT_ID = "spotify_client_id"
        private const val KEY_GRANTED_SCOPE = "granted_scope"
        private val REFRESH_THRESHOLD_MS = TimeUnit.MINUTES.toMillis(5)
    }

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long,
        scope: String? = null
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + expiresInSeconds * 1000)
            .apply {
                if (scope != null) putString(KEY_GRANTED_SCOPE, scope)
            }
            .commit()
    }

    fun getGrantedScope(): String? = prefs.getString(KEY_GRANTED_SCOPE, null)

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun isTokenExpired(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return System.currentTimeMillis() >= expiresAt - REFRESH_THRESHOLD_MS
    }

    fun hasValidToken(): Boolean {
        return getAccessToken() != null && !isTokenExpired()
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_GRANTED_SCOPE)
            .commit()
    }

    fun saveCodeVerifier(verifier: String) {
        prefs.edit().putString(KEY_CODE_VERIFIER, verifier).apply()
    }

    fun getCodeVerifier(): String? = prefs.getString(KEY_CODE_VERIFIER, null)

    fun clearCodeVerifier() {
        prefs.edit().remove(KEY_CODE_VERIFIER).apply()
    }

    fun saveClientId(clientId: String) {
        prefs.edit().putString(KEY_CLIENT_ID, clientId.trim()).apply()
    }

    fun getStoredClientId(): String? = prefs.getString(KEY_CLIENT_ID, null)?.trim()?.takeIf { it.isNotBlank() }

    fun getEffectiveClientId(buildConfigClientId: String): String {
        if (buildConfigClientId.isNotBlank()) return buildConfigClientId.trim()
        return getStoredClientId() ?: ""
    }

    fun hasClientId(buildConfigClientId: String): Boolean {
        return getEffectiveClientId(buildConfigClientId).isNotBlank()
    }
}
