package org.erthi.note

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Palette.BACK)
        window.insetsController?.hide(WindowInsets.Type.systemBars())

        prefs = getSharedPreferences("erthi_settings", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Palette.BACK)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Palette.PANEL)
        }
        header.addView(icon(R.drawable.ic_collapse, getString(R.string.about_back),
            Palette.PANEL_LIGHT, 38) { finish() })
        header.addView(label(getString(R.string.settings), 18f, Palette.WHITE, true).apply {
            setPadding(dp(14), 0, 0, 0)
        })
        root.addView(header, LinearLayout.LayoutParams(-1, -2))

        val scroll = ScrollView(this).apply { setBackgroundColor(Palette.BACK) }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
        }

        body.addView(sectionLabel(getString(R.string.settings_section_editor)))
        body.addView(toggleRow(
            getString(R.string.settings_rtl_default),
            getString(R.string.settings_rtl_desc),
            prefs.getBoolean("rtl_default", false)
        ) { prefs.edit().putBoolean("rtl_default", it).apply() })

        body.addView(sectionLabel(getString(R.string.settings_section_backup)))
        body.addView(infoRow(getString(R.string.settings_backup_info)))

        body.addView(sectionLabel(getString(R.string.settings_section_about)))
        body.addView(actionRow(getString(R.string.about_erthi)) {
            startActivity(android.content.Intent(this, AboutActivity::class.java))
        })

        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun sectionLabel(text: String) = label(text, 12f, Palette.MINT, true).apply {
        val h = dp(12); val v = dp(20)
        setPadding(h, v, h, dp(6))
        textAllCaps = true
    }

    private fun infoRow(text: String) = label(text, 14f, Palette.MUTED).apply {
        val h = dp(12); val v = dp(10)
        setPadding(h, v, h, v)
        setLineSpacing(0f, 1.4f)
    }

    private fun actionRow(label: String, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(14), dp(16), dp(14))
            background = roundShape(Palette.PANEL, 14, Palette.BORDER)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.bottomMargin = dp(8)
            layoutParams = lp
        }
        row.addView(this.label(label, 15f, Palette.WHITE), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(this.label("›", 20f, Palette.MUTED))
        return row
    }

    private fun toggleRow(
        title: String, description: String,
        initialValue: Boolean, onChange: (Boolean) -> Unit
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundShape(Palette.PANEL, 14, Palette.BORDER)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.bottomMargin = dp(8)
            layoutParams = lp
        }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(label(title, 15f, Palette.WHITE))
        if (description.isNotBlank()) texts.addView(label(description, 12f, Palette.MUTED))
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))

        var state = initialValue
        val toggle = TextView(this).apply {
            text = if (state) getString(R.string.settings_on) else getString(R.string.settings_off)
            textSize = 13f
            setTextColor(if (state) Palette.MINT else Palette.MUTED)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = roundShape(if (state) Palette.PANEL_LIGHT else Palette.PANEL, 12, Palette.BORDER)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                state = !state
                text = if (state) context.getString(R.string.settings_on) else context.getString(R.string.settings_off)
                setTextColor(if (state) Palette.MINT else Palette.MUTED)
                background = roundShape(if (state) Palette.PANEL_LIGHT else Palette.PANEL, 12, Palette.BORDER)
                onChange(state)
            }
        }
        row.addView(toggle)
        return row
    }
}
