package com.example.respotapp.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.respotapp.data.TokenManager
import com.example.respotapp.ui.RespotPrimaryButton
import com.example.respotapp.ui.RespotScreenBackground
import com.example.respotapp.ui.theme.RespotTextSecondary

@Composable
fun ClientIdGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    RespotScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Get your Client ID",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Respot needs a Spotify Developer Client ID so it can connect to your Premium account. Follow these steps:",
                style = MaterialTheme.typography.bodyLarge,
                color = RespotTextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            GuideStep(
                number = 1,
                title = "Open the Spotify Developer Dashboard",
                body = "Go to developer.spotify.com/dashboard and sign in with the same Spotify account you use for Premium."
            )
            GuideStep(
                number = 2,
                title = "Create an app",
                body = "Click “Create app”. Give it a name (for example “Respot”) and a short description, accept the terms, then create it."
            )
            GuideStep(
                number = 3,
                title = "Add the redirect URI",
                body = "Open your app’s settings and add this Redirect URI exactly:\n\n\t\t${TokenManager.REDIRECT_URI}\n\nSave the settings."
            )
            GuideStep(
                number = 4,
                title = "Copy your Client ID",
                body = "On the app overview page, copy the Client ID (a long string of letters and numbers). Paste it into Respot, then tap Connect to Spotify."
            )
            GuideStep(
                number = 5,
                title = "Development Mode allowlist",
                body = "While your Spotify app is in Development Mode, add your Spotify account email under Users in the dashboard so API calls are authorized."
            )

            Spacer(modifier = Modifier.height(16.dp))

            RespotPrimaryButton(
                text = "Open Spotify Developer Dashboard",
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://developer.spotify.com/dashboard")
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GuideStep(
    number: Int,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = RespotTextSecondary
            )
        }
    }
}
