package com.fgmachines.fgnote

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * About Erthi — stage 18.
 * Shows Erthi identity, version, website, email, privacy, and open-source licences.
 * No corporate or Nexvary/FG branding.
 */
class AboutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Palette.BACK)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(32))
        }

        // ── Back button ───────────────────────────────────────────────────────
        val back = TextView(this).apply {
            text = "← ${getString(R.string.about_back)}"
            textSize = 14f
            setTextColor(Palette.MINT)
            isClickable = true
            isFocusable = true
            setOnClickListener { finish() }
        }
        layout.addView(back)
        layout.addSpace(this, 20)

        // ── App name + Arabic name ────────────────────────────────────────────
        layout.addView(label(getString(R.string.app_name), 28f, Palette.WHITE, bold = true).apply {
            gravity = Gravity.CENTER
        })
        layout.addView(label("إرثي نوتة", 18f, Palette.MUTED).apply {
            gravity = Gravity.CENTER
            textDirection = android.view.View.TEXT_DIRECTION_RTL
        })
        layout.addSpace(this, 6)

        // ── Version ───────────────────────────────────────────────────────────
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "0.5.0-dev"
        } catch (_: Exception) { "0.5.0-dev" }
        layout.addView(label(getString(R.string.about_version, versionName), 13f, Palette.MUTED).apply {
            gravity = Gravity.CENTER
        })
        layout.addSpace(this, 24)

        // ── Divider ───────────────────────────────────────────────────────────
        layout.addView(divider())
        layout.addSpace(this, 20)

        // ── Description ──────────────────────────────────────────────────────
        layout.addView(body(getString(R.string.about_description)))
        layout.addSpace(this, 20)

        // ── Website + Email ───────────────────────────────────────────────────
        layout.addView(linkRow(getString(R.string.about_website), "https://erthi.org"))
        layout.addSpace(this, 8)
        layout.addView(linkRow(getString(R.string.about_email), "mailto:info@erthi.org"))
        layout.addSpace(this, 20)

        layout.addView(divider())
        layout.addSpace(this, 20)

        // ── Privacy policy ────────────────────────────────────────────────────
        layout.addView(label(getString(R.string.about_privacy_heading), 15f, Palette.SILVER, bold = true))
        layout.addSpace(this, 8)
        layout.addView(body(getString(R.string.about_privacy_body)))
        layout.addSpace(this, 20)

        layout.addView(divider())
        layout.addSpace(this, 20)

        // ── Open-source licences ──────────────────────────────────────────────
        layout.addView(label(getString(R.string.about_licences_heading), 15f, Palette.SILVER, bold = true))
        layout.addSpace(this, 8)
        layout.addView(body(getString(R.string.about_licences_body)))
        layout.addSpace(this, 24)

        scroll.addView(layout)
        setContentView(scroll)

        // Window insets
        layout.setOnApplyWindowInsetsListener { _, insets ->
            val top: Int; val bottom: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                top = bars.top; bottom = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                top = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottom = insets.systemWindowInsetBottom
            }
            layout.setPadding(dp(24), top + dp(24), dp(24), bottom + dp(32))
            insets
        }
    }

    private fun divider() = android.view.View(this).apply {
        setBackgroundColor(Palette.BORDER)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Palette.MUTED)
        lineSpacingMultiplier = 1.5f
        textDirection = android.view.View.TEXT_DIRECTION_LOCALE
    }

    private fun linkRow(label: String, url: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        isClickable = true
        isFocusable = true
        setOnClickListener {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            catch (_: Exception) { }
        }
        addView(TextView(this@AboutActivity).apply {
            text = "→ $label"
            textSize = 14f
            setTextColor(Palette.MINT)
            setPadding(0, dp(4), 0, dp(4))
        })
    }
}
