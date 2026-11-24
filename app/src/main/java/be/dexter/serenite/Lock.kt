package be.dexter.serenite

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Verrouillage par empreinte / visage, ou code du téléphone en secours (API système, sans dépendance). */
object Lock {
    /** Le téléphone a-t-il un écran de verrouillage (code, schéma, empreinte) ? Sans lui, pas de verrou possible. */
    fun available(ctx: Context): Boolean = ctx.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    fun prompt(act: Activity, subtitle: String, onOk: () -> Unit, onFail: (String) -> Unit) {
        val b = BiometricPrompt.Builder(act)
            .setTitle("Sérénité")
            .setSubtitle(subtitle)
        if (Build.VERSION.SDK_INT >= 30) {
            b.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        } else {
            @Suppress("DEPRECATION")
            b.setDeviceCredentialAllowed(true)
        }
        try {
            b.build().authenticate(CancellationSignal(), act.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onOk()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = onFail(errString?.toString() ?: "Échec")
            })
        } catch (e: Exception) {
            onFail(e.message ?: "Verrouillage indisponible")
        }
    }
}

@Composable
fun LockScreen(message: String?, onUnlock: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🔒", style = MaterialTheme.typography.displayMedium)
            Text("Sérénité est verrouillée", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (message != null) Muted(message)
            Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) { Text("Déverrouiller") }
        }
    }
}
