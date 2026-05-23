package io.github.reneknap.mediacenter.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.reneknap.mediacenter.R

@Composable
fun SupportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playStoreUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
    val coffeeUrl = stringResource(R.string.support_coffee_url)
    val paypalUrl = stringResource(R.string.support_paypal_url)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.support_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.support_dialog_body))
                Button(
                    onClick = { context.openUrl(playStoreUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.support_rate))
                }
                Button(
                    onClick = { context.openUrl(coffeeUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.support_coffee))
                }
                Button(
                    onClick = { context.openUrl(paypalUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.support_paypal))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.support_dialog_close))
            }
        },
    )
}

private fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No app can open the link (e.g. no browser installed); fail quietly instead of crashing.
    }
}
