package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import android.graphics.Bitmap
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.R
import androidx.core.graphics.drawable.toBitmap

@Composable
fun LoginScreen(
    onLogin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var token by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val signUpUrl = stringResource(R.string.digitalocean_signup_url)
    val createTokenUrl = stringResource(R.string.digitalocean_api_token_url)
    val context = LocalContext.current
    val density = LocalDensity.current
    val logoBitmap = remember(density) {
        val sizePx = with(density) { 120.dp.roundToPx() }
        AppCompatResources.getDrawable(context, R.mipmap.ic_launcher)
            ?.toBitmapSafely(sizePx, sizePx)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                logoBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(120.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text(stringResource(R.string.enter_api_token)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onLogin(token.trim()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.login))
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Privacy Policy link on login screen
            Button(
                onClick = { uriHandler.openUri(privacyUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.privacy_policy))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { uriHandler.openUri(signUpUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sign_up_for_digitalocean))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { uriHandler.openUri(createTokenUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.create_api_token))
            }
        }
    }
}

private fun android.graphics.drawable.Drawable.toBitmapSafely(width: Int, height: Int): Bitmap {
    val safeWidth = if (width > 0) width else 1
    val safeHeight = if (height > 0) height else 1
    return toBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
}
