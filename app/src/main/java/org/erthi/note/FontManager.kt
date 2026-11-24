package org.erthi.note

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import java.io.File

/**
 * Catalog of available font families. System fonts work via standard TypefaceSpan.
 * Downloadable fonts are declared as res/font XML files and loaded via ResourcesCompat.
 * User-imported TTF/OTF files are stored in the app's filesDir/fonts/ directory.
 * Custom fonts need a CustomTypefaceSpan because TypefaceSpan(String) only resolves
 * fonts the platform knows by name.
 */
object FontManager {

    data class FontEntry(
        val displayName: String,
        val familyName: String,
        val isCustom: Boolean = false,
        val filePath: String? = null,
        /** Resource ID for downloadable font XML resources (R.font.*). Null for system/custom fonts. */
        val resId: Int? = null,
        val category: Category = Category.LATIN,
    )

    enum class Category(val label: String) {
        ARABIC("Arabic"),
        LATIN("Latin"),
        MONOSPACE("Monospace"),
        IMPORTED("Imported"),
    }

    /** System fonts available on all API 26+ devices without network. */
    private val SYSTEM_FONTS = listOf(
        FontEntry("Default", "sans-serif", category = Category.LATIN),
        FontEntry("Serif", "serif", category = Category.LATIN),
        FontEntry("Sans-Serif Light", "sans-serif-light", category = Category.LATIN),
        FontEntry("Sans-Serif Medium", "sans-serif-medium", category = Category.LATIN),
        FontEntry("Condensed", "sans-serif-condensed", category = Category.LATIN),
        FontEntry("Cursive", "cursive", category = Category.LATIN),
        FontEntry("Monospace", "monospace", category = Category.MONOSPACE),
    )

    /** Downloadable fonts via Google Fonts provider. Loaded with ResourcesCompat on demand. */
    private val DOWNLOADABLE_FONTS = listOf(
        FontEntry("Cairo", "Cairo", resId = R.font.cairo, category = Category.ARABIC),
        FontEntry("Amiri", "Amiri", resId = R.font.amiri, category = Category.ARABIC),
        FontEntry("Noto Kufi Arabic", "Noto Kufi Arabic", resId = R.font.noto_kufi_arabic, category = Category.ARABIC),
        FontEntry("Tajawal", "Tajawal", resId = R.font.tajawal, category = Category.ARABIC),
        FontEntry("Reem Kufi", "Reem Kufi", resId = R.font.reem_kufi, category = Category.ARABIC),
        FontEntry("Playfair Display", "Playfair Display", resId = R.font.playfair_display, category = Category.LATIN),
        FontEntry("EB Garamond", "EB Garamond", resId = R.font.eb_garamond, category = Category.LATIN),
        FontEntry("Montserrat", "Montserrat", resId = R.font.montserrat, category = Category.LATIN),
    )

    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun allFonts(context: Context): List<FontEntry> =
        SYSTEM_FONTS + DOWNLOADABLE_FONTS + importedFonts(context)

    fun fontsByCategory(context: Context): Map<Category, List<FontEntry>> =
        allFonts(context).groupBy { it.category }

    fun importedFonts(context: Context): List<FontEntry> {
        val dir = fontsDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension.lowercase() in listOf("ttf", "otf") }
            ?.map { f ->
                FontEntry(
                    f.nameWithoutExtension, f.nameWithoutExtension,
                    isCustom = true, filePath = f.absolutePath,
                    category = Category.IMPORTED
                )
            }
            ?: emptyList()
    }

    /**
     * Load the Typeface for a FontEntry. For downloadable fonts this may return a system
     * fallback if the Google Fonts provider hasn't delivered the font yet.
     */
    fun loadTypeface(context: Context, entry: FontEntry): Typeface {
        if (entry.isCustom && entry.filePath != null) {
            return typefaceCache.getOrPut(entry.filePath) {
                try { Typeface.createFromFile(entry.filePath) } catch (_: Exception) { Typeface.DEFAULT }
            }
        }
        if (entry.resId != null) {
            val key = "res:${entry.resId}"
            typefaceCache[key]?.let { return it }
            val tf = try {
                ResourcesCompat.getFont(context, entry.resId)
            } catch (_: Exception) {
                null
            }
            if (tf != null) {
                typefaceCache[key] = tf
                return tf
            }
            // Font not yet downloaded — fall back to system matching the family name
            return Typeface.create(entry.familyName, Typeface.NORMAL)
        }
        return Typeface.create(entry.familyName, Typeface.NORMAL)
    }

    /** Saves an imported font file to app-private storage and returns the resulting FontEntry. */
    fun importFont(context: Context, sourceBytes: ByteArray, fileName: String): FontEntry {
        require(sourceBytes.size < 10_000_000) { "Font file exceeds 10 MB limit" }
        val dir = fontsDir(context).also { it.mkdirs() }
        val dest = File(dir, fileName.filter { it.isLetterOrDigit() || it in "._- " }.trim())
        dest.writeBytes(sourceBytes)
        return FontEntry(
            dest.nameWithoutExtension, dest.nameWithoutExtension,
            isCustom = true, filePath = dest.absolutePath,
            category = Category.IMPORTED
        )
    }

    fun deleteFont(context: Context, entry: FontEntry): Boolean {
        if (!entry.isCustom || entry.filePath == null) return false
        typefaceCache.remove(entry.filePath)
        return File(entry.filePath).delete()
    }

    private fun fontsDir(context: Context) = File(context.filesDir, "fonts")
}

/** TypefaceSpan backed by an actual Typeface object (required for custom font files). */
class CustomTypefaceSpan(val typeface: Typeface, val familyName: String) :
    android.text.style.MetricAffectingSpan() {

    override fun updateMeasureState(paint: android.text.TextPaint) {
        paint.typeface = typeface
    }

    override fun updateDrawState(paint: android.text.TextPaint) {
        paint.typeface = typeface
    }
}
