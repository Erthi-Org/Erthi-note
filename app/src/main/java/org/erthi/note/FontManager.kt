package org.erthi.note

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import java.io.File

/**
 * Catalog of available font families.
 *
 * Priority order for offline use:
 *   1. System fonts — always available, referenced by standard family name.
 *   2. Asset fonts  — OFL/Apache 2.0 TTF/OTF files bundled in assets/fonts/.
 *                     Place files there before shipping (see docs/FONTS.md).
 *   3. Downloadable — Google Fonts provider XML resources; require GMS and
 *                     internet on first use; fall back to system fonts offline.
 *   4. Imported     — User-supplied TTF/OTF files in app filesDir/fonts/.
 *
 * Custom fonts use CustomTypefaceSpan; system fonts use standard TypefaceSpan.
 */
object FontManager {

    data class FontEntry(
        val displayName: String,
        val familyName: String,
        val isCustom: Boolean = false,
        val filePath: String? = null,
        /** Resource ID for downloadable font XML resources (R.font.*). Null for others. */
        val resId: Int? = null,
        /** Asset path relative to assets/ — e.g. "fonts/Cairo-Regular.ttf". */
        val assetPath: String? = null,
        val category: Category = Category.LATIN,
    )

    enum class Category(val label: String) {
        ARABIC("Arabic"),
        LATIN("Latin"),
        MONOSPACE("Monospace"),
        IMPORTED("Imported"),
    }

    // ── 1. System fonts (always available offline) ───────────────────────────
    // Noto Sans Arabic is the AOSP Arabic face; it activates automatically for
    // Arabic code-points even when "sans-serif" is specified, so these family
    // names are reliable across all API 26+ devices.
    private val SYSTEM_FONTS = listOf(
        FontEntry("Default (Roboto)",      "sans-serif",           category = Category.LATIN),
        FontEntry("Serif",                 "serif",                category = Category.LATIN),
        FontEntry("Sans-Serif Light",      "sans-serif-light",     category = Category.LATIN),
        FontEntry("Sans-Serif Medium",     "sans-serif-medium",    category = Category.LATIN),
        FontEntry("Condensed",             "sans-serif-condensed", category = Category.LATIN),
        FontEntry("Cursive",               "cursive",              category = Category.LATIN),
        FontEntry("Monospace",             "monospace",            category = Category.MONOSPACE),
        // Noto Arabic faces — available via system on API 24+ AOSP builds
        FontEntry("Noto Naskh Arabic",     "Noto Naskh Arabic",    category = Category.ARABIC),
        FontEntry("Noto Kufi Arabic (sys)", "Noto Kufi Arabic",    category = Category.ARABIC),
    )

    // ── 2. Asset-bundled fonts (offline, OFL/Apache-2.0 licensed) ───────────
    // These entries describe TTF files that must be placed in assets/fonts/.
    // The app ships a fonts/README.txt with download instructions for CI.
    // At runtime, loadTypeface() falls back gracefully if the file is absent.
    private val ASSET_FONTS = listOf(
        FontEntry("Amiri (bundled)",         "Amiri",          assetPath = "fonts/Amiri-Regular.ttf",          category = Category.ARABIC),
        FontEntry("Cairo (bundled)",         "Cairo",          assetPath = "fonts/Cairo-Regular.ttf",          category = Category.ARABIC),
        FontEntry("Noto Kufi Arabic",        "Noto Kufi Arabic", assetPath = "fonts/NotoKufiArabic-Regular.ttf", category = Category.ARABIC),
        FontEntry("Tajawal (bundled)",       "Tajawal",        assetPath = "fonts/Tajawal-Regular.ttf",        category = Category.ARABIC),
        FontEntry("Reem Kufi (bundled)",     "Reem Kufi",      assetPath = "fonts/ReemKufi-Regular.ttf",       category = Category.ARABIC),
        FontEntry("Playfair Display (bundled)", "Playfair Display", assetPath = "fonts/PlayfairDisplay-Regular.ttf", category = Category.LATIN),
        FontEntry("EB Garamond (bundled)",   "EB Garamond",    assetPath = "fonts/EBGaramond-Regular.ttf",     category = Category.LATIN),
        FontEntry("Montserrat (bundled)",    "Montserrat",     assetPath = "fonts/Montserrat-Regular.ttf",     category = Category.LATIN),
    )

    // ── 3. Downloadable fonts (require GMS + internet on first use) ──────────
    private val DOWNLOADABLE_FONTS = listOf(
        FontEntry("Cairo",           "Cairo",           resId = R.font.cairo,            category = Category.ARABIC),
        FontEntry("Amiri",           "Amiri",           resId = R.font.amiri,            category = Category.ARABIC),
        FontEntry("Noto Kufi Arabic","Noto Kufi Arabic",resId = R.font.noto_kufi_arabic, category = Category.ARABIC),
        FontEntry("Tajawal",         "Tajawal",         resId = R.font.tajawal,          category = Category.ARABIC),
        FontEntry("Reem Kufi",       "Reem Kufi",       resId = R.font.reem_kufi,        category = Category.ARABIC),
        FontEntry("Playfair Display","Playfair Display",resId = R.font.playfair_display, category = Category.LATIN),
        FontEntry("EB Garamond",     "EB Garamond",     resId = R.font.eb_garamond,      category = Category.LATIN),
        FontEntry("Montserrat",      "Montserrat",      resId = R.font.montserrat,       category = Category.LATIN),
    )

    private val typefaceCache = mutableMapOf<String, Typeface>()

    /** All available fonts in display order. Asset-bundled fonts take priority over downloadable. */
    fun allFonts(context: Context): List<FontEntry> {
        val assetAvailable = ASSET_FONTS.filter { e ->
            e.assetPath != null && context.assetExists(e.assetPath)
        }.map { it.copy(displayName = it.displayName.removeSuffix(" (bundled)")) }
        val assetFamilyNames = assetAvailable.map { it.familyName }.toSet()
        // Only include downloadable fallback when the asset isn't available
        val downloadFallback = DOWNLOADABLE_FONTS.filter { it.familyName !in assetFamilyNames }
        // Deduplicate system entries whose family names appear in assetAvailable
        val systemFiltered = SYSTEM_FONTS.filter { it.familyName !in assetFamilyNames }
        return systemFiltered + assetAvailable + downloadFallback + importedFonts(context)
    }

    fun fontsByCategory(context: Context): Map<Category, List<FontEntry>> =
        allFonts(context).groupBy { it.category }

    fun importedFonts(context: Context): List<FontEntry> {
        val dir = fontsDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension.lowercase() in listOf("ttf", "otf") }
            ?.map { f ->
                FontEntry(f.nameWithoutExtension, f.nameWithoutExtension,
                    isCustom = true, filePath = f.absolutePath, category = Category.IMPORTED)
            } ?: emptyList()
    }

    /**
     * Load the Typeface for a FontEntry.
     * Order: asset file → downloadable resource → system name → DEFAULT.
     */
    fun loadTypeface(context: Context, entry: FontEntry): Typeface {
        // User-imported file
        if (entry.isCustom && entry.filePath != null) {
            return typefaceCache.getOrPut("file:${entry.filePath}") {
                try { Typeface.createFromFile(entry.filePath) } catch (_: Exception) { Typeface.DEFAULT }
            }
        }
        // Asset-bundled file
        if (entry.assetPath != null) {
            val key = "asset:${entry.assetPath}"
            typefaceCache[key]?.let { return it }
            val tf = try {
                Typeface.createFromAsset(context.assets, entry.assetPath)
            } catch (_: Exception) { null }
            if (tf != null) { typefaceCache[key] = tf; return tf }
            // Fall through to downloadable / system
        }
        // Downloadable resource (GMS)
        if (entry.resId != null) {
            val key = "res:${entry.resId}"
            typefaceCache[key]?.let { return it }
            val tf = try { ResourcesCompat.getFont(context, entry.resId) } catch (_: Exception) { null }
            if (tf != null) { typefaceCache[key] = tf; return tf }
        }
        // System font name (may be default on most devices for non-system names)
        return Typeface.create(entry.familyName, Typeface.NORMAL)
    }

    /** Returns true when this entry requires a real Typeface object (not just a family name). */
    fun needsCustomSpan(entry: FontEntry): Boolean =
        entry.isCustom || entry.assetPath != null || entry.resId != null

    /** Saves an imported font file to app-private storage. */
    fun importFont(context: Context, sourceBytes: ByteArray, fileName: String): FontEntry {
        require(sourceBytes.size < 10_000_000) { "Font file exceeds 10 MB limit" }
        require(isValidFontMagic(sourceBytes)) { "File does not appear to be a valid TTF/OTF font" }
        val dir = fontsDir(context).also { it.mkdirs() }
        val safeName = fileName.filter { it.isLetterOrDigit() || it in "._- " }.trim()
        val dest = File(dir, safeName)
        dest.writeBytes(sourceBytes)
        return FontEntry(dest.nameWithoutExtension, dest.nameWithoutExtension,
            isCustom = true, filePath = dest.absolutePath, category = Category.IMPORTED)
    }

    fun deleteFont(context: Context, entry: FontEntry): Boolean {
        if (!entry.isCustom || entry.filePath == null) return false
        typefaceCache.remove("file:${entry.filePath}")
        return File(entry.filePath).delete()
    }

    private fun fontsDir(context: Context) = File(context.filesDir, "fonts")

    private fun Context.assetExists(path: String): Boolean = try {
        assets.open(path).close(); true
    } catch (_: Exception) { false }

    /** Basic magic-byte check: TrueType (00 01 00 00), OTF/CFF (4F 54 54 4F), or wOFF (77 4F 46 46). */
    private fun isValidFontMagic(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val b = bytes
        return (b[0] == 0x00.toByte() && b[1] == 0x01.toByte() && b[2] == 0x00.toByte() && b[3] == 0x00.toByte()) ||
               (b[0] == 0x4F.toByte() && b[1] == 0x54.toByte() && b[2] == 0x54.toByte() && b[3] == 0x4F.toByte()) ||
               (b[0] == 0x77.toByte() && b[1] == 0x4F.toByte() && b[2] == 0x46.toByte() && b[3] == 0x46.toByte())
    }
}

/** TypefaceSpan backed by an actual Typeface object (required for custom font files). */
class CustomTypefaceSpan(val typeface: Typeface, val familyName: String) :
    android.text.style.MetricAffectingSpan() {
    override fun updateMeasureState(paint: android.text.TextPaint) { paint.typeface = typeface }
    override fun updateDrawState(paint: android.text.TextPaint) { paint.typeface = typeface }
}
