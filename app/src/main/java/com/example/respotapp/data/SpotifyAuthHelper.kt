package com.example.respotapp.data

import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object SpotifyAuthHelper {

  fun generateCodeVerifier(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
  }

  fun generateCodeChallenge(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
  }

  fun buildAuthorizationUri(clientId: String, codeChallenge: String): Uri {
    return Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
      .appendQueryParameter("client_id", clientId)
      .appendQueryParameter("response_type", "code")
      .appendQueryParameter("redirect_uri", TokenManager.REDIRECT_URI)
      .appendQueryParameter("code_challenge_method", "S256")
      .appendQueryParameter("code_challenge", codeChallenge)
      .appendQueryParameter("scope", TokenManager.SCOPES)
      .build()
  }
}
