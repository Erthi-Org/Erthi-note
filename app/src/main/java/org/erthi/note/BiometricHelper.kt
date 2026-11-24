package org.erthi.note

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.hardware.biometrics.BiometricManager
import androidx.annotation.RequiresApi

/**
 * Thin wrapper around the platform BiometricPrompt (API 28+).
 * Uses BIOMETRIC_STRONG (fingerprint / face / iris backed by Keystore).
 * On API 26–27 biometric is not available; PinActivity shows only the PIN field.
 *
 * Security: biometric success alone is treated as an unlock signal for the PIN gate.
 * We do NOT claim that biometric encrypts the database; it replaces the PIN *entry*
 * flow for convenience while the actual DB protection relies on Android's storage
 * encryption and the user's device lock.
 */
object BiometricHelper {

    /** True when the device has strong biometric hardware with enrolled credentials. */
    fun isAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 28) return false
        return if (Build.VERSION.SDK_INT >= 29) {
            val manager = context.getSystemService(BiometricManager::class.java)
            manager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } else {
            // API 28: FingerprintManager is the only available check on this API level
            @Suppress("DEPRECATION")
            val fm = context.getSystemService(android.hardware.fingerprint.FingerprintManager::class.java)
            @Suppress("DEPRECATION")
            fm?.isHardwareDetected == true && fm.hasEnrolledFingerprints()
        }
    }

    /**
     * Show the platform biometric prompt.
     * [onSuccess] is called on the main thread on successful authentication.
     * [onFailure] is called with an error message string (may be empty) on hard failure.
     * User cancellation is silent (neither callback fires).
     */
    @RequiresApi(28)
    fun prompt(
        activity: Activity,
        title: String,
        subtitle: String,
        cancel: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ): CancellationSignal {
        val signal = CancellationSignal()
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                activity.runOnUiThread { onSuccess() }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // errorCode BIOMETRIC_ERROR_USER_CANCELED (10) / BIOMETRIC_ERROR_NEGATIVE_BUTTON (13)
                // are user-initiated dismissals — not application failures, so we stay silent.
                if (errorCode != 10 && errorCode != 13) {
                    activity.runOnUiThread { onFailure(errString.toString()) }
                }
            }

            override fun onAuthenticationFailed() {
                // Single scan failed; the prompt stays visible — no callback action needed.
            }
        }

        BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(cancel, activity.mainExecutor) { _, _ ->
                signal.cancel()
            }
            .build()
            .authenticate(signal, activity.mainExecutor, callback)

        return signal
    }
}
