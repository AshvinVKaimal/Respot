package com.example.respotapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.respotapp.R
import com.example.respotapp.ui.RespotPillField
import com.example.respotapp.ui.RespotPrimaryButton
import com.example.respotapp.ui.RespotScreenBackground
import com.example.respotapp.ui.RespotSecondaryButton
import com.example.respotapp.ui.theme.RespotSpotifyGreen

@Composable
fun LoginScreen(
    isLoading: Boolean,
    hasClientId: Boolean,
    clientIdInput: String,
    errorMessage: String?,
    onClientIdChange: (String) -> Unit,
    onSaveClientId: () -> Unit,
    onLoginClick: () -> Unit,
    onOpenClientIdGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    RespotScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_respot_logo),
                contentDescription = "Respot",
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Music your way\non Respot",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(33.dp))

            Text(
                text = "Client ID:",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            RespotPillField(
                value = clientIdInput,
                onValueChange = onClientIdChange,
                placeholder = "Enter your client ID here",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            RespotSecondaryButton(
                text = "How to get your Spotify Client ID",
                onClick = onOpenClientIdGuide,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                RespotPrimaryButton(
                    text = "Connect to Spotify",
                    onClick = {
                        if (clientIdInput.isNotBlank()) {
                            onSaveClientId()
                        }
                        onLoginClick()
                    },
                    enabled = hasClientId || clientIdInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = buildAnnotatedString {
                    append("Note: You need to have a ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = RespotSpotifyGreen)) {
                        append("Spotify Premium")
                    }
                    append(" account in order to use ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append("Respot")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
