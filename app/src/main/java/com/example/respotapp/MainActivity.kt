package com.example.respotapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.respotapp.data.SpotifyAuthHelper
import com.example.respotapp.data.TokenManager
import com.example.respotapp.playback.RespotPlaybackService
import com.example.respotapp.ui.RespotRoot
import com.example.respotapp.ui.theme.RespotAppTheme
import com.example.respotapp.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val tokenManager by lazy { (application as RespotApp).tokenManager }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val viewModel = getViewModel()
        if (granted) {
            viewModel.onStoragePermissionGranted()
        } else {
            viewModel.onStoragePermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleAuthIntent(intent)

        startService(Intent(this, RespotPlaybackService::class.java))
        (application as RespotApp).playbackController.connect()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            RespotAppTheme(uiSettings = uiState.uiSettings) {
                RespotRoot(
                    viewModel = viewModel,
                    onLoginClick = { startSpotifyLogin() },
                    onRequestStoragePermission = { requestStoragePermission() }
                )
            }
        }

        requestStoragePermissionIfNeeded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun startSpotifyLogin() {
        val clientId = (application as RespotApp).authRepository.getClientId()
        if (clientId.isBlank()) return

        val verifier = SpotifyAuthHelper.generateCodeVerifier()
        tokenManager.saveCodeVerifier(verifier)
        val challenge = SpotifyAuthHelper.generateCodeChallenge(verifier)
        val authUri = SpotifyAuthHelper.buildAuthorizationUri(clientId, challenge)

        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(this, authUri)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri == null || uri.scheme != "respot" || uri.host != "auth") return

        val code = uri.getQueryParameter("code")
        if (code != null) {
            getViewModel().handleAuthCallback(code)
        }
    }

    private fun requestStoragePermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            getViewModel().onStoragePermissionGranted()
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        storagePermissionLauncher.launch(permission)
    }

    private fun getViewModel(): MainViewModel {
        // ViewModel is scoped to activity via compose; trigger via activity scope
        return androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
    }
}
