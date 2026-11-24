package org.erthi.note

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.WindowInsets
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

/**
 * Full-screen PIN entry gate. Launched by MainActivity when PIN is set.
 * On success returns RESULT_OK; on cancel finishes the calling activity.
 * PIN is stored as a SHA-256 hash in SharedPreferences — never plaintext.
 */
class PinActivity : Activity() {

    companion object {
        const val ACTION_VERIFY = "verify"
        const val ACTION_SETUP = "setup"
        const val PREF_PIN_HASH = "pin_hash"
        const val PREFS_NAME = "erthi_pin"

        fun isEnabled(activity: Activity): Boolean =
            activity.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(PREF_PIN_HASH, null) != null

        fun hash(pin: String): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    private val action: String get() = intent?.action ?: ACTION_VERIFY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Palette.BACK)
        window.insetsController?.hide(WindowInsets.Type.systemBars())

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
            hint = "••••"
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
                hint = "••••"
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
                    .putString(PREF_PIN_HASH, hash(pin)).apply()
                setResult(RESULT_OK)
                finish()
            })
            root.addSpace(this, 12)
            root.addView(action(getString(R.string.pin_skip), false) {
                setResult(RESULT_CANCELED)
                finish()
            })
        } else {
            root.addView(action(getString(R.string.pin_unlock), true) {
                val pin = pinField.text.toString()
                val stored = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getString(PREF_PIN_HASH, null)
                if (stored == null || stored == hash(pin)) {
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, R.string.pin_wrong, Toast.LENGTH_SHORT).show()
                    pinField.text.clear()
                }
            })
        }

        setContentView(root)
        pinField.requestFocus()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
}
