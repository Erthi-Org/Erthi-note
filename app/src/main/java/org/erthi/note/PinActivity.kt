package org.erthi.note

import android.app.Activity
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

/**
 * Full-screen PIN entry gate. Launched by MainActivity when PIN is set.
 * PIN stored as PBKDF2WithHmacSHA256 with a random 16-byte salt.
 * Legacy SHA-256 hashes are accepted and silently upgraded on successful login.
 */
class PinActivity : Activity() {

    companion object {
        const val ACTION_VERIFY = "verify"
        const val ACTION_SETUP = "setup"
        const val PREF_PIN_HASH = "pin_hash"
        const val PREFS_NAME = "erthi_pin"
        private const val PREF_FAIL_COUNT = "fail_count"
        private const val PREF_FAIL_TIME = "fail_time"
        private const val MAX_FREE_TRIES = 5

        fun isEnabled(activity: Activity): Boolean =
            activity.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(PREF_PIN_HASH, null) != null

        fun hashPin(pin: String): String {
            val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
            val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, 100_000, 256)
            val hash = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            return salt.hex() + ":" + hash.hex()
        }

        fun verify(pin: String, stored: String): Boolean {
            return if (':' in stored) {
                val saltHex = stored.substringBefore(':')
                val expectedHash = stored.substringAfter(':')
                val salt = saltHex.hexToBytes() ?: return false
                val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, 100_000, 256)
                val hash = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).encoded
                hash.hex() == expectedHash
            } else {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                digest.digest(pin.toByteArray(Charsets.UTF_8)).hex() == stored
            }
        }

        private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
        private fun String.hexToBytes(): ByteArray? = try {
            if (length % 2 != 0) null
            else ByteArray(length / 2) { i ->
                ((this[i * 2].digitToInt(16) shl 4) or this[i * 2 + 1].digitToInt(16)).toByte()
            }
        } catch (_: Exception) { null }

        fun throttleSeconds(prefs: android.content.SharedPreferences): Long {
            val failCount = prefs.getInt(PREF_FAIL_COUNT, 0)
            if (failCount < MAX_FREE_TRIES) return 0L
            val failTime = prefs.getLong(PREF_FAIL_TIME, 0L)
            val delay = minOf(1L shl (failCount - MAX_FREE_TRIES), 3600L) * 1000L
            val remaining = (failTime + delay - System.currentTimeMillis()) / 1000L
            return maxOf(0L, remaining)
        }
    }

    private val action: String get() = intent?.action ?: ACTION_VERIFY
    private var biometricSignal: CancellationSignal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Palette.BACK)
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.hide(android.view.WindowInsets.Type.systemBars())
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Palette.BACK)
            setPadding(dp(32), dp(32), dp(32), dp(32))
        }

        root.addView(label(
            if (action == ACTION_SETUP) getString(R.string.pin_setup_title)
            else getString(R.string.pin_title),
            22f, Palette.WHITE, true
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        })

        root.addView(label(
            if (action == ACTION_SETUP) getString(R.string.pin_setup_hint)
            else getString(R.string.pin_hint),
            14f, Palette.MUTED
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(24))
        })

        val pinField = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 28f
            setTextColor(Palette.WHITE)
            setHintTextColor(Palette.MUTED)
            hint = "\u2022\u2022\u2022\u2022"
            gravity = Gravity.CENTER
            background = roundShape(Palette.PANEL, 16, Palette.BORDER)
            val p = dp(16)
            setPadding(p, p, p, p)
            filters = arrayOf(android.text.InputFilter.LengthFilter(12))
        }
        root.addView(pinField, LinearLayout.LayoutParams(-1, dp(72)).apply {
            bottomMargin = dp(20)
        })

        if (action == ACTION_SETUP) {
            val confirmField = EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                textSize = 28f
                setTextColor(Palette.WHITE)
                setHintTextColor(Palette.MUTED)
                hint = "\u2022\u2022\u2022\u2022"
                gravity = Gravity.CENTER
                background = roundShape(Palette.PANEL, 16, Palette.BORDER)
                val p = dp(16)
                setPadding(p, p, p, p)
                filters = arrayOf(android.text.InputFilter.LengthFilter(12))
            }
            root.addView(label(getString(R.string.pin_confirm_hint), 14f, Palette.MUTED).apply {
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(8))
            })
            root.addView(confirmField, LinearLayout.LayoutParams(-1, dp(72)).apply {
                bottomMargin = dp(20)
            })
            root.addView(action(getString(R.string.pin_set), true) {
                val pin = pinField.text.toString()
                val confirm = confirmField.text.toString()
                if (pin.length < 4) {
                    Toast.makeText(this, R.string.pin_too_short, Toast.LENGTH_SHORT).show()
                    return@action
                }
                if (pin != confirm) {
                    Toast.makeText(this, R.string.pin_mismatch, Toast.LENGTH_SHORT).show()
                    return@action
                }
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putString(PREF_PIN_HASH, hashPin(pin)).apply()
                setResult(RESULT_OK)
                finish()
            })
            root.addSpace(this, 12)
            root.addView(action(getString(R.string.pin_skip), false) {
                setResult(RESULT_CANCELED)
                finish()
            })
        } else {
            // Biometric unlock (API 28+, only when enrolled biometric is available)
            if (Build.VERSION.SDK_INT >= 28 && BiometricHelper.isAvailable(this)) {
                root.addView(action(getString(R.string.biometric_unlock), true) {
                    biometricSignal?.cancel()
                    biometricSignal = BiometricHelper.prompt(
                        activity = this,
                        title = getString(R.string.pin_title),
                        subtitle = getString(R.string.biometric_subtitle),
                        cancel = getString(R.string.cancel),
                        onSuccess = {
                            setResult(RESULT_OK)
                            finish()
                        },
                        onFailure = { msg ->
                            Toast.makeText(this, msg.ifEmpty { getString(R.string.biometric_failed) },
                                Toast.LENGTH_SHORT).show()
                        }
                    )
                })
                root.addSpace(this, 12)
                root.addView(label(getString(R.string.or_use_pin), 13f, Palette.MUTED).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, dp(8))
                })
            }
            root.addView(action(getString(R.string.pin_unlock), false) {
                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                val remaining = throttleSeconds(prefs)
                if (remaining > 0) {
                    Toast.makeText(this,
                        getString(R.string.pin_throttle, remaining), Toast.LENGTH_SHORT).show()
                    return@action
                }
                val pin = pinField.text.toString()
                val stored = prefs.getString(PREF_PIN_HASH, null)
                if (stored == null || verify(pin, stored)) {
                    prefs.edit().remove(PREF_FAIL_COUNT).remove(PREF_FAIL_TIME).apply()
                    if (stored != null && ':' !in stored) {
                        prefs.edit().putString(PREF_PIN_HASH, hashPin(pin)).apply()
                    }
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val newCount = prefs.getInt(PREF_FAIL_COUNT, 0) + 1
                    prefs.edit()
                        .putInt(PREF_FAIL_COUNT, newCount)
                        .putLong(PREF_FAIL_TIME, System.currentTimeMillis())
                        .apply()
                    Toast.makeText(this, R.string.pin_wrong, Toast.LENGTH_SHORT).show()
                    pinField.text.clear()
                }
            })
        }

        setContentView(root)
        pinField.requestFocus()
    }

    override fun onDestroy() {
        biometricSignal?.cancel()
        super.onDestroy()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
}
