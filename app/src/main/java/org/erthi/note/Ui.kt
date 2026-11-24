package org.erthi.note

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * Andalusian dark palette — Deep Glossy Black, Metallic Silver, Andalusian Gold.
 * All text/background pairs meet WCAG AA (4.5:1).
 */
internal object Palette {
    var isDark = true

    val BACK        get() = if (isDark) 0xFF0A0A0A.toInt() else 0xFFF5F3EF.toInt()
    val PANEL       get() = if (isDark) 0xFF1C1C1C.toInt() else 0xFFEBE9E4.toInt()
    val PANEL_LIGHT get() = if (isDark) 0xFF242424.toInt() else 0xFFDFDDD8.toInt()
    val MINT        get() = if (isDark) 0xFFC8A86E.toInt() else 0xFF8B6914.toInt()
    val WHITE       get() = if (isDark) 0xFFF0EDE8.toInt() else 0xFF1A1612.toInt()
    val MUTED       get() = if (isDark) 0xFF787878.toInt() else 0xFF5A5651.toInt()
    val RED         get() = if (isDark) 0xFFE05A5A.toInt() else 0xFFCC2222.toInt()
    val BORDER      get() = if (isDark) 0xFF363636.toInt() else 0xFFCFCDC8.toInt()
    val SILVER      get() = 0xFFB8B8B8.toInt()

    fun applyTheme(dark: Boolean) { isDark = dark }
    fun init(prefs: android.content.SharedPreferences) {
        isDark = prefs.getBoolean("dark_theme", true)
    }
}

internal fun Context.dp(value: Int): Int = (resources.displayMetrics.density * value).roundToInt()

internal fun shape(fill: Int, radius: Float, stroke: Int? = null): GradientDrawable =
    GradientDrawable().apply {
        setColor(fill)
        cornerRadius = radius
        if (stroke != null) setStroke(1, stroke)
    }

internal fun Context.roundShape(fill: Int, radiusDp: Int, stroke: Int? = null) =
    shape(fill, dp(radiusDp).toFloat(), stroke)

internal fun Context.icon(
    drawable: Int,
    label: String,
    tintBack: Int = Palette.PANEL_LIGHT,
    tintIcon: Int? = null,
    size: Int = 44,
    click: () -> Unit
): ImageView = ImageView(this).apply {
    setImageResource(drawable)
    if (tintIcon != null) {
        imageTintList = android.content.res.ColorStateList.valueOf(tintIcon)
    }
    scaleType = ImageView.ScaleType.FIT_CENTER
    val p = dp(11)
    setPadding(p, p, p, p)
    background = roundShape(tintBack, size / 2, Palette.BORDER)
    contentDescription = label
    isClickable = true
    isFocusable = true
    setOnClickListener { click() }
    layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
}

internal fun Context.label(
    text: String, sizeSp: Float, color: Int = Palette.WHITE,
    bold: Boolean = false
): TextView = TextView(this).apply {
    this.text = text
    textSize = sizeSp
    setTextColor(color)
    if (bold) setTypeface(typeface, Typeface.BOLD)
    gravity = Gravity.CENTER_VERTICAL
    textDirection = View.TEXT_DIRECTION_FIRST_STRONG
}

internal fun Context.action(
    text: String, primary: Boolean, click: () -> Unit
): TextView = label(
    text, 15f,
    if (primary) Palette.BACK else Palette.WHITE,
    bold = true
).apply {
    gravity = Gravity.CENTER
    background = roundShape(if (primary) Palette.MINT else Palette.PANEL_LIGHT, 16, Palette.BORDER)
    val vertical = dp(15)
    setPadding(dp(18), vertical, dp(18), vertical)
    isClickable = true
    isFocusable = true
    setOnClickListener { click() }
}

internal fun LinearLayout.addSpace(context: Context, heightDp: Int) {
    addView(View(context), LinearLayout.LayoutParams(1, context.dp(heightDp)))
}

internal fun View.detach() {
    (parent as? ViewGroup)?.removeView(this)
}
