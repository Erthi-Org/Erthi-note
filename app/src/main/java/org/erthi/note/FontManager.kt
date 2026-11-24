package org.erthi.note

import android.content.Context
import android.graphics.Typeface
import android.text.style.TypefaceSpan
import java.io.File

/**
 * Catalog of available font families. System fonts work via standard TypefaceSpan.
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
    )

    /** Fonts available on all API 26+ devices. */
    val SYSTEM_FONTS = listOf(
        FontEntry("Default", "sans-serif"),
        FontEntry("Serif", "serif"),
        FontEntry("Monospace", "monospace"),
        FontEntry("Sans-Serif Light", "sans-serif-light"),
        FontEntry("Sans-Serif Medium", "sans-serif-medium"),
        FontEntry("Condensed", "sans-serif-condensed"),
        FontEntry("Cursive", "cursive"),
    )

    private val cache = mutableMapOf<String, Typeface>()

    fun importedFonts(context: Context): List<FontEntry> {
        val dir = fontsDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension.lowercase() in listOf("ttf", "otf") }
            ?.map { f -> FontEntry(f.nameWithoutExtension, f.nameWithoutExtension,
                isCustom = true, filePath = f.absolutePath) }
            ?: emptyList()
    }

    fun allFonts(context: Context): List<FontEntry> = SYSTEM_FONTS + importedFonts(context)

    fun loadTypeface(entry: FontEntry): Typeface {
        if (!entry.isCustom || entry.filePath == null) {
            return Typeface.create(entry.familyName, Typeface.NORMAL)
        }
        return cache.getOrPut(entry.filePath) {
            try {
                Typeface.createFromFile(entry.filePath)
            } catch (_: Exception) {
                Typeface.DEFAULT
            }
        }
    }

    /** Saves an imported font file to app-private storage and returns the resulting FontEntry. */
    fun importFont(context: Context, sourceBytes: ByteArray, fileName: String): FontEntry {
        require(sourceBytes.size < 10_000_000) { "Font file exceeds 10 MB limit" }
        val dir = fontsDir(context).also { it.mkdirs() }
        val dest = File(dir, fileName.filter { it.isLetterOrDigit() || it in "._- " }.trim())
        dest.writeBytes(sourceBytes)
        return FontEntry(dest.nameWithoutExtension, dest.nameWithoutExtension,
            isCustom = true, filePath = dest.absolutePath)
    }

    fun deleteFont(context: Context, entry: FontEntry): Boolean {
        if (!entry.isCustom || entry.filePath == null) return false
        cache.remove(entry.filePath)
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
